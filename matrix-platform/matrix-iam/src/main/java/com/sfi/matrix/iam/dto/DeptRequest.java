package com.sfi.matrix.iam.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 部门增改请求。id 为空=新增，非空=修改。 */
public class DeptRequest {

    private Long id;

    @NotBlank(message = "部门名称不能为空")
    private String deptName;

    private Long parentId;

    @NotNull(message = "租户 ID 不能为空")
    private String tenantId;

    private Integer sortNo;
    private Long leaderId;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public Long getLeaderId() { return leaderId; }
    public void setLeaderId(Long leaderId) { this.leaderId = leaderId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
