package com.sfi.matrix.support.service;

import com.sfi.matrix.iam.service.IamService;
import com.sfi.matrix.support.constant.SupportConstants;
import com.sfi.matrix.support.entity.SysDictType;
import com.sfi.matrix.support.repository.SysDictTypeRepository;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.security.identity.LoginUser;
import com.sfi.scaffold.starter.tenant.annotation.TenantIgnore;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字典类型服务：CRUD + 平台兜底 + 缓存。
 *
 * <p>类型列表按租户缓存；"读"走平台兜底（租户未定义的类型补充平台那份）。
 * 写操作失效本租户与平台租户的类型列表缓存。</p>
 *
 * @author matrix
 */
@Service
public class DictTypeService {

    private final SysDictTypeRepository typeRepository;
    private final IamService iamService;

    public DictTypeService(SysDictTypeRepository typeRepository, IamService iamService) {
        this.typeRepository = typeRepository;
        this.iamService = iamService;
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = SupportConstants.CACHE_DICT_TYPE, allEntries = true)
    public SysDictType save(Long id, String dictType, String dictName, String remark,
                            Integer status, String tenantId) {
        SysDictType entity;
        if (id != null) {
            entity = get(id);
        } else {
            String tid = resolveTenantId(tenantId);
            entity = typeRepository.findByDictTypeAndTenantIdAndDeletedFalse(dictType, tid).orElseGet(SysDictType::new);
            if (entity.getId() == null) {
                entity.setDictType(dictType);
                entity.setTenantId(tid);
            }
        }
        if (dictName != null) entity.setDictName(dictName);
        if (remark != null) entity.setRemark(remark);
        if (status != null) entity.setStatus(status);
        return typeRepository.save(entity);
    }

    public SysDictType get(Long id) {
        return typeRepository.findById(id)
                .filter(t -> !Boolean.TRUE.equals(t.getDeleted()))
                .orElseThrow(() -> BizException.of(GlobalErrorCode.NOT_FOUND, "字典类型不存在：" + id));
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = SupportConstants.CACHE_DICT_TYPE, allEntries = true)
    public void delete(Long id) {
        SysDictType t = get(id);
        t.setDeleted(Boolean.TRUE);
        typeRepository.save(t);
    }

    /** 类型列表（带平台兜底）：按租户缓存，key 为租户ID。 */
    @Cacheable(value = SupportConstants.CACHE_DICT_TYPE, key = "#tenantId")
    @TenantIgnore
    public List<SysDictType> listWithPlatformFallback(String tenantId) {
        String platformTenantId = iamService.getPlatformTenantId();
        List<SysDictType> result = typeRepository.findByDeletedFalseOrderByDictTypeAsc().stream()
                .filter(t -> tenantId == null || tenantId.equals(t.getTenantId()))
                .collect(java.util.stream.Collectors.toList());
        if (!platformTenantId.equals(tenantId)) {
            java.util.Map<String, SysDictType> merged = new java.util.LinkedHashMap<>();
            for (SysDictType t : typeRepository.findByDeletedFalseOrderByDictTypeAsc().stream()
                    .filter(t -> platformTenantId.equals(t.getTenantId()))
                    .collect(java.util.stream.Collectors.toList())) {
                merged.put(t.getDictType(), t);
            }
            for (SysDictType t : result) {
                merged.put(t.getDictType(), t);
            }
            result = new java.util.ArrayList<>(merged.values());
        }
        return result;
    }

    /** 当前登录租户ID（未登录时返回 null，走平台租户上下文下的查询）。 */
    public static String currentTenantId() {
        LoginUser user = LoginContext.currentOrNull();
        return user == null ? null : user.getTenantId();
    }

    private String resolveTenantId(String tenantId) {
        if (tenantId != null && !tenantId.isEmpty()) return tenantId;
        return currentTenantId();
    }
}
