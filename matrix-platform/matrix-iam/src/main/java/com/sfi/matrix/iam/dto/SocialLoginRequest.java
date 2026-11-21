package com.sfi.matrix.iam.dto;

import javax.validation.constraints.NotBlank;

/**
 * 社交登录请求：provider + 授权 code + 目标租户。
 *
 * @author matrix
 */
public class SocialLoginRequest {

    @NotBlank(message = "登录源不能为空")
    private String provider;

    @NotBlank(message = "授权 code 不能为空")
    private String code;

    @NotBlank(message = "租户编码不能为空")
    private String tenantCode;

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
}
