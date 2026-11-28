package com.sfi.matrix.iam.service;

import com.sfi.matrix.iam.entity.SysUser;
import com.sfi.matrix.iam.entity.SysUserProfile;
import com.sfi.matrix.iam.repository.SysUserProfileRepository;
import com.sfi.matrix.iam.repository.SysUserRepository;
import com.sfi.scaffold.starter.data.jpa.service.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户单实体服务：get/list/page/save/delete 骨架继承 BaseService，本类只承载用户的业务规则。
 *
 * <p>用户资料（sys_user_profile）随用户增改幂等维护；建用户的密码编码在 Controller 完成后传入，
 * 跨实体的租户归属/角色分配属于聚合编排，保留在 {@link IamService}。</p>
 *
 * @author lianghang
 */
@Service
public class UserService extends BaseService<SysUser> {

    private final SysUserRepository userRepository;
    private final SysUserProfileRepository userProfileRepository;

    public UserService(SysUserRepository userRepository, SysUserProfileRepository userProfileRepository) {
        super(userRepository);
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    /** 新增用户：用户名唯一校验 + 幂等建资料。encodedPassword 由 Controller 编码后传入。 */
    @Transactional(rollbackFor = Exception.class)
    public SysUser create(String username, String encodedPassword, String nickname) {
        return create(username, encodedPassword, nickname, null, null, null, null, null, null);
    }

    /**
     * 新增用户（完整字段）：修复此前新增时 mobile/email/status/maxSessions/sex/remark 被丢弃的问题。
     * encodedPassword 由 Controller 编码后传入。
     */
    @Transactional(rollbackFor = Exception.class)
    public SysUser create(String username, String encodedPassword, String nickname,
                          String mobile, String email, Integer status, Integer maxSessions,
                          Integer sex, String remark) {
        userRepository.findByUsernameAndDeletedFalse(username).ifPresent(u -> {
            throw new IllegalArgumentException("用户名已存在：" + username);
        });
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setStatus(status == null ? 1 : status);
        user.setMobile(mobile);
        user.setEmail(email);
        if (maxSessions != null) {
            user.setMaxSessions(maxSessions);
        }
        user = userRepository.save(user);
        saveProfile(user.getId(), nickname, sex, remark);
        return user;
    }

    /** 修改用户：基础字段 + 幂等更新资料。查不到由 require 抛 NOT_FOUND。 */
    @Transactional(rollbackFor = Exception.class)
    public SysUser update(Long id, String nickname, String mobile, String email,
                          String accountType, Integer status, Integer maxSessions) {
        return update(id, nickname, mobile, email, accountType, status, maxSessions, null, null);
    }

    /** 修改用户（完整字段）：含资料 sex/remark。 */
    @Transactional(rollbackFor = Exception.class)
    public SysUser update(Long id, String nickname, String mobile, String email,
                          String accountType, Integer status, Integer maxSessions,
                          Integer sex, String remark) {
        SysUser u = require(id);
        if (mobile != null) {
            u.setMobile(mobile);
        }
        if (email != null) {
            u.setEmail(email);
        }
        if (accountType != null && !accountType.trim().isEmpty()) {
            u.setAccountType(accountType);
        }
        if (status != null) {
            u.setStatus(status);
        }
        if (maxSessions != null) {
            u.setMaxSessions(maxSessions);
        }
        u = userRepository.save(u);
        saveProfile(id, nickname, sex, remark);
        return u;
    }

    /** 建/更新用户资料，幂等。公开供 IamService 编排层（建租户管理员/平台超管）复用。 */
    @Transactional(rollbackFor = Exception.class)
    public void saveProfile(Long userId, String nickname) {
        saveProfile(userId, nickname, null, null);
    }

    /** 建/更新用户资料（完整字段），幂等；全部字段为 null 时不落库。 */
    @Transactional(rollbackFor = Exception.class)
    public void saveProfile(Long userId, String nickname, Integer sex, String remark) {
        if (nickname == null && sex == null && remark == null) {
            return;
        }
        SysUserProfile profile = userProfileRepository.findByUserIdAndDeletedFalse(userId)
                .orElseGet(() -> {
                    SysUserProfile p = new SysUserProfile();
                    p.setUserId(userId);
                    return p;
                });
        if (nickname != null) {
            profile.setNickname(nickname);
        }
        if (sex != null) {
            profile.setSex(sex);
        }
        if (remark != null) {
            profile.setRemark(remark);
        }
        userProfileRepository.save(profile);
    }

    /** 读取用户资料（1:1），无资料返回 null。 */
    public SysUserProfile profileOf(Long userId) {
        return userProfileRepository.findByUserIdAndDeletedFalse(userId).orElse(null);
    }
}
