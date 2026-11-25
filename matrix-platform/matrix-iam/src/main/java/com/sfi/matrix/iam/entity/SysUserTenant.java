package com.sfi.matrix.iam.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;
import com.sfi.scaffold.starter.data.jpa.logicdelete.JpaDeletedFilterAspect;
import org.hibernate.annotations.Filter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 用户-租户归属（一人多租户的核心关联）。
 *
 * <p>一个账号可挂多个租户，同一账号在不同租户下可属于不同部门、拥有不同角色。
 * deptId 放这里而非 sys_user，正是为了支持"张三是 A 租户销售部、B 租户财务部"。</p>
 *
 * <p><b>刻意不加租户 @Filter</b>：登录时要跨租户查出"这个账号挂了哪些租户"，
 * 加了过滤就查不全；它只存归属关系、不含业务数据，按 userId 查询即安全。
 * 切换到具体租户后，由 repository 显式按 tenantId 条件取当前归属。</p>
 *
 * @author lianghang
 */
@Entity
@Table(name = "sys_user_tenant")
@Filter(name = JpaDeletedFilterAspect.FILTER_NAME, condition = "deleted = false")
public class SysUserTenant extends BaseEntity {

    /** 逻辑删除标记，true=已删除；由字段名探测自动过滤 */
    @Column(name = "deleted")
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "tenant_id")
    private String tenantId;

    /** 该账号在此租户下的部门 ID（对应 sys_dept.id） */
    @Column(name = "dept_id")
    private Long deptId;

    /** 在此租户下是否启用：1启用 0停用 */
    @Column(name = "status")
    private Integer status = 1;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
