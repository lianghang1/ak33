package com.sfi.matrix.iam.dto;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求（账密 + 租户编码）。多登录方式后续通过认证策略扩展，本 DTO 承载账密方式。
 *
 * @author matrix
 */
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 选定租户编码；空时走"列租户"流程由前端选择一个 */
    @NotBlank(message = "租户编码不能为空")
    private String tenantCode;

    private String captchaId;
    private String captchaCode;

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
