package com.sfi.matrix.iam.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 套餐-菜单关联（套餐能开通哪些菜单，全局表）。
 *
 * @author matrix
 */
@Entity
@Table(name = "sys_package_menu")
public class SysPackageMenu extends BaseEntity {

    @Column(name = "package_id")
    private Long packageId;

    @Column(name = "menu_id")
    private Long menuId;

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }
}
