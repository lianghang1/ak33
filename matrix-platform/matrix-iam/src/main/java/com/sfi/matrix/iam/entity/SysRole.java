package com.sfi.matrix.iam.entity;

import com.sfi.matrix.iam.constant.IamConstants;
import com.sfi.scaffold.starter.data.jpa.datascope.DataScopeType;
import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;
import com.sfi.scaffold.starter.tenant.aspect.JpaTenantFilterAspect;
import org.hibernate.annotations.Filter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 系统角色（IAM 通用能力）。
 *
 * @author matrix
 */
@Entity
@Table(name = "sys_role")
@Filter(name = JpaTenantFilterAspect.FILTER_NAME, condition = "tenant_id = :tenantId")
public class SysRole extends BaseEntity {

    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @Column(name = "role_code", nullable = false, length = 64)
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 64)
    private String roleName;

    @Column(name = "sort_no")
    private Integer sortNo;

    /**
     * 角色类型（决定权限推导行为）：PLATFORM_ADMIN / TENANT_ADMIN / NORMAL。
     * 超管为隐式权限，菜单不落 sys_role_menu，由服务层按 scope/套餐动态推导。
     */
    @Column(name = "role_type", nullable = false, length = 32)
    private String roleType = IamConstants.ROLE_TYPE_NORMAL;

    /** 是否内置角色（内置不可删，与 roleType 是两个维度） */
    @Column(name = "built_in", nullable = false)
    private Boolean builtIn = Boolean.FALSE;

    /**
     * 数据范围：取值见框架 {@link DataScopeType}（默认 ALL）。
     * 当前仅作字段预留与可配置，归约/过滤能力已下沉框架，执行层待业务表落地后接入。
     */
    @Column(name = "data_scope", nullable = false, length = 32)
    private String dataScope = DataScopeType.ALL.name();

    @Column(name = "remark", length = 255)
    private String remark;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public Boolean getBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(Boolean builtIn) {
        this.builtIn = builtIn;
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
