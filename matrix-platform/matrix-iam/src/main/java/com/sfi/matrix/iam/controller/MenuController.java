package com.sfi.matrix.iam.controller;

import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.matrix.iam.constant.IamConstants;
import com.sfi.matrix.iam.dto.MenuRequest;
import com.sfi.matrix.iam.entity.SysMenu;
import com.sfi.matrix.iam.service.IamService;
import com.sfi.matrix.iam.service.MenuService;
import com.sfi.scaffold.common.request.IdRequest;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.security.identity.LoginUser;
import com.sfi.scaffold.starter.web.crud.BaseCrudController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 菜单接口：菜单定义维护走 CRUD 基类；菜单树与当前用户可见菜单属权限计算，保留 IamService。
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/iam/menu")
public class MenuController extends BaseCrudController<SysMenu, MenuRequest, MenuService> {

    private final IamService iamService;

    public MenuController(MenuService service, IamService iamService) {
        super(service, SysMenu.class);
        this.iamService = iamService;
    }

    @Override
    @OperateLog(module = "菜单管理", operation = "新增/修改菜单")
    @PostMapping("/save")
    public R<SysMenu> save(@Valid @RequestBody MenuRequest req) {
        if (req.getId() == null) {
            String scope = req.getMenuScope() == null || req.getMenuScope().isEmpty()
                    ? IamConstants.MENU_SCOPE_TENANT : req.getMenuScope();
            return R.ok(service.create(req.getMenuName(), req.getMenuType(), req.getParentId(),
                    req.getPermission(), scope, req.getSortNo() == null ? 0 : req.getSortNo()));
        }
        return R.ok(service.update(req.getId(), req.getMenuName(), req.getMenuType(),
                req.getParentId(), req.getPath(), req.getComponent(), req.getIcon(),
                req.getPermission(), req.getMenuScope(), req.getSortNo(), req.getVisible()));
    }

    @Override
    @OperateLog(module = "菜单管理", operation = "删除菜单")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        return super.delete(req);
    }

    /** 菜单树：tenantId→套餐范围；scope→按范围；都不传→全部（非分页，菜单量小） */
    @GetMapping("/tree")
    public R<List<SysMenu>> tree(@RequestParam(required = false) String tenantId,
                                 @RequestParam(required = false) String scope) {
        if (tenantId != null && !tenantId.isEmpty()) {
            return R.ok(iamService.menuTreeOfTenant(tenantId));
        }
        if (scope != null && !scope.isEmpty()) {
            return R.ok(iamService.menuTreeByScope(scope.toUpperCase()));
        }
        return R.ok(iamService.menuTree());
    }

    /** 当前登录用户在其租户下实际可见的菜单 */
    @GetMapping("/my")
    public R<List<SysMenu>> my(@RequestParam(required = false) String tenantId) {
        LoginUser current = LoginContext.current();
        String tid = tenantId == null || tenantId.isEmpty() ? current.getTenantId() : tenantId;
        return R.ok(iamService.menuTreeForUser(current.getAccountId(), tid));
    }
}
