package com.sfi.matrix.support.config;

import com.sfi.conflux.auth.config.AuthConfig;
import com.sfi.conflux.auth.config.AuthConfigProvider;
import com.sfi.conflux.auth.exception.AuthErrorCode;
import com.sfi.conflux.common.util.JsonUtils;
import com.sfi.matrix.support.service.ConfigService;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.security.identity.LoginUser;
import org.springframework.stereotype.Component;

/**
 * 登录渠道配置源：从 config 中心读取，覆盖 conflux-auth starter 的 yml 默认实现。
 *
 * <p>配置坐标：group = {@code conflux.auth.{configId}}，key = {@code channelCode}，
 * value = AuthConfig 的 JSON（明文落库，敏感字段对外出口由 ConfigController 加密）。
 * JSON 解析统一走 conflux 框架共享的 {@link JsonUtils}。</p>
 *
 * <p>租户：由本 provider 根据登录态决定传当前登录租户或 null；
 * ConfigService 只按传入的 tenantId 查，查不到回落平台，不判断登录态。</p>
 *
 * <p>用 {@code @Component} 注册，starter 的 {@code @ConditionalOnMissingBean(AuthConfigProvider)}
 * 自动让位，无需改 conflux 源码。</p>
 *
 * @author matrix
 */
@Component
public class DbAuthConfigProvider implements AuthConfigProvider {

    private static final String GROUP_PREFIX = "conflux.auth.";

    private final ConfigService configService;

    public DbAuthConfigProvider(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public AuthConfig getConfig(String configId, String channelCode) {
        String json = configService.get(GROUP_PREFIX + configId, channelCode, currentTenantId());
        if (json == null) {
            throw AuthErrorCode.CONFIG_MISSING.of(
                    "登录配置缺失: configId=" + configId + ", channelCode=" + channelCode);
        }
        try {
            AuthConfig config = JsonUtils.fromJson(json, AuthConfig.class);
            // 公共字段以调用方入参为准，不信任 JSON 内可能不一致的值
            return config.setConfigId(configId).setChannelCode(channelCode);
        } catch (Exception e) {
            throw AuthErrorCode.CONFIG_MISSING.of(
                    "登录配置解析失败: configId=" + configId + ", channelCode=" + channelCode);
        }
    }

    /** 当前登录租户；无登录态返回 null（由 ConfigService 回落平台）。 */
    private static String currentTenantId() {
        LoginUser user = LoginContext.currentOrNull();
        return user == null ? null : user.getTenantId();
    }
}
