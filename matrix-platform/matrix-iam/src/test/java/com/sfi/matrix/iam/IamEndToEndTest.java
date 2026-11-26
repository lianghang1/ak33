package com.sfi.matrix.iam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfi.matrix.iam.audit.repository.SysLoginLogRepository;
import com.sfi.matrix.iam.audit.repository.SysOperateLogRepository;
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
        String body = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(body);
        assertEquals(0, root.path("code").asInt(), "响应应成功: " + body);
        return root.path("data");
    }

    private MvcResult postJson(String url, Object body, String token) throws Exception {
        MockHttpServletRequestBuilder builder = post(url).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
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
        com.sfi.matrix.iam.audit.entity.SysLoginLog log = loginLogRepository.findAll().stream()
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
        com.sfi.matrix.iam.audit.entity.SysLoginLog failed = loginLogRepository.findAll().stream()
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
        // 建套餐
        Map<String, String> pkg = new HashMap<>();
        pkg.put("packageCode", "PKG_TEST");
        pkg.put("packageName", "测试套餐");
        ok(postJson("/api/iam/package", pkg, adminToken));

        // 建租户（带管理员）
        Map<String, Object> tenant = new HashMap<>();
        tenant.put("tenantCode", "T1");
        tenant.put("tenantName", "租户一");
        tenant.put("packageCode", "PKG_TEST");
        tenant.put("adminUsername", "t1admin");
        tenant.put("adminPassword", "admin123");
        tenant.put("adminName", "租户一管理员");
        JsonNode created = ok(postJson("/api/iam/tenant", tenant, adminToken));
        assertNotNull(created);
        String tenantId = created.path("id").asText();

        // 建用户
        Map<String, String> user = new HashMap<>();
        user.put("username", "zhangsan");
        user.put("password", "admin123");
        user.put("nickname", "张三");
        JsonNode u = ok(postJson("/api/iam/user", user, adminToken));
        assertNotNull(u.path("id"));

        // 建角色
        Map<String, String> role = new HashMap<>();
        role.put("roleCode", "TEST_ROLE");
        role.put("roleName", "测试角色");
        role.put("tenantId", tenantId);
        role.put("dataScope", "ALL");
        JsonNode r = ok(postJson("/api/iam/role", role, adminToken));
        assertNotNull(r.path("id"));

        // 建部门
        Map<String, Object> dept = new HashMap<>();
        dept.put("deptName", "研发部");
        dept.put("tenantId", tenantId);
        dept.put("sortNo", 1);
        JsonNode d = ok(postJson("/api/iam/dept", dept, adminToken));
        assertNotNull(d.path("id"));
    }

    @Test
    @Order(4)
    void queryBeanListWorks() throws Exception {
        // QueryBean 通路②：用户列表分页（EQ status=1）
        // 不带条件：验证分页通路
        MvcResult result = mockMvc.perform(get("/api/iam/users")
                        .param("page", "0").param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode page = ok(result);
        assertTrue(page.has("content"), "应返回分页 content");
        assertTrue(page.path("totalElements").asLong() >= 2,
                "至少有平台管理员和 zhangsan，实际响应=" + page.toString());

        // 带字符串条件：验证 QueryBean 前缀协议（CN_ 模糊匹配）
        MvcResult filtered = mockMvc.perform(get("/api/iam/users")
                        .param("CN_username", "zhang")
                        .param("page", "0").param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode filteredPage = ok(filtered);
        assertEquals(1, filteredPage.path("totalElements").asLong(),
                "CN_username=zhang 应只匹配 zhangsan，实际=" + filteredPage.toString());
    }

    @Test
    @Order(5)
    void operateLogAndAuditQuery() throws Exception {
        // 操作日志已由 @OperateLog 落库
        assertTrue(operateLogRepository.count() >= 1, "操作日志应落库");
        MvcResult result = mockMvc.perform(get("/api/audit/operate-logs")
                        .param("page", "0").param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode page = ok(result);
        assertTrue(page.has("content"));
    }

    @Test
    @Order(6)
    void logout() throws Exception {
        ok(postJson("/api/auth/logout", new HashMap<>(), adminToken));
        assertNull(tokenService.resolve(adminToken), "登出后 token 应失效");
    }
}
