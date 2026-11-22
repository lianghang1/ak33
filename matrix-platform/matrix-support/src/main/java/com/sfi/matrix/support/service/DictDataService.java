package com.sfi.matrix.support.service;

import com.sfi.matrix.iam.service.IamService;
import com.sfi.matrix.support.constant.SupportConstants;
import com.sfi.matrix.support.entity.SysDictData;
import com.sfi.matrix.support.repository.SysDictDataRepository;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
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
public class DictDataService {

    private final SysDictDataRepository dataRepository;
    private final IamService iamService;

    public DictDataService(SysDictDataRepository dataRepository, IamService iamService) {
        this.dataRepository = dataRepository;
        this.iamService = iamService;
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = SupportConstants.CACHE_DICT_DATA, allEntries = true)
    public SysDictData save(Long id, String dictType, String dictCode, String dictLabel,
                            int sortNo, Integer status, String tenantId) {
        SysDictData data;
        if (id != null) {
            data = get(id);
        } else {
            String tid = tenantId != null && !tenantId.isEmpty() ? tenantId : DictTypeService.currentTenantId();
            data = dataRepository.findByDictTypeAndDictCodeAndTenantIdAndDeletedFalse(dictType, dictCode, tid)
                    .orElseGet(SysDictData::new);
            if (data.getId() == null) {
                data.setDictType(dictType);
                data.setDictCode(dictCode);
                data.setTenantId(tid);
            }
        }
        if (dictLabel != null) data.setDictLabel(dictLabel);
        data.setSortNo(sortNo);
        if (status != null) data.setStatus(status);
        return dataRepository.save(data);
    }

    public SysDictData get(Long id) {
        return dataRepository.findById(id)
                .filter(d -> !Boolean.TRUE.equals(d.getDeleted()))
                .orElseThrow(() -> BizException.of(GlobalErrorCode.NOT_FOUND, "字典选项不存在：" + id));
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = SupportConstants.CACHE_DICT_DATA, allEntries = true)
    public void delete(Long id) {
        SysDictData d = get(id);
        d.setDeleted(Boolean.TRUE);
        dataRepository.save(d);
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
