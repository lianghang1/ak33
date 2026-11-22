package com.sfi.matrix.support.controller;

import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.matrix.support.constant.SupportConstants;
import com.sfi.matrix.support.dto.DictDataRequest;
import com.sfi.matrix.support.dto.IdRequest;
import com.sfi.matrix.support.entity.SysDictData;
import com.sfi.matrix.support.service.DictDataService;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.query.QueryUtilsX;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.security.identity.LoginUser;
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
 * 字典选项接口。
 *
 * <p>路径保持 {@code /api/dict} 不变（前端已按此对接），底层数据由单表 sys_dict
 * 演进为 sys_dict_data。新增独立的字典类型管理走 {@code /api/dict-type}。
 * 列表走 QueryUtilsX 通路②；写操作 POST + JSON DTO。</p>
 *
 * @author matrix
 */
@RestController
@RequestMapping("/api/dict")
public class DictDataController {

    private final DictDataService dictDataService;

    public DictDataController(DictDataService dictDataService) {
        this.dictDataService = dictDataService;
    }

    @OperateLog(module = "字典管理", operation = "维护字典选项")
    @PostMapping
    public R<SysDictData> save(@Valid @RequestBody DictDataRequest req) {
        return R.ok(dictDataService.save(req.getId(), req.getDictType(), req.getDictCode(), req.getDictLabel(),
                req.getSortNo() == null ? 0 : req.getSortNo(), req.getStatus(), req.getTenantId()));
    }

    @OperateLog(module = "字典管理", operation = "删除字典选项")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        dictDataService.delete(req.getId());
        return R.ok();
    }

    @GetMapping
    public R<SysDictData> get(@RequestParam Long id) {
        return R.ok(dictDataService.get(id));
    }

    /** 选项分页列表（QueryBean 通路②） */
    @GetMapping("/page")
    public R<Page<SysDictData>> page(HttpServletRequest request) {
        int page = parseInt(request.getParameter("page"), 0);
        int size = Math.min(parseInt(request.getParameter("size"), 15), 200);
        return R.ok(QueryUtilsX.doQuery(QueryUtilsX.parse(request.getParameterMap()),
                PageRequest.of(page, size), SysDictData.class));
    }

    /** 按类型取选项（带平台兜底）——前端兼容端点 */
    @GetMapping("/list")
    public R<List<SysDictData>> list(@RequestParam String dictType) {
        LoginUser current = LoginContext.currentOrNull();
        String tenantId = current == null ? null : current.getTenantId();
        return R.ok(dictDataService.listByTypeWithPlatformFallback(dictType, tenantId));
    }

    private static int parseInt(String v, int def) {
        try { return v == null || v.isEmpty() ? def : Integer.parseInt(v); } catch (NumberFormatException e) { return def; }
    }
}
