package com.sfi.matrix.iam.dto;

import javax.validation.constraints.NotBlank;

import java.util.List;

/**
 * 用户增改请求。id 为空=新增（需 password），非空=修改。
 *
 * <p>对齐芋道用户管理：账号资料 + 归属部门 + 角色 + 性别/备注一次提交；
 * tenantId/deptId/roleIds 由前端按当前登录租户传入，服务端做租户内合法性校验。</p>
 */
public class UserRequest {

    private Long id;

    @NotBlank(message = "账号不能为空")
    private String username;

    /** 新增必填、修改可空（留空不改密码） */
    private String password;

    private String nickname;
    private String mobile;
    private String email;
    private String accountType;
    private Integer status;

    /** 最大在线会话数；留空表示跟随全局默认 */
    private Integer maxSessions;

    /** 用户资料：性别 0未知 1男 2女 */
    private Integer sex;

    /** 用户资料：备注 */
    private String remark;

    /** 归属租户（前端按当前登录租户传；为空时服务端用当前租户兜底） */
    private String tenantId;

    /** 该租户下的部门 ID（芋道用户必属一个部门） */
    private Long deptId;

    /** 该租户下的角色 ID 列表（一次提交，服务端覆盖式分配） */
    private List<Long> roleIds;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
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
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public List<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(List<Long> roleIds) { this.roleIds = roleIds; }
}
