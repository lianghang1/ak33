package com.sfi.matrix.support.service;

import com.sfi.matrix.iam.service.IamService;
import com.sfi.matrix.support.constant.SupportConstants;
import com.sfi.matrix.support.dto.DictOptionValue;
import com.sfi.matrix.support.entity.SysDict;
import com.sfi.matrix.support.entity.SysDictOption;
import com.sfi.matrix.support.repository.SysDictOptionRepository;
import com.sfi.matrix.support.repository.SysDictRepository;
import com.sfi.scaffold.starter.data.jpa.service.BaseService;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.tenant.annotation.TenantIgnore;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典选项服务：CRUD + 平台兜底取值 + 缓存。
 *
 * <p>取值按 dictCode 解析字典 id，租户选项覆盖平台同 code 选项。</p>
 *
 * @author lianghang
 */
@Service
public class DictOptionService extends BaseService<SysDictOption> {

    private final SysDictOptionRepository optionRepository;
    private final SysDictRepository dictRepository;
    private final IamService iamService;

    public DictOptionService(SysDictOptionRepository optionRepository, SysDictRepository dictRepository,
                             IamService iamService) {
        super(optionRepository);
        this.optionRepository = optionRepository;
        this.dictRepository = dictRepository;
        this.iamService = iamService;
    }

    /** 覆盖基类 save：id 为空按 dictId+optionCode 幂等，非空按 id 修改。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = SupportConstants.CACHE_DICT_OPTION, allEntries = true)
    public SysDictOption save(SysDictOption incoming) {
        SysDictOption entity;
        if (incoming.getId() != null) {
            entity = require(incoming.getId());
        } else {
            String tid = incoming.getTenantId() != null && !incoming.getTenantId().isEmpty()
                    ? incoming.getTenantId() : LoginContext.currentTenantIdOrNull();
            entity = optionRepository.findByDictIdAndOptionCodeAndTenantIdAndDeletedFalse(
                    incoming.getDictId(), incoming.getOptionCode(), tid).orElseGet(SysDictOption::new);
            if (entity.getId() == null) {
                entity.setDictId(incoming.getDictId());
                entity.setOptionCode(incoming.getOptionCode());
                entity.setTenantId(tid);
            }
        }
        if (incoming.getOptionCode() != null) entity.setOptionCode(incoming.getOptionCode());
        if (incoming.getOptionLabel() != null) entity.setOptionLabel(incoming.getOptionLabel());
        entity.setSortNo(incoming.getSortNo() == null ? 0 : incoming.getSortNo());
        if (incoming.getStatus() != null) entity.setStatus(incoming.getStatus());
        return optionRepository.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = SupportConstants.CACHE_DICT_OPTION, allEntries = true)
    @Override
    public void delete(Long id) {
        super.delete(id);
    }

    /** 按字典编码取选项（带平台兜底）：租户选项覆盖平台同 code 选项。 */
    @Cacheable(value = SupportConstants.CACHE_DICT_OPTION, key = "#tenantId + '|' + #dictCode")
    @TenantIgnore
    public List<DictOptionValue> values(String dictCode, String tenantId) {
        Map<String, SysDictOption> merged = new LinkedHashMap<>();
        String platformTenantId = iamService.getPlatformTenantId();
        List<SysDict> dicts = dictRepository.findByDeletedFalseOrderByDictCodeAsc();
        SysDict tenantDict = null;
        SysDict platformDict = null;
        for (SysDict d : dicts) {
            if (!dictCode.equals(d.getDictCode())) continue;
            if (platformTenantId.equals(d.getTenantId())) platformDict = d;
            if (tenantId != null && tenantId.equals(d.getTenantId())) tenantDict = d;
        }
        if (platformDict != null && !platformTenantId.equals(tenantId)) {
            for (SysDictOption o : optionsOf(platformDict.getId())) merged.put(o.getOptionCode(), o);
        }
        if (tenantDict != null) {
            for (SysDictOption o : optionsOf(tenantDict.getId())) merged.put(o.getOptionCode(), o);
        }
        return toValues(merged);
    }

    /** 批量取：按编码逐个取值，组装 Map。 */
    @TenantIgnore
    public Map<String, List<DictOptionValue>> valuesBatch(List<String> dictCodes, String tenantId) {
        Map<String, List<DictOptionValue>> result = new LinkedHashMap<>();
        if (dictCodes == null) return result;
        for (String code : dictCodes) {
            if (code != null && !code.isEmpty()) result.put(code, values(code, tenantId));
        }
        return result;
    }

    @TenantIgnore
    private List<SysDictOption> optionsOf(Long dictId) {
        return optionRepository.findByDictIdAndDeletedFalseOrderBySortNoAsc(dictId);
    }

    private List<DictOptionValue> toValues(Map<String, SysDictOption> merged) {
        List<DictOptionValue> result = new ArrayList<>();
        for (SysDictOption o : merged.values()) {
            result.add(new DictOptionValue(o.getOptionCode(), o.getOptionLabel(), o.getSortNo()));
        }
        return result;
    }
}
