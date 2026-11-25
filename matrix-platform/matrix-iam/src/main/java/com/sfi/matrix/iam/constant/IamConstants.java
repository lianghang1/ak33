package com.sfi.matrix.iam.constant;

/**
 * IAM 全局常量：角色类型、租户类型、菜单范围、内置编码。
 *
 * <p>集中定义，避免魔法字符串散落在实体 / 服务 / 初始化器里。
 * 约定：特殊角色用 roleType 区分行为，built_in 只表达"系统内置、不可删"。</p>
 *
 * @author lianghang
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

    // ===== 账号类型 sys_user.account_type =====
    /** 内部操作员（账密登录，挂 RBAC 角色） */
    public static final String ACCOUNT_TYPE_OPERATOR = "OPERATOR";
    /** 应用/三方系统（凭据换 token，权限走 scope/RBAC，本阶段不做签名链） */
    public static final String ACCOUNT_TYPE_APP = "APP";
    /** C 端会员（社交授权，本阶段只留类型） */
    public static final String ACCOUNT_TYPE_MEMBER = "MEMBER";

    // ===== 登录身份扩展属性 key（LoginUser.attributes） =====
    /** 当前用户在当前租户下的部门 ID（来自 sys_user_tenant.dept_id） */
    public static final String ATTR_DEPT_ID = "deptId";
    // 数据范围决策的会话键统一用框架 com.sfi.scaffold.common.datascope.DataScopeKeys.ATTR_DECISION，
    // 不在业务侧重复定义，避免与框架漂移。

    // ===== 平台管理权限点 sys_menu.permission（URL 权限拦截用） =====
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
    /** 字典管理（读） */
    public static final String PERM_DICT = "platform:dict:list";
    /** 字典维护（写）——与 support.SupportConstants.PERM_DICT_SAVE 值一致 */
    public static final String PERM_DICT_SAVE = "platform:dict:save";
    /** 参数管理（读） */
    public static final String PERM_CONFIG = "platform:config:list";
    /** 参数维护（写）——与 support.SupportConstants.PERM_CONFIG_SAVE 值一致 */
    public static final String PERM_CONFIG_SAVE = "platform:config:save";
    /** 敏感参数（读明文/改值与标志）——与 support.SupportConstants.PERM_CONFIG_SECRET 值一致 */
    public static final String PERM_CONFIG_SECRET = "platform:config:secret";
    /** 文件管理（读）——与 support.SupportConstants.PERM_FILE 值一致 */
    public static final String PERM_FILE = "platform:file:list";
    /** 文件维护（写）——与 support.SupportConstants.PERM_FILE_SAVE 值一致 */
    public static final String PERM_FILE_SAVE = "platform:file:save";
}
