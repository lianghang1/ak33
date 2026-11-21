package com.sfi.matrix.iam.dto;

import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

/**
 * 租户增改请求。id 为空=新增，非空=修改。
 *
 * @author matrix
 */
public class TenantRequest {

    private Long id;

    @NotBlank(message = "租户编码不能为空")
    private String tenantCode;

    @NotBlank(message = "租户名称不能为空")
    private String tenantName;

    private Long packageId;
    private LocalDate expireTime;
    private Integer status;
    private String remark;

    // 新增时可选：同时初始化管理员
    private String adminUsername;
    private String adminPassword;
    private String adminName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public LocalDate getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDate expireTime) { this.expireTime = expireTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }
}
