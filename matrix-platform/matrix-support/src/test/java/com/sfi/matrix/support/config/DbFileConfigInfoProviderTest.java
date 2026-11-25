package com.sfi.matrix.support.config;

import com.sfi.conflux.common.exception.GatewayException;
import com.sfi.conflux.file.config.FileChannelConfig;
import com.sfi.matrix.support.service.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DbFileConfigProvider：从 config 中心读取扁平文件渠道配置，
 * 统一走 conflux 框架共享的 JsonUtils 反序列化为 attr，
 * 公共维度与平台个性凭证正确落位，配置缺失/坏 JSON 抛 CONFIG_MISSING。
 *
 * @author lianghang
 */
class DbFileConfigInfoProviderTest {

    private ConfigService configService;
    private DbFileConfigProvider provider;

    @BeforeEach
    void setUp() {
        configService = mock(ConfigService.class);
        provider = new DbFileConfigProvider(configService);
    }

    @Test
    void getConfig_bindsFlatAttr() {
        String json = "{\"defaultPlatform\":\"aliyun-oss-1\","
                + "\"accessKey\":\"AK\",\"secretKey\":\"SK\","
                + "\"endPoint\":\"https://oss.example.com\","
                + "\"bucketName\":\"bucket-1\",\"domain\":\"https://cdn.example.com/\","
                + "\"basePath\":\"root/\",\"multipartThreshold\":\"134217728\"}";
        when(configService.get(eq("conflux.file.default"), eq("aliyun-oss-1"), isNull()))
                .thenReturn(json);

        FileChannelConfig cfg = provider.getConfig("default", "aliyun-oss-1");

        assertEquals("default", cfg.getConfigId());
        assertEquals("aliyun-oss-1", cfg.getChannelCode());
        assertEquals("AK", cfg.getAttr().get("accessKey"));
        assertEquals("SK", cfg.getAttr().get("secretKey"));
        assertEquals("bucket-1", cfg.getAttr().get("bucketName"));
        assertEquals("root/", cfg.getAttr().get("basePath"));
        assertEquals("134217728", cfg.getAttr().get("multipartThreshold"));
        verify(configService).get(eq("conflux.file.default"), eq("aliyun-oss-1"), isNull());
    }

    @Test
    void getConfig_missing_throwsConfigMissing() {
        when(configService.get(eq("conflux.file.default"), eq("aliyun-oss-1"), isNull()))
                .thenReturn(null);
        assertThrows(GatewayException.class, () -> provider.getConfig("default", "aliyun-oss-1"));
    }

    @Test
    void getConfig_badJson_throwsConfigMissing() {
        when(configService.get(eq("conflux.file.default"), eq("aliyun-oss-1"), isNull()))
                .thenReturn("{bad");
        assertThrows(GatewayException.class, () -> provider.getConfig("default", "aliyun-oss-1"));
    }
}
