package com.sfi.matrix.iam.controller;

import com.sfi.matrix.iam.audit.service.AuditService;
import com.sfi.matrix.iam.auth.AuthService;
import com.sfi.matrix.iam.auth.AuthType;
import com.sfi.matrix.iam.dto.ChangePasswordRequest;
import com.sfi.matrix.iam.dto.KickOutRequest;
import com.sfi.matrix.iam.dto.LoginRequest;
import com.sfi.matrix.iam.dto.TenantCodeRequest;
import com.sfi.matrix.iam.entity.SysTenant;
import com.sfi.matrix.iam.entity.SysUser;
import com.sfi.matrix.iam.service.IamService;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.common.util.IpUtils;
import com.sfi.scaffold.starter.protection.ratelimit.RateLimit;
import com.sfi.scaffold.starter.security.anonymous.Anonymous;
import com.sfi.scaffold.starter.security.captcha.CaptchaService;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.security.identity.LoginUser;
import com.sfi.scaffold.starter.security.policy.LoginFailureGuard;
import com.sfi.scaffold.starter.security.policy.PasswordPolicyValidator;
import com.sfi.scaffold.starter.security.token.SessionInfo;
import com.sfi.scaffold.starter.security.token.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 登录入口（单接口 + 认证方式参数）：
 * <ol>
 *   <li>仅一个 login 接口，authType 决定认证方式（账密 / 社交），由 {@link AuthService} 分发到策略；</li>
 *   <li>tenantCode 为空时按凭证列出可登录租户，非空时选定租户签发 token；</li>
 *   <li>switch-tenant / enter-tenant 为已登录态换租户，logout 走框架 TokenService 撤销会话。</li>
 * </ol>
 * 所有写操作 POST 一律 {@link RequestBody} JSON + {@link Valid} DTO。
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final IamService iamService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final LoginFailureGuard loginFailureGuard;
    private final CaptchaService captchaService;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final AuditService auditService;

    public AuthController(AuthService authService, IamService iamService, TokenService tokenService,
                          PasswordEncoder passwordEncoder, LoginFailureGuard loginFailureGuard,
                          CaptchaService captchaService, PasswordPolicyValidator passwordPolicyValidator,
                          AuditService auditService) {
        this.authService = authService;
        this.iamService = iamService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.loginFailureGuard = loginFailureGuard;
        this.captchaService = captchaService;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.auditService = auditService;
    }

    /**
     * 登录防刷前置校验（全部走 scaffold 组件）：账号锁定 → IP 封锁 → 按需验证码。
     * 账密方式才做账号维度锁定；社交方式以 IP 维度为准。
     */
    private void guardBeforeLogin(String username, String ip, String captchaId, String captchaCode) {
        if (username != null && loginFailureGuard.isLocked(username)) {
            throw BizException.of(GlobalErrorCode.TOO_MANY_REQUESTS, "账号已临时锁定，请稍后再试");
        }
        if (loginFailureGuard.isIpBlocked(ip)) {
            throw BizException.of(GlobalErrorCode.TOO_MANY_REQUESTS, "当前 IP 登录失败次数过多，请稍后再试");
        }
        int failCount = username == null ? 0 : loginFailureGuard.currentCount(username);
        if (captchaService.required(failCount)
                && !captchaService.verifyAndConsume(captchaId, captchaCode)) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "验证码错误或已失效");
        }
    }

    /** 生成登录验证码（匿名放行） */
    @Anonymous
    @RateLimit(limit = 30, windowSeconds = 60, dimension = "ip")
    @PostMapping("/captcha")
    public R<Map<String, String>> captcha() {
        return R.ok(captchaService.generate());
    }

    /** 统一登录（匿名放行）：authType 区分方式；tenantCode 空则列租户，非空则签发 token */
    @Anonymous
    @RateLimit(limit = 10, windowSeconds = 60, dimension = "ip")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        String ip = IpUtils.clientIp(request);
        String authType = req.getAuthType();
        boolean password = AuthType.PASSWORD.equals(authType);
        String principal = password ? req.getUsername() : req.getProvider();
        try {
            guardBeforeLogin(password ? req.getUsername() : null, ip,
                    req.getCaptchaId(), req.getCaptchaCode());
            Map<String, String> credentials = credentialsOf(authType, req, ip);
            if (req.getTenantCode() == null || req.getTenantCode().trim().isEmpty()) {
                SysUser user = authService.authenticate(authType, credentials);
                List<SysTenant> tenants = iamService.listLoginTenantsByUser(user);
                if (tenants.isEmpty()) {
                    throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "账号未开通任何有效租户");
                }
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("needSelectTenant", true);
                result.put("tenants", tenants.stream().map(this::tenantBrief).collect(Collectors.toList()));
                return R.ok(result);
            }
            LoginUser loginUser = authService.authenticateAndAssemble(authType, credentials, req.getTenantCode());
            auditService.recordLogin(principal, authType, req.getTenantCode(), request, true, null);
            return R.ok(buildTokenResponse(loginUser));
        } catch (RuntimeException e) {
            auditService.recordLogin(principal, authType, req.getTenantCode(), request, false, e.getMessage());
            throw e;
        }
    }

    /** 切换租户：已登录用户切到该账号的另一个租户，返回新 token */
    @PostMapping("/switch-tenant")
    public R<Map<String, Object>> switchTenant(@Valid @RequestBody TenantCodeRequest req) {
        LoginUser current = currentUser();
        LoginUser loginUser = iamService.switchTenant(current.getAccountId(), req.getTenantCode());
        return R.ok(buildTokenResponse(loginUser));
    }

    /** 平台超管"进入"业务租户：以目标租户 TENANT_ADMIN 身份签发 token */
    @PostMapping("/enter-tenant")
    public R<Map<String, Object>> enterTenant(@Valid @RequestBody TenantCodeRequest req) {
        LoginUser current = currentUser();
        iamService.assertPlatformAdmin(current.getAccountId(), current.getTenantId());
        LoginUser loginUser = iamService.impersonateTenant(current.getAccountId(), req.getTenantCode());
        return R.ok(buildTokenResponse(loginUser));
    }

    /** 当前登录身份回显 */
    @PostMapping("/me")
    public R<LoginUser> me() {
        return R.ok(currentUser());
    }

    /** 退出登录：撤销当前 token 会话（幂等） */
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                          @RequestHeader(value = "X-Token", required = false) String xToken) {
        String token = resolveToken(authorization, xToken);
        if (token != null && !token.isEmpty()) {
            tokenService.revoke(token);
        }
        return R.ok();
    }

    /** 当前账号在线会话列表（在线用户管理） */
    @GetMapping("/online-sessions")
    public R<List<SessionInfo>> onlineSessions() {
        return R.ok(tokenService.onlineSessions(currentUser().getAccountId()));
    }

    /** 全部在线会话（平台管理，踢人用 token 调 logout 或本类踢人接口） */
    @GetMapping("/online-all")
    public R<List<Map<String, Object>>> onlineAll() {
        return R.ok(tokenService.listAllOnlineSessions().entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("token", e.getKey());
            m.put("session", e.getValue());
            return m;
        }).collect(Collectors.toList()));
    }

    /** 踢人：使某账号所有会话立即失效（改密/封禁场景） */
    @PostMapping("/kick-out")
    public R<Void> kickOut(@Valid @RequestBody KickOutRequest req) {
        tokenService.revokeAll(req.getAccountId());
        return R.ok();
    }

    /** 修改当前用户密码：校验原密码 + 新密码策略 + 改密后全端下线 */
    @PostMapping("/change-password")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        LoginUser current = currentUser();
        String storedHash = iamService.passwordHash(current.getAccountNo());
        if (storedHash == null || !passwordEncoder.matches(req.getOldPassword(), storedHash)) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "原密码不正确");
        }
        passwordPolicyValidator.validate(req.getNewPassword());
        iamService.changePassword(current.getAccountId(), passwordEncoder.encode(req.getNewPassword()));
        tokenService.revokeAll(current.getAccountId());
        return R.ok();
    }

    /** 签发 token 的统一出口：accountType 由认证策略装配决定，禁止在此写死覆盖 */
    private Map<String, Object> buildTokenResponse(LoginUser loginUser) {
        loginUser.setDevice("internal");
        SessionInfo session = iamService.sessionOf(loginUser);
        String token = tokenService.create(session, false);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("needSelectTenant", false);
        m.put("token", token);
        m.put("accountId", loginUser.getAccountId());
        m.put("tenantId", loginUser.getTenantId());
        m.put("accountType", loginUser.getAccountType());
        m.put("roles", loginUser.getRoles());
        m.put("permissions", loginUser.getPermissions());
        return m;
    }

    private Map<String, Object> tenantBrief(SysTenant t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tenantId", t.getId());
        m.put("tenantCode", t.getTenantCode());
        m.put("tenantName", t.getTenantName());
        return m;
    }

    /** 按认证方式组装凭证：账密取 username/password，社交取 provider/code */
    private static Map<String, String> credentialsOf(String authType, LoginRequest req, String ip) {
        Map<String, String> map = new LinkedHashMap<>();
        if (AuthType.PASSWORD.equals(authType)) {
            map.put("username", req.getUsername());
            map.put("password", req.getPassword());
            map.put("ip", ip);
        } else {
            map.put("provider", req.getProvider());
            map.put("code", req.getCode());
        }
        return map;
    }

    private static String resolveToken(String authorization, String xToken) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return xToken;
    }

    private LoginUser currentUser() {
        return LoginContext.current();
    }
}
