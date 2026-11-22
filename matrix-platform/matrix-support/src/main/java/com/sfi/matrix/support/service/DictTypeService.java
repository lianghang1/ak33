package com.sfi.matrix.support.service;

import com.sfi.matrix.iam.service.IamService;
import com.sfi.matrix.support.constant.SupportConstants;
import com.sfi.matrix.support.entity.SysDictType;
import com.sfi.matrix.support.repository.SysDictTypeRepository;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.security.identity.LoginUser;
import com.sfi.scaffold.starter.data.jpa.service.BaseService;
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
public class DictTypeService extends BaseService<SysDictType> {

    private final SysDictTypeRepository typeRepository;
    private final IamService iamService;

    public DictTypeService(SysDictTypeRepository typeRepository, IamService iamService) {
        super(typeRepository);
        this.typeRepository = typeRepository;
        this.iamService = iamService;
    }

    /** 覆盖基类 save：base 控制器把 DTO 转成实体后调本方法，幂等/缓存逻辑留在 Service。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = SupportConstants.CACHE_DICT_TYPE, allEntries = true)
    public SysDictType save(SysDictType incoming) {
        SysDictType entity;
        if (incoming.getId() != null) {
            entity = require(incoming.getId()); // 更新传入不存在的 id 按 NOT_FOUND 处理
        } else {
            String tid = resolveTenantId(incoming.getTenantId());
            entity = typeRepository
                    .findByDictTypeAndTenantIdAndDeletedFalse(incoming.getDictType(), tid)
                    .orElseGet(SysDictType::new);
            if (entity.getId() == null) {
                entity.setDictType(incoming.getDictType());
                entity.setTenantId(tid);
            }
        }
        if (incoming.getDictName() != null) entity.setDictName(incoming.getDictName());
        if (incoming.getRemark() != null) entity.setRemark(incoming.getRemark());
        if (incoming.getStatus() != null) entity.setStatus(incoming.getStatus());
        return typeRepository.save(entity);
    }

    // get(Long) 继承 BaseService：逻辑删除过滤，查不到返回 null。

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = SupportConstants.CACHE_DICT_TYPE, allEntries = true)
    @Override
    public void delete(Long id) {
        super.delete(id); // 逻辑删除骨架在基类，这里只叠加缓存失效
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
