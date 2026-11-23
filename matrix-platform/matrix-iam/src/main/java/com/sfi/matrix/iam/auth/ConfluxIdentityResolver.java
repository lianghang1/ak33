package com.sfi.matrix.iam.auth;

import com.sfi.conflux.auth.api.AuthCallbackRequest;
import com.sfi.conflux.auth.api.AuthResult;
import com.sfi.conflux.auth.api.AuthUser;
import com.sfi.conflux.auth.engine.AuthGateway;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.starter.security.online.ExternalIdentity;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Conflux 身份核验适配器：把 matrix 登录骨架转交的凭证交给 conflux {@link AuthGateway} 换身份，
 * 并把 conflux 的统一身份 {@link AuthUser} 转成 scaffold 的 {@link ExternalIdentity}。
 *
 * <p><b>职责边界</b>：授权跳转、回调、state 校验、票据验签全部在 conflux 门面内部闭环；
 * 本类只做两件事——把凭证原样转交 conflux、把结果翻译成骨架认识的身份对象。
 * matrix 登录骨架不碰任何授权流程细节。</p>
 *
 * <p><b>凭证映射</b>：{@code channelCode} 取 credentials 的 {@code channelCode}（缺省用 provider），
 * {@code configId} 取 {@code configId}（缺省用 provider）；整个 credentials map 作为原始回调参数
 * 交给 conflux，code/state 等键由 conflux 按渠道自行取用、框架不写死字段名。</p>
 *
 * <p>独成 Bean 而非写死在门面策略里，是为了可插拔：有 conflux starter 时本 Bean 存在、社交登录
 * 走真实核验；单测可 mock 本 Bean 隔离 conflux。</p>
 *
 * @author matrix
 */
@Component
public class ConfluxIdentityResolver {

    private final AuthGateway authGateway;

    public ConfluxIdentityResolver(AuthGateway authGateway) {
        this.authGateway = authGateway;
    }

    /**
     * 核验凭证换统一身份。
     *
     * @param provider    登录源标识（同时作为缺省 channelCode / configId）
     * @param credentials 登录骨架转交的凭证（原样转交 conflux）
     * @return scaffold 统一外部身份
     */
    public ExternalIdentity resolve(String provider, Map<String, String> credentials) {
        String configId = firstNonBlank(credentials.get("configId"), provider);
        String channelCode = firstNonBlank(credentials.get("channelCode"), provider);

        AuthResult<AuthUser> result = authGateway.parseCallback(
                configId, channelCode, new AuthCallbackRequest(credentials));
        if (result == null || !result.isSuccess() || result.getData() == null) {
            String code = result == null ? "CONFLUX_NO_RESULT" : result.getErrorCode();
            String msg = result == null ? "conflux 未返回结果" : result.getErrorMsg();
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED,
                    "社交身份核验失败[" + code + "]：" + msg);
        }
        return toExternalIdentity(result.getData());
    }

    /** AuthUser → ExternalIdentity：字段一一对应，缺失资料留空。 */
    private static ExternalIdentity toExternalIdentity(AuthUser user) {
        ExternalIdentity identity = new ExternalIdentity();
        identity.setProvider(user.getChannelCode());
        identity.setOpenId(user.getOpenId());
        identity.setUnionId(user.getUnionId());
        identity.setNickname(user.getUsername());
        identity.setAvatar(user.getAvatar());
        return identity;
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.trim().isEmpty() ? a : b;
    }
}
