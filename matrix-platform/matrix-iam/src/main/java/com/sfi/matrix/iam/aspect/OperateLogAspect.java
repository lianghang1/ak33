package com.sfi.matrix.iam.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfi.matrix.iam.annotation.OperateLog;
import com.sfi.matrix.iam.entity.SysOperateLog;
import com.sfi.matrix.iam.service.AuditService;
import com.sfi.scaffold.common.util.IpUtils;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.security.identity.LoginUser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 操作审计切面：拦截 {@link OperateLog} 方法，落库一条审计记录。
 *
 * <p>参数经 JSON 序列化 + 敏感字段脱敏 + 截断；序列化失败降级为类型名，
 * 审计失败不影响业务主流程。</p>
 *
 * @author lianghang
 */
@Aspect
@Component
public class OperateLogAspect {

    private static final Pattern SENSITIVE = Pattern.compile(
            "(\"(password|passwd|secret|token|accessToken|oldPassword|newPassword|adminPassword)\"\\s*:\\s*\")[^\"]*(\")");

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public OperateLogAspect(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(operateLog)")
    public Object around(ProceedingJoinPoint pjp, OperateLog operateLog) throws Throwable {
        return proceedAndAudit(pjp, operateLog.module(), operateLog.operation());
    }

    private Object proceedAndAudit(ProceedingJoinPoint pjp, String module, String operation) throws Throwable {
        long start = System.currentTimeMillis();
        boolean success = true;
        String errorMsg = null;
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            success = false;
            errorMsg = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
            if (errorMsg.length() > 480) errorMsg = errorMsg.substring(0, 480);
            throw t;
        } finally {
            try {
                auditService.recordOperate(buildLog(pjp, module, operation, success, errorMsg,
                        System.currentTimeMillis() - start));
            } catch (Exception ignored) {
                // 审计失败不影响业务
            }
        }
    }

    private SysOperateLog buildLog(ProceedingJoinPoint pjp, String module, String operation, boolean success,
                                   String errorMsg, long costMs) {
        SysOperateLog log = new SysOperateLog();
        log.setModule(module);
        log.setOperation(operation);
        log.setMethod(pjp.getSignature().toShortString());
        log.setSuccess(success);
        log.setErrorMsg(errorMsg);
        log.setCostMs(costMs);
        log.setParams(serializeArgs(pjp.getArgs()));

        LoginUser user = LoginContext.currentOrNull();
        if (user != null) {
            log.setOperUserId(user.getAccountId());
            log.setOperUserName(user.getDisplayName() == null ? user.getAccountNo() : user.getDisplayName());
            log.setTenantId(user.getTenantId());
        }
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            log.setUri(request.getRequestURI());
            log.setIp(IpUtils.clientIp(request));
        }
        return log;
    }

    private String serializeArgs(Object[] args) {
        if (args == null || args.length == 0) return "";
        try {
            String json = objectMapper.writeValueAsString(Arrays.stream(args)
                    .filter(a -> !(a instanceof HttpServletRequest))
                    .collect(Collectors.toList()));
            json = SENSITIVE.matcher(json).replaceAll("$1***$3");
            return json.length() > 1900 ? json.substring(0, 1900) + "..." : json;
        } catch (Exception e) {
            return Arrays.stream(args).map(a -> a == null ? "null" : a.getClass().getSimpleName())
                    .collect(Collectors.joining(","));
        }
    }
}
