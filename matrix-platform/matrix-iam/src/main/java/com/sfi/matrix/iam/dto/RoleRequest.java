package com.sfi.matrix.iam.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 角色增改请求。id 为空=新增，非空=修改。 */
public class RoleRequest {

    private Long id;

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    @NotNull(message = "租户 ID 不能为空")
    private String tenantId;

    /** 数据范围：ALL/DEPT_CUSTOM/DEPT_ONLY/DEPT_AND_CHILD/SELF */
    private String dataScope;
    /** 自定义部门 ID，逗号分隔（DEPT_CUSTOM 时有效） */
    private String dataScopeDeptIds;

    private Integer sortNo;
    private String remark;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getDataScope() { return dataScope; }
    public void setDataScope(String dataScope) { this.dataScope = dataScope; }
    public String getDataScopeDeptIds() { return dataScopeDeptIds; }
    public void setDataScopeDeptIds(String dataScopeDeptIds) { this.dataScopeDeptIds = dataScopeDeptIds; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
