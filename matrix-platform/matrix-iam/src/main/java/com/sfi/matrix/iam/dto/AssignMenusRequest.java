package com.sfi.matrix.iam.dto;

import javax.validation.constraints.NotNull;
import java.util.List;

/** 给角色分配菜单；套餐菜单分配复用（packageId 非空时走套餐分配）。 */
public class AssignMenusRequest {

    /** 角色 ID（角色授权时必填） */
    private Long roleId;

    /** 套餐 ID（套餐菜单分配时必填） */
    private Long packageId;

    @NotNull(message = "租户 ID 不能为空")
    private String tenantId;

    private List<Long> menuIds;

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public List<Long> getMenuIds() { return menuIds; }
    public void setMenuIds(List<Long> menuIds) { this.menuIds = menuIds; }
}
