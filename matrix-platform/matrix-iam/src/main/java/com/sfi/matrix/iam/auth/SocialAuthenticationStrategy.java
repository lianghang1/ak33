package com.sfi.matrix.iam.auth;

import com.sfi.matrix.iam.entity.SysUser;
import com.sfi.matrix.iam.entity.SysUserProfile;
import com.sfi.matrix.iam.entity.SysUserSocial;
import com.sfi.matrix.iam.repository.SysUserProfileRepository;
import com.sfi.matrix.iam.repository.SysUserRepository;
import com.sfi.matrix.iam.repository.SysUserSocialRepository;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.starter.security.online.ExternalIdentity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 社交登录门面策略：社交登录体系的唯一入口，type 固定为 SOCIAL。
 *
 * <p><b>门面 = 骨架，不是实现</b>：本策略把社交登录的完整流程收口在一个地方——
 * 取 provider → 凭证核验 → 外部身份 → 绑定表找/建账号 → 统一 SysUser。骨架本身
 * <b>不接任何具体登录源</b>（微信/飞书/小程序等都不做），凭证核验委托
 * {@link ConfluxIdentityResolver}——由 conflux 门面完成授权回调、state 校验、票据验签，
 * 本策略只把它返回的身份翻译成账号，不碰任何授权流程细节。</p>
 *
 * <p>{@link #resolveIdentity(String, Map)} 仍是可覆盖扩展点：默认走 conflux 核验；
 * 特殊登录源可覆盖本方法自行核验并返回 {@link ExternalIdentity}，账号绑定/注册/资料建档
 * 等公共流程全部复用本门面，无需重写。</p>
 *
 * @author matrix
 */
@Component
public class SocialAuthenticationStrategy implements AuthenticationStrategy {

    private final SysUserSocialRepository socialRepository;
    private final SysUserRepository userRepository;
    private final SysUserProfileRepository profileRepository;
    private final ConfluxIdentityResolver identityResolver;

    public SocialAuthenticationStrategy(SysUserSocialRepository socialRepository,
                                        SysUserRepository userRepository,
                                        SysUserProfileRepository profileRepository,
                                        ConfluxIdentityResolver identityResolver) {
        this.socialRepository = socialRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.identityResolver = identityResolver;
    }

    @Override
    public String type() {
        return AuthType.SOCIAL;
    }

    /** 门面主流程：凭证 → 账号，骨架全程在这里，子类无需重复。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser authenticate(Map<String, String> credentials) {
        String provider = credentials.get("provider");
        if (provider == null) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "社交登录缺少 provider");
        }

        // ① 凭证核验：交给扩展点，骨架不解释凭证内容（code / ticket / SAMLResponse 由子类自取）
        ExternalIdentity identity = resolveIdentity(provider, credentials);
        if (identity == null || identity.getOpenId() == null) {
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "社交身份换取失败");
        }

        // ② 统一外部身份 → 绑定表找账号
        SysUserSocial social = socialRepository
                .findByProviderAndOpenIdAndDeletedFalse(provider, identity.getOpenId()).orElse(null);
        // ③ 有 unionId 时按 unionId 兜底（同一用户多应用 openId 不同）
        if (social == null && identity.getUnionId() != null) {
            List<SysUserSocial> byUnion = socialRepository
                    .findByProviderAndUnionIdAndDeletedFalse(provider, identity.getUnionId());
            if (!byUnion.isEmpty()) {
                social = byUnion.get(0);
            }
        }

        if (social != null) {
            SysUser user = userRepository.findById(social.getUserId()).orElse(null);
            if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
                throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "绑定账号不存在或已禁用");
            }
            return user;
        }

        // ④ 未绑定：注册即登录，建 MEMBER 账号 + 资料 + 绑定关系
        SysUser user = new SysUser();
        user.setAccountType("MEMBER");
        user.setUsername(provider + "_" + identity.getOpenId());
        user.setStatus(1);
        user = userRepository.save(user);

        SysUserProfile profile = new SysUserProfile();
        profile.setUserId(user.getId());
        profile.setNickname(identity.getNickname());
        profile.setAvatar(identity.getAvatar());
        profileRepository.save(profile);

        SysUserSocial binding = new SysUserSocial();
        binding.setUserId(user.getId());
        binding.setProvider(provider);
        binding.setOpenId(identity.getOpenId());
        binding.setUnionId(identity.getUnionId());
        socialRepository.save(binding);
        return user;
    }

    /**
     * 凭证核验扩展点：把登录源凭证核验为统一外部身份。
     *
     * <p>默认委托 {@link ConfluxIdentityResolver} 走 conflux 门面核验（授权回调/state/验签
     * 全在 conflux 内部）。特殊登录源可覆盖本方法自行核验并返回 {@link ExternalIdentity}。</p>
     *
     * @param provider    登录源标识
     * @param credentials 登录骨架转交的凭证（本策略不解释其内容）
     * @return 统一外部身份
     */
    protected ExternalIdentity resolveIdentity(String provider, Map<String, String> credentials) {
        return identityResolver.resolve(provider, credentials);
    }
}
