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
 * <b>不接任何具体登录源</b>（微信/飞书/小程序/CAS 都不做），凭证核验仅留一个
 * 扩展点 {@link #resolveIdentity(String, Map)}，默认抛「未接入」显式失败而非静默放行。</p>
 *
 * <p>接入某个登录源时，只需覆盖 {@code resolveIdentity()}：自行从 credentials 取所需
 * 凭证键并核验、返回 {@link ExternalIdentity}。账号绑定/注册/资料建档等公共流程
 * 全部复用本门面，无需重写。</p>
 *
 * @author matrix
 */
@Component
public class SocialAuthenticationStrategy implements AuthenticationStrategy {

    private final SysUserSocialRepository socialRepository;
    private final SysUserRepository userRepository;
    private final SysUserProfileRepository profileRepository;

    public SocialAuthenticationStrategy(SysUserSocialRepository socialRepository,
                                        SysUserRepository userRepository,
                                        SysUserProfileRepository profileRepository) {
        this.socialRepository = socialRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
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
     * <p>骨架默认抛「未接入」，显式失败。接入具体登录源时覆盖本方法：自行从 credentials
     * 取所需键（code / ticket / SAMLResponse 等）、调用第三方核验、封装 ExternalIdentity 返回。
     * 不声明为抽象，是为了让门面本身可作为 SOCIAL 策略注册而无需强制子类。</p>
     *
     * @param provider    登录源标识
     * @param credentials 登录骨架转交的凭证（本策略不解释其内容）
     * @return 统一外部身份
     */
    protected ExternalIdentity resolveIdentity(String provider, Map<String, String> credentials) {
        throw BizException.of(GlobalErrorCode.BAD_REQUEST, "登录源未接入：" + provider);
    }
}
