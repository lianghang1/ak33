package com.sfi.matrix.support.service;

import com.sfi.matrix.iam.service.IamService;
import com.sfi.matrix.support.constant.SupportConstants;
import com.sfi.matrix.support.entity.SysDict;
import com.sfi.matrix.support.entity.SysDictOption;
import com.sfi.matrix.support.repository.SysDictOptionRepository;
import com.sfi.matrix.support.repository.SysDictRepository;
import com.sfi.scaffold.starter.data.jpa.logicdelete.DeletedFieldAccessor;
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
import java.util.stream.Collectors;

/**
 * 字典服务：CRUD + 平台兜底 + 缓存 + 级联删除。
 *
 * <p>字典编码可修改；删除字典时级联逻辑删除其下所有选项。</p>
 *
 * @author lianghang
 */
@Service
public class DictService extends BaseService<SysDict> {

    private final SysDictRepository dictRepository;
    private final SysDictOptionRepository optionRepository;
    private final IamService iamService;

    public DictService(SysDictRepository dictRepository, SysDictOptionRepository optionRepository,
                       IamService iamService) {
        super(dictRepository);
        this.dictRepository = dictRepository;
        this.optionRepository = optionRepository;
        this.iamService = iamService;
    }

    /** 覆盖基类 save：id 为空按 dictCode 幂等，非空按 id 修改；编码允许修改。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = SupportConstants.CACHE_DICT, allEntries = true)
    public SysDict save(SysDict incoming) {
        SysDict entity;
        if (incoming.getId() != null) {
            entity = require(incoming.getId());
        } else {
            String tid = resolveTenantId(incoming.getTenantId());
            entity = dictRepository.findByDictCodeAndTenantIdAndDeletedFalse(incoming.getDictCode(), tid)
                    .orElseGet(SysDict::new);
            if (entity.getId() == null) {
                entity.setDictCode(incoming.getDictCode());
                entity.setTenantId(tid);
            }
        }
        if (incoming.getDictCode() != null) entity.setDictCode(incoming.getDictCode());
        if (incoming.getDictName() != null) entity.setDictName(incoming.getDictName());
        if (incoming.getRemark() != null) entity.setRemark(incoming.getRemark());
        if (incoming.getStatus() != null) entity.setStatus(incoming.getStatus());
        return dictRepository.save(entity);
    }

    /** 删除字典：级联逻辑删除其下所有选项，再删主表。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = SupportConstants.CACHE_DICT, allEntries = true)
    public void delete(Long id) {
        List<SysDictOption> options = optionRepository.findByDictIdAndDeletedFalseOrderBySortNoAsc(id);
        for (SysDictOption option : options) {
            DeletedFieldAccessor.markDeleted(option);
        }
        optionRepository.saveAll(options);
        super.delete(id);
    }

    /** 字典列表（带平台兜底）：按租户缓存，key 为租户ID。 */
    @Cacheable(value = SupportConstants.CACHE_DICT, key = "#tenantId")
    @TenantIgnore
    public List<SysDict> listWithPlatformFallback(String tenantId) {
        String platformTenantId = iamService.getPlatformTenantId();
        List<SysDict> result = dictRepository.findByDeletedFalseOrderByDictCodeAsc().stream()
                .filter(t -> tenantId == null || tenantId.equals(t.getTenantId()))
                .collect(Collectors.toList());
        if (!platformTenantId.equals(tenantId)) {
            Map<String, SysDict> merged = new LinkedHashMap<>();
            for (SysDict t : dictRepository.findByDeletedFalseOrderByDictCodeAsc().stream()
                    .filter(t -> platformTenantId.equals(t.getTenantId()))
                    .collect(Collectors.toList())) {
                merged.put(t.getDictCode(), t);
            }
            for (SysDict t : result) {
                merged.put(t.getDictCode(), t);
            }
            result = new ArrayList<>(merged.values());
        }
        return result;
    }

    private String resolveTenantId(String tenantId) {
        if (tenantId != null && !tenantId.isEmpty()) return tenantId;
        return LoginContext.currentTenantIdOrNull();
    }
}
