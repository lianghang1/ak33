package com.sfi.matrix.iam.entity;

import com.sfi.matrix.iam.constant.IamConstants;
import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 系统用户（账号主体，IAM 通用能力，跨系统复用）。
 *
 * <p>本表只存登录鉴权必需的核心字段（账号/密码/类型/状态），展示类资料
 * （昵称/头像/性别/生日/身份证/地址等）在 {@link SysUserProfile}（1:1），
 * 第三方绑定在 {@link SysUserSocial}（1:N）。应用（APP）也是一种用户，
 * 同样用 username/password 凭据换 token，靠 accountType 区分。</p>
 *
 * <p>一人多租户模型：本表不含 tenantId，username 全局唯一；用户与租户的
 * 归属关系在 {@link SysUserTenant}，部门也挂在该关联上——同一账号在不同
 * 租户下可属于不同部门。</p>
 *
 * @author matrix
 */
@Entity
@Table(name = "sys_user")
public class SysUser extends BaseEntity {

    /** 账号类型：OPERATOR / APP / MEMBER，默认 OPERATOR */
    @Column(name = "account_type", nullable = false, length = 16)
    private String accountType = IamConstants.ACCOUNT_TYPE_OPERATOR;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(name = "password", nullable = false, length = 128)
    private String password;

    @Column(name = "mobile", length = 32)
    private String mobile;

    @Column(name = "email", length = 128)
    private String email;

    /** 状态：1启用 0禁用 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
