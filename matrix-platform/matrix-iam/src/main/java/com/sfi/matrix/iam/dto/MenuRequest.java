package com.sfi.matrix.iam.dto;

import javax.validation.constraints.NotBlank;

/** 菜单增改请求。id 为空=新增，非空=修改。 */
public class MenuRequest {

    private Long id;

    @NotBlank(message = "菜单名称不能为空")
    private String menuName;

    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    private Long parentId;
    private String path;
    private String component;
    private String icon;
    private String permission;
    /** URL 权限规则（Ant 风格，多个逗号分隔），声明该菜单守护的后端 URL */
    private String apiPattern;
    /** PLATFORM / TENANT / COMMON */
    private String menuScope;
    private Integer sortNo;
    private Boolean visible;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMenuName() { return menuName; }
    public void setMenuName(String menuName) { this.menuName = menuName; }
    public String getMenuType() { return menuType; }
    public void setMenuType(String menuType) { this.menuType = menuType; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public String getApiPattern() { return apiPattern; }
    public void setApiPattern(String apiPattern) { this.apiPattern = apiPattern; }
    public String getMenuScope() { return menuScope; }
    public void setMenuScope(String menuScope) { this.menuScope = menuScope; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public Boolean getVisible() { return visible; }
    public void setVisible(Boolean visible) { this.visible = visible; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
