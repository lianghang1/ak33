package com.sfi.matrix.iam.controller;

import com.sfi.matrix.iam.annotation.OperateLog;
import com.sfi.matrix.iam.dto.AssignMenusRequest;
import com.sfi.matrix.iam.dto.RoleRequest;
import com.sfi.matrix.iam.entity.SysRole;
import com.sfi.matrix.iam.service.IamService;
import com.sfi.matrix.iam.service.RoleService;
import com.sfi.scaffold.common.request.IdRequest;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.starter.web.crud.BaseCrudController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import javax.validation.Valid;

/**
 * 角色接口：list/get/delete 走 CRUD 基类；角色菜单授权属跨实体编排，保留 IamService。
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/iam/role")
public class RoleController extends BaseCrudController<SysRole, RoleRequest, RoleService> {

    private final IamService iamService;

    public RoleController(RoleService service, IamService iamService) {
        super(service, SysRole.class);
        this.iamService = iamService;
    }

    @Override
    @OperateLog(module = "角色管理", operation = "新增/修改角色")
    @PostMapping("/save")
    public R<SysRole> save(@Valid @RequestBody RoleRequest req) {
        if (req.getId() == null) {
            return R.ok(service.create(req.getRoleCode(), req.getRoleName(), req.getTenantId(),
                    req.getDataScope() == null ? "ALL" : req.getDataScope(),
                    req.getDataScopeDeptIds(), req.getSortNo(), req.getRemark()));
        }
        return R.ok(service.update(req.getId(), req.getRoleName(), req.getDataScope(),
                req.getDataScopeDeptIds(), req.getSortNo(), req.getRemark()));
    }

    @Override
    @OperateLog(module = "角色管理", operation = "删除角色")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        return super.delete(req);
    }

    @GetMapping("/menus")
    public R<List<Long>> menus(@RequestParam Long roleId, @RequestParam String tenantId) {
        return R.ok(iamService.roleMenuIds(roleId, tenantId));
    }

    @OperateLog(module = "角色管理", operation = "分配角色菜单")
    @PostMapping("/assign-menus")
    public R<Void> assignMenus(@Valid @RequestBody AssignMenusRequest req) {
        iamService.assignMenus(req.getRoleId(), req.getMenuIds(), req.getTenantId());
        return R.ok();
    }
}
