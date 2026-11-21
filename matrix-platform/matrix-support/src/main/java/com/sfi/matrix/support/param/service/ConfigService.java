package com.sfi.matrix.support.param.service;

import com.sfi.matrix.support.param.entity.SysConfig;
import com.sfi.matrix.support.param.repository.SysConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统参数服务。
 *
 * @author matrix
 */
@Service
public class ConfigService {

    private final SysConfigRepository configRepository;

    public ConfigService(SysConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public SysConfig set(String configKey, String configValue, String remark, String tenantId) {
        SysConfig config = configRepository.findByConfigKeyAndDeletedFalse(configKey).orElse(new SysConfig());
        config.setConfigKey(configKey);
        config.setConfigValue(configValue);
        config.setRemark(remark);
        config.setTenantId(tenantId);
        return configRepository.save(config);
    }

    public String get(String configKey) {
        return configRepository.findByConfigKeyAndDeletedFalse(configKey)
                .map(SysConfig::getConfigValue).orElse(null);
    }
}
