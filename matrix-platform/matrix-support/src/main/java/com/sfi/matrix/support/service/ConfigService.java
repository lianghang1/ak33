package com.sfi.matrix.support.service;

import com.sfi.matrix.iam.service.IamService;
import com.sfi.matrix.support.constant.SupportConstants;
import com.sfi.matrix.support.entity.SysConfig;
import com.sfi.matrix.support.repository.SysConfigRepository;
import com.sfi.scaffold.common.crypto.DataCipherHolder;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.security.identity.LoginUser;
import com.sfi.scaffold.starter.tenant.annotation.TenantIgnore;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统参数服务：CRUD + 平台默认兜底 + 分组 + 敏感值选择性加密 + 缓存。
 *
 * <p>取值顺序：当前租户配置 → 平台租户（PLATFORM）配置 → null。
 * 写入按 (tenant_id, config_group, config_key) 定位，支持租户覆盖平台默认。</p>
 *
 * <p><b>加密约定</b>：Repository 中 config_value 始终是 DB 原始值——sensitive=true 时为密文，
 * 否则明文。Service 对外一律返回<b>解密后的明文副本</b>（不修改被 JPA 管理的实体，避免
 * 脏检查把明文写回库）。缓存保存的是明文副本，写操作失效缓存。密钥由 starter-crypto
 * 从环境变量装配，缺失时启动即失败，不允许静默明文落库。</p>
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
    @CacheEvict(value = {SupportConstants.CACHE_CONFIG, SupportConstants.CACHE_CONFIG_GROUP}, allEntries = true)
    public SysConfig save(Long id, String configGroup, String configKey, String configValue,
                          String valueType, Boolean sensitive, String remark, String tenantId) {
        String group = defaultGroup(configGroup);
        SysConfig config;
        if (id != null) {
            config = getRaw(id);
        } else {
            String tid = tenantId != null && !tenantId.isEmpty() ? tenantId : DictTypeService.currentTenantId();
            config = configRepository
                    .findByTenantIdAndConfigGroupAndConfigKeyAndDeletedFalse(tid, group, configKey)
                    .orElseGet(SysConfig::new);
            if (config.getId() == null) {
                config.setConfigGroup(group);
                config.setConfigKey(configKey);
                config.setTenantId(tid);
            }
        }
        boolean isSensitive = Boolean.TRUE.equals(sensitive);
        // API 入参 configValue 一律为明文：敏感项加密后落库
        config.setConfigValue(isSensitive ? DataCipherHolder.get().encrypt(configValue) : configValue);
        config.setSensitive(isSensitive);
        if (valueType != null) config.setValueType(valueType);
        if (remark != null) config.setRemark(remark);
        configRepository.save(config);
        return plainCopy(config);
    }

    /** 详情：返回明文副本。 */
    public SysConfig get(Long id) {
        return plainCopy(getRaw(id));
    }

    private SysConfig getRaw(Long id) {
        return configRepository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getDeleted()))
                .orElseThrow(() -> BizException.of(GlobalErrorCode.NOT_FOUND, "参数不存在：" + id));
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {SupportConstants.CACHE_CONFIG, SupportConstants.CACHE_CONFIG_GROUP}, allEntries = true)
    public void delete(Long id) {
        SysConfig c = getRaw(id);
        c.setDeleted(Boolean.TRUE);
        configRepository.save(c);
    }

    /**
     * 取值（默认分组）：当前租户 → 平台兜底，返回明文。
     *
     * <p>委托给分组版本；同类自调用不经代理，故本方法自身带缓存，
     * 分组版本的缓存仅在外部直接调用时生效。</p>
     */
    @Cacheable(value = SupportConstants.CACHE_CONFIG, key = "#currentTenantId + '|DEFAULT|' + #configKey")
    @TenantIgnore
    public String get(String configKey, String currentTenantId) {
        return get(SupportConstants.CONFIG_GROUP_DEFAULT, configKey, currentTenantId);
    }

    /**
     * 分组取值：当前租户 → 平台兜底，敏感值解密返回明文。
     *
     * <p>注意：缓存键含 group；本方法返回的明文会被缓存，故解密在方法内完成。</p>
     */
    @Cacheable(value = SupportConstants.CACHE_CONFIG, key = "#currentTenantId + '|' + #group + '|' + #configKey")
    @TenantIgnore
    public String get(String group, String configKey, String currentTenantId) {
        String g = defaultGroup(group);
        String tid = currentTenantId;
        SysConfig config = findInternal(g, configKey, tid);
        if (config == null && tid != null) {
            String platformTenantId = iamService.getPlatformTenantId();
            if (!platformTenantId.equals(tid)) {
                config = findInternal(g, configKey, platformTenantId);
            }
        }
        return config == null ? null : decryptIfSensitive(config);
    }

    /** 整组取值（带平台兜底）：返回该组所有参数的明文副本。 */
    @Cacheable(value = SupportConstants.CACHE_CONFIG_GROUP, key = "#currentTenantId + '|' + #group")
    @TenantIgnore
    public List<SysConfig> listByGroup(String group, String currentTenantId) {
        String g = defaultGroup(group);
        List<SysConfig> tenantList = listInternal(g, currentTenantId);
        List<SysConfig> result = new ArrayList<>();
        String platformTenantId = iamService.getPlatformTenantId();
        if (!platformTenantId.equals(currentTenantId)) {
            for (SysConfig c : listInternal(g, platformTenantId)) {
                result.add(plainCopy(c));
            }
        }
        for (SysConfig c : tenantList) {
            // 租户覆盖同 key：先移除平台那份
            result.removeIf(x -> x.getConfigKey().equals(c.getConfigKey()));
            result.add(plainCopy(c));
        }
        return result;
    }

    @TenantIgnore
    private SysConfig findInternal(String group, String configKey, String tenantId) {
        return configRepository.findByTenantIdAndConfigGroupAndConfigKeyAndDeletedFalse(tenantId, group, configKey)
                .orElse(null);
    }

    @TenantIgnore
    private List<SysConfig> listInternal(String group, String tenantId) {
        return configRepository.findByTenantIdAndConfigGroupAndDeletedFalseOrderByConfigKeyAsc(tenantId, group);
    }

    /** 敏感值解密（DB 原始值 → 明文）。 */
    private String decryptIfSensitive(SysConfig config) {
        String raw = config.getConfigValue();
        if (raw != null && Boolean.TRUE.equals(config.getSensitive())) {
            return DataCipherHolder.get().decrypt(raw);
        }
        return raw;
    }

    /** 明文副本：不修改被 JPA 管理的实体，避免脏检查把解密后的明文写回库。 */
    private SysConfig plainCopy(SysConfig src) {
        SysConfig copy = new SysConfig();
        copy.setId(src.getId());
        copy.setTenantId(src.getTenantId());
        copy.setConfigGroup(src.getConfigGroup());
        copy.setConfigKey(src.getConfigKey());
        copy.setValueType(src.getValueType());
        copy.setSensitive(src.getSensitive());
        copy.setRemark(src.getRemark());
        copy.setCreateTime(src.getCreateTime());
        copy.setUpdateTime(src.getUpdateTime());
        copy.setCreateBy(src.getCreateBy());
        copy.setUpdateBy(src.getUpdateBy());
        copy.setDeleted(src.getDeleted());
        copy.setConfigValue(decryptIfSensitive(src));
        return copy;
    }

    private static String defaultGroup(String group) {
        return group == null || group.isEmpty() ? SupportConstants.CONFIG_GROUP_DEFAULT : group;
    }
}
