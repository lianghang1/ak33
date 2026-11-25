package com.sfi.matrix.iam.repository;

import com.sfi.matrix.iam.entity.SysUserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户资料 Repository（1:1）。
 *
 * @author lianghang
 */
@Repository
public interface SysUserProfileRepository extends JpaRepository<SysUserProfile, Long> {

    Optional<SysUserProfile> findByUserIdAndDeletedFalse(Long userId);
}
