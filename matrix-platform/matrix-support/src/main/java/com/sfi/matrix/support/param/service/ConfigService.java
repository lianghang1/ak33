package com.sfi.matrix.support.param.service;

import com.sfi.matrix.iam.service.IamService;
import com.sfi.matrix.support.param.entity.SysConfig;
import com.sfi.matrix.support.param.repository.SysConfigRepository;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.security.identity.LoginUser;
import com.sfi.scaffold.starter.tenant.annotation.TenantIgnore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统参数服务：CRUD + 平台默认兜底。
 *
 * <p>取值顺序：当前租户配置 → 平台租户（PLATFORM）配置 → null。
 * 写入按 (tenant_id, config_key) 定位，支持租户覆盖平台默认。</p>
 *
 * @author matrix
 */
@Service
public class ConfigService {

    private final SysConfigRepository configRepository;
    private final IamService iamService;

    public ConfigService(SysConfigRepository configRepository, IamService iamService) {
        this.configRepository = configRepository;
        this.iamService = iamService;
    }

    @Transactional(rollbackFor = Exception.class)
    public SysConfig save(Long id, String configKey, String configValue, String remark, String tenantId) {
        SysConfig config;
        if (id != null) {
            config = get(id);
        } else {
            // 未显式指定租户时，归属当前登录租户（平台超管即在 PLATFORM 租户）
            String tid = tenantId;
            if (tid == null || tid.isEmpty()) {
                LoginUser user = LoginContext.currentOrNull();
                tid = user == null ? null : user.getTenantId();
            }
            final String finalTenantId = tid;
            config = configRepository
                    .findByTenantIdAndConfigKeyAndDeletedFalse(tid, configKey)
                    .orElseGet(SysConfig::new);
            if (config.getId() == null) {
                config.setConfigKey(configKey);
                config.setTenantId(finalTenantId);
            }
        }
        config.setConfigValue(configValue);
        config.setRemark(remark);
        return configRepository.save(config);
    }

    public SysConfig get(Long id) {
        return configRepository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getDeleted()))
                .orElseThrow(() -> BizException.of(GlobalErrorCode.NOT_FOUND, "参数不存在：" + id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysConfig c = get(id);
        c.setDeleted(Boolean.TRUE);
        configRepository.save(c);
    }

    /** 取值：当前租户 → 平台兜底。 */
    @TenantIgnore
    public String get(String configKey, String currentTenantId) {
        SysConfig config = findInternal(configKey, currentTenantId);
        if (config == null && currentTenantId != null) {
            String platformTenantId = iamService.getPlatformTenantId();
            if (!platformTenantId.equals(currentTenantId)) {
                config = findInternal(configKey, platformTenantId);
            }
        }
        return config == null ? null : config.getConfigValue();
    }

    @TenantIgnore
    private SysConfig findInternal(String configKey, String tenantId) {
        return configRepository.findByTenantIdAndConfigKeyAndDeletedFalse(tenantId, configKey).orElse(null);
    }

    /** 兼容旧签名：无显式租户时按值直查（有租户过滤器时即当前租户） */
    public String get(String configKey) {
        return configRepository.findByConfigKeyAndDeletedFalse(configKey)
                .map(SysConfig::getConfigValue).orElse(null);
    }
}
