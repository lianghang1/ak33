package com.sfi.matrix.iam.auth;

import com.sfi.matrix.iam.entity.SysUser;

import java.util.Map;

/**
 * 认证策略：把"证明你是谁"（凭证校验）从"你能看到什么"（身份装配）中剥离。
 *
 * <p>每种登录方式（账密 / 短信 / 社交）实现一个策略，统一产出 {@link SysUser}；
 * 后续由 IamService 的公用装配逻辑按租户组装 LoginUser。新增登录方式不改登录主流程，
 * 只需新增一个策略 Bean，符合开闭原则。</p>
 *
 * @author lianghang
 */
public interface AuthenticationStrategy {

    /** 认证方式标识，见 {@link AuthType} */
    String type();

    /**
     * 校验凭证并返回账号。校验失败抛 BizException。
     *
     * @param credentials 凭证参数（username/password 或 code/openId 等，按策略约定）
     * @return 校验通过的账号
     */
    SysUser authenticate(Map<String, String> credentials);
}
