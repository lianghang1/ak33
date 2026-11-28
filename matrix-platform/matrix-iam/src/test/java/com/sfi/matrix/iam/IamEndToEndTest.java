package com.sfi.matrix.iam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfi.matrix.iam.repository.SysLoginLogRepository;
import com.sfi.matrix.iam.repository.SysOperateLogRepository;
import com.sfi.matrix.iam.config.IamAutoConfiguration;
import com.sfi.scaffold.starter.security.token.TokenService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 平台层端到端测试：H2 + Liquibase + 真实 Redis 会话。
 *
 * <p>覆盖完整链路：平台初始化 → 列租户 → 登录 → 建租户/用户/角色/部门 →
 * QueryBean 列表 → 审计日志 → 登出。使用 MockMvc 走真实 HTTP 层。</p>
 *
 * @author lianghang
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IamEndToEndTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(IamAutoConfiguration.class)
    static class TestApp {
    }

    private static Process redis;

    @BeforeAll
    static void startRedis() throws Exception {
        redis = new ProcessBuilder("redis-server", "--port", "6399", "--save", "", "--appendonly", "no")
                .start();
        // 等 redis 就绪
        for (int i = 0; i < 30; i++) {
            try {
                Process p = new ProcessBuilder("redis-cli", "-p", "6399", "ping").start();
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                if ("PONG".equals(r.readLine())) {
                    p.waitFor();
                    return;
                }
                p.waitFor();
            } catch (Exception ignored) {
                Thread.sleep(100);
            }
        }
        fail("Redis 启动超时");
    }

    @AfterAll
    static void stopRedis() {
        if (redis != null) {
            redis.destroy();
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TokenService tokenService;
    @Autowired SysLoginLogRepository loginLogRepository;
    @Autowired SysOperateLogRepository operateLogRepository;

    private static String adminToken;

    private JsonNode ok(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(body);
        assertEquals(0, root.path("code").asInt(), "响应应成功: " + body);
        return root.path("data");
    }

    private MvcResult postJson(String url, Object body, String token) throws Exception {
        MockHttpServletRequestBuilder builder = post(url).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return mockMvc.perform(builder).andReturn();
    }

    @Test
    @Order(1)
    void platformInitAndListTenants() throws Exception {
        // 平台超管列租户：统一 login 接口不传 tenantCode
        Map<String, String> req = new HashMap<>();
        req.put("authType", "PASSWORD");
        req.put("username", "admin");
        req.put("password", "admin123");
        JsonNode result = ok(postJson("/api/auth/login", req, null));
        assertTrue(result.path("needSelectTenant").asBoolean());
        JsonNode tenants = result.path("tenants");
        assertTrue(tenants.isArray());
        assertTrue(tenants.size() >= 1, "至少有平台租户");
        boolean hasPlatform = false;
        for (JsonNode t : tenants) {
            if ("PLATFORM".equals(t.path("tenantCode").asText())) hasPlatform = true;
        }
        assertTrue(hasPlatform, "应包含 PLATFORM 租户");
    }

    @Test
    @Order(2)
    void loginAsPlatformAdmin() throws Exception {
        Map<String, String> req = new HashMap<>();
        req.put("authType", "PASSWORD");
        req.put("username", "admin");
        req.put("password", "admin123");
        req.put("tenantCode", "PLATFORM");
        JsonNode data = ok(postJson("/api/auth/login", req, null));
        adminToken = data.path("token").asText();
        assertNotNull(adminToken);
        assertFalse(adminToken.isEmpty());
        assertFalse(data.path("tenantId").asText().isEmpty(), "tenantId 应为平台租户 ID");
        assertEquals("OPERATOR", data.path("accountType").asText());
        // 登录日志落库，且关键审计字段齐全
        assertTrue(loginLogRepository.count() >= 1, "登录日志应落库");
        com.sfi.matrix.iam.entity.SysLoginLog log = loginLogRepository.findAll().stream()
                .filter(l -> Boolean.TRUE.equals(l.getSuccess()))
                .findFirst().orElse(null);
        assertNotNull(log, "成功登录日志应落库");
        assertEquals(Boolean.TRUE, log.getSuccess(), "登录成功标志");
        assertEquals("SUCCESS", log.getReasonCode(), "成功原因码");
        assertNotNull(log.getUserId(), "成功登录应记录账号ID");
        assertNotNull(log.getTraceId(), "应记录 traceId");
        assertNotNull(log.getLoginTime(), "应记录登录时间");
        assertNotNull(log.getCostMs(), "应记录耗时");
        assertNotNull(log.getParams(), "应记录入参摘要");
        assertFalse(log.getParams().contains("admin123"), "入参摘要不得包含密码");
        assertEquals("internal", log.getSource(), "账密登录来源应为 internal");
        assertNotNull(log.getDeviceId(), "服务端应生成物理设备标识");
    }

    @Test
    @Order(20)
    void failedLoginRecordsReasonCode() throws Exception {
        Map<String, String> req = new HashMap<>();
        req.put("authType", "PASSWORD");
        req.put("username", "admin");
        req.put("password", "wrong-password");
        req.put("tenantCode", "PLATFORM");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
        com.sfi.matrix.iam.entity.SysLoginLog failed = loginLogRepository.findAll().stream()
                .filter(l -> Boolean.FALSE.equals(l.getSuccess()))
                .findFirst().orElse(null);
        assertNotNull(failed, "失败登录应落库");
        assertEquals(Boolean.FALSE, failed.getSuccess());
        assertNotNull(failed.getReasonCode(), "失败应记录原因码");
        assertFalse("SUCCESS".equals(failed.getReasonCode()), "失败原因码不应为 SUCCESS");
        assertFalse(failed.getParams().contains("wrong-password"), "入参摘要不得包含密码");
    }

    @Test
    @Order(21)
    void sameDeviceReLoginRevokesOldSession() throws Exception {
        Map<String, String> req = new HashMap<>();
        req.put("authType", "PASSWORD");
        req.put("username", "admin");
        req.put("password", "admin123");
        req.put("tenantCode", "PLATFORM");
        String body = objectMapper.writeValueAsString(req);

        // 同一设备连续登录两次
        MvcResult first = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body)
                .header("X-Device-Id", "test-device-same")).andReturn();
        String token1 = ok(first).path("token").asText();
        MvcResult second = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body)
                .header("X-Device-Id", "test-device-same")).andReturn();
        String token2 = ok(second).path("token").asText();
        assertFalse(token1.equals(token2), "两次登录应签发不同 token");

        // 旧 token 已被顶掉，解析失效
        assertNull(tokenService.resolve(token1), "同设备重登后旧会话应失效");
        assertNotNull(tokenService.resolve(token2), "新会话应有效");
        // 响应头回写设备标识
        assertEquals("test-device-same", first.getResponse().getHeader("X-Device-Id"));
    }

    @Test
    @Order(3)
    void createTenantUserRoleDept() throws Exception {
        // 平台租户 ID：adminToken 的当前登录租户（plan 查询按当前租户隔离）
        JsonNode platformIdNode = ok(mockMvc.perform(get("/api/iam/tenant/platform-tenant-id")
                .header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk()).andReturn());
        String platformTenantId = platformIdNode.asText();

        // 建套餐
        Map<String, String> pkg = new HashMap<>();
        pkg.put("packageCode", "PKG_TEST");
        pkg.put("packageName", "测试套餐");
        ok(postJson("/api/iam/package/save", pkg, adminToken));

        // 建租户（带管理员）
        Map<String, Object> tenant = new HashMap<>();
        tenant.put("tenantCode", "T1");
        tenant.put("tenantName", "租户一");
        tenant.put("packageCode", "PKG_TEST");
        tenant.put("adminUsername", "t1admin");
        tenant.put("adminPassword", "admin123");
        tenant.put("adminName", "租户一管理员");
        JsonNode created = ok(postJson("/api/iam/tenant/save", tenant, adminToken));
        assertNotNull(created);
        String tenantId = created.path("id").asText();

        // 建用户（归属平台租户，plan 查询才能按当前租户找到）
        Map<String, Object> user = new HashMap<>();
        user.put("username", "zhangsan");
        user.put("password", "admin123");
        user.put("nickname", "张三");
        user.put("tenantId", platformTenantId);
        JsonNode u = ok(postJson("/api/iam/user/save", user, adminToken));
        assertNotNull(u.path("id"));

        // 建角色
        Map<String, String> role = new HashMap<>();
        role.put("roleCode", "TEST_ROLE");
        role.put("roleName", "测试角色");
        role.put("tenantId", tenantId);
        role.put("dataScope", "ALL");
        JsonNode r = ok(postJson("/api/iam/role/save", role, adminToken));
        assertNotNull(r.path("id"));

        // 建部门
        Map<String, Object> dept = new HashMap<>();
        dept.put("deptName", "研发部");
        dept.put("tenantId", tenantId);
        dept.put("sortNo", 1);
        JsonNode d = ok(postJson("/api/iam/dept/save", dept, adminToken));
        assertNotNull(d.path("id"));

        // ===== 芋道式聚合创建（在平台租户内，plan 才能按当前租户查到）=====
        // 平台租户内建部门
        Map<String, Object> pdept = new HashMap<>();
        pdept.put("deptName", "平台研发部");
        pdept.put("tenantId", platformTenantId);
        pdept.put("sortNo", 1);
        String pdeptId = ok(postJson("/api/iam/dept/save", pdept, adminToken)).path("id").asText();
        // 平台租户内建角色
        Map<String, Object> prole = new HashMap<>();
        prole.put("roleCode", "PLAT_TEST_ROLE");
        prole.put("roleName", "平台测试角色");
        prole.put("tenantId", platformTenantId);
        prole.put("dataScope", "ALL");
        String proleId = ok(postJson("/api/iam/role/save", prole, adminToken)).path("id").asText();

        Map<String, Object> fullUser = new HashMap<>();
        fullUser.put("username", "lisi");
        fullUser.put("password", "admin123");
        fullUser.put("nickname", "李四");
        fullUser.put("mobile", "13800000000");
        fullUser.put("email", "lisi@test.com");
        fullUser.put("sex", 1);
        fullUser.put("tenantId", platformTenantId);
        fullUser.put("deptId", pdeptId);
        fullUser.put("roleIds", java.util.Collections.singletonList(proleId));
        JsonNode lu = ok(postJson("/api/iam/user/save", fullUser, adminToken));
        assertNotNull(lu.path("id"), "聚合创建应返回用户");
        String lisiId = lu.path("id").asText();

        // 详情回显：手机号/邮箱/部门/角色都应正确落库（验证新增不再丢字段）
        MvcResult detailResult = mockMvc.perform(get("/api/iam/user/detail")
                        .param("id", lisiId).param("tenantId", platformTenantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode detail = ok(detailResult);
        assertEquals("李四", detail.path("nickname").asText(), "昵称应落库");
        assertEquals("13800000000", detail.path("mobile").asText(), "手机号应落库(修复丢弃bug)");
        assertEquals("lisi@test.com", detail.path("email").asText(), "邮箱应落库(修复丢弃bug)");
        assertEquals(pdeptId, detail.path("deptId").asText(), "归属部门应落库");
        assertTrue(detail.path("roleIds").isArray() && detail.path("roleIds").size() == 1,
                "角色应落库，实际=" + detail.toString());

        // 部门树接口：应包含刚建的平台研发部
        MvcResult treeResult = mockMvc.perform(get("/api/iam/dept/tree")
                        .param("tenantId", platformTenantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode tree = ok(treeResult);
        assertTrue(tree.isArray() && tree.size() >= 1, "部门树应非空，实际=" + tree.toString());

        // plan 列表：lisi 应带部门名和角色名（验证关联列查询）
        MvcResult planLisi = mockMvc.perform(get("/api/iam/user/list/UserListPlan")
                        .param("CN_username", "lisi")
                        .param("page", "1").param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode lisiPage = ok(planLisi);
        assertEquals(1, lisiPage.path("total").asLong(), "plan 应查到 lisi，实际=" + lisiPage.toString());
        JsonNode lisiRow = lisiPage.path("list").get(0);
        assertEquals("平台研发部", lisiRow.path("deptName").asText(), "plan 应返回部门名，实际=" + lisiPage.toString());
        assertEquals("平台测试角色", lisiRow.path("roleNames").asText(), "plan 应返回角色名，实际=" + lisiPage.toString());
    }

    @Test
    @Order(4)
    void queryBeanListWorks() throws Exception {
        // QueryBean 通路②：用户列表分页（EQ status=1）
        // 不带条件：验证分页通路
        MvcResult result = mockMvc.perform(get("/api/iam/user/list")
                        .param("page", "1").param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode page = ok(result);
        assertTrue(page.has("list"), "应返回统一分页 list（PageResult）");
        assertTrue(page.path("total").asLong() >= 2,
                "至少有平台管理员和 zhangsan，实际响应=" + page.toString());
        assertEquals(1, page.path("pageNum").asLong(), "页码应统一为 1 基");

        // 带字符串条件：验证 QueryBean 前缀协议（CN_ 模糊匹配）
        MvcResult filtered = mockMvc.perform(get("/api/iam/user/list")
                        .param("CN_username", "zhang")
                        .param("page", "1").param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode filteredPage = ok(filtered);
        assertEquals(1, filteredPage.path("total").asLong(),
                "CN_username=zhang 应只匹配 zhangsan，实际=" + filteredPage.toString());

        // QueryPlan 通路③：/list/UserListPlan 关联 sys_user_profile 查出昵称
        MvcResult planResult = mockMvc.perform(get("/api/iam/user/list/UserListPlan")
                        .param("CN_username", "zhang")
                        .param("page", "1").param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode planPage = ok(planResult);
        assertEquals(1, planPage.path("total").asLong(),
                "plan 查询 CN_username=zhang 应只匹配 zhangsan，实际=" + planPage.toString());
        JsonNode planRow = planPage.path("list").get(0);
        assertEquals("张三", planRow.path("nickname").asText(),
                "plan 应关联资料表查出昵称，实际=" + planPage.toString());

        // plan 昵称模糊搜索：字段已在计划类声明，严格字段校验应放行
        MvcResult nickFiltered = mockMvc.perform(get("/api/iam/user/list/UserListPlan")
                        .param("CN_nickname", "张")
                        .param("page", "1").param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode nickPage = ok(nickFiltered);
        assertTrue(nickPage.path("total").asLong() >= 1,
                "CN_nickname=张 应命中 zhangsan，实际=" + nickPage.toString());

        // ===== 角色 RoleListPlan：关键表 join + 聚合列 =====
        MvcResult rolePlan = mockMvc.perform(get("/api/iam/role/list/RoleListPlan")
                        .param("CN_roleCode", "PLAT_TEST_ROLE")
                        .param("page", "1").param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode rolePlanPage = ok(rolePlan);
        assertEquals(1, rolePlanPage.path("total").asLong(),
                "plan 应查到 PLAT_TEST_ROLE，实际=" + rolePlanPage.toString());
        JsonNode roleRow = rolePlanPage.path("list").get(0);
        assertEquals("平台测试角色", roleRow.path("roleName").asText(),
                "plan 应返回角色名，实际=" + rolePlanPage.toString());
        assertEquals("运营平台", roleRow.path("tenantName").asText(),
                "plan 应关联租户名，实际=" + rolePlanPage.toString());
        assertTrue(roleRow.has("userCount") && roleRow.has("menuCount"),
                "plan 应返回用户数/菜单数聚合列，实际=" + rolePlanPage.toString());
        assertEquals(1, roleRow.path("status").asInt(),
                "角色默认应为启用状态，实际=" + rolePlanPage.toString());

        // 角色 plan 按状态过滤（EQ_status）
        MvcResult roleStatusFiltered = mockMvc.perform(get("/api/iam/role/list/RoleListPlan")
                        .param("EQ_status", "1")
                        .param("page", "1").param("size", "50")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode roleStatusPage = ok(roleStatusFiltered);
        assertTrue(roleStatusPage.path("total").asLong() >= 1,
                "EQ_status=1 应命中启用角色，实际=" + roleStatusPage.toString());

        // change-status：停用再启用 PLAT_TEST_ROLE
        String rolePlanId = roleRow.path("id").asText();
        ok(postJson("/api/iam/role/change-status",
                new HashMap<String, Object>() {{
                    put("ids", java.util.Collections.singletonList(rolePlanId));
                    put("status", 0);
                }}, adminToken));
        MvcResult roleDisabled = mockMvc.perform(get("/api/iam/role/list/RoleListPlan")
                        .param("EQ_id", rolePlanId)
                        .param("page", "1").param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        assertEquals(0, ok(roleDisabled).path("list").get(0).path("status").asInt(),
                "change-status 应停用角色，实际=" + ok(roleDisabled).toString());

        // ===== 菜单全字段落库：createFull 应接收 path/component/icon/apiPattern =====
        java.util.Map<String, Object> menu = new java.util.HashMap<>();
        menu.put("menuName", "测试全字段菜单");
        menu.put("menuType", "C");
        menu.put("path", "/system/fullfield");
        menu.put("component", "/system/fullfield/index.vue");
        menu.put("icon", "Settings");
        menu.put("permission", "system:fullfield:list");
        menu.put("apiPattern", "/api/iam/fullfield/**");
        menu.put("menuScope", "TENANT");
        menu.put("sortNo", 3);
        menu.put("visible", true);
        JsonNode createdMenu = ok(postJson("/api/iam/menu/save", menu, adminToken));
        String menuId = createdMenu.path("id").asText();
        assertEquals("/system/fullfield", createdMenu.path("path").asText(),
                "新增菜单应落库路由路径，实际=" + createdMenu.toString());
        assertEquals("/api/iam/fullfield/**", createdMenu.path("apiPattern").asText(),
                "新增菜单应落库 URL 权限规则，实际=" + createdMenu.toString());

        // 回显：get 接口应返回 apiPattern
        MvcResult menuGet = mockMvc.perform(get("/api/iam/menu/get")
                        .param("id", menuId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode menuDetail = ok(menuGet);
        assertEquals("/system/fullfield/index.vue", menuDetail.path("component").asText(),
                "get 应回显组件路径，实际=" + menuDetail.toString());
        assertEquals("/api/iam/fullfield/**", menuDetail.path("apiPattern").asText(),
                "get 应回显 URL 权限规则，实际=" + menuDetail.toString());
    }

    @Test
    @Order(5)
    void operateLogAndAuditQuery() throws Exception {
        // 操作日志已由 @OperateLog 落库
        assertTrue(operateLogRepository.count() >= 1, "操作日志应落库");
        MvcResult result = mockMvc.perform(get("/api/audit/operate-logs")
                        .param("page", "1").param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode page = ok(result);
        assertTrue(page.has("list"));
    }

    @Test
    @Order(6)
    void logout() throws Exception {
        ok(postJson("/api/auth/logout", new HashMap<>(), adminToken));
        assertNull(tokenService.resolve(adminToken), "登出后 token 应失效");
    }
}
