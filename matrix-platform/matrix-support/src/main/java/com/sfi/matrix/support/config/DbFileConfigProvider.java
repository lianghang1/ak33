package com.sfi.matrix.support.config;

import com.sfi.conflux.common.exception.GatewayException;
import com.sfi.conflux.common.util.JsonUtils;
import com.sfi.conflux.file.config.FileChannelConfig;
import com.sfi.conflux.file.config.FileConfigProvider;
import com.sfi.conflux.file.exception.FileErrorCode;
import com.sfi.matrix.support.service.ConfigService;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.security.identity.LoginUser;
import org.dromara.x.file.storage.core.FileStorageProperties;
import org.springframework.stereotype.Component;

/**
 * 文件渠道配置源：从 config 中心读取，覆盖 conflux-file starter 的 yml 默认实现。
 *
 * <p>配置坐标：group = {@code conflux.file.{configId}}，key = {@code channelCode}，
 * value = x-file-storage {@link FileStorageProperties} 的 JSON（明文落库，
 * 敏感字段对外出口由 ConfigController 加密）。
 * JSON 解析统一走 conflux 框架共享的 {@link JsonUtils}。</p>
 *
 * <p>租户：由本 provider 根据登录态决定传当前登录租户或 null；
 * ConfigService 只按传入的 tenantId 查，查不到回落平台，不判断登录态。</p>
 *
 * <p>用 {@code @Component} 注册，starter 的 {@code @ConditionalOnMissingBean(FileConfigProvider)}
 * 自动让位，无需改 conflux 源码。</p>
 *
 * @author matrix
 */
@Component
public class DbFileConfigProvider implements FileConfigProvider {

    private static final String GROUP_PREFIX = "conflux.file.";

    private final ConfigService configService;

    public DbFileConfigProvider(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public FileChannelConfig getConfig(String configId, String channelCode) {
        String json = configService.get(GROUP_PREFIX + configId, channelCode, currentTenantId());
        if (json == null) {
            throw new GatewayException(FileErrorCode.CONFIG_MISSING,
                    "文件配置缺失: configId={}, channelCode={}", configId, channelCode);
        }
        try {
            FileStorageProperties props = JsonUtils.fromJson(json, FileStorageProperties.class);
            return new FileChannelConfig(configId, channelCode, props);
        } catch (Exception e) {
            throw new GatewayException(FileErrorCode.CONFIG_MISSING,
                    "文件配置解析失败: configId={}, channelCode={}", configId, channelCode, e);
        }
    }

    /** 当前登录租户；无登录态返回 null（由 ConfigService 回落平台）。 */
    private static String currentTenantId() {
        LoginUser user = LoginContext.currentOrNull();
        return user == null ? null : user.getTenantId();
    }
}
