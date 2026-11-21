package com.sfi.matrix.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfi.matrix.iam.config.IamAutoConfiguration;
import com.sfi.matrix.support.dict.repository.SysDictRepository;
import com.sfi.matrix.support.param.repository.SysConfigRepository;
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
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * support 端到端测试：字典/参数 CRUD + 平台默认兜底。
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
    @Autowired SysDictRepository dictRepository;
    @Autowired SysConfigRepository configRepository;

    private static String adminToken;

    private JsonNode ok(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
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
    void dictCrud() throws Exception {
        Map<String, Object> dict = new HashMap<>();
        dict.put("dictType", "sys_normal_disable");
        dict.put("dictCode", "1");
        dict.put("dictLabel", "正常");
        dict.put("sortNo", 1);
        // 平台租户上下文下新增（tenantId 留空，走当前登录租户）
        JsonNode saved = ok(postJson("/api/dict", dict, adminToken));
        assertNotNull(saved.path("id"));
        assertTrue(dictRepository.count() >= 1);

        // 按类型查询（带平台兜底）
        MvcResult list = mockMvc.perform(get("/api/dict/list")
                        .param("dictType", "sys_normal_disable")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = ok(list);
        assertTrue(items.isArray() && items.size() >= 1);
    }

    @Test
    @Order(3)
    void configCrudWithPlatformFallback() throws Exception {
        // 平台租户设置全局参数
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("configKey", "global.timeout");
        cfg.put("configValue", "100");
        cfg.put("remark", "全局超时");
        JsonNode saved = ok(postJson("/api/config", cfg, adminToken));
        assertNotNull(saved.path("id"));

        // 当前在平台租户，取值应命中
        MvcResult got = mockMvc.perform(get("/api/config/get")
                        .param("configKey", "global.timeout")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        assertEquals("100", ok(got).asText());
        assertTrue(configRepository.count() >= 1);
    }

    @Test
    @Order(4)
    void configQueryBeanPage() throws Exception {
        MvcResult page = mockMvc.perform(get("/api/config/page")
                        .param("page", "0").param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode data = ok(page);
        assertTrue(data.has("content"));
        assertEquals(1, data.path("totalElements").asLong());
    }
}
