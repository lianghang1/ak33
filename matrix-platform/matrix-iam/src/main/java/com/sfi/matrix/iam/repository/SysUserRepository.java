package com.sfi.matrix.iam.repository;

import com.sfi.matrix.iam.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户 Repository。
 *
 * @author lianghang
 */
@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    Optional<SysUser> findByUsernameAndDeletedFalse(String username);
}
