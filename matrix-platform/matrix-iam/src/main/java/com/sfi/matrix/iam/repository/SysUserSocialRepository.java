package com.sfi.matrix.iam.repository;

import com.sfi.matrix.iam.entity.SysUserSocial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户社交绑定 Repository（1:N）。
 *
 * @author lianghang
 */
@Repository
public interface SysUserSocialRepository extends JpaRepository<SysUserSocial, Long> {

    List<SysUserSocial> findByUserIdAndDeletedFalse(Long userId);

    Optional<SysUserSocial> findByProviderAndOpenIdAndDeletedFalse(String provider, String openId);

    /** 按 unionId 找同一个人（跨应用账号合并用）。 */
    List<SysUserSocial> findByProviderAndUnionIdAndDeletedFalse(String provider, String unionId);
}
