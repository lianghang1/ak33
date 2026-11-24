package com.sfi.matrix.iam.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;
import com.sfi.scaffold.starter.data.jpa.logicdelete.JpaDeletedFilterAspect;
import org.hibernate.annotations.Filter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 租户套餐（平台全局定义，控制租户能开通哪些菜单/功能）。
 *
 * <p>全局表，不加租户 @Filter。sys_tenant.packageId 指向本表；
 * sys_package_menu 描述套餐含哪些菜单。最终权限 = 角色菜单 ∩ 套餐菜单。</p>
 *
 * @author matrix
 */
@Entity
@Table(name = "sys_tenant_package")
@Filter(name = JpaDeletedFilterAspect.FILTER_NAME, condition = "deleted = false")
public class SysTenantPackage extends BaseEntity {

    /** 逻辑删除标记，true=已删除；由字段名探测自动过滤 */
    @Column(name = "deleted")
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "package_code")
    private String packageCode;

    @Column(name = "package_name")
    private String packageName;

    @Column(name = "remark")
    private String remark;

    @Column(name = "status")
    private Integer status = 1;

    public String getPackageCode() {
        return packageCode;
    }

    public void setPackageCode(String packageCode) {
        this.packageCode = packageCode;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
