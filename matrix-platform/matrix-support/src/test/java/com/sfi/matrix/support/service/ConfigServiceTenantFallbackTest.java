package com.sfi.matrix.support.service;

import com.sfi.matrix.iam.service.IamService;
import com.sfi.matrix.support.entity.SysConfig;
import com.sfi.matrix.support.repository.SysConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ConfigService 租户兜底：按传入 tenantId 查、缺则回落平台。
 * ConfigService 不判断登录态——传什么 tenantId 就查什么，查不到照常回落平台。
 *
 * @author matrix
 */
class ConfigServiceTenantFallbackTest {

    private SysConfigRepository repository;
    private IamService iamService;
    private ConfigService service;

    private static final String PLATFORM = "PLATFORM";

    @BeforeEach
    void setUp() {
        repository = mock(SysConfigRepository.class);
        iamService = mock(IamService.class);
        service = new ConfigService(repository, iamService);
        when(iamService.getPlatformTenantId()).thenReturn(PLATFORM);
    }

    private static SysConfig config(String tenantId, String value) {
        SysConfig c = new SysConfig();
        c.setTenantId(tenantId);
        c.setConfigGroup("conflux.auth.web");
        c.setConfigKey("wechat_open");
        c.setConfigValue(value);
        return c;
    }

    @Test
    void get_currentTenantPresent_usesTenantValue() {
        when(repository.findByTenantIdAndConfigGroupAndConfigKeyAndDeletedFalse(
                eq("t1"), eq("conflux.auth.web"), eq("wechat_open")))
                .thenReturn(Optional.of(config("t1", "tenant-value")));

        assertEquals("tenant-value", service.get("conflux.auth.web", "wechat_open", "t1"));
    }

    @Test
    void get_currentTenantMissing_fallsBackToPlatform() {
        when(repository.findByTenantIdAndConfigGroupAndConfigKeyAndDeletedFalse(
                eq("t1"), anyString(), eq("wechat_open")))
                .thenReturn(Optional.empty());
        when(repository.findByTenantIdAndConfigGroupAndConfigKeyAndDeletedFalse(
                eq(PLATFORM), anyString(), eq("wechat_open")))
                .thenReturn(Optional.of(config(PLATFORM, "platform-value")));

        assertEquals("platform-value", service.get("conflux.auth.web", "wechat_open", "t1"));
    }

    @Test
    void get_nullTenant_queriesNullThenFallsBackToPlatform() {
        // Service 不判断登录态：传 null 就按 null 查，查不到照常回落平台。
        // 无登录态由调用方（provider）自己决定传哪个 tenantId。
        when(repository.findByTenantIdAndConfigGroupAndConfigKeyAndDeletedFalse(
                isNull(), eq("conflux.auth.web"), eq("wechat_open")))
                .thenReturn(Optional.empty());
        when(repository.findByTenantIdAndConfigGroupAndConfigKeyAndDeletedFalse(
                eq(PLATFORM), eq("conflux.auth.web"), eq("wechat_open")))
                .thenReturn(Optional.of(config(PLATFORM, "platform-value")));

        assertEquals("platform-value", service.get("conflux.auth.web", "wechat_open", null));
        verify(repository).findByTenantIdAndConfigGroupAndConfigKeyAndDeletedFalse(
                isNull(), eq("conflux.auth.web"), eq("wechat_open"));
        verify(repository).findByTenantIdAndConfigGroupAndConfigKeyAndDeletedFalse(
                eq(PLATFORM), eq("conflux.auth.web"), eq("wechat_open"));
    }

    @Test
    void get_platformTenantOnly_noFallbackLoop() {
        when(repository.findByTenantIdAndConfigGroupAndConfigKeyAndDeletedFalse(
                eq(PLATFORM), eq("conflux.auth.web"), eq("wechat_open")))
                .thenReturn(Optional.empty());

        assertNull(service.get("conflux.auth.web", "wechat_open", PLATFORM));
    }
}
