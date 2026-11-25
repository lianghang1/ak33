package com.sfi.matrix.cemetery;

/**
 * 公墓业务常量：菜单权限点。
 *
 * <p>权限点用于 URL 权限拦截（菜单 api_pattern）与编程式校验，读/写分离，
 * 与平台层 dict/config 的权限点设计保持一致。</p>
 *
 * @author lianghang
 */
public final class CemeteryConstants {

    private CemeteryConstants() {}

    /** 公墓数据查询（GET） */
    public static final String PERM_CEMETERY_READ = "cemetery:read";

    /** 公墓数据维护（POST：规划/初始化/改/删） */
    public static final String PERM_CEMETERY_WRITE = "cemetery:write";
}
