package com.sfi.matrix.iam.auth;

import com.sfi.matrix.iam.entity.SysUser;
import com.sfi.matrix.iam.repository.SysUserRepository;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.starter.security.policy.LoginFailureGuard;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 账密认证策略：查账号 + BCrypt 校验 + 失败计数（账号/IP 两维）。
 *
 * <p>把原来揉在 AuthController 里的 checkPassword 收敛到这里，
 * 所有登录入口共用同一套密码校验与防刷逻辑。</p>
 *
 * @author matrix
 */
@Component
public class PasswordAuthenticationStrategy implements AuthenticationStrategy {

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginFailureGuard failureGuard;

    public PasswordAuthenticationStrategy(SysUserRepository userRepository,
                                          PasswordEncoder passwordEncoder,
                                          LoginFailureGuard failureGuard) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.failureGuard = failureGuard;
    }

    @Override
    public String type() {
        return AuthType.PASSWORD;
    }

    @Override
    public SysUser authenticate(Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        String ip = credentials.get("ip");
        if (username == null || password == null) {
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        SysUser user = userRepository.findByUsernameAndDeletedFalse(username).orElse(null);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())
                || !passwordEncoder.matches(password, user.getPassword())) {
            failureGuard.recordFailure(username);
            if (ip != null) {
                failureGuard.recordIpFailure(ip);
            }
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        failureGuard.reset(username);
        return user;
    }
}
