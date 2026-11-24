package com.sfi.matrix.iam.audit.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;
import com.sfi.scaffold.starter.data.jpa.logicdelete.JpaDeletedFilterAspect;
import org.hibernate.annotations.Filter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 登录审计日志：记录每次登录尝试（成功/失败）的账号、来源、结果与原因。
 *
 * <p>不加租户 @Filter：登录失败时可能尚未定位租户，按 user 审计即可。</p>
 *
 * @author matrix
 */
@Entity
@Table(name = "sys_login_log")
@Filter(name = JpaDeletedFilterAspect.FILTER_NAME, condition = "deleted = false")
public class SysLoginLog extends BaseEntity {

    /** 逻辑删除标记，true=已删除；由字段名探测自动过滤 */
    @Column(name = "deleted")
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "username")
    private String username;

    @Column(name = "auth_type")
    private String authType;

    @Column(name = "tenant_code")
    private String tenantCode;

    @Column(name = "ip")
    private String ip;

    @Column(name = "user_agent")
    private String userAgent;

    /** 1成功 0失败 */
    @Column(name = "success")
    private Boolean success;

    @Column(name = "reason")
    private String reason;

    @Column(name = "login_time")
    private LocalDateTime loginTime;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
