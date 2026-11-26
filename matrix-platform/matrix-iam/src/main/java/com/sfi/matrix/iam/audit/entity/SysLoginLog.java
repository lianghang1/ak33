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
 * @author lianghang
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

    /** 登录成功的账号ID，失败时为空 */
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "auth_type")
    private String authType;

    @Column(name = "tenant_code")
    private String tenantCode;

    @Column(name = "ip")
    private String ip;

    /** IP属地：国家|区域|省|市|ISP */
    @Column(name = "login_location")
    private String loginLocation;

    @Column(name = "user_agent")
    private String userAgent;

    /** User-Agent 解析出的浏览器 */
    @Column(name = "browser")
    private String browser;

    /** User-Agent 解析出的操作系统 */
    @Column(name = "os")
    private String os;

    /** 客户端设备标识，来自 X-Device-Id 头 */
    @Column(name = "device")
    private String device;

    /** 1成功 0失败 */
    @Column(name = "success")
    private Boolean success;

    /** 失败原因码：全局业务码枚举名或具体码；成功为 SUCCESS */
    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "reason")
    private String reason;

    /** 登录入参摘要，密码/授权码等敏感值已剔除 */
    @Column(name = "params")
    private String params;

    /** 链路追踪ID，串联同一次请求的所有日志 */
    @Column(name = "trace_id")
    private String traceId;

    /** 登录校验耗时（毫秒） */
    @Column(name = "cost_ms")
    private Long costMs;

    @Column(name = "login_time")
    private LocalDateTime loginTime;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getLoginLocation() { return loginLocation; }
    public void setLoginLocation(String loginLocation) { this.loginLocation = loginLocation; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }
    public String getOs() { return os; }
    public void setOs(String os) { this.os = os; }
    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Long getCostMs() { return costMs; }
    public void setCostMs(Long costMs) { this.costMs = costMs; }
    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
