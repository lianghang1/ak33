package com.sfi.matrix.support.constant;

/**
 * support 全局常量：缓存名与权限点集中定义，避免字面量散落。
 *
 * <p>权限点值与 iam 端已播种的菜单保持一致（platform:dict:list / platform:config:list），
 * 写操作新增细粒度权限由 PlatformInitializer 同步播种。</p>
 *
 * @author matrix
 */
public final class SupportConstants {

    private SupportConstants() {
    }

    // ===== 缓存名（scaffold-starter-cache 两级缓存）=====
    /** 字典类型列表缓存：key = 租户ID */
    public static final String CACHE_DICT_TYPE = "support:dictType";
    /** 字典选项列表缓存：key = 租户ID + 类型 */
    public static final String CACHE_DICT_DATA = "support:dictData";
    /** 系统参数缓存：key = 租户ID + 分组 + 参数键 */
    public static final String CACHE_CONFIG = "support:config";
    /** 系统参数分组列表缓存：key = 租户ID + 分组 */
    public static final String CACHE_CONFIG_GROUP = "support:configGroup";
    /** 默认配置分组（未指定 group 的参数归此组） */
    public static final String CONFIG_GROUP_DEFAULT = "DEFAULT";

    // ===== 平台管理权限点 sys_menu.permission（@RequirePermission 用）=====
    /** 字典管理（读：列表/详情/选项） */
    public static final String PERM_DICT = "platform:dict:list";
    /** 字典维护（写：新增/修改/删除） */
    public static final String PERM_DICT_SAVE = "platform:dict:save";
    /** 参数管理（读：列表/详情/取值） */
    public static final String PERM_CONFIG = "platform:config:list";
    /** 参数维护（写：新增/修改/删除） */
    public static final String PERM_CONFIG_SAVE = "platform:config:save";
}
