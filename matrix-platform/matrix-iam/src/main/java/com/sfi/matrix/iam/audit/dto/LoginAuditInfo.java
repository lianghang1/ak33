package com.sfi.matrix.iam.audit.dto;

/**
 * 登录审计记录入参：承载 controller 侧采集的业务信息，环境信息（IP/UA/设备/traceId）由 AuditService 从请求补全。
 *
 * @author lianghang
 */
public class LoginAuditInfo {

    private String username;
    private Long userId;
    private String authType;
    private String tenantCode;
    private boolean success;
    private String params;
    private long costMs;
    private Throwable error;

    public String getUsername() { return username; }
    public LoginAuditInfo setUsername(String username) { this.username = username; return this; }
    public Long getUserId() { return userId; }
    public LoginAuditInfo setUserId(Long userId) { this.userId = userId; return this; }
    public String getAuthType() { return authType; }
    public LoginAuditInfo setAuthType(String authType) { this.authType = authType; return this; }
    public String getTenantCode() { return tenantCode; }
    public LoginAuditInfo setTenantCode(String tenantCode) { this.tenantCode = tenantCode; return this; }
    public boolean isSuccess() { return success; }
    public LoginAuditInfo setSuccess(boolean success) { this.success = success; return this; }
    public String getParams() { return params; }
    public LoginAuditInfo setParams(String params) { this.params = params; return this; }
    public long getCostMs() { return costMs; }
    public LoginAuditInfo setCostMs(long costMs) { this.costMs = costMs; return this; }
    public Throwable getError() { return error; }
    public LoginAuditInfo setError(Throwable error) { this.error = error; return this; }
}
