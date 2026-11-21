package com.sfi.matrix.iam.controller;

import com.sfi.matrix.iam.constant.IamConstants;
import com.sfi.matrix.iam.entity.*;
import com.sfi.matrix.iam.service.IamService;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.starter.security.identity.LoginUser;
import com.sfi.scaffold.starter.security.permission.RequirePermission;
import com.sfi.scaffold.starter.security.policy.PasswordPolicyValidator;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * IAM 能力接口：租户 / 套餐 / 用户 / 部门 / 角色 / 菜单 / 授权（随 jar 复用）。
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

    // ===== 租户 =====
    /** 返回唯一平台租户的 ID（供字典/参数等模块做默认配置兜底；也便于排查） */
    @GetMapping("/platform-tenant-id")
    public R<String> platformTenantId() {
        return R.ok(iamService.getPlatformTenantId());
    }

    /**
     * 平台层：建租户并初始化管理员。传 adminUsername/adminPassword 即自动建管理员账号、
     * 内置管理员角色；不传则只建租户壳。管理员为隐式角色，菜单按套餐动态推导。
     */
    @RequirePermission(IamConstants.PERM_TENANT)
    @PostMapping("/tenant")
    public R<SysTenant> createTenant(@RequestParam String tenantCode,
                                     @RequestParam String tenantName,
                                     @RequestParam(required = false) Long packageId,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expireTime,
                                     @RequestParam(required = false) String remark,
                                     @RequestParam(required = false) String adminUsername,
                                     @RequestParam(required = false) String adminPassword,
                                     @RequestParam(required = false) String adminName) {
        if (adminPassword != null && !adminPassword.isEmpty()) {
            passwordPolicyValidator.validate(adminPassword);
        }
        return R.ok(iamService.createTenant(tenantCode, tenantName, packageId, expireTime, remark,
                adminUsername,
                adminPassword == null || adminPassword.isEmpty() ? null : passwordEncoder.encode(adminPassword),
                adminName));
    }

    @RequirePermission(IamConstants.PERM_TENANT)
    @GetMapping("/tenants")
    public R<List<SysTenant>> tenants() {
        return R.ok(iamService.listTenants());
    }

    // ===== 套餐 =====
    @RequirePermission(IamConstants.PERM_PACKAGE)
    @PostMapping("/package")
    public R<SysTenantPackage> createPackage(@RequestParam String packageCode,
                                             @RequestParam String packageName,
                                             @RequestParam(required = false) String remark) {
        return R.ok(iamService.createPackage(packageCode, packageName, remark));
    }

    @RequirePermission(IamConstants.PERM_PACKAGE)
    @GetMapping("/packages")
    public R<List<SysTenantPackage>> packages() {
        return R.ok(iamService.listPackages());
    }

    @RequirePermission(IamConstants.PERM_PACKAGE)
    @PostMapping("/assign-package-menus")
    public R<Void> assignPackageMenus(@RequestParam Long packageId, @RequestBody List<Long> menuIds) {
        iamService.assignPackageMenus(packageId, menuIds);
        return R.ok();
    }

    // ===== 用户 =====
    @RequirePermission(IamConstants.PERM_USER)
    @PostMapping("/user")
    public R<SysUser> createUser(@RequestParam String username,
                                 @RequestParam String password,
                                 @RequestParam(required = false) String realName) {
        passwordPolicyValidator.validate(password);
        return R.ok(iamService.createUser(username, passwordEncoder.encode(password), realName));
    }

    @RequirePermission(IamConstants.PERM_USER)
    @GetMapping("/users")
    public R<List<SysUser>> users() {
        return R.ok(iamService.listUsers());
    }

    /** 给用户开通租户归属 */
    @RequirePermission(IamConstants.PERM_USER)
    @PostMapping("/assign-tenant")
    public R<SysUserTenant> assignTenant(@RequestParam Long userId,
                                         @RequestParam String tenantId,
                                         @RequestParam(required = false) Long deptId) {
        return R.ok(iamService.assignTenant(userId, tenantId, deptId));
    }

    @RequirePermission(IamConstants.PERM_USER)
    @GetMapping("/user-tenants")
    public R<List<SysUserTenant>> userTenants(@RequestParam Long userId) {
        return R.ok(iamService.userTenants(userId));
    }

    // ===== 部门 =====
    @RequirePermission(IamConstants.PERM_DEPT)
    @PostMapping("/dept")
    public R<SysDept> createDept(@RequestParam String deptName,
                                 @RequestParam(required = false) Long parentId,
                                 @RequestParam String tenantId,
                                 @RequestParam(defaultValue = "0") int sortNo) {
        return R.ok(iamService.createDept(deptName, parentId, tenantId, sortNo));
    }

    @RequirePermission(IamConstants.PERM_DEPT)
    @GetMapping("/depts")
    public R<List<SysDept>> depts() {
        return R.ok(iamService.listDepts());
    }

    // ===== 角色 =====
    @RequirePermission(IamConstants.PERM_ROLE)
    @PostMapping("/role")
    public R<SysRole> createRole(@RequestParam String roleCode,
                                 @RequestParam String roleName,
                                 @RequestParam String tenantId,
                                 @RequestParam(required = false, defaultValue = "ALL") String dataScope) {
        return R.ok(iamService.createRole(roleCode, roleName, tenantId, dataScope));
    }

    @RequirePermission(IamConstants.PERM_ROLE)
    @GetMapping("/roles")
    public R<List<SysRole>> roles() {
        return R.ok(iamService.listRoles());
    }

    // ===== 菜单（全局定义） =====
    @RequirePermission(IamConstants.PERM_MENU)
    @PostMapping("/menu")
    public R<SysMenu> createMenu(@RequestParam String menuName,
                                 @RequestParam String menuType,
                                 @RequestParam(required = false) Long parentId,
                                 @RequestParam(required = false) String permission,
                                 @RequestParam(required = false) String menuScope,
                                 @RequestParam(defaultValue = "0") int sortNo) {
        if (menuScope == null || menuScope.isEmpty()) {
            return R.ok(iamService.createMenu(menuName, menuType, parentId, permission, sortNo));
        }
        return R.ok(iamService.createMenu(menuName, menuType, parentId, permission, menuScope, sortNo));
    }

    /**
     * 菜单查询：
     * <ul>
     *   <li>传 tenantId：该租户套餐范围内的菜单（租户层）；</li>
     *   <li>传 scope：按范围过滤（platform/tenant/common，平台维护用）；</li>
     *   <li>都不传：全部全局菜单。</li>
     * </ul>
     */
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

    /** 当前登录用户在其租户下实际可见的菜单（统一走权限解析器，含隐式超管） */
    @GetMapping("/my-menus")
    public R<List<SysMenu>> myMenus(@RequestParam(required = false) String tenantId) {
        LoginUser current = currentUser();
        String tid = tenantId == null || tenantId.isEmpty() ? current.getTenantId() : tenantId;
        return R.ok(iamService.menuTreeForUser(current.getAccountId(), tid));
    }

    private LoginUser currentUser() {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser) {
            return (LoginUser) auth.getPrincipal();
        }
        throw BizException.of(GlobalErrorCode.UNAUTHORIZED);
    }

    // ===== 授权 =====
    @RequirePermission(IamConstants.PERM_ROLE)
    @PostMapping("/assign-roles")
    public R<Void> assignRoles(@RequestParam Long userId,
                               @RequestBody List<Long> roleIds,
                               @RequestParam String tenantId) {
        iamService.assignRoles(userId, roleIds, tenantId);
        return R.ok();
    }

    @RequirePermission(IamConstants.PERM_ROLE)
    @PostMapping("/assign-menus")
    public R<Void> assignMenus(@RequestParam Long roleId,
                               @RequestBody List<Long> menuIds,
                               @RequestParam String tenantId) {
        iamService.assignMenus(roleId, menuIds, tenantId);
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
}
