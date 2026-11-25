package com.sfi.matrix.iam.controller;

import com.sfi.matrix.iam.audit.service.AuditService;
import com.sfi.matrix.iam.auth.AuthService;
import com.sfi.matrix.iam.auth.AuthType;
import com.sfi.matrix.iam.dto.ChangePasswordRequest;
import com.sfi.matrix.iam.dto.KickOutRequest;
import com.sfi.matrix.iam.dto.LoginRequest;
import com.sfi.matrix.iam.dto.LoginTenantsRequest;
import com.sfi.matrix.iam.dto.SocialLoginRequest;
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
 * 登录入口（多方式归一 + 一人多租户）：
 * <ol>
 *   <li>登录方式经 {@link AuthService} 分发到认证策略（账密 / 社交 / 短信），
 *       统一产出账号后走公用装配 {@code IamService.assembleForAuth}；</li>
 *   <li>login-tenants 校验凭证后返回可登录租户列表；login 选定租户签发 token；</li>
 *   <li>switch-tenant / enter-tenant 已登录态换租户；logout 走框架 TokenService 撤销会话。</li>
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
     * 账密方式才做账号维度锁定；社交/短信以 IP 维度为准。
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

    /** 第一步：账密校验通过，返回该账号可登录的租户列表（匿名放行） */
    @Anonymous
    @RateLimit(limit = 20, windowSeconds = 60, dimension = "ip")
    @PostMapping("/login-tenants")
    public R<List<Map<String, Object>>> loginTenants(@Valid @RequestBody LoginTenantsRequest req,
                                                     HttpServletRequest request) {
        String ip = IpUtils.clientIp(request);
        guardBeforeLogin(req.getUsername(), ip, req.getCaptchaId(), req.getCaptchaCode());
        SysUser user = authService.authenticate(AuthType.PASSWORD, credentialsOf(req.getUsername(), req.getPassword(), ip, null, null));
        List<SysTenant> tenants = iamService.listLoginTenantsByUser(user);
        if (tenants.isEmpty()) {
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "账号未开通任何有效租户");
        }
        return R.ok(tenants.stream().map(this::tenantBrief).collect(Collectors.toList()));
    }

    /** 第二步：账密 + 租户编码登录，签发 token（匿名放行） */
    @Anonymous
    @RateLimit(limit = 10, windowSeconds = 60, dimension = "ip")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        try {
            String ip = IpUtils.clientIp(request);
            guardBeforeLogin(req.getUsername(), ip, req.getCaptchaId(), req.getCaptchaCode());
            LoginUser loginUser = authService.authenticateAndAssemble(AuthType.PASSWORD,
                    credentialsOf(req.getUsername(), req.getPassword(), ip, null, null), req.getTenantCode());
            auditService.recordLogin(req.getUsername(), AuthType.PASSWORD, req.getTenantCode(),
                    request, true, null);
            return R.ok(buildTokenResponse(loginUser));
        } catch (RuntimeException e) {
            auditService.recordLogin(req.getUsername(), AuthType.PASSWORD, req.getTenantCode(),
                    request, false, e.getMessage());
            throw e;
        }
    }

    /** 社交登录（匿名放行）：provider + code → 外部身份 → 统一装配 */
    @Anonymous
    @RateLimit(limit = 10, windowSeconds = 60, dimension = "ip")
    @PostMapping("/social-login")
    public R<Map<String, Object>> socialLogin(@Valid @RequestBody SocialLoginRequest req,
                                              HttpServletRequest request) {
        try {
            LoginUser loginUser = authService.authenticateAndAssemble(AuthType.SOCIAL,
                    credentialsOf(null, null, null, req.getProvider(), req.getCode()), req.getTenantCode());
            auditService.recordLogin(req.getProvider(), AuthType.SOCIAL, req.getTenantCode(),
                    request, true, null);
            return R.ok(buildTokenResponse(loginUser));
        } catch (RuntimeException e) {
            auditService.recordLogin(req.getProvider(), AuthType.SOCIAL, req.getTenantCode(),
                    request, false, e.getMessage());
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

    private static Map<String, String> credentialsOf(String username, String password, String ip,
                                                     String provider, String code) {
        Map<String, String> map = new LinkedHashMap<>();
        if (username != null) map.put("username", username);
        if (password != null) map.put("password", password);
        if (ip != null) map.put("ip", ip);
        if (provider != null) map.put("provider", provider);
        if (code != null) map.put("code", code);
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
