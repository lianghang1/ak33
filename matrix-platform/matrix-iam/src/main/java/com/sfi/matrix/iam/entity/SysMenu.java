package com.sfi.matrix.iam.entity;

import com.sfi.matrix.iam.constant.IamConstants;
import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 菜单/权限点（平台全局定义，IAM 通用能力）。
 *
 * <p>菜单/权限点由平台统一定义，不分租户存多份；租户能用哪些菜单由
 * {@link SysTenantPackage}（套餐）控制，用户能用哪些由角色控制。
 * 本表不加租户 @Filter（全局只读定义）。parentId 自关联构成菜单树；
 * menuType 区分目录/菜单/按钮；permission 为权限标识（如 cemetery:tomb:list）。</p>
 *
 * @author matrix
 */
@Entity
@Table(name = "sys_menu")
public class SysMenu extends BaseEntity {

    @Column(name = "menu_name")
    private String menuName;

    /** 类型：M目录 C菜单 F按钮 */
    @Column(name = "menu_type")
    private String menuType;

    /** 范围：PLATFORM 运营平台 / TENANT 租户业务 / COMMON 通用；套餐只能选 TENANT/COMMON */
    @Column(name = "menu_scope")
    private String menuScope = IamConstants.MENU_SCOPE_TENANT;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "path")
    private String path;

    @Column(name = "component")
    private String component;

    @Column(name = "icon")
    private String icon;

    /** 权限标识 */
    @Column(name = "permission")
    private String permission;

    @Column(name = "sort_no")
    private Integer sortNo;

    @Column(name = "visible")
    private Boolean visible = Boolean.TRUE;

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getMenuType() {
        return menuType;
    }

    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }

    public String getMenuScope() {
        return menuScope;
    }

    public void setMenuScope(String menuScope) {
        this.menuScope = menuScope;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
