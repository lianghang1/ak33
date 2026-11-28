package com.sfi.matrix.iam.plan;

import com.sfi.scaffold.query.plan.annotation.ColumnX;
import com.sfi.scaffold.query.plan.annotation.JoinColumnX;
import com.sfi.scaffold.query.plan.annotation.JoinTableX;
import com.sfi.scaffold.query.plan.annotation.TableX;

/**
 * 用户列表查询计划（通路③ QueryPlan）：芋道式"当前租户下用户"视图。
 *
 * <p>主表 sys_user，inner join sys_user_tenant 保证只显示当前租户下有归属的用户
 * （Controller 注入 EQ_tenantId）；left join sys_user_profile 取昵称、left join sys_dept
 * 取部门名；角色名用子查询 group_concat 聚合（一个用户多角色不导致行重复）。</p>
 *
 * <p>走 {@code GET /api/iam/user/list/UserListPlan}，由 {@code @QueryPlanX} 白名单开放。
 * 主表声明 deleted 字段触发 DeletedParamFilter 自动注入 u.deleted=false。</p>
 *
 * @author lianghang
 */
@TableX(name = "sys_user", alias = "u")
@JoinTableX(name = "sys_user_tenant", alias = "ut", joinType = "inner join",
        columns = @JoinColumnX(sql = "ut.user_id = u.id AND ut.deleted = false"))
@JoinTableX(name = "sys_user_profile", alias = "p", joinType = "left join",
        columns = @JoinColumnX(sql = "p.user_id = u.id AND p.deleted = false"))
@JoinTableX(name = "sys_dept", alias = "d", joinType = "left join",
        columns = @JoinColumnX(sql = "d.id = ut.dept_id AND d.deleted = false"))
@JoinTableX(name = "sys_tenant", alias = "t", joinType = "left join",
        columns = @JoinColumnX(sql = "t.id = ut.tenant_id AND t.deleted = false"))
public class UserListPlan {

    @ColumnX("u.id")
    private Long id;

    @ColumnX("u.username")
    private String username;

    /** 昵称：来自 sys_user_profile */
    @ColumnX("p.nickname")
    private String nickname;

    @ColumnX("u.mobile")
    private String mobile;

    @ColumnX("u.email")
    private String email;

    @ColumnX("u.account_type")
    private String accountType;

    @ColumnX("u.status")
    private Integer status;

    @ColumnX("u.max_sessions")
    private Integer maxSessions;

    /** 归属租户（供 Controller 注入当前租户过滤） */
    @ColumnX("ut.tenant_id")
    private String tenantId;

    /** 租户名称：平台超管看全部时用于区分用户所属租户 */
    @ColumnX("t.tenant_name")
    private String tenantName;

    /** 该租户下的部门 ID */
    @ColumnX("ut.dept_id")
    private Long deptId;

    /** 部门名：来自 sys_dept */
    @ColumnX("d.dept_name")
    private String deptName;

    /** 角色名（逗号分隔）：子查询聚合，避免多角色导致行重复 */
    @ColumnX("(select group_concat(r.role_name separator ',') from sys_user_role ur " +
            "join sys_role r on r.id = ur.role_id " +
            "where ur.user_id = u.id and ur.tenant_id = ut.tenant_id and ur.deleted = false and r.deleted = false)")
    private String roleNames;

    /**
     * 角色 ID（前后补逗号，如 ,1,5,）：供前端按角色精确筛选。
     * 前端传 CN_roleIds=,{roleId}, 用包含匹配即可精确命中，避免 1 误命中 12。
     */
    @ColumnX("(select concat(',', group_concat(ur.role_id separator ','), ',') from sys_user_role ur " +
            "where ur.user_id = u.id and ur.tenant_id = ut.tenant_id and ur.deleted = false)")
    private String roleIds;

    /** 创建时间：来自 sys_user.create_time，支持按创建时间倒序（新用户置顶） */
    @ColumnX("u.create_time")
    private java.time.LocalDateTime createTime;

    /** 最后登录时间：来自 sys_login_log 最近一次成功登录 */
    @ColumnX("(select max(l.login_time) from sys_login_log l " +
            "where l.user_id = u.id and l.success = true)")
    private java.time.LocalDateTime lastLoginTime;

    /** 声明该字段以触发 DeletedParamFilter 自动注入主表逻辑删除条件 */
    @ColumnX("u.deleted")
    private Boolean deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getMaxSessions() { return maxSessions; }
    public void setMaxSessions(Integer maxSessions) { this.maxSessions = maxSessions; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getRoleNames() { return roleNames; }
    public void setRoleNames(String roleNames) { this.roleNames = roleNames; }
    public String getRoleIds() { return roleIds; }
    public void setRoleIds(String roleIds) { this.roleIds = roleIds; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
    public java.time.LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(java.time.LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}
