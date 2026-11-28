package com.sfi.matrix.iam.plan;

import com.sfi.scaffold.query.plan.annotation.ColumnX;
import com.sfi.scaffold.query.plan.annotation.JoinColumnX;
import com.sfi.scaffold.query.plan.annotation.JoinTableX;
import com.sfi.scaffold.query.plan.annotation.TableX;

/**
 * 角色列表查询计划（通路③ QueryPlan）：芋道式角色管理视图。
 *
 * <p>主表 sys_role，left join sys_tenant 取租户名（平台超管看全部时区分归属）；
 * 用户数、菜单数用子查询聚合，避免多行 join 导致行重复。</p>
 *
 * <p>走 {@code GET /api/iam/role/list/RoleListPlan}；非平台超管由 Controller 强制注入
 * EQ_tenantId 做租户隔离。主表声明 deleted 字段触发 DeletedParamFilter 自动注入 r.deleted=false。</p>
 *
 * @author lianghang
 */
@TableX(name = "sys_role", alias = "r")
@JoinTableX(name = "sys_tenant", alias = "t", joinType = "left join",
        columns = @JoinColumnX(sql = "t.id = r.tenant_id AND t.deleted = false"))
public class RoleListPlan {

    @ColumnX("r.id")
    private Long id;

    @ColumnX("r.role_code")
    private String roleCode;

    @ColumnX("r.role_name")
    private String roleName;

    @ColumnX("r.role_type")
    private String roleType;

    @ColumnX("r.built_in")
    private Boolean builtIn;

    @ColumnX("r.data_scope")
    private String dataScope;

    @ColumnX("r.data_scope_dept_ids")
    private String dataScopeDeptIds;

    @ColumnX("r.sort_no")
    private Integer sortNo;

    @ColumnX("r.status")
    private Integer status;

    @ColumnX("r.remark")
    private String remark;

    /** 归属租户（供 Controller 注入当前租户过滤） */
    @ColumnX("r.tenant_id")
    private String tenantId;

    /** 租户名称：平台超管看全部时用于区分角色所属租户 */
    @ColumnX("t.tenant_name")
    private String tenantName;

    /** 创建时间 */
    @ColumnX("r.create_time")
    private java.time.LocalDateTime createTime;

    /** 已分配用户数 */
    @ColumnX("(select count(*) from sys_user_role ur where ur.role_id = r.id and ur.deleted = false)")
    private Long userCount;

    /** 已分配菜单数 */
    @ColumnX("(select count(*) from sys_role_menu rm where rm.role_id = r.id and rm.deleted = false)")
    private Long menuCount;

    /** 声明该字段以触发 DeletedParamFilter 自动注入主表逻辑删除条件 */
    @ColumnX("r.deleted")
    private Boolean deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }
    public Boolean getBuiltIn() { return builtIn; }
    public void setBuiltIn(Boolean builtIn) { this.builtIn = builtIn; }
    public String getDataScope() { return dataScope; }
    public void setDataScope(String dataScope) { this.dataScope = dataScope; }
    public String getDataScopeDeptIds() { return dataScopeDeptIds; }
    public void setDataScopeDeptIds(String dataScopeDeptIds) { this.dataScopeDeptIds = dataScopeDeptIds; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
    public Long getUserCount() { return userCount; }
    public void setUserCount(Long userCount) { this.userCount = userCount; }
    public Long getMenuCount() { return menuCount; }
    public void setMenuCount(Long menuCount) { this.menuCount = menuCount; }
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}
