package com.sfi.matrix.iam.dto;

import javax.validation.constraints.NotBlank;

/**
 * 租户编码请求：切换租户 / 超管进入租户 / 列租户凭证共用。
 *
 * @author lianghang
 */
public class TenantCodeRequest {

    @NotBlank(message = "租户编码不能为空")
    private String tenantCode;

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }
}
