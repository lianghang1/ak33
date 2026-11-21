package com.sfi.matrix.iam.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;

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
public class SysTenantPackage extends BaseEntity {

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
}
