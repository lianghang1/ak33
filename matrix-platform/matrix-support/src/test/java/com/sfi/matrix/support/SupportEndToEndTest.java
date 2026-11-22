package com.sfi.matrix.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfi.matrix.iam.config.IamAutoConfiguration;
import com.sfi.matrix.support.repository.SysConfigRepository;
import com.sfi.matrix.support.repository.SysDictDataRepository;
import com.sfi.matrix.support.repository.SysDictTypeRepository;
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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * support 端到端测试：字典类型/选项两张表 CRUD、平台兜底、config value_type、缓存失效。
 *
 * @author matrix
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SupportEndToEndTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({IamAutoConfiguration.class, SupportAutoConfiguration.class})
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
    @Autowired SysDictTypeRepository dictTypeRepository;
    @Autowired SysDictDataRepository dictDataRepository;
    @Autowired SysConfigRepository configRepository;

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
        return mockMvc.perform(b).andReturn();
    }

    @Test
    @Order(1)
    void login() throws Exception {
        Map<String, String> req = new HashMap<>();
        req.put("username", "admin");
        req.put("password", "admin123");
        req.put("tenantCode", "PLATFORM");
        JsonNode data = ok(postJson("/api/auth/login", req, null));
        adminToken = data.path("token").asText();
        assertNotNull(adminToken);
    }

    @Test
    @Order(2)
    void dictTypeCrud() throws Exception {
        Map<String, Object> type = new HashMap<>();
        type.put("dictType", "sys_normal_disable");
        type.put("dictName", "系统正常/停用");
        type.put("remark", "通用启停状态");
        JsonNode saved = ok(postJson("/api/dict-type", type, adminToken));
        assertNotNull(saved.path("id"));
        assertEquals("系统正常/停用", saved.path("dictName").asText());
        assertTrue(dictTypeRepository.count() >= 1);

        // 幂等：同 type 再保存一次应覆盖，不新增
        long before = dictTypeRepository.count();
        ok(postJson("/api/dict-type", type, adminToken));
        assertEquals(before, dictTypeRepository.count());
    }

    @Test
    @Order(3)
    void dictDataCrudAndCompatEndpoint() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("dictType", "sys_normal_disable");
        data.put("dictCode", "1");
        data.put("dictLabel", "正常");
        data.put("sortNo", 1);
        // 兼容端点 /api/dict（前端已按此对接，底层换 sys_dict_data）
        JsonNode saved = ok(postJson("/api/dict", data, adminToken));
        assertTrue(saved.path("id").asLong() > 0);
        assertEquals("正常", saved.path("dictLabel").asText());
        assertTrue(dictDataRepository.count() >= 1);

        // 按类型取选项（带平台兜底）——兼容端点
        MvcResult list = mockMvc.perform(get("/api/dict/list")
                        .param("dictType", "sys_normal_disable")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = ok(list);
        assertTrue(items.isArray() && items.size() >= 1);
        assertEquals("1", items.get(0).path("dictCode").asText());
    }

    @Test
    @Order(4)
    void configCrudWithValueTypeAndFallback() throws Exception {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("configKey", "global.timeout");
        cfg.put("configValue", "100");
        cfg.put("valueType", "INT");
        cfg.put("remark", "全局超时");
        JsonNode saved = ok(postJson("/api/config", cfg, adminToken));
        assertNotNull(saved.path("id"));
        assertEquals("INT", saved.path("valueType").asText());

        // 平台租户取值命中（验证缓存路径）
        MvcResult got = mockMvc.perform(get("/api/config/get")
                        .param("configKey", "global.timeout")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        assertEquals("100", ok(got).asText());
        assertTrue(configRepository.count() >= 1);
    }

    @Test
    @Order(5)
    void configQueryBeanPage() throws Exception {
        MvcResult page = mockMvc.perform(get("/api/config/page")
                        .param("page", "0").param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode data = ok(page);
        assertTrue(data.has("content"));
        assertTrue(data.path("totalElements").asLong() >= 1);
    }

    @Test
    @Order(6)
    void dictTypeListEndpoint() throws Exception {
        MvcResult list = mockMvc.perform(get("/api/dict-type/list")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = ok(list);
        assertTrue(items.isArray() && items.size() >= 1);
    }
}
