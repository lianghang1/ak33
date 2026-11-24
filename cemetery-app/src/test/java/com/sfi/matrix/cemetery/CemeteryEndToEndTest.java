package com.sfi.matrix.cemetery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfi.matrix.cemetery.entity.CemeteryArea;
import com.sfi.matrix.cemetery.entity.GridCell;
import com.sfi.matrix.cemetery.entity.Tomb;
import com.sfi.matrix.cemetery.repository.CemeteryAreaRepository;
import com.sfi.matrix.cemetery.repository.GridCellRepository;
import com.sfi.matrix.cemetery.repository.TombRepository;
import com.sfi.matrix.cemetery.service.TombService;
import com.sfi.matrix.iam.config.IamAutoConfiguration;
import com.sfi.matrix.iam.service.MenuUrlPermissionProvider;
import com.sfi.matrix.support.SupportAutoConfiguration;
import com.sfi.scaffold.starter.security.permission.UrlPermissionRule;
import org.junit.jupiter.api.*;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 公墓 P0 端到端测试：墓区多级树、格位幂等、墓位初始化编号快照、穴位数与售价。
 *
 * @author matrix
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CemeteryEndToEndTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({IamAutoConfiguration.class, SupportAutoConfiguration.class, CemeteryAutoConfiguration.class})
    static class TestApp {
    }

    private static Process redis;

    @BeforeAll
    static void startRedis() throws Exception {
        redis = new ProcessBuilder("redis-server", "--port", "6399", "--save", "", "--appendonly", "no").start();
        for (int i = 0; i < 30; i++) {
            try {
                Process p = new ProcessBuilder("redis-cli", "-p", "6399", "ping").start();
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = r.readLine();
                p.waitFor();
                if ("PONG".equals(line)) return;
            } catch (Exception ignored) {
                Thread.sleep(100);
            }
        }
        fail("Redis 启动超时");
    }

    @AfterAll
    static void stopRedis() {
        if (redis != null) redis.destroy();
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CemeteryAreaRepository areaRepository;
    @Autowired GridCellRepository gridCellRepository;
    @Autowired TombRepository tombRepository;
    @Autowired MenuUrlPermissionProvider menuUrlPermissionProvider;

    private static String adminToken;

    private JsonNode ok(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertEquals(0, root.path("code").asInt(), root.toString());
        return root.path("data");
    }

    private MvcResult postJson(String url, Object body, String token) throws Exception {
        MockHttpServletRequestBuilder b = post(url).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
        if (token != null) b.header("Authorization", "Bearer " + token);
        return mockMvc.perform(b).andExpect(status().isOk()).andReturn();
    }

    private MvcResult getJson(String url, String token) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
    }

    /** 用于预期失败的场景：不断言 HTTP 状态（业务异常可能是 400）。 */
    private MvcResult postJsonRaw(String url, Object body, String token) throws Exception {
        MockHttpServletRequestBuilder b = post(url).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
        if (token != null) b.header("Authorization", "Bearer " + token);
        return mockMvc.perform(b).andReturn();
    }

    @Test
    @Order(1)
    void login() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", "admin");
        body.put("password", "admin123");
        body.put("tenantCode", "PLATFORM");
        JsonNode data = ok(postJson("/api/auth/login", body, null));
        adminToken = data.path("token").asText();
        assertNotNull(adminToken);
    }

    @Test
    @Order(2)
    void areaTreeMultiLevel() throws Exception {
        // 根：玫瑰园
        JsonNode root = ok(postJson("/api/cemetery/area/save",
                areaDto(null, "RY", "玫瑰园", null), adminToken));
        long rootId = root.path("id").asLong();
        assertEquals(1, root.path("level").asInt());
        assertEquals("玫瑰园", root.path("fullPath").asText());

        // 子：东区
        JsonNode child = ok(postJson("/api/cemetery/area/save",
                areaDto(null, "DQU", "东区", rootId), adminToken));
        long childId = child.path("id").asLong();
        assertEquals(2, child.path("level").asInt());
        assertEquals("玫瑰园/东区", child.path("fullPath").asText());

        // 孙：二区（验证多级 level=3、fullPath 三段）
        JsonNode grand = ok(postJson("/api/cemetery/area/save",
                areaDto(null, "EQU", "二区", childId), adminToken));
        long grandId = grand.path("id").asLong();
        assertEquals(3, grand.path("level").asInt());
        assertEquals("玫瑰园/东区/二区", grand.path("fullPath").asText());

        // 树：含三级
        JsonNode tree = ok(getJson("/api/cemetery/area/tree", adminToken));
        assertTrue(tree.isArray() && tree.size() >= 3);

        // 移动：把二区移到根下 → level 变 2、fullPath 变 玫瑰园/二区，子节点联动
        Map<String, Object> move = new HashMap<>();
        move.put("id", grandId);
        move.put("newParentId", rootId);
        JsonNode moved = ok(postJson("/api/cemetery/area/move", move, adminToken));
        assertEquals(2, moved.path("level").asInt());
        assertEquals("玫瑰园/二区", moved.path("fullPath").asText());
    }

    @Test
    @Order(3)
    void gridGenerateIdempotent() throws Exception {
        long leafAreaId = findLeafAreaId();
        Map<String, Object> dto = new HashMap<>();
        dto.put("areaId", leafAreaId);
        dto.put("rows", 2);
        dto.put("cols", 3);
        JsonNode first = ok(postJson("/api/cemetery/grid/generate", dto, adminToken));
        assertEquals(6, first.asInt());
        JsonNode second = ok(postJson("/api/cemetery/grid/generate", dto, adminToken));
        assertEquals(0, second.asInt(), "重复铺格位应幂等，不再新建");

        JsonNode cells = ok(getJson("/api/cemetery/grid/list?areaId=" + leafAreaId, adminToken));
        assertTrue(cells.isArray() && cells.size() >= 6);
    }

    @Test
    @Order(4)
    void tombInitWithCodeAndPrice() throws Exception {
        long leafAreaId = findLeafAreaId();
        List<GridCell> uninit = gridCellRepository.findByAreaIdAndInitializedFalseAndDeletedFalse(leafAreaId);
        assertFalse(uninit.isEmpty());
        List<Long> cellIds = new ArrayList<>();
        cellIds.add(uninit.get(0).getId());
        cellIds.add(uninit.get(1).getId());

        Map<String, Object> dto = new HashMap<>();
        dto.put("cellIds", cellIds);
        dto.put("holes", 6); // 穴位数 6，验证非 1/2/3 枚举
        dto.put("salePrice", new BigDecimal("88888.00"));
        JsonNode count = ok(postJson("/api/cemetery/tomb/init", dto, adminToken));
        assertEquals(2, count.asInt());

        List<Tomb> tombs = tombRepository.findByAreaIdAndDeletedFalse(leafAreaId);
        assertEquals(2, tombs.size());
        Tomb t = tombs.get(0);
        assertEquals(TombService.STATUS_INITIALIZED, t.getStatus());
        assertEquals(Integer.valueOf(6), t.getHoles());
        assertEquals(0, new BigDecimal("88888.00").compareTo(t.getSalePrice()));
        assertTrue(t.getCode().matches("[A-Z0-9]+-\\d{2}-\\d{2}"), "编号格式应为 区码-排-位：" + t.getCode());
        assertNotNull(t.getBasePath());
        assertNotNull(t.getFullPath());
        assertNotNull(t.getCodeStrategy());

        // 格位回写 tombId + initialized
        GridCell cell = gridCellRepository.findById(cellIds.get(0)).orElseThrow(AssertionError::new);
        assertTrue(cell.getInitialized());
        assertNotNull(cell.getTombId());

        // 幂等：同格位再初始化应跳过
        JsonNode again = ok(postJson("/api/cemetery/tomb/init", dto, adminToken));
        assertEquals(0, again.asInt(), "已初始化格位应跳过");
    }

    @Test
    @Order(5)
    void rootOfUsesGardenName() throws Exception {
        // 编号的园名取根节点：建一条三级链再初始化，验证 fullPath 以根名开头
        JsonNode root = ok(postJson("/api/cemetery/area/save",
                areaDto(null, "LY", "林园", null), adminToken));
        long rootId = root.path("id").asLong();
        JsonNode zone = ok(postJson("/api/cemetery/area/save",
                areaDto(null, "BQU", "B区", rootId), adminToken));
        long zoneId = zone.path("id").asLong();

        Map<String, Object> grid = new HashMap<>();
        grid.put("areaId", zoneId);
        grid.put("rows", 1);
        grid.put("cols", 1);
        ok(postJson("/api/cemetery/grid/generate", grid, adminToken));
        List<GridCell> cells = gridCellRepository.findByAreaIdAndInitializedFalseAndDeletedFalse(zoneId);
        Map<String, Object> init = new HashMap<>();
        init.put("cellIds", Collections.singletonList(cells.get(0).getId()));
        init.put("holes", 2);
        init.put("salePrice", new BigDecimal("1000"));
        ok(postJson("/api/cemetery/tomb/init", init, adminToken));

        Tomb t = tombRepository.findByAreaIdAndDeletedFalse(zoneId).get(0);
        assertTrue(t.getBasePath().startsWith("林园/"), "基础路径应以根园名开头：" + t.getBasePath());
        assertTrue(t.getFullPath().startsWith("林园/"), "全路径应以根园名开头：" + t.getFullPath());
        assertEquals("BQU-01-01", t.getCode(), "区码应取叶子墓区编码");
    }

    @Test
    @Order(6)
    void gridCellUpdateAndDeleteRules() throws Exception {
        long leafAreaId = findLeafAreaId();
        List<GridCell> all = gridCellRepository.findByAreaIdAndDeletedFalse(leafAreaId);
        GridCell free = all.stream().filter(c -> !Boolean.TRUE.equals(c.getInitialized())).findFirst().orElseThrow();
        GridCell used = all.stream().filter(c -> Boolean.TRUE.equals(c.getInitialized())).findFirst().orElseThrow();

        // 空格位：改坐标成功
        Map<String, Object> upd = new HashMap<>();
        upd.put("id", free.getId());
        upd.put("rowNo", free.getRowNo());
        upd.put("colNo", 99); // 改成未被占用的列
        JsonNode updated = ok(postJson("/api/cemetery/grid/save", upd, adminToken));
        assertEquals(99, updated.path("colNo").asInt());

        // 空格位：删除成功（逻辑删除后按主键查不到）
        Map<String, Object> del = new HashMap<>();
        del.put("id", free.getId());
        ok(postJson("/api/cemetery/grid/delete", del, adminToken));
        assertTrue(gridCellRepository.findById(free.getId()).isEmpty());

        // 已初始化格位：改坐标被拒
        Map<String, Object> badUpd = new HashMap<>();
        badUpd.put("id", used.getId());
        badUpd.put("rowNo", 1);
        badUpd.put("colNo", 1);
        JsonNode updRej = okRaw(postJsonRaw("/api/cemetery/grid/save", badUpd, adminToken));
        assertNotEquals(0, updRej.path("code").asInt(), "已初始化格位改坐标应被拒");

        // 已初始化格位：删除被拒
        Map<String, Object> badDel = new HashMap<>();
        badDel.put("id", used.getId());
        JsonNode delRej = okRaw(postJsonRaw("/api/cemetery/grid/delete", badDel, adminToken));
        assertNotEquals(0, delRej.path("code").asInt(), "已初始化格位删除应被拒");
    }

    @Test
    @Order(7)
    void tombUpdateAndDeleteRollbackCell() throws Exception {
        long leafAreaId = findLeafAreaId();
        Tomb tomb = tombRepository.findByAreaIdAndDeletedFalse(leafAreaId).stream()
                .filter(t -> TombService.STATUS_INITIALIZED.equals(t.getStatus())).findFirst().orElseThrow();
        long tombId = tomb.getId();
        long cellId = tomb.getGridCellId();
        String codeBefore = tomb.getCode();

        // 改：穴位/售价可变，编号快照不变
        Map<String, Object> upd = new HashMap<>();
        upd.put("id", tombId);
        upd.put("holes", 8);
        upd.put("salePrice", new BigDecimal("99999.00"));
        JsonNode updated = ok(postJson("/api/cemetery/tomb/save", upd, adminToken));
        assertEquals(8, updated.path("holes").asInt());
        assertEquals(codeBefore, updated.path("code").asText(), "编号快照不应被修改");
        assertEquals("INITIALIZED", updated.path("status").asText(), "状态不应被修改");

        // 删：回退格位为空闲
        Map<String, Object> del = new HashMap<>();
        del.put("id", tombId);
        ok(postJson("/api/cemetery/tomb/delete", del, adminToken));
        assertTrue(tombRepository.findById(tombId).isEmpty());
        GridCell cell = gridCellRepository.findById(cellId).orElseThrow();
        assertFalse(cell.getInitialized(), "删墓位后格位应回退为未初始化");
        assertNull(cell.getTombId(), "删墓位后格位 tombId 应清空");

        // 非 INITIALIZED 态删除被拒：直接改状态后尝试删除
        Tomb another = tombRepository.findByAreaIdAndDeletedFalse(leafAreaId).stream()
                .filter(t -> !t.getId().equals(tombId)).findFirst().orElseThrow();
        another.setStatus("SOLD");
        tombRepository.save(another);
        Map<String, Object> del2 = new HashMap<>();
        del2.put("id", another.getId());
        JsonNode delRej = okRaw(postJsonRaw("/api/cemetery/tomb/delete", del2, adminToken));
        assertNotEquals(0, delRej.path("code").asInt(), "非初始化态墓位删除应被拒");
    }

    @Test
    @Order(8)
    void cemeteryUrlPermissionRulesSeeded() {
        List<UrlPermissionRule> rules = menuUrlPermissionProvider.loadRules();
        // 读规则：GET:/api/cemetery/** → cemetery:read
        assertTrue(rules.stream().anyMatch(r -> "GET".equals(r.getMethod())
                        && "/api/cemetery/**".equals(r.getPattern())
                        && r.getPermissions().contains(CemeteryConstants.PERM_CEMETERY_READ)),
                "应播种公墓读权限规则");
        // 写规则：POST:/api/cemetery/** → cemetery:write
        assertTrue(rules.stream().anyMatch(r -> "POST".equals(r.getMethod())
                        && "/api/cemetery/**".equals(r.getPattern())
                        && r.getPermissions().contains(CemeteryConstants.PERM_CEMETERY_WRITE)),
                "应播种公墓写权限规则");
    }

    /** 解析响应但不断言业务码（用于预期失败的场景）。 */
    private JsonNode okRaw(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private long findLeafAreaId() {
        return areaRepository.findByAreaNameAndDeletedFalse("二区").orElseThrow(AssertionError::new).getId();
    }

    private Map<String, Object> areaDto(Long id, String code, String name, Long parentId) {
        Map<String, Object> m = new HashMap<>();
        if (id != null) m.put("id", id);
        m.put("areaCode", code);
        m.put("areaName", name);
        if (parentId != null) m.put("parentId", parentId);
        return m;
    }
}
