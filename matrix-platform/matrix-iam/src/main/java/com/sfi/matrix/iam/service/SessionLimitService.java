package com.sfi.matrix.iam.service;

import com.sfi.matrix.iam.entity.SysUser;
import com.sfi.matrix.iam.repository.SysUserRepository;
import com.sfi.scaffold.starter.security.token.SessionInfo;
import com.sfi.scaffold.starter.security.token.TokenService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 多点登录会话限量：签发新会话前，按设备和应用层上限清理该账号的旧会话。
 *
 * <p>同一 deviceId 重登先顶掉自身旧会话（不占其他设备名额）；剩余会话数达到上限时，
 * 按登录时间从早到晚踢除，为新会话留出位置。上限取用户 max_sessions，为空时取全局默认。</p>
 *
 * @author lianghang
 */
@Service
public class SessionLimitService {

    /** 全局默认最大在线会话数，用户未单独配置时生效 */
    public static final int DEFAULT_MAX_SESSIONS = 5;

    private final TokenService tokenService;
    private final SysUserRepository userRepository;

    public SessionLimitService(TokenService tokenService, SysUserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    /**
     * 签发新会话前清理旧会话。deviceId 为空时跳过同设备顶替，仅按数量限量。
     *
     * @param accountId 账号ID
     * @param deviceId  本次登录的物理设备标识
     */
    public void enforceBeforeCreate(Long accountId, String deviceId) {
        int max = maxSessionsOf(accountId);
        Map<String, SessionInfo> sessions = tokenService.onlineSessionTokens(accountId);
        if (sessions.isEmpty()) {
            return;
        }

        // 同设备重登：顶掉该设备的旧会话，不占其他设备名额
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            List<String> sameDeviceTokens = new ArrayList<String>();
            for (Map.Entry<String, SessionInfo> e : sessions.entrySet()) {
                if (deviceId.equals(e.getValue().getDeviceId())) {
                    sameDeviceTokens.add(e.getKey());
                }
            }
            for (String token : sameDeviceTokens) {
                tokenService.revoke(token);
                sessions.remove(token);
            }
        }

        // 按数量限量：剩余达到上限时踢最早的，留出一个新会话位置
        if (sessions.size() >= max) {
            List<Map.Entry<String, SessionInfo>> ordered =
                    new ArrayList<Map.Entry<String, SessionInfo>>(sessions.entrySet());
            ordered.sort(new Comparator<Map.Entry<String, SessionInfo>>() {
                @Override
                public int compare(Map.Entry<String, SessionInfo> a, Map.Entry<String, SessionInfo> b) {
                    return Long.compare(a.getValue().getLoginTime(), b.getValue().getLoginTime());
                }
            });
            int revokeCount = sessions.size() - max + 1;
            for (int i = 0; i < revokeCount; i++) {
                tokenService.revoke(ordered.get(i).getKey());
            }
        }
    }

    /** 用户专属上限，为空或未配置时取全局默认；至少为 1 */
    private int maxSessionsOf(Long accountId) {
        Integer configured = userRepository.findById(accountId)
                .map(SysUser::getMaxSessions)
                .orElse(null);
        if (configured == null || configured < 1) {
            return DEFAULT_MAX_SESSIONS;
        }
        return configured;
    }
}
