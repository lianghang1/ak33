package com.sfi.matrix.iam.plan;

import com.sfi.scaffold.query.plan.annotation.ColumnX;
import com.sfi.scaffold.query.plan.annotation.TableX;

/**
 * 套餐列表查询计划（通路③ QueryPlan）：主表 sys_tenant_package，
 * 子查询聚合已分配菜单数、已开通租户数，避免 N+1 查询。
 *
 * <p>走 {@code GET /api/iam/package/list/PackageListPlan}。</p>
 *
 * @author lianghang
 */
@TableX(name = "sys_tenant_package", alias = "p")
public class PackageListPlan {

    @ColumnX("p.id")
    private Long id;

    @ColumnX("p.package_code")
    private String packageCode;

    @ColumnX("p.package_name")
    private String packageName;

    @ColumnX("p.status")
    private Integer status;

    @ColumnX("p.remark")
    private String remark;

    @ColumnX("p.create_time")
    private java.time.LocalDateTime createTime;

    /** 已分配菜单数 */
    @ColumnX("(select count(*) from sys_package_menu pm where pm.package_id = p.id and pm.deleted = false)")
    private Long menuCount;

    /** 已开通该套餐的租户数 */
    @ColumnX("(select count(*) from sys_tenant t where t.package_id = p.id and t.deleted = false)")
    private Long tenantCount;

    /** 声明该字段以触发 DeletedParamFilter 自动注入主表逻辑删除条件 */
    @ColumnX("p.deleted")
    private Boolean deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPackageCode() { return packageCode; }
    public void setPackageCode(String packageCode) { this.packageCode = packageCode; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
    public Long getMenuCount() { return menuCount; }
    public void setMenuCount(Long menuCount) { this.menuCount = menuCount; }
    public Long getTenantCount() { return tenantCount; }
    public void setTenantCount(Long tenantCount) { this.tenantCount = tenantCount; }
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}
