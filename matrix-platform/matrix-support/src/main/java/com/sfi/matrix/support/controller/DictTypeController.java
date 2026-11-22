package com.sfi.matrix.support.controller;

import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.matrix.support.constant.SupportConstants;
import com.sfi.matrix.support.dto.DictTypeRequest;
import com.sfi.matrix.support.dto.IdRequest;
import com.sfi.matrix.support.entity.SysDictType;
import com.sfi.matrix.support.service.DictTypeService;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.security.identity.LoginUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 字典类型接口。管理"有哪些字典"，列表带平台兜底。写操作 POST + JSON DTO。
 *
 * @author matrix
 */
@RestController
@RequestMapping("/api/dict-type")
public class DictTypeController {

    private final DictTypeService dictTypeService;

    public DictTypeController(DictTypeService dictTypeService) {
        this.dictTypeService = dictTypeService;
    }

    @OperateLog(module = "字典管理", operation = "维护字典类型")
    @PostMapping
    public R<SysDictType> save(@Valid @RequestBody DictTypeRequest req) {
        return R.ok(dictTypeService.save(req.getId(), req.getDictType(), req.getDictName(),
                req.getRemark(), req.getStatus(), req.getTenantId()));
    }

    @OperateLog(module = "字典管理", operation = "删除字典类型")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        dictTypeService.delete(req.getId());
        return R.ok();
    }

    @GetMapping
    public R<SysDictType> get(@RequestParam Long id) {
        return R.ok(dictTypeService.get(id));
    }

    /** 类型列表（带平台兜底） */
    @GetMapping("/list")
    public R<List<SysDictType>> list() {
        return R.ok(dictTypeService.listWithPlatformFallback(DictTypeService.currentTenantId()));
    }
}
