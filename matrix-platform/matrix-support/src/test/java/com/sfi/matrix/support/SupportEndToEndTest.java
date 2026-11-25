package com.sfi.matrix.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfi.conflux.auth.config.AuthConfigProvider;
import com.sfi.conflux.file.config.FileConfigProvider;
import com.sfi.matrix.iam.config.IamAutoConfiguration;
import com.sfi.matrix.support.config.DbAuthConfigProvider;
import com.sfi.matrix.support.config.DbFileConfigProvider;
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
 * @author lianghang
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
    @Autowired AuthConfigProvider authConfigProvider;
    @Autowired FileConfigProvider fileConfigProvider;
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
        JsonNode saved = ok(postJson("/api/dict-type/save", type, adminToken));
        assertNotNull(saved.path("id"));
        assertEquals("系统正常/停用", saved.path("dictName").asText());
        assertTrue(dictTypeRepository.count() >= 1);

        // 基类 get 端点：GET /get?id=
        MvcResult got = mockMvc.perform(get("/api/dict-type/get")
                        .param("id", saved.path("id").asText())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        assertEquals("系统正常/停用", ok(got).path("dictName").asText());

        // 幂等：同 type 再保存一次应覆盖，不新增
        long before = dictTypeRepository.count();
        ok(postJson("/api/dict-type/save", type, adminToken));
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
        // 基类 save 端点 /api/dict/save（路径 /api/dict 保持，底层换 sys_dict_data）
        JsonNode saved = ok(postJson("/api/dict/save", data, adminToken));
        assertTrue(saved.path("id").asLong() > 0);
        assertEquals("正常", saved.path("dictLabel").asText());
        assertTrue(dictDataRepository.count() >= 1);

        // 基类分页端点 GET /list（返回 PageResult）
        MvcResult page = mockMvc.perform(get("/api/dict/list")
                        .param("pageNum", "1").param("pageSize", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode pageData = ok(page);
        assertTrue(pageData.has("list") && pageData.path("list").isArray());
        assertTrue(pageData.path("total").asLong() >= 1);

        // 按类型取选项（带平台兜底）——避让端点 /options
        MvcResult list = mockMvc.perform(get("/api/dict/options")
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
        JsonNode saved = ok(postJson("/api/config/create", cfg, adminToken));
        assertNotNull(saved.path("id"));
        assertEquals("INT", saved.path("valueType").asText());
        assertEquals("100", saved.path("configValue").asText(), "非敏感值对外明文");

        // 平台租户取值命中（验证缓存路径）——避让端点 /value
        MvcResult got = mockMvc.perform(get("/api/config/value")
                        .param("configKey", "global.timeout")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        assertEquals("100", ok(got).asText());
        assertTrue(configRepository.count() >= 1);
    }

    @Test
    @Order(5)
    void configListEndpoint() throws Exception {
        MvcResult page = mockMvc.perform(get("/api/config/list")
                        .param("pageNum", "1").param("pageSize", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode data = ok(page);
        assertTrue(data.has("list") && data.path("list").isArray());
        assertTrue(data.path("total").asLong() >= 1);
    }

    @Test
    @Order(6)
    void dictTypeFallbackListEndpoint() throws Exception {
        MvcResult list = mockMvc.perform(get("/api/dict-type/options")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = ok(list);
        assertTrue(items.isArray() && items.size() >= 1);
    }

    @Test
    @Order(7)
    void configSensitiveProtection() throws Exception {
        final String plainApiKey = "wechat-api-v3-key-plaintext-123456";
        // 微信支付一组多 key：appId（非敏感）+ apiKey（敏感）
        Map<String, Object> appId = new HashMap<>();
        appId.put("configGroup", "wechat.pay");
        appId.put("configKey", "appId");
        appId.put("configValue", "wx1234567890abcdef");
        ok(postJson("/api/config/create", appId, adminToken));

        Map<String, Object> apiKey = new HashMap<>();
        apiKey.put("configGroup", "wechat.pay");
        apiKey.put("configKey", "apiKey");
        apiKey.put("configValue", plainApiKey);
        apiKey.put("sensitive", true);
        JsonNode saved = ok(postJson("/api/config/create", apiKey, adminToken));
        assertTrue(saved.path("sensitive").asBoolean());
        // create 出口即加密：configValue 是密文对象，不是明文
        JsonNode cipherOut = saved.path("configValue");
        assertTrue(cipherOut.path("encrypted").asBoolean(), "敏感值出口应为密文对象");
        assertTrue(cipherOut.has("cipher") && cipherOut.path("cipher").asText().length() > 0);
        assertNotEquals(plainApiKey, cipherOut.path("cipher").asText(), "出口不应为明文");
        String id = saved.path("id").asText();
        String tid = saved.path("tenantId").asText();

        // 1) DB 存明文（不再加密落库）
        String dbRaw = configRepository
                .findByTenantIdAndConfigGroupAndConfigKeyAndDeletedFalse(tid, "wechat.pay", "apiKey")
                .orElseThrow().getConfigValue();
        assertEquals(plainApiKey, dbRaw, "DB 应存明文，内部可直接读取");

        // 2) GET /get 详情出口为密文对象
        MvcResult detail = mockMvc.perform(get("/api/config/get").param("id", id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        assertTrue(ok(detail).path("configValue").path("encrypted").asBoolean());

        // 3) GET /list 列表敏感值为密文对象
        MvcResult page = mockMvc.perform(get("/api/config/list")
                        .param("pageNum", "1").param("pageSize", "50")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        boolean foundSecretCipher = false;
        for (JsonNode item : ok(page).path("list")) {
            if ("apiKey".equals(item.path("configKey").asText())) {
                foundSecretCipher = item.path("configValue").path("encrypted").asBoolean();
            }
        }
        assertTrue(foundSecretCipher, "列表敏感值应为密文对象");

        // 4) /value 与 /values 出口同样加密
        MvcResult got = mockMvc.perform(get("/api/config/value")
                        .param("group", "wechat.pay").param("configKey", "apiKey")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        assertTrue(ok(got).path("encrypted").asBoolean(), "取值端点敏感值应为密文对象");
        MvcResult group = mockMvc.perform(get("/api/config/values")
                        .param("group", "wechat.pay")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        for (JsonNode item : ok(group)) {
            if ("apiKey".equals(item.path("configKey").asText())) {
                assertTrue(item.path("configValue").path("encrypted").asBoolean());
            }
        }

        // 5) GET /secret 返回明文（独立权限点 + 审计）
        MvcResult secret = mockMvc.perform(get("/api/config/secret").param("id", id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn();
        assertEquals(plainApiKey, ok(secret).asText(), "/secret 应返回解密明文");

        // 6) /update 传敏感值应被拒绝，且不改 sensitive
        Map<String, Object> badUpdate = new HashMap<>();
        badUpdate.put("id", Long.valueOf(id));
        badUpdate.put("configValue", "hacked-value");
        assertEquals(400, postJson("/api/config/update", badUpdate, adminToken)
                .getResponse().getStatus(), "/update 不得改敏感值");

        // 7) POST /secret 改值（明文提交），DB 落明文，出口仍密文
        Map<String, Object> secretReq = new HashMap<>();
        secretReq.put("id", Long.valueOf(id));
        secretReq.put("configValue", "wechat-api-v3-key-updated-654321");
        JsonNode changed = ok(postJson("/api/config/secret", secretReq, adminToken));
        assertTrue(changed.path("configValue").path("encrypted").asBoolean());
        assertEquals("wechat-api-v3-key-updated-654321", configRepository
                .findByTenantIdAndConfigGroupAndConfigKeyAndDeletedFalse(tid, "wechat.pay", "apiKey")
                .orElseThrow().getConfigValue(), "/secret 改值应明文落库");

        // 8) /save 已停用
        Map<String, Object> saveReq = new HashMap<>();
        saveReq.put("configKey", "whatever");
        saveReq.put("configValue", "x");
        assertEquals(400, postJson("/api/config/save", saveReq, adminToken)
                .getResponse().getStatus(), "/save 应停用");
    }

    /** conflux 配置源必须由 matrix 的 Db provider 覆盖 starter 默认 yml 实现。 */
    @Test
    @Order(8)
    void confluxProvidersOverrideStarterDefaults() {
        assertInstanceOf(DbAuthConfigProvider.class, authConfigProvider,
                "登录配置源必须是 Db 版，不能是 starter 默认 yml 版");
        assertInstanceOf(DbFileConfigProvider.class, fileConfigProvider,
                "文件配置源必须是 Db 版，不能是 starter 默认 yml 版");
    }
}
