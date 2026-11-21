package com.sfi.matrix.iam.controller;

import com.sfi.matrix.iam.entity.SysTenant;
import com.sfi.matrix.iam.service.IamService;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.common.util.IpUtils;
import com.sfi.scaffold.starter.protection.ratelimit.RateLimit;
import com.sfi.scaffold.starter.security.anonymous.Anonymous;
import com.sfi.scaffold.starter.security.captcha.CaptchaService;
import com.sfi.scaffold.starter.security.identity.AccountType;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.security.identity.LoginUser;
import com.sfi.scaffold.starter.security.policy.LoginFailureGuard;
import com.sfi.scaffold.starter.security.token.SessionInfo;
import com.sfi.scaffold.starter.security.token.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 登录入口（一人多租户）：
 * <ol>
 *   <li>login-tenants：账号密码校验通过后返回可登录的租户列表；</li>
 *   <li>login：账号密码 + tenantCode 选定租户，建 token；</li>
 *   <li>switch-tenant：带 token 切换到该账号的另一个租户，换新 token。</li>
 * </ol>
 * 会话由 scaffold TokenService 承载，密码 BCrypt 校验。
 *
 * @author matrix
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IamService iamService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final LoginFailureGuard loginFailureGuard;
    private final CaptchaService captchaService;

    public AuthController(IamService iamService, TokenService tokenService, PasswordEncoder passwordEncoder,
                          LoginFailureGuard loginFailureGuard, CaptchaService captchaService) {
        this.iamService = iamService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.loginFailureGuard = loginFailureGuard;
        this.captchaService = captchaService;
    }

    /**
     * 登录防刷前置校验（全部走 scaffold 组件）：账号锁定 → IP 封锁 → 按需验证码。
     * 失败计数由 captcha.after_fail 模式共用到 captchaService.required()。
     */
    private void guardBeforeLogin(String username, String ip, String captchaId, String captchaCode) {
        if (loginFailureGuard.isLocked(username)) {
            throw BizException.of(GlobalErrorCode.TOO_MANY_REQUESTS, "账号已临时锁定，请稍后再试");
        }
        if (loginFailureGuard.isIpBlocked(ip)) {
            throw BizException.of(GlobalErrorCode.TOO_MANY_REQUESTS, "当前 IP 登录失败次数过多，请稍后再试");
        }
        if (captchaService.required(loginFailureGuard.currentCount(username))
                && !captchaService.verifyAndConsume(captchaId, captchaCode)) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "验证码错误或已失效");
        }
    }

    /** 密码校验：失败记账号+IP两维计数，成功清零。 */
    private void checkPassword(String username, String password, String ip) {
        String storedHash = iamService.passwordHash(username);
        if (storedHash == null || !passwordEncoder.matches(password, storedHash)) {
            loginFailureGuard.recordFailure(username);
            loginFailureGuard.recordIpFailure(ip);
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        loginFailureGuard.reset(username);
    }

    /** 生成登录验证码（匿名放行）：返回 captchaId 与 base64 图片，登录时回填 captchaId/captchaCode */
    @Anonymous
    @RateLimit(limit = 30, windowSeconds = 60, dimension = "ip")
    @PostMapping("/captcha")
    public R<Map<String, String>> captcha() {
        return R.ok(captchaService.generate());
    }

    /** 第一步：校验账号密码，返回该账号可登录的租户列表（匿名放行） */
    @Anonymous
    @RateLimit(limit = 20, windowSeconds = 60, dimension = "ip")
    @PostMapping("/login-tenants")
    public R<List<Map<String, Object>>> loginTenants(@RequestParam String username,
                                                     @RequestParam String password,
                                                     @RequestParam(required = false) String captchaId,
                                                     @RequestParam(required = false) String captchaCode,
                                                     HttpServletRequest request) {
        String ip = IpUtils.clientIp(request);
        guardBeforeLogin(username, ip, captchaId, captchaCode);
        checkPassword(username, password, ip);
        List<SysTenant> tenants = iamService.listLoginTenants(username);
        if (tenants.isEmpty()) {
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "账号未开通任何有效租户");
        }
        List<Map<String, Object>> result = tenants.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tenantId", t.getId());
            m.put("tenantCode", t.getTenantCode());
            m.put("tenantName", t.getTenantName());
            return m;
        }).collect(Collectors.toList());
        return R.ok(result);
    }

    /** 第二步：账号密码 + tenantCode 登录，返回 token（匿名放行） */
    @Anonymous
    @RateLimit(limit = 10, windowSeconds = 60, dimension = "ip")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestParam String username,
                                        @RequestParam String password,
                                        @RequestParam String tenantCode,
                                        @RequestParam(required = false) String captchaId,
                                        @RequestParam(required = false) String captchaCode,
                                        HttpServletRequest request) {
        String ip = IpUtils.clientIp(request);
        guardBeforeLogin(username, ip, captchaId, captchaCode);
        checkPassword(username, password, ip);
        LoginUser loginUser = iamService.buildLoginUser(username, tenantCode);
        return R.ok(buildTokenResponse(loginUser));
    }

    /** 切换租户：已登录用户切到该账号的另一个租户，返回新 token */
    @PostMapping("/switch-tenant")
    public R<Map<String, Object>> switchTenant(@RequestParam String tenantCode) {
        LoginUser current = currentUser();
        LoginUser loginUser = iamService.switchTenant(current.getAccountId(), tenantCode);
        return R.ok(buildTokenResponse(loginUser));
    }

    /**
     * 平台超管"进入"业务租户：不写租户成员/角色表，直接以目标租户 TENANT_ADMIN 身份签发 token。
     * 前置校验当前身份必须是平台超管；新 token 的 tenantId 为目标租户，attributes 记录来源。
     */
    @PostMapping("/enter-tenant")
    public R<Map<String, Object>> enterTenant(@RequestParam String tenantCode) {
        LoginUser current = currentUser();
        iamService.assertPlatformAdmin(current.getAccountId(), current.getTenantId());
        LoginUser loginUser = iamService.impersonateTenant(current.getAccountId(), tenantCode);
        return R.ok(buildTokenResponse(loginUser));
    }

    /** 当前登录身份回显 */
    @PostMapping("/me")
    public R<LoginUser> me() {
        return R.ok(currentUser());
    }

    private Map<String, Object> buildTokenResponse(LoginUser loginUser) {
        loginUser.setAccountType(AccountType.OPERATOR);
        loginUser.setDevice("internal");
        SessionInfo session = iamService.sessionOf(loginUser);
        String token = tokenService.create(session, false);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", token);
        m.put("accountId", loginUser.getAccountId());
        m.put("tenantId", loginUser.getTenantId());
        m.put("roles", loginUser.getRoles());
        m.put("permissions", loginUser.getPermissions());
        return m;
    }

    private LoginUser currentUser() {
        return LoginContext.current();
    }
}
