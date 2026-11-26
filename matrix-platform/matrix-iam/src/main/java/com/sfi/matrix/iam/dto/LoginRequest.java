package com.sfi.matrix.iam.dto;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求（统一入口）：authType 决定认证方式，tenantCode 决定是否直接签发 token。
 *
 * <p>authType=PASSWORD 传 username/password；authType=SOCIAL 传 provider/code。
 * tenantCode 为空时返回可登录租户列表，非空时选定租户签发 token。</p>
 *
 * @author lianghang
 */
public class LoginRequest {

    /** 认证方式：PASSWORD / SOCIAL */
    @NotBlank(message = "认证方式不能为空")
    private String authType;

    /** 账密方式：账号 */
    private String username;

    /** 账密方式：密码 */
    private String password;

    /** 社交方式：登录源标识 */
    private String provider;

    /** 社交方式：授权 code */
    private String code;

    /** 选定租户编码；空时返回可登录租户列表 */
    private String tenantCode;

    /** 验证码 id（按需） */
    private String captchaId;

    /** 验证码内容（按需） */
    private String captchaCode;

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public String getCaptchaCode() {
        return captchaCode;
    }

    public void setCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
    }
}
