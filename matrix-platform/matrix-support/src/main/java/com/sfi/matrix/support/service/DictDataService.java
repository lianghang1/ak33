package com.sfi.matrix.support.service;

import com.sfi.matrix.iam.service.IamService;
import com.sfi.matrix.support.constant.SupportConstants;
import com.sfi.matrix.support.entity.SysDictData;
import com.sfi.matrix.support.repository.SysDictDataRepository;
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
 * 字典选项服务：CRUD + 平台默认兜底 + 缓存。
 *
 * <p>租户选项优先；租户未定义的 code 回退到平台租户那份，实现
 * "平台定义全局默认、租户按需覆盖"。选项列表按租户+类型缓存，
 * 写操作失效本租户与平台两级缓存。</p>
 *
 * @author matrix
 */
@Service
public class DictDataService extends BaseService<SysDictData> {

    private final SysDictDataRepository dataRepository;
    private final IamService iamService;

    public DictDataService(SysDictDataRepository dataRepository, IamService iamService) {
        super(dataRepository);
        this.dataRepository = dataRepository;
        this.iamService = iamService;
    }

    /** 覆盖基类 save：base 控制器把 DTO 转成实体后调本方法，幂等/缓存逻辑留在 Service。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = SupportConstants.CACHE_DICT_DATA, allEntries = true)
    public SysDictData save(SysDictData incoming) {
        SysDictData data;
        if (incoming.getId() != null) {
            data = require(incoming.getId()); // 更新传入不存在的 id 按 NOT_FOUND 处理
        } else {
            String tid = incoming.getTenantId() != null && !incoming.getTenantId().isEmpty()
                    ? incoming.getTenantId() : LoginContext.currentTenantIdOrNull();
            data = dataRepository.findByDictTypeAndDictCodeAndTenantIdAndDeletedFalse(
                    incoming.getDictType(), incoming.getDictCode(), tid).orElseGet(SysDictData::new);
            if (data.getId() == null) {
                data.setDictType(incoming.getDictType());
                data.setDictCode(incoming.getDictCode());
                data.setTenantId(tid);
            }
        }
        if (incoming.getDictLabel() != null) data.setDictLabel(incoming.getDictLabel());
        data.setSortNo(incoming.getSortNo() == null ? 0 : incoming.getSortNo());
        if (incoming.getStatus() != null) data.setStatus(incoming.getStatus());
        return dataRepository.save(data);
    }

    // get(Long) 继承 BaseService：逻辑删除过滤，查不到返回 null。

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = SupportConstants.CACHE_DICT_DATA, allEntries = true)
    @Override
    public void delete(Long id) {
        super.delete(id); // 逻辑删除骨架在基类，这里只叠加缓存失效
    }

    /** 按类型取选项，租户过滤由 scaffold @Filter 自动完成。 */
    public List<SysDictData> listByType(String dictType) {
        return dataRepository.findByDictTypeAndDeletedFalseOrderBySortNoAsc(dictType);
    }

    /**
     * 带平台兜底的选项列表（兼容前端 GET /api/dict/list 契约）。
     * key = 租户ID + '|' + 类型；@TenantIgnore 跨租户读取平台那份。
     */
    @Cacheable(value = SupportConstants.CACHE_DICT_DATA, key = "#tenantId + '|' + #dictType")
    @TenantIgnore
    public List<SysDictData> listByTypeWithPlatformFallback(String dictType, String tenantId) {
        List<SysDictData> tenantData = listByTypeInternal(dictType, tenantId);
        Map<String, SysDictData> merged = new LinkedHashMap<>();
        String platformTenantId = iamService.getPlatformTenantId();
        if (!platformTenantId.equals(tenantId)) {
            for (SysDictData d : listByTypeInternal(dictType, platformTenantId)) {
                merged.put(d.getDictCode(), d);
            }
        }
        for (SysDictData d : tenantData) {
            merged.put(d.getDictCode(), d);
        }
        return new ArrayList<>(merged.values());
    }

    @TenantIgnore
    private List<SysDictData> listByTypeInternal(String dictType, String tenantId) {
        return dataRepository.findByDictTypeAndDeletedFalseOrderBySortNoAsc(dictType).stream()
                .filter(d -> tenantId == null || tenantId.equals(d.getTenantId()))
                .collect(Collectors.toList());
    }
}
