package com.sfi.matrix.iam.controller;

import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.matrix.iam.dto.DeptRequest;
import com.sfi.matrix.iam.entity.SysDept;
import com.sfi.matrix.iam.service.DeptService;
import com.sfi.scaffold.common.request.IdRequest;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.starter.web.crud.BaseCrudController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 部门接口：list/get/delete/save 走 CRUD 基类，子部门校验在 DeptService.delete。
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/iam/dept")
public class DeptController extends BaseCrudController<SysDept, DeptRequest, DeptService> {

    public DeptController(DeptService service) {
        super(service, SysDept.class);
    }

    @Override
    @OperateLog(module = "部门管理", operation = "新增/修改部门")
    @PostMapping("/save")
    public R<SysDept> save(@Valid @RequestBody DeptRequest req) {
        if (req.getId() == null) {
            return R.ok(service.create(req.getDeptName(), req.getParentId(), req.getTenantId(),
                    req.getSortNo() == null ? 0 : req.getSortNo(), req.getLeaderId()));
        }
        return R.ok(service.update(req.getId(), req.getDeptName(), req.getParentId(),
                req.getSortNo(), req.getLeaderId(), req.getStatus()));
    }

    @Override
    @OperateLog(module = "部门管理", operation = "删除部门")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        return super.delete(req);
    }
}
