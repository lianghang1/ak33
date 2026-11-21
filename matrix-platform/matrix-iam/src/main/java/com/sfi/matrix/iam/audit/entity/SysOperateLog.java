package com.sfi.matrix.iam.audit.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;
import com.sfi.scaffold.starter.tenant.aspect.JpaTenantFilterAspect;
import org.hibernate.annotations.Filter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 操作审计日志：记录被 {@code @OperateLog} 标注的方法调用。
 *
 * @author matrix
 */
@Entity
@Table(name = "sys_operate_log")
@Filter(name = JpaTenantFilterAspect.FILTER_NAME, condition = "tenant_id = :tenantId")
public class SysOperateLog extends BaseEntity {

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "module")
    private String module;

    @Column(name = "operation")
    private String operation;

    @Column(name = "method")
    private String method;

    @Column(name = "uri")
    private String uri;

    @Column(name = "ip")
    private String ip;

    @Column(name = "oper_user_id")
    private Long operUserId;

    @Column(name = "oper_user_name")
    private String operUserName;

    /** 参数摘要（脱敏、截断） */
    @Column(name = "params")
    private String params;

    /** 1成功 0失败 */
    @Column(name = "success")
    private Boolean success;

    @Column(name = "error_msg")
    private String errorMsg;

    @Column(name = "cost_ms")
    private Long costMs;

    @Column(name = "oper_time")
    private LocalDateTime operTime;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public Long getOperUserId() { return operUserId; }
    public void setOperUserId(Long operUserId) { this.operUserId = operUserId; }
    public String getOperUserName() { return operUserName; }
    public void setOperUserName(String operUserName) { this.operUserName = operUserName; }
    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public Long getCostMs() { return costMs; }
    public void setCostMs(Long costMs) { this.costMs = costMs; }
    public LocalDateTime getOperTime() { return operTime; }
    public void setOperTime(LocalDateTime operTime) { this.operTime = operTime; }
}
