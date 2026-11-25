package com.sfi.matrix.cemetery.service;

import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.security.identity.LoginUser;

/**
 * 当前租户 ID 取值：从登录上下文取，未登录（如初始化/单测）返回 null 由调用方决定兜底。
 *
 * @author lianghang
 */
public final class TenantIdProvider {

    private TenantIdProvider() {}

    public static String current() {
        LoginUser user = LoginContext.currentOrNull();
        return user == null ? null : user.getTenantId();
    }
}
