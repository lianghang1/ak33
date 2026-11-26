package com.sfi.matrix.support.service;

import com.sfi.matrix.iam.service.IamService;
import com.sfi.matrix.support.constant.SupportConstants;
import com.sfi.matrix.support.dto.DictOptionValue;
import com.sfi.matrix.support.entity.SysDict;
import com.sfi.matrix.support.entity.SysDictOption;
import com.sfi.matrix.support.repository.SysDictOptionRepository;
import com.sfi.matrix.support.repository.SysDictRepository;
import com.sfi.scaffold.starter.tenant.annotation.TenantIgnore;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典选项取值服务：按字典编码取选项，带平台兜底与缓存。
 *
 * <p>取值按 dictCode 解析字典 id，租户选项覆盖平台同 code 选项。
 * 缓存按 租户ID + 字典编码 分键，写入时全量失效。</p>
 *
 * @author lianghang
 */
@Service
public class DictOptionQueryService {

    private final SysDictOptionRepository optionRepository;
    private final SysDictRepository dictRepository;
    private final IamService iamService;

    public DictOptionQueryService(SysDictOptionRepository optionRepository, SysDictRepository dictRepository,
                                  IamService iamService) {
        this.optionRepository = optionRepository;
        this.dictRepository = dictRepository;
        this.iamService = iamService;
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
