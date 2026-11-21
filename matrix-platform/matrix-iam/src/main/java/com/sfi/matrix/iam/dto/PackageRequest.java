package com.sfi.matrix.iam.dto;

import javax.validation.constraints.NotBlank;

/** 套餐增改请求。id 为空=新增，非空=修改。 */
public class PackageRequest {

    private Long id;

    @NotBlank(message = "套餐编码不能为空")
    private String packageCode;

    @NotBlank(message = "套餐名称不能为空")
    private String packageName;

    private String remark;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPackageCode() { return packageCode; }
    public void setPackageCode(String packageCode) { this.packageCode = packageCode; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
