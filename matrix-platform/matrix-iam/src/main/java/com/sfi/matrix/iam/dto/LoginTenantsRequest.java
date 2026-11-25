package com.sfi.matrix.iam.dto;

import javax.validation.constraints.NotBlank;

/**
 * 列租户请求：校验账密后返回该账号可登录的租户列表。
 *
 * @author lianghang
 */
public class LoginTenantsRequest {

    @NotBlank(message = "账号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

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
