package com.sfi.matrix.iam.service;

import com.sfi.matrix.iam.config.PlatformInitializer;
import com.sfi.matrix.iam.constant.IamConstants;
import com.sfi.matrix.iam.entity.*;
import com.sfi.matrix.iam.repository.*;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.starter.data.jpa.datascope.DataScopeType;
import com.sfi.scaffold.starter.security.identity.LoginUser;
import com.sfi.scaffold.starter.security.token.SessionInfo;
import com.sfi.scaffold.starter.tenant.annotation.TenantIgnore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * IAM 核心服务：租户、套餐、用户、部门、角色、菜单、授权，以及"一人多租户"的身份装配。
 *
 * <p>登录分两步：① 校验密码后列出该账号可登录的租户；② 选定租户后按该租户组装
 * roles/permissions 写进 LoginUser。切换租户复用第②步。权限统一由 {@link #resolveMenuIds}
 * 推导：内置超管（PLATFORM_ADMIN/TENANT_ADMIN）按 scope/套餐动态取菜单、不落库；
 * 普通角色取 sys_role_menu 再按租户类型裁剪。会话存取、租户过滤、权限拦截由 scaffold 承担。</p>
 *
 * <p>注意：登录/切租户发生在无登录态阶段，scaffold 的租户 @Filter 不生效，
 * 因此本类所有按租户的查询都<b>显式带 tenantId 条件</b>，不依赖过滤器。</p>
 *
 * @author matrix
 */
@Service
public class IamService {

    private final SysTenantRepository tenantRepository;
    private final SysTenantPackageRepository packageRepository;
    private final SysPackageMenuRepository packageMenuRepository;
    private final SysUserRepository userRepository;
    private final SysUserTenantRepository userTenantRepository;
    private final SysDeptRepository deptRepository;
    private final SysRoleRepository roleRepository;
    private final SysMenuRepository menuRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysRoleMenuRepository roleMenuRepository;

    /** 每个租户内置的管理员角色编码（建租户时自动创建，作为该租户的功能基线） */
    public static final String TENANT_ADMIN_ROLE_CODE = IamConstants.TENANT_ADMIN_ROLE_CODE;

    public IamService(SysTenantRepository tenantRepository,
                      SysTenantPackageRepository packageRepository,
                      SysPackageMenuRepository packageMenuRepository,
                      SysUserRepository userRepository,
                      SysUserTenantRepository userTenantRepository,
                      SysDeptRepository deptRepository,
                      SysRoleRepository roleRepository,
                      SysMenuRepository menuRepository,
                      SysUserRoleRepository userRoleRepository,
                      SysRoleMenuRepository roleMenuRepository) {
        this.tenantRepository = tenantRepository;
        this.packageRepository = packageRepository;
        this.packageMenuRepository = packageMenuRepository;
        this.userRepository = userRepository;
        this.userTenantRepository = userTenantRepository;
        this.deptRepository = deptRepository;
        this.roleRepository = roleRepository;
        this.menuRepository = menuRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleMenuRepository = roleMenuRepository;
    }

    // ===== 登录身份装配（一人多租户） =====

    /** 取密码 hash 供 controller 校验 */
    public String passwordHash(String username) {
        return userRepository.findByUsernameAndDeletedFalse(username)
                .map(SysUser::getPassword).orElse(null);
    }

    /**
     * 第一步：列出账号可登录的租户（controller 校验密码通过后调用）。
     * 未找到账号返回空列表。
     */
    public List<SysTenant> listLoginTenants(String username) {
        SysUser user = userRepository.findByUsernameAndDeletedFalse(username).orElse(null);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            return Collections.emptyList();
        }
        List<Long> tenantIds = userTenantRepository.findByUserIdAndDeletedFalse(user.getId()).stream()
                .filter(ut -> Integer.valueOf(1).equals(ut.getStatus()))
                .map(ut -> Long.valueOf(ut.getTenantId()))
                .collect(Collectors.toList());
        if (tenantIds.isEmpty()) {
            return Collections.emptyList();
        }
        return tenantRepository.findByIdInAndDeletedFalse(tenantIds).stream()
                .filter(t -> Integer.valueOf(1).equals(t.getStatus()))
                .filter(t -> t.getExpireTime() == null || !t.getExpireTime().isBefore(LocalDate.now()))
                .collect(Collectors.toList());
    }

    /** 第二步：按用户名 + 租户编码组装登录身份 */
    public LoginUser buildLoginUser(String username, String tenantCode) {
        SysUser user = userRepository.findByUsernameAndDeletedFalse(username).orElse(null);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "账号不存在或已禁用");
        }
        SysTenant tenant = resolveActiveTenant(tenantCode);
        return assemble(user, tenant);
    }

    /**
     * 切换租户：按账号 id + 租户编码重新组装身份（已认证用户，不再校验密码）。
     * 标记 @TenantIgnore：切换发生在旧租户登录态下，必须跨旧租户过滤去查目标租户的归属/角色/权限。
     */
    @TenantIgnore
    public LoginUser switchTenant(Long userId, String tenantCode) {
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> BizException.of(GlobalErrorCode.UNAUTHORIZED, "账号不存在"));
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "账号已禁用");
        }
        SysTenant tenant = resolveActiveTenant(tenantCode);
        return assemble(user, tenant);
    }

    private SysTenant resolveActiveTenant(String tenantCode) {
        SysTenant tenant = tenantRepository.findByTenantCodeAndDeletedFalse(tenantCode)
                .orElseThrow(() -> BizException.of(GlobalErrorCode.UNAUTHORIZED, "租户不存在"));
        if (!Integer.valueOf(1).equals(tenant.getStatus())) {
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "租户已停用");
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDate.now())) {
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "租户已过期");
        }
        return tenant;
    }

    /** 核心装配：校验归属 + 算角色 + 算权限（角色菜单 ∩ 套餐菜单） */
    private LoginUser assemble(SysUser user, SysTenant tenant) {
        SysUserTenant membership = userTenantRepository
                .findByUserIdAndTenantIdAndDeletedFalse(user.getId(), tenant.getId().toString())
                .orElseThrow(() -> BizException.of(GlobalErrorCode.UNAUTHORIZED, "账号未开通该租户"));
        if (!Integer.valueOf(1).equals(membership.getStatus())) {
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "账号在该租户下已停用");
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setAccountId(user.getId());
        loginUser.setAccountNo(user.getUsername());
        loginUser.setDisplayName(user.getRealName() == null ? user.getUsername() : user.getRealName());
        loginUser.setTenantId(tenant.getId().toString());
        loginUser.setRoles(new HashSet<>(userRoleCodes(user.getId(), tenant.getId().toString())));
        loginUser.setPermissions(userPermissions(user.getId(), tenant.getId().toString()));
        // 部门归属随会话透传（展示/审计用）；空值写空串，保持 attributes 键稳定
        loginUser.getAttributes().put(IamConstants.ATTR_DEPT_ID,
                membership.getDeptId() == null ? "" : membership.getDeptId().toString());
        return loginUser;
    }

    /** 供 controller 组装 SessionInfo */
    public SessionInfo sessionOf(LoginUser loginUser) {
        return SessionInfo.from(loginUser);
    }

    // ===== 租户 =====

    /**
     * 平台层：建租户 + 自动初始化该租户的管理员。
     *
     * <p>一次事务内完成 5 步：① 建租户；② 建内置管理员角色（built_in，编码
     * {@link #TENANT_ADMIN_ROLE_CODE}，roleType=TENANT_ADMIN）；③ 建管理员账号；
     * ④ 挂租户归属；⑤ 授管理员角色。</p>
     *
     * <p><b>关键：管理员的菜单不再落 sys_role_menu。</b>roleType=TENANT_ADMIN 是隐式角色，
     * 权限推导时直接取本租户套餐内菜单（见 {@link #resolveMenuIds}）。这样平台后续给套餐加菜单，
     * 已存在租户的管理员自动生效，无需重建角色授权、也无需同步任务。</p>
     *
     * <p>adminUsername 为空时只建租户、不建管理员（兼容仅需租户壳的场景）。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public SysTenant createTenant(String tenantCode, String tenantName, Long packageId,
                                  LocalDate expireTime, String remark,
                                  String adminUsername, String encodedAdminPassword, String adminName) {
        tenantRepository.findByTenantCodeAndDeletedFalse(tenantCode).ifPresent(t -> {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "租户编码已存在：" + tenantCode);
        });
        SysTenant tenant = new SysTenant();
        tenant.setTenantCode(tenantCode);
        tenant.setTenantName(tenantName);
        tenant.setPackageId(packageId);
        tenant.setExpireTime(expireTime);
        tenant.setRemark(remark);
        tenant.setStatus(1);
        tenant = tenantRepository.save(tenant);
        String tenantId = tenant.getId().toString();

        if (adminUsername != null && !adminUsername.trim().isEmpty()) {
            // 内置租户管理员角色：隐式权限，菜单不落库
            SysRole adminRole = new SysRole();
            adminRole.setRoleCode(TENANT_ADMIN_ROLE_CODE);
            adminRole.setRoleName("租户管理员");
            adminRole.setTenantId(tenantId);
            adminRole.setRoleType(IamConstants.ROLE_TYPE_TENANT_ADMIN);
            adminRole.setBuiltIn(Boolean.TRUE);
            adminRole.setSortNo(0);
            adminRole = roleRepository.save(adminRole);

            // 管理员账号（编码后密码由 controller 传入）
            SysUser admin = new SysUser();
            admin.setUsername(adminUsername);
            admin.setPassword(encodedAdminPassword);
            admin.setRealName(adminName);
            admin.setStatus(1);
            admin = userRepository.save(admin);

            // 挂租户归属 + 授管理员角色（角色归属要落库，菜单授权不落库）
            SysUserTenant ut = new SysUserTenant();
            ut.setUserId(admin.getId());
            ut.setTenantId(tenantId);
            ut.setStatus(1);
            userTenantRepository.save(ut);

            SysUserRole ur = new SysUserRole();
            ur.setUserId(admin.getId());
            ur.setRoleId(adminRole.getId());
            ur.setTenantId(tenantId);
            userRoleRepository.save(ur);
        }
        return tenant;
    }

    /** 取某租户套餐允许的菜单ID集合；未配套餐返回空集合 */
    private List<Long> packageMenuIdsOfTenant(String tenantId) {
        SysTenant tenant = tenantRepository.findById(Long.valueOf(tenantId)).orElse(null);
        if (tenant == null || tenant.getPackageId() == null) {
            return Collections.emptyList();
        }
        return packageMenuRepository.findByPackageIdAndDeletedFalse(tenant.getPackageId()).stream()
                .map(SysPackageMenu::getMenuId)
                .collect(Collectors.toList());
    }

    /** 平台层租户列表：排除虚拟 PLATFORM 租户，避免它被当成业务租户改动 */
    public List<SysTenant> listTenants() {
        return tenantRepository.findAll().stream()
                .filter(t -> !Boolean.TRUE.equals(t.getDeleted()))
                .filter(t -> !IamConstants.PLATFORM_TENANT_CODE.equals(t.getTenantCode()))
                .collect(Collectors.toList());
    }

    // ===== 套餐 =====

    @Transactional(rollbackFor = Exception.class)
    public SysTenantPackage createPackage(String packageCode, String packageName, String remark) {
        packageRepository.findByPackageCodeAndDeletedFalse(packageCode).ifPresent(p -> {
            throw new IllegalArgumentException("套餐编码已存在：" + packageCode);
        });
        SysTenantPackage pkg = new SysTenantPackage();
        pkg.setPackageCode(packageCode);
        pkg.setPackageName(packageName);
        pkg.setRemark(remark);
        pkg.setStatus(1);
        return packageRepository.save(pkg);
    }

    public List<SysTenantPackage> listPackages() {
        return packageRepository.findAll().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignPackageMenus(Long packageId, List<Long> menuIds) {
        // 套餐只能装 TENANT/COMMON 菜单，PLATFORM 运营平台菜单不进租户套餐
        List<Long> tenantMenuIds = filterAssignableMenuIds(menuIds);
        packageMenuRepository.deleteByPackageId(packageId);
        for (Long menuId : tenantMenuIds) {
            SysPackageMenu pm = new SysPackageMenu();
            pm.setPackageId(packageId);
            pm.setMenuId(menuId);
            packageMenuRepository.save(pm);
        }
    }

    /** 过滤出可进套餐/可分配的菜单：排除 PLATFORM 范围 */
    private List<Long> filterAssignableMenuIds(List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return Collections.emptyList();
        }
        return menuRepository.findByIdInAndDeletedFalse(menuIds).stream()
                .filter(m -> !IamConstants.MENU_SCOPE_PLATFORM.equals(m.getMenuScope()))
                .map(SysMenu::getId)
                .collect(Collectors.toList());
    }

    // ===== 用户 =====

    @Transactional(rollbackFor = Exception.class)
    public SysUser createUser(String username, String encodedPassword, String realName) {
        userRepository.findByUsernameAndDeletedFalse(username).ifPresent(u -> {
            throw new IllegalArgumentException("用户名已存在：" + username);
        });
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setRealName(realName);
        user.setStatus(1);
        return userRepository.save(user);
    }

    public List<SysUser> listUsers() {
        return userRepository.findAll().stream()
                .filter(u -> !Boolean.TRUE.equals(u.getDeleted()))
                .collect(Collectors.toList());
    }

    /** 给用户开通租户归属（可带该租户下的部门） */
    @Transactional(rollbackFor = Exception.class)
    public SysUserTenant assignTenant(Long userId, String tenantId, Long deptId) {
        userTenantRepository.findByUserIdAndTenantIdAndDeletedFalse(userId, tenantId)
                .ifPresent(ut -> {
                    throw new IllegalArgumentException("该账号已开通此租户");
                });
        SysUserTenant ut = new SysUserTenant();
        ut.setUserId(userId);
        ut.setTenantId(tenantId);
        ut.setDeptId(deptId);
        ut.setStatus(1);
        return userTenantRepository.save(ut);
    }

    public List<SysUserTenant> userTenants(Long userId) {
        return userTenantRepository.findByUserIdAndDeletedFalse(userId);
    }

    // ===== 部门 =====

    @Transactional(rollbackFor = Exception.class)
    public SysDept createDept(String deptName, Long parentId, String tenantId, Integer sortNo) {
        SysDept dept = new SysDept();
        dept.setDeptName(deptName);
        dept.setParentId(parentId);
        dept.setTenantId(tenantId);
        dept.setSortNo(sortNo == null ? 0 : sortNo);
        dept.setStatus(1);
        return deptRepository.save(dept);
    }

    public List<SysDept> listDepts() {
        return deptRepository.findByDeletedFalseOrderBySortNoAsc();
    }

    // ===== 角色 =====

    public SysRole createRole(String roleCode, String roleName, String tenantId) {
        return createRole(roleCode, roleName, tenantId, DataScopeType.ALL.name());
    }

    @Transactional(rollbackFor = Exception.class)
    public SysRole createRole(String roleCode, String roleName, String tenantId, String dataScope) {
        // 内置超管编码保留，普通创建不得占用（否则可被拿来伪装隐式超管）
        if (IamConstants.TENANT_ADMIN_ROLE_CODE.equals(roleCode)
                || IamConstants.PLATFORM_ADMIN_ROLE_CODE.equals(roleCode)) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "该角色编码为系统内置，不可手动创建：" + roleCode);
        }
        roleRepository.findByRoleCodeAndTenantIdAndDeletedFalse(roleCode, tenantId).ifPresent(r -> {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "角色编码已存在：" + roleCode);
        });
        String scope = (dataScope == null || dataScope.trim().isEmpty())
                ? DataScopeType.ALL.name() : dataScope.trim();
        if (!DataScopeType.isValid(scope)) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "数据范围非法：" + dataScope
                    + "，可选值：" + DataScopeType.CODES);
        }
        scope = DataScopeType.of(scope).name();
        SysRole role = new SysRole();
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setTenantId(tenantId);
        role.setRoleType(IamConstants.ROLE_TYPE_NORMAL);
        role.setSortNo(0);
        role.setBuiltIn(Boolean.FALSE);
        role.setDataScope(scope);
        return roleRepository.save(role);
    }

    public List<SysRole> listRoles() {
        return roleRepository.findByDeletedFalseOrderBySortNoAsc();
    }

    // ===== 菜单（全局定义） =====

    @Transactional(rollbackFor = Exception.class)
    public SysMenu createMenu(String menuName, String menuType, Long parentId, String permission, int sortNo) {
        return createMenu(menuName, menuType, parentId, permission, IamConstants.MENU_SCOPE_TENANT, sortNo);
    }

    /** 带范围创建菜单（平台种子初始化用） */
    @Transactional(rollbackFor = Exception.class)
    public SysMenu createMenu(String menuName, String menuType, Long parentId, String permission,
                              String menuScope, int sortNo) {
        SysMenu menu = new SysMenu();
        menu.setMenuName(menuName);
        menu.setMenuType(menuType);
        menu.setParentId(parentId);
        menu.setPermission(permission);
        menu.setMenuScope(menuScope);
        menu.setSortNo(sortNo);
        menu.setVisible(Boolean.TRUE);
        return menuRepository.save(menu);
    }

    /** 按权限标识查菜单（种子幂等判断） */
    public SysMenu findMenuByPermission(String permission) {
        return menuRepository.findByPermissionAndDeletedFalse(permission).orElse(null);
    }

    /** 平台层：查看全部全局菜单定义（用于配套餐） */
    public List<SysMenu> menuTree() {
        return menuRepository.findByDeletedFalseOrderBySortNoAsc();
    }

    /** 按单个范围查菜单（平台菜单维护用） */
    public List<SysMenu> menuTreeByScope(String scope) {
        return menuRepository.findByMenuScopeInAndDeletedFalseOrderBySortNoAsc(
                Collections.singletonList(scope));
    }

    /**
     * 租户层：租户管理员可见的菜单范围 = 本租户套餐内的菜单。
     * 未配套餐时返回空（租户没有任何可用菜单，需平台先配套餐）。
     */
    public List<SysMenu> menuTreeOfTenant(String tenantId) {
        List<Long> allowed = packageMenuIdsOfTenant(tenantId);
        if (allowed.isEmpty()) {
            return Collections.emptyList();
        }
        return menuRepository.findByIdInAndDeletedFalse(allowed).stream()
                .sorted(Comparator.comparing(SysMenu::getSortNo,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    // ===== 授权 =====

    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds, String tenantId) {
        userRoleRepository.deleteByUserIdAndTenantId(userId, tenantId);
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            ur.setTenantId(tenantId);
            userRoleRepository.save(ur);
        }
    }

    /**
     * 租户层：给本租户角色配菜单。
     *
     * <p>硬约束：所配菜单必须全部落在本租户套餐范围内，超出即拒绝（租户管理员的分配上限
     * 就是它自己租户的套餐）。套餐未配置（packageId 为空）时放行，兼容未上套餐的租户。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds, String tenantId) {
        // 1) 角色必须属于该租户，防止跨租户给别人的角色授权
        SysRole role = roleRepository.findById(roleId)
                .orElseThrow(() -> BizException.of(GlobalErrorCode.NOT_FOUND, "角色不存在：" + roleId));
        if (!tenantId.equals(role.getTenantId())) {
            throw BizException.of(GlobalErrorCode.FORBIDDEN, "角色不属于该租户，拒绝跨租户授权");
        }
        // 1.1) 内置超管是隐式权限，菜单由 scope/套餐动态推导，不允许手工分配
        if (isSuperRole(role)) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST,
                    "内置超管角色的菜单由系统自动推导，不可手动分配：" + role.getRoleCode());
        }
        // 2) 套餐范围校验：租户管理员的分配上限就是本租户套餐
        SysTenant tenant = tenantRepository.findById(Long.valueOf(tenantId)).orElse(null);
        if (tenant != null && tenant.getPackageId() != null) {
            Set<Long> allowed = packageMenuIdsOfTenant(tenantId).stream().collect(Collectors.toSet());
            List<Long> outOfScope = menuIds.stream().filter(id -> !allowed.contains(id)).collect(Collectors.toList());
            if (!outOfScope.isEmpty()) {
                throw BizException.of(GlobalErrorCode.BAD_REQUEST,
                        "菜单超出租户套餐范围，拒绝分配：" + outOfScope);
            }
        }
        // 3) 写入授权
        grantMenus(roleId, menuIds, tenantId);
    }

    /** 直接写入角色-菜单授权（内部方法：建租户初始化、已校验的租户授权共用） */
    private void grantMenus(Long roleId, List<Long> menuIds, String tenantId) {
        roleMenuRepository.deleteByRoleIdAndTenantId(roleId, tenantId);
        for (Long menuId : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            rm.setTenantId(tenantId);
            roleMenuRepository.save(rm);
        }
    }

    // ===== 权限计算（显式按 tenantId 隔离） =====

    public List<String> userRoleCodes(Long userId, String tenantId) {
        List<Long> roleIds = userRoleRepository.findByUserIdAndTenantIdAndDeletedFalse(userId, tenantId).stream()
                .map(SysUserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleRepository.findByIdInAndDeletedFalse(roleIds).stream()
                .filter(r -> tenantId.equals(r.getTenantId()))
                .map(SysRole::getRoleCode).collect(Collectors.toList());
    }

    /**
     * 统一菜单范围解析器：算出该用户在该租户下最终可见的菜单 ID。
     *
     * <p>全部权限出口（接口权限点、菜单树）都走这里，避免各算各的产生"看得见点不了"。
     * 按角色类型分流：</p>
     * <ul>
     *   <li>PLATFORM_ADMIN：全部 PLATFORM + COMMON 菜单（平台运营能力，动态、不落库）；</li>
     *   <li>TENANT_ADMIN：本租户套餐内菜单（动态、不落库，套餐加菜单自动生效）；</li>
     *   <li>NORMAL：sys_role_menu 显式授权，再按租户类型裁剪（租户∩套餐 / 平台∩平台菜单）；</li>
     *   <li>无角色：空集合。</li>
     * </ul>
     * <p>用户若同时有多个角色，按"最宽"口径取并集（超管优先）。</p>
     */
    public Set<Long> resolveMenuIds(Long userId, String tenantId) {
        List<SysRole> roles = userRolesOf(userId, tenantId);
        if (roles.isEmpty()) {
            return Collections.emptySet();
        }
        SysTenant tenant = tenantRepository.findById(Long.valueOf(tenantId)).orElse(null);
        boolean platformTenant = tenant != null
                && IamConstants.PLATFORM_TENANT_CODE.equals(tenant.getTenantCode());

        Set<Long> result = new HashSet<>();

        // ① 平台超管：全部平台菜单（仅平台租户下生效）
        if (platformTenant && roles.stream().anyMatch(r -> isType(r, IamConstants.ROLE_TYPE_PLATFORM_ADMIN))) {
            result.addAll(allMenuIdsOfScopes(IamConstants.MENU_SCOPE_PLATFORM, IamConstants.MENU_SCOPE_COMMON));
        }

        // ② 租户超管：套餐内菜单（动态）
        if (!platformTenant && roles.stream().anyMatch(r -> isType(r, IamConstants.ROLE_TYPE_TENANT_ADMIN))) {
            result.addAll(packageMenuIdsOfTenant(tenantId));
        }

        // ③ 普通角色：显式授权 ∩ 可见范围
        List<Long> normalRoleIds = roles.stream()
                .filter(r -> !isSuperRole(r))
                .map(SysRole::getId).collect(Collectors.toList());
        if (!normalRoleIds.isEmpty()) {
            Set<Long> granted = roleMenuRepository.findByRoleIdInAndDeletedFalse(normalRoleIds).stream()
                    .filter(rm -> tenantId.equals(rm.getTenantId()) && !Boolean.TRUE.equals(rm.getDeleted()))
                    .map(SysRoleMenu::getMenuId).collect(Collectors.toSet());
            if (!granted.isEmpty()) {
                Set<Long> visible = platformTenant
                        ? allMenuIdsOfScopes(IamConstants.MENU_SCOPE_PLATFORM, IamConstants.MENU_SCOPE_COMMON)
                        : packageMenuIdsOfTenant(tenantId).stream().collect(Collectors.toSet());
                // 套餐未配置（visible 为空）时兼容：普通角色按已授权全给，不裁剪
                if (!visible.isEmpty()) {
                    granted.retainAll(visible);
                }
                result.addAll(granted);
            }
        }
        return result;
    }

    /** 用户权限 = 解析出的菜单 ID 转权限标识 */
    public Set<String> userPermissions(Long userId, String tenantId) {
        Set<Long> menuIds = resolveMenuIds(userId, tenantId);
        if (menuIds.isEmpty()) {
            return Collections.emptySet();
        }
        return menuRepository.findAllById(menuIds).stream()
                .map(SysMenu::getPermission)
                .filter(p -> p != null && !p.isEmpty())
                .collect(Collectors.toSet());
    }

    /** 当前用户在该租户可见的菜单树（平台/租户/普通角色统一走解析器） */
    public List<SysMenu> menuTreeForUser(Long userId, String tenantId) {
        Set<Long> menuIds = resolveMenuIds(userId, tenantId);
        if (menuIds.isEmpty()) {
            return Collections.emptyList();
        }
        return menuRepository.findByIdInAndDeletedFalse(new ArrayList<>(menuIds)).stream()
                .sorted(Comparator.comparing(SysMenu::getSortNo,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    // ===== 平台域（虚拟 PLATFORM 租户 + 内置平台超管） =====

    /** 平台租户 ID 缓存：运行期不变，避免字典/参数等高频兜底查询反复查库 */
    private volatile String platformTenantIdCache;

    /**
     * 返回唯一平台租户的 ID（字符串，与 sys_dict/sys_config.tenant_id 类型一致）。
     *
     * <p><b>用途：默认配置兜底。</b>字典、参数等模块按本租户 tenantId 查不到配置时，
     * 用本方法拿到平台租户 ID 再查一次，取平台那份作为全局默认值。</p>
     *
     * <p>标 {@link TenantIgnore}：调用方可能处于任意租户上下文，必须跨租户过滤按编码定位平台租户。
     * 平台租户由 {@link PlatformInitializer} 启动时幂等创建，正常情况下必然存在。</p>
     */
    @TenantIgnore
    public String getPlatformTenantId() {
        String cached = platformTenantIdCache;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (platformTenantIdCache == null) {
                SysTenant platform = tenantRepository
                        .findByTenantCodeAndDeletedFalse(IamConstants.PLATFORM_TENANT_CODE)
                        .orElseThrow(() -> BizException.of(GlobalErrorCode.SYSTEM_ERROR,
                                "平台租户未初始化，无法提供默认配置"));
                platformTenantIdCache = platform.getId().toString();
            }
            return platformTenantIdCache;
        }
    }

    /**
     * 初始化运营平台域（幂等）：创建虚拟 PLATFORM 租户 + PLATFORM_ADMIN 内置角色。
     * 平台管理员账号由调用方在此租户下建账号并授角色（见 initPlatformAdmin）。
     */
    @Transactional(rollbackFor = Exception.class)
    public SysTenant initPlatformDomain() {
        SysTenant platform = tenantRepository.findByTenantCodeAndDeletedFalse(IamConstants.PLATFORM_TENANT_CODE)
                .orElse(null);
        if (platform == null) {
            platform = new SysTenant();
            platform.setTenantCode(IamConstants.PLATFORM_TENANT_CODE);
            platform.setTenantName("运营平台");
            platform.setStatus(1);
            platform = tenantRepository.save(platform);
        }
        String tenantId = platform.getId().toString();
        roleRepository.findByRoleCodeAndTenantIdAndDeletedFalse(
                IamConstants.PLATFORM_ADMIN_ROLE_CODE, tenantId).orElseGet(() -> {
            SysRole role = new SysRole();
            role.setRoleCode(IamConstants.PLATFORM_ADMIN_ROLE_CODE);
            role.setRoleName("平台管理员");
            role.setTenantId(tenantId);
            role.setRoleType(IamConstants.ROLE_TYPE_PLATFORM_ADMIN);
            role.setBuiltIn(Boolean.TRUE);
            role.setSortNo(0);
            return roleRepository.save(role);
        });
        return platform;
    }

    /**
     * 初始化平台超管账号（幂等）：建账号 → 挂 PLATFORM 租户 → 授 PLATFORM_ADMIN 角色。
     * 菜单不落库，权限由 roleType 动态推导。
     */
    @Transactional(rollbackFor = Exception.class)
    public SysUser initPlatformAdmin(String username, String encodedPassword, String realName) {
        SysTenant platform = initPlatformDomain();
        String tenantId = platform.getId().toString();
        SysRole adminRole = roleRepository.findByRoleCodeAndTenantIdAndDeletedFalse(
                IamConstants.PLATFORM_ADMIN_ROLE_CODE, tenantId)
                .orElseThrow(() -> BizException.of(GlobalErrorCode.SYSTEM_ERROR, "平台管理员角色未初始化"));

        SysUser admin = userRepository.findByUsernameAndDeletedFalse(username).orElseGet(() -> {
            SysUser u = new SysUser();
            u.setUsername(username);
            u.setPassword(encodedPassword);
            u.setRealName(realName);
            u.setStatus(1);
            return userRepository.save(u);
        });
        userTenantRepository.findByUserIdAndTenantIdAndDeletedFalse(admin.getId(), tenantId).orElseGet(() -> {
            SysUserTenant ut = new SysUserTenant();
            ut.setUserId(admin.getId());
            ut.setTenantId(tenantId);
            ut.setStatus(1);
            return userTenantRepository.save(ut);
        });
        List<SysUserRole> exists = userRoleRepository.findByUserIdAndTenantIdAndDeletedFalse(admin.getId(), tenantId);
        if (exists.stream().noneMatch(ur -> adminRole.getId().equals(ur.getRoleId()))) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(admin.getId());
            ur.setRoleId(adminRole.getId());
            ur.setTenantId(tenantId);
            userRoleRepository.save(ur);
        }
        return admin;
    }

    /**
     * 平台超管"进入"业务租户：不写 sys_user_tenant / sys_user_role，直接以目标租户的
     * TENANT_ADMIN 身份装一个临时 LoginUser，仅用于本次签发 token（审计用 attributes 留痕）。
     * 调用方必须先校验当前身份确为平台超管。
     */
    @TenantIgnore
    public LoginUser impersonateTenant(Long platformAdminId, String tenantCode) {
        SysTenant tenant = resolveActiveTenant(tenantCode);
        if (IamConstants.PLATFORM_TENANT_CODE.equals(tenant.getTenantCode())) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "平台租户无需进入");
        }
        SysUser admin = userRepository.findById(platformAdminId)
                .orElseThrow(() -> BizException.of(GlobalErrorCode.UNAUTHORIZED, "账号不存在"));
        String tenantId = tenant.getId().toString();

        LoginUser loginUser = new LoginUser();
        loginUser.setAccountId(admin.getId());
        loginUser.setAccountNo(admin.getUsername());
        loginUser.setDisplayName(admin.getRealName() == null ? admin.getUsername() : admin.getRealName());
        loginUser.setTenantId(tenantId);
        loginUser.setRoles(new HashSet<>(Collections.singletonList(TENANT_ADMIN_ROLE_CODE)));
        loginUser.setPermissions(userPermissionsOfSuper(IamConstants.ROLE_TYPE_TENANT_ADMIN, tenantId));
        loginUser.getAttributes().put("impersonatedFrom", IamConstants.PLATFORM_TENANT_CODE);
        return loginUser;
    }

    /** 超管权限快捷计算：按角色类型直接取对应菜单的权限标识（不经用户角色关联） */
    private Set<String> userPermissionsOfSuper(String roleType, String tenantId) {
        Set<Long> menuIds = IamConstants.ROLE_TYPE_PLATFORM_ADMIN.equals(roleType)
                ? allMenuIdsOfScopes(IamConstants.MENU_SCOPE_PLATFORM, IamConstants.MENU_SCOPE_COMMON)
                : packageMenuIdsOfTenant(tenantId).stream().collect(Collectors.toSet());
        if (menuIds.isEmpty()) {
            return Collections.emptySet();
        }
        return menuRepository.findAllById(menuIds).stream()
                .map(SysMenu::getPermission)
                .filter(p -> p != null && !p.isEmpty())
                .collect(Collectors.toSet());
    }

    /** 校验指定用户在该租户下是否持有平台超管角色，否则拒绝（供 enter-tenant 前置校验） */
    public void assertPlatformAdmin(Long userId, String tenantId) {
        boolean ok = userRolesOf(userId, tenantId).stream()
                .anyMatch(r -> isType(r, IamConstants.ROLE_TYPE_PLATFORM_ADMIN));
        if (!ok) {
            throw BizException.of(GlobalErrorCode.FORBIDDEN, "仅运营平台超级管理员可执行该操作");
        }
    }

    // ===== 内部工具 =====

    /** 用户在该租户下的有效角色（显式带 tenantId，不依赖 @Filter） */
    private List<SysRole> userRolesOf(Long userId, String tenantId) {
        List<Long> roleIds = userRoleRepository.findByUserIdAndTenantIdAndDeletedFalse(userId, tenantId).stream()
                .map(SysUserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleRepository.findByIdInAndDeletedFalse(roleIds).stream()
                .filter(r -> tenantId.equals(r.getTenantId()))
                .collect(Collectors.toList());
    }

    private boolean isType(SysRole role, String roleType) {
        return roleType != null && roleType.equals(role.getRoleType());
    }

    private boolean isSuperRole(SysRole role) {
        return isType(role, IamConstants.ROLE_TYPE_PLATFORM_ADMIN)
                || isType(role, IamConstants.ROLE_TYPE_TENANT_ADMIN);
    }

    private Set<Long> allMenuIdsOfScopes(String... scopes) {
        List<String> scopeList = Arrays.asList(scopes);
        return menuRepository.findByMenuScopeInAndDeletedFalseOrderBySortNoAsc(scopeList).stream()
                .map(SysMenu::getId).collect(Collectors.toSet());
    }
}
