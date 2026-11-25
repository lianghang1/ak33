package com.sfi.matrix.support.service;

import com.sfi.matrix.iam.service.IamService;
import com.sfi.matrix.support.constant.SupportConstants;
import com.sfi.matrix.support.dto.ConfigCreateRequest;
import com.sfi.matrix.support.dto.ConfigSecretRequest;
import com.sfi.matrix.support.dto.ConfigUpdateRequest;
import com.sfi.matrix.support.dto.ConfigVO;
import com.sfi.matrix.support.dto.SecretCipher;
import com.sfi.matrix.support.entity.SysConfig;
import com.sfi.matrix.support.repository.SysConfigRepository;
import com.sfi.scaffold.common.crypto.DataCipherHolder;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.starter.data.jpa.service.BaseService;
import com.sfi.scaffold.starter.tenant.annotation.TenantIgnore;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 系统参数服务：CRUD + 平台默认兜底 + 分组 + 敏感值出口加密 + 缓存。
 *
 * <p><b>敏感值口径</b>：DB 与内部 Service 全程明文——sensitive 项落库即明文，conflux provider
 * 等内部代码直接 {@code get()} 拿明文使用；只有对外 HTTP 出口（{@link ConfigVO}）才把敏感值
 * 加密成 {@link SecretCipher} 密文对象。加密只在出口发生，不污染存储与内部读取。</p>
 *
 * <p><b>写入三段式</b>：{@link #create} 纯新增（业务键重复拒绝）定 sensitive 初值；
 * {@link #update} 改基础信息与非敏感值、绝不碰 sensitive；{@link #updateSecret} 是改敏感值和
 * sensitive 标志的唯一入口（Controller 侧独立权限点 + 审计）。基类 save 已停用。</p>
 *
 * <p>取值顺序：当前租户 → 平台租户（PLATFORM）→ null。</p>
 *
 * @author matrix
 */
@Service
public class ConfigService extends BaseService<SysConfig> {

    private final SysConfigRepository configRepository;
    private final IamService iamService;

    public ConfigService(SysConfigRepository configRepository, IamService iamService) {
        super(configRepository);
        this.configRepository = configRepository;
        this.iamService = iamService;
    }

    // ==================== 写：明文落库 ====================

    /** 纯新增：业务键 (tenantId, group, key) 已存在则冲突拒绝，不做静默覆盖。 */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {SupportConstants.CACHE_CONFIG, SupportConstants.CACHE_CONFIG_GROUP}, allEntries = true)
    @TenantIgnore
    public SysConfig create(ConfigCreateRequest req) {
        String group = defaultGroup(req.getConfigGroup());
        String tid = req.getTenantId() != null && !req.getTenantId().isEmpty()
                ? req.getTenantId() : DictTypeService.currentTenantId();
        boolean exists = configRepository
                .findByTenantIdAndConfigGroupAndConfigKeyAndDeletedFalse(tid, group, req.getConfigKey())
                .isPresent();
        if (exists) {
            throw BizException.of(GlobalErrorCode.CONFLICT, "参数已存在，请改用修改：{}", req.getConfigKey());
        }
        SysConfig config = new SysConfig();
        config.setTenantId(tid);
        config.setConfigGroup(group);
        config.setConfigKey(req.getConfigKey());
        config.setConfigValue(req.getConfigValue());          // 明文落库
        config.setSensitive(Boolean.TRUE.equals(req.getSensitive()));
        config.setValueType(req.getValueType());
        config.setRemark(req.getRemark());
        return configRepository.save(config);
    }

    /**
     * 基础信息修改：只改 remark/valueType 与非敏感值，绝不碰 sensitive。
     * 目标已敏感时传 configValue → 拒绝并引导走 /secret。
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {SupportConstants.CACHE_CONFIG, SupportConstants.CACHE_CONFIG_GROUP}, allEntries = true)
    public SysConfig update(ConfigUpdateRequest req) {
        SysConfig config = require(req.getId());
        if (req.getConfigValue() != null && Boolean.TRUE.equals(config.getSensitive())) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST,
                    "敏感参数的值必须通过 /secret 修改：{}", config.getConfigKey());
        }
        if (req.getConfigValue() != null) {
            config.setConfigValue(req.getConfigValue());      // 明文落库
        }
        if (req.getValueType() != null) {
            config.setValueType(req.getValueType());
        }
        if (req.getRemark() != null) {
            config.setRemark(req.getRemark());
        }
        return configRepository.save(config);
    }

    /** 敏感专用修改：改值和/或 sensitive 标志，明文落库。两个字段都不给则拒绝。 */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {SupportConstants.CACHE_CONFIG, SupportConstants.CACHE_CONFIG_GROUP}, allEntries = true)
    public SysConfig updateSecret(ConfigSecretRequest req) {
        if (req.getConfigValue() == null && req.getSensitive() == null) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "configValue 与 sensitive 至少提供一个");
        }
        SysConfig config = require(req.getId());
        if (req.getConfigValue() != null) {
            config.setConfigValue(req.getConfigValue());      // 明文落库
        }
        if (req.getSensitive() != null) {
            config.setSensitive(req.getSensitive());
        }
        return configRepository.save(config);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {SupportConstants.CACHE_CONFIG, SupportConstants.CACHE_CONFIG_GROUP}, allEntries = true)
    @Override
    public void delete(Long id) {
        super.delete(id);
    }

    // ==================== 内部取值：全程明文 ====================

    /** 详情：返回明文实体（内部用；对外出口由 Controller 转 {@link ConfigVO}）。 */
    @Override
    public SysConfig get(Long id) {
        return super.get(id);
    }

    /** 取值条目（默认分组）：带平台兜底，返回明文实体，供 Controller 组装对外视图。 */
    @TenantIgnore
    public SysConfig valueEntry(String configKey, String currentTenantId) {
        return resolve(SupportConstants.CONFIG_GROUP_DEFAULT, configKey, currentTenantId);
    }

    /** 取值条目（指定分组）：带平台兜底，返回明文实体。 */
    @TenantIgnore
    public SysConfig valueEntry(String group, String configKey, String currentTenantId) {
        return resolve(group, configKey, currentTenantId);
    }

    /** 内部取值（默认分组）：明文，供 conflux provider 等后端代码直接调用，不经 HTTP。 */
    @Cacheable(value = SupportConstants.CACHE_CONFIG, key = "#currentTenantId + '|DEFAULT|' + #configKey")
    @TenantIgnore
    public String get(String configKey, String currentTenantId) {
        return get(SupportConstants.CONFIG_GROUP_DEFAULT, configKey, currentTenantId);
    }

    /** 内部取值（指定分组）：明文，当前租户 → 平台兜底。 */
    @Cacheable(value = SupportConstants.CACHE_CONFIG, key = "#currentTenantId + '|' + #group + '|' + #configKey")
    @TenantIgnore
    public String get(String group, String configKey, String currentTenantId) {
        SysConfig config = resolve(group, configKey, currentTenantId);
        return config == null ? null : config.getConfigValue();
    }

    /** 整组取值（带平台兜底）：明文实体列表，内部使用。 */
    @Cacheable(value = SupportConstants.CACHE_CONFIG_GROUP, key = "#currentTenantId + '|' + #group")
    @TenantIgnore
    public List<SysConfig> listByGroup(String group, String currentTenantId) {
        String g = defaultGroup(group);
        List<SysConfig> tenantList = listInternal(g, currentTenantId);
        List<SysConfig> result = new ArrayList<>();
        String platformTenantId = iamService.getPlatformTenantId();
        if (!platformTenantId.equals(currentTenantId)) {
            for (SysConfig c : listInternal(g, platformTenantId)) {
                result.add(copyPlain(c));
            }
        }
        for (SysConfig c : tenantList) {
            result.removeIf(x -> x.getConfigKey().equals(c.getConfigKey()));
            result.add(copyPlain(c));
        }
        return result;
    }

    /** GET /secret：取敏感明文（Controller 侧独立权限点 + 审计）。非敏感配置也允许读取。 */
    public String secretPlain(Long id) {
        SysConfig config = require(id);
        return config.getConfigValue();
    }

    // ==================== 对外出口：加密成密文对象 ====================

    /** 明文实体 → 对外视图：敏感值加密成 {@link SecretCipher}，非敏感值原样明文。 */
    public ConfigVO toVO(SysConfig src) {
        ConfigVO vo = new ConfigVO();
        vo.setId(src.getId());
        vo.setTenantId(src.getTenantId());
        vo.setConfigGroup(src.getConfigGroup());
        vo.setConfigKey(src.getConfigKey());
        vo.setValueType(src.getValueType());
        vo.setSensitive(src.getSensitive());
        vo.setRemark(src.getRemark());
        vo.setCreateTime(src.getCreateTime());
        vo.setUpdateTime(src.getUpdateTime());
        vo.setCreateBy(src.getCreateBy());
        vo.setUpdateBy(src.getUpdateBy());
        vo.setConfigValue(Boolean.TRUE.equals(src.getSensitive())
                ? encrypt(src.getConfigValue()) : src.getConfigValue());
        return vo;
    }

    /** 单值出口：敏感返回密文对象、非敏感返回明文字符串。 */
    public Object valueOf(SysConfig src) {
        if (src == null) {
            return null;
        }
        return Boolean.TRUE.equals(src.getSensitive()) ? encrypt(src.getConfigValue()) : src.getConfigValue();
    }

    private static SecretCipher encrypt(String plain) {
        return new SecretCipher(true, plain == null ? null : DataCipherHolder.get().encrypt(plain));
    }

    // ==================== 内部工具 ====================

    /** 取值：按传入的 tenantId 查，缺则回落平台租户；登录态由调用方决定，不在此处判断。 */
    @TenantIgnore
    private SysConfig resolve(String group, String configKey, String tenantId) {
        String g = defaultGroup(group);
        SysConfig config = findInternal(g, configKey, tenantId);
        if (config == null) {
            String platformTenantId = iamService.getPlatformTenantId();
            if (!platformTenantId.equals(tenantId)) {
                config = findInternal(g, configKey, platformTenantId);
            }
        }
        return config;
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

    /** 明文副本：脱离 JPA 管理，避免被缓存后污染持久化上下文。 */
    private static SysConfig copyPlain(SysConfig src) {
        SysConfig copy = new SysConfig();
        copy.setId(src.getId());
        copy.setTenantId(src.getTenantId());
        copy.setConfigGroup(src.getConfigGroup());
        copy.setConfigKey(src.getConfigKey());
        copy.setValueType(src.getValueType());
        copy.setSensitive(src.getSensitive());
        copy.setRemark(src.getRemark());
        copy.setConfigValue(src.getConfigValue());
        copy.setCreateTime(src.getCreateTime());
        copy.setUpdateTime(src.getUpdateTime());
        copy.setCreateBy(src.getCreateBy());
        copy.setUpdateBy(src.getUpdateBy());
        copy.setDeleted(src.getDeleted());
        return copy;
    }

    private static String defaultGroup(String group) {
        return group == null || group.isEmpty() ? SupportConstants.CONFIG_GROUP_DEFAULT : group;
    }
}
