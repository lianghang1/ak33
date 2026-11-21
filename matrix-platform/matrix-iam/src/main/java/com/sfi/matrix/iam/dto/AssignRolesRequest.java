package com.sfi.matrix.iam.dto;

import javax.validation.constraints.NotNull;
import java.util.List;

/** 给用户分配角色。 */
public class AssignRolesRequest {

    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    @NotNull(message = "租户 ID 不能为空")
    private String tenantId;

    private List<Long> roleIds;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public List<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(List<Long> roleIds) { this.roleIds = roleIds; }
}
