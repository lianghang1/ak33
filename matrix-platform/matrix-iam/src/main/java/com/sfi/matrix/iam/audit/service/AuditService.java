package com.sfi.matrix.iam.audit.service;

import com.sfi.matrix.iam.audit.entity.SysLoginLog;
import com.sfi.matrix.iam.audit.entity.SysOperateLog;
import com.sfi.matrix.iam.audit.repository.SysLoginLogRepository;
import com.sfi.matrix.iam.audit.repository.SysOperateLogRepository;
import com.sfi.scaffold.common.util.IpUtils;
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

    private final SysLoginLogRepository loginLogRepository;
    private final SysOperateLogRepository operateLogRepository;

    public AuditService(SysLoginLogRepository loginLogRepository,
                        SysOperateLogRepository operateLogRepository) {
        this.loginLogRepository = loginLogRepository;
        this.operateLogRepository = operateLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordLogin(String username, String authType, String tenantCode, HttpServletRequest request,
                            boolean success, String reason) {
        try {
            SysLoginLog log = new SysLoginLog();
            log.setUsername(username);
            log.setAuthType(authType);
            log.setTenantCode(tenantCode);
            log.setSuccess(success);
            log.setReason(reason);
            log.setLoginTime(LocalDateTime.now());
            if (request != null) {
                log.setIp(IpUtils.clientIp(request));
                String ua = request.getHeader("User-Agent");
                log.setUserAgent(ua == null ? null : ua.substring(0, Math.min(ua.length(), 500)));
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
}
