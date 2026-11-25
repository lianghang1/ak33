package com.sfi.matrix.support.config;

import com.sfi.conflux.auth.config.AuthConfig;
import com.sfi.conflux.common.exception.GatewayException;
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
 * DbAuthConfigProvider：从 config 中心按 group/key 取 AuthConfig JSON，
 * 无登录态时 tenantId 传 null（由 ConfigService 回落平台），公共字段以入参为准，
 * 配置缺失抛 CONFIG_MISSING。
 *
 * @author matrix
 */
class DbAuthConfigProviderTest {

    private ConfigService configService;
    private DbAuthConfigProvider provider;

    @BeforeEach
    void setUp() {
        configService = mock(ConfigService.class);
        provider = new DbAuthConfigProvider(configService);
    }

    @Test
    void getConfig_readsByGroupAndKey_withNullTenantAndDeserializes() {
        String json = "{\"configId\":\"stale\",\"channelCode\":\"stale\","
                + "\"clientId\":\"wx-app\",\"clientSecret\":\"sec\","
                + "\"redirectUri\":\"https://cb\",\"scope\":\"snsapi_login\","
                + "\"attr\":{\"corpId\":\"corp-1\"}}";
        when(configService.get(eq("conflux.auth.web"), eq("wechat_open"), isNull()))
                .thenReturn(json);

        AuthConfig cfg = provider.getConfig("web", "wechat_open");

        assertEquals("web", cfg.getConfigId(), "configId 应以入参为准");
        assertEquals("wechat_open", cfg.getChannelCode(), "channelCode 应以入参为准");
        assertEquals("wx-app", cfg.getClientId());
        assertEquals("sec", cfg.getClientSecret());
        assertEquals("https://cb", cfg.getRedirectUri());
        assertEquals("snsapi_login", cfg.getScope());
        assertEquals("corp-1", cfg.getAttr().get("corpId"));
        verify(configService).get(eq("conflux.auth.web"), eq("wechat_open"), isNull());
    }

    @Test
    void getConfig_missing_throwsConfigMissing() {
        when(configService.get(eq("conflux.auth.web"), eq("wechat_open"), isNull()))
                .thenReturn(null);
        assertThrows(GatewayException.class, () -> provider.getConfig("web", "wechat_open"));
    }

    @Test
    void getConfig_badJson_throwsConfigMissing() {
        when(configService.get(eq("conflux.auth.web"), eq("wechat_open"), isNull()))
                .thenReturn("{not-json");
        assertThrows(GatewayException.class, () -> provider.getConfig("web", "wechat_open"));
    }
}
