package com.sfi.matrix.iam.controller;

import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.matrix.iam.constant.IamConstants;
import com.sfi.matrix.iam.dto.AssignMenusRequest;
import com.sfi.matrix.iam.dto.AssignRolesRequest;
import com.sfi.matrix.iam.dto.AssignTenantRequest;
import com.sfi.matrix.iam.dto.DeptRequest;
import com.sfi.matrix.iam.dto.IdRequest;
import com.sfi.matrix.iam.dto.MenuRequest;
import com.sfi.matrix.iam.dto.PackageRequest;
import com.sfi.matrix.iam.dto.RoleRequest;
import com.sfi.matrix.iam.dto.TenantRequest;
import com.sfi.matrix.iam.dto.UserRequest;
import com.sfi.matrix.iam.entity.*;
import com.sfi.matrix.iam.service.IamService;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.query.QueryUtilsX;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.security.identity.LoginUser;
import com.sfi.scaffold.starter.security.permission.RequirePermission;
import com.sfi.scaffold.starter.security.policy.PasswordPolicyValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * IAM 能力接口：租户 / 套餐 / 用户 / 部门 / 角色 / 菜单 / 授权（随 jar 复用）。
 *
 * <p>规范：只用 GET / POST；POST 一律 {@code @RequestBody} JSON DTO；
 * 列表 / 分页查询统一走 QueryUtilsX 通路②（JPA 实体），不返回 findAll()。</p>
 *
 * @author matrix
 */
@RestController
@RequestMapping("/api/iam")
public class IamController {

    private final IamService iamService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;

    public IamController(IamService iamService, PasswordEncoder passwordEncoder,
                         PasswordPolicyValidator passwordPolicyValidator) {
        this.iamService = iamService;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    /** 列表分页：从 request 取 map，走 QueryUtilsX 通路②（JPA 实体） */
    private <T> Page<T> query(HttpServletRequest request, Class<T> entityClass) {
        int page = parsePage(request.getParameter("page"), 0);
        int size = parseSize(request.getParameter("size"), 15);
        return QueryUtilsX.doQuery(QueryUtilsX.parse(request.getParameterMap()),
                PageRequest.of(page, size), entityClass);
    }

    private static int parsePage(String v, int def) {
        try { return v == null || v.isEmpty() ? def : Integer.parseInt(v); } catch (NumberFormatException e) { return def; }
    }

    private static int parseSize(String v, int def) {
        try { return v == null || v.isEmpty() ? def : Math.min(Integer.parseInt(v), 200); } catch (NumberFormatException e) { return def; }
    }

    // ===== 租户 =====
    /** 返回唯一平台租户 ID（供字典/参数做默认配置兜底） */
    @GetMapping("/platform-tenant-id")
    public R<String> platformTenantId() {
        return R.ok(iamService.getPlatformTenantId());
    }

    @OperateLog(module = "租户管理", operation = "新增/修改租户")
    @RequirePermission(IamConstants.PERM_TENANT)
    @PostMapping("/tenant")
    public R<SysTenant> saveTenant(@Valid @RequestBody TenantRequest req) {
        if (req.getId() == null) {
            if (req.getAdminPassword() != null && !req.getAdminPassword().isEmpty()) {
                passwordPolicyValidator.validate(req.getAdminPassword());
            }
            return R.ok(iamService.createTenant(req.getTenantCode(), req.getTenantName(), req.getPackageId(),
                    req.getExpireTime(), req.getRemark(), req.getAdminUsername(),
                    req.getAdminPassword() == null || req.getAdminPassword().isEmpty()
                            ? null : passwordEncoder.encode(req.getAdminPassword()), req.getAdminName()));
        }
        return R.ok(iamService.updateTenant(req.getId(), req.getTenantName(), req.getPackageId(),
                req.getExpireTime(), req.getStatus(), req.getRemark()));
    }

    @RequirePermission(IamConstants.PERM_TENANT)
    @GetMapping("/tenant")
    public R<SysTenant> getTenant(@RequestParam Long id) {
        return R.ok(iamService.getTenant(id));
    }

    @RequirePermission(IamConstants.PERM_TENANT)
    @PostMapping("/tenant/delete")
    public R<Void> deleteTenant(@Valid @RequestBody IdRequest req) {
        iamService.deleteTenant(req.getId());
        return R.ok();
    }

    @RequirePermission(IamConstants.PERM_TENANT)
    @GetMapping("/tenants")
    public R<Page<SysTenant>> tenants(HttpServletRequest request) {
        return R.ok(query(request, SysTenant.class));
    }

    // ===== 套餐 =====
    @RequirePermission(IamConstants.PERM_PACKAGE)
    @PostMapping("/package")
    public R<SysTenantPackage> savePackage(@Valid @RequestBody PackageRequest req) {
        if (req.getId() == null) {
            return R.ok(iamService.createPackage(req.getPackageCode(), req.getPackageName(), req.getRemark()));
        }
        return R.ok(iamService.updatePackage(req.getId(), req.getPackageName(), req.getRemark(), req.getStatus()));
    }

    @RequirePermission(IamConstants.PERM_PACKAGE)
    @GetMapping("/package")
    public R<SysTenantPackage> getPackage(@RequestParam Long id) {
        return R.ok(iamService.getPackage(id));
    }

    @RequirePermission(IamConstants.PERM_PACKAGE)
    @PostMapping("/package/delete")
    public R<Void> deletePackage(@Valid @RequestBody IdRequest req) {
        iamService.deletePackage(req.getId());
        return R.ok();
    }

    @RequirePermission(IamConstants.PERM_PACKAGE)
    @GetMapping("/packages")
    public R<Page<SysTenantPackage>> packages(HttpServletRequest request) {
        return R.ok(query(request, SysTenantPackage.class));
    }

    @RequirePermission(IamConstants.PERM_PACKAGE)
    @PostMapping("/assign-package-menus")
    public R<Void> assignPackageMenus(@Valid @RequestBody AssignMenusRequest req) {
        iamService.assignPackageMenus(req.getPackageId(), req.getMenuIds());
        return R.ok();
    }

    // ===== 用户 =====
    @OperateLog(module = "用户管理", operation = "新增/修改用户")
    @RequirePermission(IamConstants.PERM_USER)
    @PostMapping("/user")
    public R<SysUser> saveUser(@Valid @RequestBody UserRequest req) {
        if (req.getId() == null) {
            passwordPolicyValidator.validate(req.getPassword());
            return R.ok(iamService.createUser(req.getUsername(), passwordEncoder.encode(req.getPassword()),
                    req.getNickname()));
        }
        return R.ok(iamService.updateUser(req.getId(), req.getNickname(), req.getMobile(), req.getEmail(),
                req.getAccountType(), req.getStatus()));
    }

    @RequirePermission(IamConstants.PERM_USER)
    @GetMapping("/user")
    public R<SysUser> getUser(@RequestParam Long id) {
        return R.ok(iamService.getUser(id));
    }

    @OperateLog(module = "用户管理", operation = "删除用户")
    @RequirePermission(IamConstants.PERM_USER)
    @PostMapping("/user/delete")
    public R<Void> deleteUser(@Valid @RequestBody IdRequest req) {
        iamService.deleteUser(req.getId());
        return R.ok();
    }

    @RequirePermission(IamConstants.PERM_USER)
    @GetMapping("/users")
    public R<Page<SysUser>> users(HttpServletRequest request) {
        return R.ok(query(request, SysUser.class));
    }

    @RequirePermission(IamConstants.PERM_USER)
    @PostMapping("/assign-tenant")
    public R<SysUserTenant> assignTenant(@Valid @RequestBody AssignTenantRequest req) {
        return R.ok(iamService.assignTenant(req.getUserId(), req.getTenantId(), req.getDeptId()));
    }

    @RequirePermission(IamConstants.PERM_USER)
    @GetMapping("/user-tenants")
    public R<List<SysUserTenant>> userTenants(@RequestParam Long userId) {
        return R.ok(iamService.userTenants(userId));
    }

    // ===== 部门 =====
    @RequirePermission(IamConstants.PERM_DEPT)
    @PostMapping("/dept")
    public R<SysDept> saveDept(@Valid @RequestBody DeptRequest req) {
        if (req.getId() == null) {
            return R.ok(iamService.createDept(req.getDeptName(), req.getParentId(), req.getTenantId(),
                    req.getSortNo() == null ? 0 : req.getSortNo(), req.getLeaderId()));
        }
        return R.ok(iamService.updateDept(req.getId(), req.getDeptName(), req.getParentId(),
                req.getSortNo(), req.getLeaderId(), req.getStatus()));
    }

    @RequirePermission(IamConstants.PERM_DEPT)
    @GetMapping("/dept")
    public R<SysDept> getDept(@RequestParam Long id) {
        return R.ok(iamService.getDept(id));
    }

    @RequirePermission(IamConstants.PERM_DEPT)
    @PostMapping("/dept/delete")
    public R<Void> deleteDept(@Valid @RequestBody IdRequest req) {
        iamService.deleteDept(req.getId());
        return R.ok();
    }

    @RequirePermission(IamConstants.PERM_DEPT)
    @GetMapping("/depts")
    public R<Page<SysDept>> depts(HttpServletRequest request) {
        return R.ok(query(request, SysDept.class));
    }

    // ===== 角色 =====
    @RequirePermission(IamConstants.PERM_ROLE)
    @PostMapping("/role")
    public R<SysRole> saveRole(@Valid @RequestBody RoleRequest req) {
        if (req.getId() == null) {
            return R.ok(iamService.createRole(req.getRoleCode(), req.getRoleName(), req.getTenantId(),
                    req.getDataScope() == null ? "ALL" : req.getDataScope(),
                    req.getDataScopeDeptIds(), req.getSortNo(), req.getRemark()));
        }
        return R.ok(iamService.updateRole(req.getId(), req.getRoleName(), req.getDataScope(),
                req.getDataScopeDeptIds(), req.getSortNo(), req.getRemark()));
    }

    @RequirePermission(IamConstants.PERM_ROLE)
    @GetMapping("/role")
    public R<SysRole> getRole(@RequestParam Long id) {
        return R.ok(iamService.getRole(id));
    }

    @RequirePermission(IamConstants.PERM_ROLE)
    @PostMapping("/role/delete")
    public R<Void> deleteRole(@Valid @RequestBody IdRequest req) {
        iamService.deleteRole(req.getId());
        return R.ok();
    }

    @RequirePermission(IamConstants.PERM_ROLE)
    @GetMapping("/roles")
    public R<Page<SysRole>> roles(HttpServletRequest request) {
        return R.ok(query(request, SysRole.class));
    }

    // ===== 菜单（全局定义） =====
    @RequirePermission(IamConstants.PERM_MENU)
    @PostMapping("/menu")
    public R<SysMenu> saveMenu(@Valid @RequestBody MenuRequest req) {
        if (req.getId() == null) {
            String scope = req.getMenuScope() == null || req.getMenuScope().isEmpty()
                    ? IamConstants.MENU_SCOPE_TENANT : req.getMenuScope();
            return R.ok(iamService.createMenu(req.getMenuName(), req.getMenuType(), req.getParentId(),
                    req.getPermission(), scope, req.getSortNo() == null ? 0 : req.getSortNo()));
        }
        return R.ok(iamService.updateMenu(req.getId(), req.getMenuName(), req.getMenuType(),
                req.getParentId(), req.getPath(), req.getComponent(), req.getIcon(),
                req.getPermission(), req.getMenuScope(), req.getSortNo(), req.getVisible()));
    }

    @RequirePermission(IamConstants.PERM_MENU)
    @GetMapping("/menu")
    public R<SysMenu> getMenu(@RequestParam Long id) {
        return R.ok(iamService.getMenu(id));
    }

    @RequirePermission(IamConstants.PERM_MENU)
    @PostMapping("/menu/delete")
    public R<Void> deleteMenu(@Valid @RequestBody IdRequest req) {
        iamService.deleteMenu(req.getId());
        return R.ok();
    }

    /** 菜单查询： tenantId→套餐范围；scope→按范围；都不传→全部（非分页，菜单量小） */
    @GetMapping("/menus")
    public R<List<SysMenu>> menus(@RequestParam(required = false) String tenantId,
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
    @GetMapping("/my-menus")
    public R<List<SysMenu>> myMenus(@RequestParam(required = false) String tenantId) {
        LoginUser current = currentUser();
        String tid = tenantId == null || tenantId.isEmpty() ? current.getTenantId() : tenantId;
        return R.ok(iamService.menuTreeForUser(current.getAccountId(), tid));
    }

    // ===== 授权 =====
    @OperateLog(module = "角色管理", operation = "分配角色")
    @RequirePermission(IamConstants.PERM_ROLE)
    @PostMapping("/assign-roles")
    public R<Void> assignRoles(@Valid @RequestBody AssignRolesRequest req) {
        iamService.assignRoles(req.getUserId(), req.getRoleIds(), req.getTenantId());
        return R.ok();
    }

    @RequirePermission(IamConstants.PERM_ROLE)
    @PostMapping("/assign-menus")
    public R<Void> assignMenus(@Valid @RequestBody AssignMenusRequest req) {
        iamService.assignMenus(req.getRoleId(), req.getMenuIds(), req.getTenantId());
        return R.ok();
    }

    // ===== 权限计算（带租户维度） =====
    @RequirePermission(IamConstants.PERM_ROLE)
    @GetMapping("/user-roles")
    public R<List<String>> userRoles(@RequestParam Long userId, @RequestParam String tenantId) {
        return R.ok(iamService.userRoleCodes(userId, tenantId));
    }

    @RequirePermission(IamConstants.PERM_ROLE)
    @GetMapping("/user-permissions")
    public R<Set<String>> userPermissions(@RequestParam Long userId, @RequestParam String tenantId) {
        return R.ok(iamService.userPermissions(userId, tenantId));
    }

    private LoginUser currentUser() {
        return LoginContext.current();
    }
}
