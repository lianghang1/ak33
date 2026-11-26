package com.sfi.matrix.iam.audit.service;

import com.sfi.matrix.iam.audit.dto.LoginAuditInfo;
import com.sfi.matrix.iam.audit.entity.SysLoginLog;
import com.sfi.matrix.iam.audit.entity.SysOperateLog;
import com.sfi.matrix.iam.audit.repository.SysLoginLogRepository;
import com.sfi.matrix.iam.audit.repository.SysOperateLogRepository;
import com.sfi.matrix.iam.audit.util.UserAgentUtils;
import com.sfi.scaffold.common.constant.TraceConstants;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.ErrorCode;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.common.util.IpUtils;
import com.sfi.scaffold.starter.ip.IpRegionUtil;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 审计记录服务：集中落库登录/操作日志。
 *
 * <p>日志写入用 REQUIRES_NEW：即使业务失败回滚，审计记录也保留。
 * 任何审计异常都被吞掉，绝不影响主流程。</p>
 *
 * @author lianghang
 */
@Service
public class AuditService {

    private static final String SUCCESS_CODE = "SUCCESS";
    private static final String SYSTEM_ERROR_CODE = "SYSTEM_ERROR";

    private final SysLoginLogRepository loginLogRepository;
    private final SysOperateLogRepository operateLogRepository;

    public AuditService(SysLoginLogRepository loginLogRepository,
                        SysOperateLogRepository operateLogRepository) {
        this.loginLogRepository = loginLogRepository;
        this.operateLogRepository = operateLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordLogin(LoginAuditInfo info, HttpServletRequest request) {
        try {
            SysLoginLog log = new SysLoginLog();
            log.setUsername(info.getUsername());
            log.setUserId(info.getUserId());
            log.setAuthType(info.getAuthType());
            log.setTenantCode(info.getTenantCode());
            log.setSuccess(info.isSuccess());
            log.setParams(info.getParams());
            log.setCostMs(info.getCostMs());
            log.setLoginTime(LocalDateTime.now());
            log.setReasonCode(reasonCodeOf(info));
            log.setReason(reasonOf(info));
            log.setTraceId(MDC.get(TraceConstants.TRACE_ID_KEY));
            if (request != null) {
                String ip = IpUtils.clientIp(request);
                log.setIp(ip);
                log.setLoginLocation(truncate(IpRegionUtil.getRegion(ip), 128));
                String ua = request.getHeader("User-Agent");
                log.setUserAgent(truncate(ua, 500));
                log.setBrowser(truncate(UserAgentUtils.browser(ua), 64));
                log.setOs(truncate(UserAgentUtils.os(ua), 64));
                log.setDevice(truncate(request.getHeader("X-Device-Id"), 128));
            }
            loginLogRepository.save(log);
        } catch (Exception ignored) {
            // 审计失败不影响业务
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordOperate(SysOperateLog log) {
        try {
            log.setOperTime(LocalDateTime.now());
            operateLogRepository.save(log);
        } catch (Exception ignored) {
            // 审计失败不影响业务
        }
    }

    /** 失败原因码：业务异常取错误码，其他异常归 SYSTEM_ERROR，成功记 SUCCESS。 */
    private static String reasonCodeOf(LoginAuditInfo info) {
        if (info.isSuccess()) {
            return SUCCESS_CODE;
        }
        Throwable error = info.getError();
        if (error instanceof BizException) {
            ErrorCode code = ((BizException) error).getErrorCode();
            if (code instanceof GlobalErrorCode) {
                return ((GlobalErrorCode) code).name();
            }
            return code == null ? SYSTEM_ERROR_CODE : String.valueOf(code.getCode());
        }
        return SYSTEM_ERROR_CODE;
    }

    /** 失败原因文案：业务异常取异常消息并截断，避免超长文本入库。 */
    private static String reasonOf(LoginAuditInfo info) {
        return info.getError() == null ? null : truncate(info.getError().getMessage(), 255);
    }

    /** 统一截断，防止审计文本超过列宽；空值原样返回。 */
    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
