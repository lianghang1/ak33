package com.sfi.matrix.support.controller.dict;

import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.matrix.support.dto.DictRequest;
import com.sfi.matrix.support.entity.SysDict;
import com.sfi.matrix.support.service.dict.DictService;
import com.sfi.scaffold.common.request.IdRequest;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.starter.web.crud.BaseCrudController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 字典接口：标准 CRUD 由 {@link BaseCrudController} 提供（list/get/save/delete）。
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/dict")
public class DictController extends BaseCrudController<SysDict, DictRequest, DictService> {

    public DictController(DictService service) {
        super(service, SysDict.class);
    }

    @Override
    @OperateLog(module = "字典管理", operation = "维护字典")
    @PostMapping("/save")
    public R<SysDict> save(@Valid @RequestBody DictRequest dto) {
        return super.save(dto);
    }

    @Override
    @OperateLog(module = "字典管理", operation = "删除字典")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        return super.delete(req);
    }
}
