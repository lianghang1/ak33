package com.sfi.matrix.support.config;

import com.sfi.conflux.common.exception.GatewayException;
import com.sfi.conflux.file.config.FileChannelConfig;
import com.sfi.matrix.support.service.ConfigService;
import org.dromara.x.file.storage.core.FileStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DbFileConfigProvider：从 config 中心取 FileStorageProperties JSON，
 * 统一走 conflux 框架共享的 JsonUtils 反序列化，
 * 平台配置字段正确落位，配置缺失/坏 JSON 抛 CONFIG_MISSING。
 *
 * @author matrix
 */
class DbFileConfigProviderTest {

    private ConfigService configService;
    private DbFileConfigProvider provider;

    @BeforeEach
    void setUp() {
        configService = mock(ConfigService.class);
        provider = new DbFileConfigProvider(configService);
    }

    @Test
    void getConfig_bindsConcretePlatformConfig() {
        // 这是一条 conflux.file.default / aliyun-oss-1 的配置，含 RSA 级别长密钥场景的字段
        String json = "{\"defaultPlatform\":\"aliyun-oss-1\","
                + "\"aliyunOss\":[{\"platform\":\"aliyun-oss-1\","
                + "\"accessKey\":\"AK\",\"secretKey\":\"SK\","
                + "\"endPoint\":\"https://oss.example.com\","
                + "\"bucketName\":\"bucket-1\",\"domain\":\"https://cdn.example.com/\","
                + "\"basePath\":\"root/\",\"multipartThreshold\":134217728}]}";
        when(configService.get(eq("conflux.file.default"), eq("aliyun-oss-1"), isNull()))
                .thenReturn(json);

        FileChannelConfig cfg = provider.getConfig("default", "aliyun-oss-1");

        assertEquals("default", cfg.getConfigId());
        assertEquals("aliyun-oss-1", cfg.getChannelCode());
        FileStorageProperties props = cfg.getProperties();
        assertEquals("aliyun-oss-1", props.getDefaultPlatform());
        assertEquals(1, props.getAliyunOss().size());
        FileStorageProperties.AliyunOssConfig oss = assertInstanceOf(
                FileStorageProperties.AliyunOssConfig.class, props.getAliyunOss().get(0),
                "JsonUtils 必须把通配符泛型解析成具体 AliyunOssConfig，而非 LinkedHashMap");
        assertEquals("AK", oss.getAccessKey());
        assertEquals("SK", oss.getSecretKey());
        assertEquals("bucket-1", oss.getBucketName());
        assertEquals(134217728, oss.getMultipartThreshold());
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
