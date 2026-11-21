package com.sfi.matrix.support.dict.service;

import com.sfi.matrix.iam.service.IamService;
import com.sfi.matrix.support.dict.entity.SysDict;
import com.sfi.matrix.support.dict.repository.SysDictRepository;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.security.identity.LoginUser;
import com.sfi.scaffold.starter.tenant.annotation.TenantIgnore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据字典服务：CRUD + 平台默认兜底。
 *
 * <p>租户自身字典优先；租户未定义的条目回退到平台租户（PLATFORM）的那份，
 * 实现"平台定义全局默认、租户按需覆盖"。</p>
 *
 * @author matrix
 */
@Service
public class DictService {

    private final SysDictRepository dictRepository;
    private final IamService iamService;

    public DictService(SysDictRepository dictRepository, IamService iamService) {
        this.dictRepository = dictRepository;
        this.iamService = iamService;
    }

    @Transactional(rollbackFor = Exception.class)
    public SysDict save(Long id, String dictType, String dictCode, String dictLabel,
                        int sortNo, Integer status, String tenantId) {
        SysDict dict;
        if (id != null) {
            dict = get(id);
        } else {
            // 未显式指定租户时，归属当前登录租户（平台超管即在 PLATFORM 租户）
            String tid = tenantId;
            if (tid == null || tid.isEmpty()) {
                LoginUser user = LoginContext.currentOrNull();
                tid = user == null ? null : user.getTenantId();
            }
            final String finalTenantId = tid;
            // 幂等：同租户 + 类型 + 编码已存在则更新
            dict = dictRepository
                    .findByDictTypeAndDictCodeAndTenantIdAndDeletedFalse(dictType, dictCode, tid)
                    .orElseGet(SysDict::new);
            if (dict.getId() == null) {
                dict.setDictType(dictType);
                dict.setDictCode(dictCode);
                dict.setTenantId(finalTenantId);
            }
        }
        dict.setDictLabel(dictLabel);
        dict.setSortNo(sortNo);
        if (status != null) dict.setStatus(status);
        return dictRepository.save(dict);
    }

    public SysDict get(Long id) {
        return dictRepository.findById(id)
                .filter(d -> !Boolean.TRUE.equals(d.getDeleted()))
                .orElseThrow(() -> BizException.of(GlobalErrorCode.NOT_FOUND, "字典不存在：" + id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysDict d = get(id);
        d.setDeleted(Boolean.TRUE);
        dictRepository.save(d);
    }

    /** 按类型取字典选项：租户过滤由 scaffold @Filter 自动完成 */
    public List<SysDict> listByType(String dictType) {
        return dictRepository.findByDictTypeAndDeletedFalseOrderBySortNoAsc(dictType);
    }

    /**
     * 带平台兜底的字典列表：租户字典优先，平台字典补充未覆盖的 code。
     * 需要 @TenantIgnore 跨租户读取平台那份。
     */
    @TenantIgnore
    public List<SysDict> listByTypeWithPlatformFallback(String dictType, String currentTenantId) {
        List<SysDict> tenantDicts = listByTypeInternal(dictType, currentTenantId);
        Map<String, SysDict> merged = new LinkedHashMap<>();
        // 平台字典先放（作为默认）
        String platformTenantId = iamService.getPlatformTenantId();
        if (!platformTenantId.equals(currentTenantId)) {
            for (SysDict d : listByTypeInternal(dictType, platformTenantId)) {
                merged.put(d.getDictCode(), d);
            }
        }
        // 租户字典覆盖同 code
        for (SysDict d : tenantDicts) {
            merged.put(d.getDictCode(), d);
        }
        return new ArrayList<>(merged.values());
    }

    @TenantIgnore
    private List<SysDict> listByTypeInternal(String dictType, String tenantId) {
        return dictRepository.findByDictTypeAndDeletedFalseOrderBySortNoAsc(dictType).stream()
                .filter(d -> tenantId == null || tenantId.equals(d.getTenantId()))
                .collect(java.util.stream.Collectors.toList());
    }
}
