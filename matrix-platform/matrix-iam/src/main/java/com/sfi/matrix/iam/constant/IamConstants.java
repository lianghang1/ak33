package com.sfi.matrix.iam.constant;

/**
 * IAM 全局常量：角色类型、租户类型、菜单范围、内置编码。
 *
 * <p>集中定义，避免魔法字符串散落在实体 / 服务 / 初始化器里。
 * 约定：特殊角色用 roleType 区分行为，built_in 只表达"系统内置、不可删"。</p>
 *
 * @author matrix
 */
public final class IamConstants {

    private IamConstants() {
    }

    // ===== 角色类型 sys_role.role_type（决定权限推导行为） =====
    /** 运营平台超管：取全部 PLATFORM + COMMON 菜单，动态推导 */
    public static final String ROLE_TYPE_PLATFORM_ADMIN = "PLATFORM_ADMIN";
    /** 租户超管：取本租户套餐内菜单，动态推导 */
    public static final String ROLE_TYPE_TENANT_ADMIN = "TENANT_ADMIN";
    /** 普通角色：取 sys_role_menu 显式授权 */
    public static final String ROLE_TYPE_NORMAL = "NORMAL";

    // ===== 内置角色编码（初始化幂等用；判断行为看 roleType，不看编码） =====
    public static final String PLATFORM_ADMIN_ROLE_CODE = "PLATFORM_ADMIN";
    public static final String TENANT_ADMIN_ROLE_CODE = "TENANT_ADMIN";

    // ===== 平台默认租户 =====
    /**
     * 唯一的虚拟运营平台租户编码。
     * 双重职责：① 承载平台运营能力（租户/套餐/菜单管理）；
     * ② 作为全系统默认配置的归属（字典、参数等租户未配置时回退到此租户查询）。
     */
    public static final String PLATFORM_TENANT_CODE = "PLATFORM";

    // ===== 菜单范围 sys_menu.menu_scope =====
    /** 运营平台菜单：租户管理/套餐/菜单定义等，不进套餐 */
    public static final String MENU_SCOPE_PLATFORM = "PLATFORM";
    /** 租户业务菜单：进套餐、由角色分配 */
    public static final String MENU_SCOPE_TENANT = "TENANT";
    /** 通用菜单：平台与租户都可用（个人中心等） */
    public static final String MENU_SCOPE_COMMON = "COMMON";

    // ===== 登录身份扩展属性 key（LoginUser.attributes） =====
    /** 当前用户在当前租户下的部门 ID（来自 sys_user_tenant.dept_id） */
    public static final String ATTR_DEPT_ID = "deptId";

    // ===== 角色数据范围 sys_role.data_scope（预留字段，执行逻辑待业务表落地后实现） =====
    /** 全部数据（默认）：不拼部门/本人过滤条件，等同 scaffold 的 DataScopeType.ALL */
    public static final String DATA_SCOPE_ALL = "ALL";
    /** 指定部门：按 sys_role_dept 显式授权的部门过滤（关联表待数据权限落地时建） */
    public static final String DATA_SCOPE_DEPT_CUSTOM = "DEPT_CUSTOM";
    /** 本部门：仅当前用户所属部门 */
    public static final String DATA_SCOPE_DEPT_ONLY = "DEPT_ONLY";
    /** 本部门及以下：当前部门 + 递归子部门 */
    public static final String DATA_SCOPE_DEPT_CHILD = "DEPT_CHILD";
    /** 仅本人：按 create_by 匹配，对齐 scaffold 的 DataScopeType.SELF */
    public static final String DATA_SCOPE_SELF = "SELF";

    /** 合法的数据范围取值（供 createRole/updateRole 校验） */
    public static final java.util.Set<String> DATA_SCOPES = java.util.Collections.unmodifiableSet(
            new java.util.HashSet<>(java.util.Arrays.asList(
                    DATA_SCOPE_ALL, DATA_SCOPE_DEPT_CUSTOM, DATA_SCOPE_DEPT_ONLY,
                    DATA_SCOPE_DEPT_CHILD, DATA_SCOPE_SELF)));

    // ===== 平台管理权限点 sys_menu.permission（@RequirePermission 用） =====
    // 与 seedPlatformMenus 一一对应：接口标注的权限点必须在 PLATFORM 菜单里存在，
    // 否则平台超管动态推导时也拿不到、会被 403。
    /** 租户管理 */
    public static final String PERM_TENANT = "platform:tenant:list";
    /** 套餐管理 */
    public static final String PERM_PACKAGE = "platform:package:list";
    /** 用户管理 */
    public static final String PERM_USER = "platform:user:list";
    /** 部门管理 */
    public static final String PERM_DEPT = "platform:dept:list";
    /** 角色管理（含授权） */
    public static final String PERM_ROLE = "platform:role:list";
    /** 菜单管理 */
    public static final String PERM_MENU = "platform:menu:list";
    /** 字典管理 */
    public static final String PERM_DICT = "platform:dict:list";
    /** 参数管理 */
    public static final String PERM_CONFIG = "platform:config:list";
}
