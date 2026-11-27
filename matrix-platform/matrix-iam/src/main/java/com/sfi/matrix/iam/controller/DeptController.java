package com.sfi.matrix.iam.controller;

import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.matrix.iam.controller.support.IamPageQuery;
import com.sfi.matrix.iam.dto.DeptRequest;
import com.sfi.matrix.iam.entity.SysDept;
import com.sfi.matrix.iam.service.IamService;
import com.sfi.scaffold.common.request.IdRequest;
import com.sfi.scaffold.common.result.PageResult;
import com.sfi.scaffold.common.result.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 部门接口：部门维护。
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/iam")
public class DeptController {

    private final IamService iamService;

    public DeptController(IamService iamService) {
        this.iamService = iamService;
    }

    @OperateLog(module = "部门管理", operation = "新增/修改部门")
    @PostMapping("/dept")
    public R<SysDept> saveDept(@Valid @RequestBody DeptRequest req) {
        if (req.getId() == null) {
            return R.ok(iamService.createDept(req.getDeptName(), req.getParentId(), req.getTenantId(),
                    req.getSortNo() == null ? 0 : req.getSortNo(), req.getLeaderId()));
        }
        return R.ok(iamService.updateDept(req.getId(), req.getDeptName(), req.getParentId(),
                req.getSortNo(), req.getLeaderId(), req.getStatus()));
    }

    @GetMapping("/dept")
    public R<SysDept> getDept(@RequestParam Long id) {
        return R.ok(iamService.getDept(id));
    }

    @OperateLog(module = "部门管理", operation = "删除部门")
    @PostMapping("/dept/delete")
    public R<Void> deleteDept(@Valid @RequestBody IdRequest req) {
        iamService.deleteDept(req.getId());
        return R.ok();
    }

    @GetMapping("/depts")
    public R<PageResult<SysDept>> depts(HttpServletRequest request) {
        return R.ok(IamPageQuery.query(request, SysDept.class));
    }
}
