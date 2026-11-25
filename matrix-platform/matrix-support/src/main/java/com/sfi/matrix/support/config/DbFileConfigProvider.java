package com.sfi.matrix.support.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sfi.conflux.common.exception.GatewayException;
import com.sfi.conflux.common.util.JsonUtils;
import com.sfi.conflux.file.config.FileChannelConfig;
import com.sfi.conflux.file.config.FileConfigProvider;
import com.sfi.conflux.file.exception.FileErrorCode;
import com.sfi.matrix.support.service.ConfigService;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import org.dromara.x.file.storage.core.FileStorageProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

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
 * @author lianghang
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
        String json = configService.get(GROUP_PREFIX + configId, channelCode, LoginContext.currentTenantIdOrNull());
        if (json == null) {
            throw new GatewayException(FileErrorCode.CONFIG_MISSING,
                    "文件配置缺失: configId={}, channelCode={}", configId, channelCode);
        }

        try {
            Map<String, String> attrConfig = JsonUtils.fromJson(json, new TypeReference<Map<String, String>>() {});
            return new FileChannelConfig(configId, channelCode, attrConfig);
        } catch (Exception e) {
            throw new GatewayException(FileErrorCode.CONFIG_MISSING,
                    "文件配置解析失败: configId=" + configId + ", channelCode=" + channelCode, e);
        }
    }

}
