package com.sfi.matrix.support.dict.controller;

import com.sfi.matrix.support.dict.entity.SysDict;
import com.sfi.matrix.support.dict.service.DictService;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.starter.security.permission.RequirePermission;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据字典接口。
 *
 * @author matrix
 */
@RestController
@RequestMapping("/api/dict")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    /** 维护字典：需平台字典管理权限（权限点与 IamConstants.PERM_DICT 对应，support 不依赖 iam 故用字面量） */
    @RequirePermission("platform:dict:list")
    @PostMapping
    public R<SysDict> create(@RequestParam String dictType,
                             @RequestParam String dictCode,
                             @RequestParam String dictLabel,
                             @RequestParam(defaultValue = "0") int sortNo,
                             @RequestParam(required = false) String tenantId) {
        return R.ok(dictService.create(dictType, dictCode, dictLabel, sortNo, tenantId));
    }

    @GetMapping("/list")
    public R<List<SysDict>> list(@RequestParam String dictType) {
        return R.ok(dictService.listByType(dictType));
    }
}
