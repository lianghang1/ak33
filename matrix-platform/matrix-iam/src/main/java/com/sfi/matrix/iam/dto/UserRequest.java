package com.sfi.matrix.iam.dto;

import javax.validation.constraints.NotBlank;

/** 用户增改请求。id 为空=新增（需 password），非空=修改。 */
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
}
