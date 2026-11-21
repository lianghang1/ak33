package com.sfi.matrix.iam.repository;

import com.sfi.matrix.iam.entity.SysUserTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户-租户归属 Repository（不参与租户过滤，登录时要跨租户查全部归属）。
 *
 * @author matrix
 */
@Repository
public interface SysUserTenantRepository extends JpaRepository<SysUserTenant, Long> {

    /** 查账号挂的所有租户（登录选租户用，跨租户） */
    List<SysUserTenant> findByUserIdAndDeletedFalse(Long userId);

    /** 查账号在指定租户下的归属（切换/建身份用） */
    Optional<SysUserTenant> findByUserIdAndTenantIdAndDeletedFalse(Long userId, String tenantId);

    void deleteByUserId(Long userId);
}
