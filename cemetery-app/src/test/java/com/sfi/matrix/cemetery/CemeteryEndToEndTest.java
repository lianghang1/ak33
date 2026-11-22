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
import com.sfi.matrix.support.SupportAutoConfiguration;
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
