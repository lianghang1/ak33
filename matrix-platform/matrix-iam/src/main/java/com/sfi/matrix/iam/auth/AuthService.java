package com.sfi.matrix.iam.auth;

import com.sfi.matrix.iam.entity.SysUser;
import com.sfi.matrix.iam.service.IamService;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.starter.security.identity.LoginUser;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 认证中枢：按方式分发到策略，统一产出账号，再走公用身份装配。
 *
 * <p>登录主流程 = 认证（策略） + 装配（IamService），所有登录方式共用。
 * 新增登录方式只需新增 {@link AuthenticationStrategy} Bean，无需改 Controller。</p>
 *
 * @author matrix
 */
@Service
public class AuthService {

    private final Map<String, AuthenticationStrategy> strategies;
    private final IamService iamService;

    public AuthService(List<AuthenticationStrategy> strategyList, IamService iamService) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(AuthenticationStrategy::type, Function.identity()));
        this.iamService = iamService;
    }

    /** 认证并装配：凭证 → 账号（策略） → LoginUser（公用装配） */
    public LoginUser authenticateAndAssemble(String authType, Map<String, String> credentials,
                                             String tenantCode) {
        AuthenticationStrategy strategy = strategies.get(authType);
        if (strategy == null) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "不支持的认证方式：" + authType);
        }
        SysUser user = strategy.authenticate(credentials);
        // 装配前再次确认密码方式之外账号状态正常（策略内已校验，这里兜底社交/短信新建账号）
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "账号已禁用");
        }
        return iamService.assembleForAuth(user, tenantCode);
    }

    /** 仅认证、不装配：用于"列租户"第一步（校验凭证后返回可登录租户） */
    public SysUser authenticate(String authType, Map<String, String> credentials) {
        AuthenticationStrategy strategy = strategies.get(authType);
        if (strategy == null) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "不支持的认证方式：" + authType);
        }
        return strategy.authenticate(credentials);
    }

    /** 凭证 map 便捷构造 */
    public static Map<String, String> credentials(String... kv) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(kv[i], kv[i + 1]);
        }
        return map;
    }
}
