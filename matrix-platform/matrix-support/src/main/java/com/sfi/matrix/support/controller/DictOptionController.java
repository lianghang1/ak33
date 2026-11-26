package com.sfi.matrix.support.controller;

import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.matrix.support.dto.DictOptionRequest;
import com.sfi.matrix.support.dto.DictOptionValue;
import com.sfi.matrix.support.entity.SysDictOption;
import com.sfi.matrix.support.service.DictOptionQueryService;
import com.sfi.matrix.support.service.DictOptionService;
import com.sfi.scaffold.common.request.IdRequest;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.web.crud.BaseCrudController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 字典选项接口：标准 CRUD 由 {@link BaseCrudController} 提供，取值端点按字典编码取选项（带平台兜底）。
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/dict/option")
public class DictOptionController extends BaseCrudController<SysDictOption, DictOptionRequest, DictOptionService> {

    private final DictOptionQueryService queryService;

    public DictOptionController(DictOptionService service, DictOptionQueryService queryService) {
        super(service, SysDictOption.class);
        this.queryService = queryService;
    }

    @Override
    @OperateLog(module = "字典管理", operation = "维护字典选项")
    @PostMapping("/save")
    public R<SysDictOption> save(@Valid @RequestBody DictOptionRequest dto) {
        return super.save(dto);
    }

    @Override
    @OperateLog(module = "字典管理", operation = "删除字典选项")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        return super.delete(req);
    }

    /** 按字典编码取选项（带平台兜底，精简 VO）。 */
    @GetMapping("/values")
    public R<List<DictOptionValue>> values(@RequestParam String dictCode) {
        return R.ok(queryService.values(dictCode, LoginContext.currentTenantIdOrNull()));
    }

    /** 批量取选项：dictCodes 为逗号分隔的字典编码。 */
    @GetMapping("/values/batch")
    public R<Map<String, List<DictOptionValue>>> valuesBatch(@RequestParam List<String> dictCodes) {
        return R.ok(service.valuesBatch(dictCodes, LoginContext.currentTenantIdOrNull()));
    }
}
