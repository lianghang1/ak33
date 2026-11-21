package com.sfi.matrix.support.dict.service;

import com.sfi.matrix.support.dict.entity.SysDict;
import com.sfi.matrix.support.dict.repository.SysDictRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 数据字典服务。
 *
 * @author matrix
 */
@Service
public class DictService {

    private final SysDictRepository dictRepository;

    public DictService(SysDictRepository dictRepository) {
        this.dictRepository = dictRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public SysDict create(String dictType, String dictCode, String dictLabel, int sortNo, String tenantId) {
        SysDict dict = new SysDict();
        dict.setDictType(dictType);
        dict.setDictCode(dictCode);
        dict.setDictLabel(dictLabel);
        dict.setSortNo(sortNo);
        dict.setStatus(1);
        dict.setTenantId(tenantId);
        return dictRepository.save(dict);
    }

    /** 按类型取字典选项（租户过滤由 scaffold 的 @Filter 自动完成） */
    public List<SysDict> listByType(String dictType) {
        return dictRepository.findByDictTypeAndDeletedFalseOrderBySortNoAsc(dictType);
    }
}
