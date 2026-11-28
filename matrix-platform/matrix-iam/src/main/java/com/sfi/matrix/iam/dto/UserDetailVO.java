package com.sfi.matrix.iam.dto;

import java.util.List;

/**
 * 用户详情回显（编辑抽屉用）：账号资料 + 用户资料 + 当前租户归属（部门）+ 角色 ID 列表。
 *
 * <p>一次请求返回编辑所需的全部数据，避免前端多次往返拼装。</p>
 */
public class UserDetailVO {

    private Long id;
    private String username;
    private String nickname;
    private String mobile;
    private String email;
    private String accountType;
    private Integer status;
    private Integer maxSessions;
    private Integer sex;
    private String remark;

    /** 当前租户下的部门 ID */
    private Long deptId;
    /** 当前租户 */
    private String tenantId;
    /** 当前租户下的角色 ID 列表 */
    private List<Long> roleIds;

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
    public Integer getSex() { return sex; }
    public void setSex(Integer sex) { this.sex = sex; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public List<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(List<Long> roleIds) { this.roleIds = roleIds; }
}
