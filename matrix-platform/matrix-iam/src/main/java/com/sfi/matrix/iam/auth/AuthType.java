package com.sfi.matrix.iam.auth;

/**
 * 认证方式标识。登录入口统一，具体凭证校验由对应 {@link AuthenticationStrategy} 实现。
 *
 * @author lianghang
 */
public final class AuthType {

    /** 账号 + 密码 */
    public static final String PASSWORD = "PASSWORD";
    /** 手机号 + 短信验证码（预留，未接入） */
    public static final String SMS = "SMS";
    /** 社交授权 code（预留，未接入第三方） */
    public static final String SOCIAL = "SOCIAL";

    private AuthType() {
    }
}
