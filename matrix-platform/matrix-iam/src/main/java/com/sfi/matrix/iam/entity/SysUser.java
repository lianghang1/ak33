package com.sfi.matrix.iam.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 系统用户（账号主体，IAM 通用能力，跨系统复用）。
 *
 * <p>一人多租户模型：本表只存账号本体，不含 tenantId，username 全局唯一。
 * 用户与租户的归属关系在 {@link SysUserTenant}（一个账号可挂多个租户），
 * 部门也挂在该关联上——同一账号在不同租户下可属于不同部门。</p>
 *
 * @author matrix
 */
@Entity
@Table(name = "sys_user")
public class SysUser extends BaseEntity {

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(name = "password", nullable = false, length = 128)
    private String password;

    @Column(name = "real_name", length = 64)
    private String realName;

    @Column(name = "mobile", length = 32)
    private String mobile;

    @Column(name = "email", length = 128)
    private String email;

    /** 状态：1启用 0禁用 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

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

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
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
