package com.sfi.matrix.support.dict.controller;

import com.sfi.matrix.support.dict.entity.SysDict;
import com.sfi.matrix.support.dict.service.DictService;
import com.sfi.matrix.support.dto.DictRequest;
import com.sfi.matrix.support.dto.IdRequest;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.query.QueryUtilsX;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.security.identity.LoginUser;
import com.sfi.scaffold.starter.security.permission.RequirePermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 数据字典接口。列表走 QueryUtilsX 通路②；写操作 POST + JSON DTO。
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

    /** 维护字典：需平台字典管理权限（support 不依赖 iam 常量，按约定字面量） */
    @RequirePermission("platform:dict:list")
    @PostMapping
    public R<SysDict> save(@Valid @RequestBody DictRequest req) {
        return R.ok(dictService.save(req.getId(), req.getDictType(), req.getDictCode(), req.getDictLabel(),
                req.getSortNo() == null ? 0 : req.getSortNo(), req.getStatus(), req.getTenantId()));
    }

    @RequirePermission("platform:dict:list")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        dictService.delete(req.getId());
        return R.ok();
    }

    @RequirePermission("platform:dict:list")
    @GetMapping
    public R<SysDict> get(@RequestParam Long id) {
        return R.ok(dictService.get(id));
    }

    /** 字典分页列表（QueryBean 通路②） */
    @GetMapping("/page")
    public R<Page<SysDict>> page(HttpServletRequest request) {
        int page = parseInt(request.getParameter("page"), 0);
        int size = Math.min(parseInt(request.getParameter("size"), 15), 200);
        return R.ok(QueryUtilsX.doQuery(QueryUtilsX.parse(request.getParameterMap()),
                PageRequest.of(page, size), SysDict.class));
    }

    /** 按类型取字典选项（带平台兜底） */
    @GetMapping("/list")
    public R<List<SysDict>> list(@RequestParam String dictType) {
        LoginUser current = LoginContext.currentOrNull();
        String tenantId = current == null ? null : current.getTenantId();
        return R.ok(dictService.listByTypeWithPlatformFallback(dictType, tenantId));
    }

    private static int parseInt(String v, int def) {
        try { return v == null || v.isEmpty() ? def : Integer.parseInt(v); } catch (NumberFormatException e) { return def; }
    }
}
