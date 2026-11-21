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
import com.sfi.scaffold.starter.security.online.OAuthIdentityProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 社交认证策略：授权 code → 外部身份 → 绑定表找/建账号 → 统一 SysUser。
 *
 * <p><b>架构就位、实现可插拔</b>：本策略只依赖框架 SPI {@link OAuthIdentityProvider}，
 * 不接任何具体第三方。业务接入微信/支付宝/企业微信时，只需提供一个
 * {@code OAuthIdentityProvider} Bean（code 换 openId/unionId），无需改登录主流程。</p>
 *
 * <p>没有配置任何实现时明确抛"未接入登录源"，而不是静默放行。</p>
 *
 * @author matrix
 */
@Component
public class SocialAuthenticationStrategy implements AuthenticationStrategy {

    private final ObjectProvider<OAuthIdentityProvider> providers;
    private final SysUserSocialRepository socialRepository;
    private final SysUserRepository userRepository;
    private final SysUserProfileRepository profileRepository;

    public SocialAuthenticationStrategy(ObjectProvider<OAuthIdentityProvider> providers,
                                        SysUserSocialRepository socialRepository,
                                        SysUserRepository userRepository,
                                        SysUserProfileRepository profileRepository) {
        this.providers = providers;
        this.socialRepository = socialRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @Override
    public String type() {
        return AuthType.SOCIAL;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser authenticate(Map<String, String> credentials) {
        String provider = credentials.get("provider");
        String code = credentials.get("code");
        if (provider == null || code == null) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "社交登录缺少 provider 或 code");
        }

        OAuthIdentityProvider identityProvider = providers.stream()
                .filter(p -> provider.equals(p.provider()))
                .findFirst()
                .orElseThrow(() -> BizException.of(GlobalErrorCode.BAD_REQUEST,
                        "登录源未接入：" + provider));

        ExternalIdentity identity = identityProvider.exchange(code);
        if (identity == null || identity.getOpenId() == null) {
            throw BizException.of(GlobalErrorCode.UNAUTHORIZED, "社交身份换取失败");
        }

        // ① 先按 provider + openId 找绑定
        SysUserSocial social = socialRepository
                .findByProviderAndOpenIdAndDeletedFalse(provider, identity.getOpenId()).orElse(null);
        // ② 有 unionId 时按 unionId 兜底（同一用户多应用 openId 不同）
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

        // ③ 未绑定：按"账号即主体"模式新建一个 MEMBER 账号并落绑定关系（注册即登录）
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
}
