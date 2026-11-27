package com.sfi.matrix.iam.controller;

import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.scaffold.starter.web.crud.PageQuerySupport;
import com.sfi.matrix.iam.dto.AssignMenusRequest;
import com.sfi.matrix.iam.dto.RoleRequest;
import com.sfi.matrix.iam.entity.SysRole;
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
 * 角色接口：角色维护与角色菜单授权。
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/iam/role")
public class RoleController {

    private final IamService iamService;

    public RoleController(IamService iamService) {
        this.iamService = iamService;
    }

    @OperateLog(module = "角色管理", operation = "新增/修改角色")
    @PostMapping("/save")
    public R<SysRole> save(@Valid @RequestBody RoleRequest req) {
        if (req.getId() == null) {
            return R.ok(iamService.createRole(req.getRoleCode(), req.getRoleName(), req.getTenantId(),
                    req.getDataScope() == null ? "ALL" : req.getDataScope(),
                    req.getDataScopeDeptIds(), req.getSortNo(), req.getRemark()));
        }
        return R.ok(iamService.updateRole(req.getId(), req.getRoleName(), req.getDataScope(),
                req.getDataScopeDeptIds(), req.getSortNo(), req.getRemark()));
    }

    @GetMapping("/get")
    public R<SysRole> get(@RequestParam Long id) {
        return R.ok(iamService.getRole(id));
    }

    @OperateLog(module = "角色管理", operation = "删除角色")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        iamService.deleteRole(req.getId());
        return R.ok();
    }

    @GetMapping("/list")
    public R<PageResult<SysRole>> list(HttpServletRequest request) {
        return R.ok(PageQuerySupport.query(request, SysRole.class));
    }

    @OperateLog(module = "角色管理", operation = "分配角色菜单")
    @PostMapping("/assign-menus")
    public R<Void> assignMenus(@Valid @RequestBody AssignMenusRequest req) {
        iamService.assignMenus(req.getRoleId(), req.getMenuIds(), req.getTenantId());
        return R.ok();
    }
}
