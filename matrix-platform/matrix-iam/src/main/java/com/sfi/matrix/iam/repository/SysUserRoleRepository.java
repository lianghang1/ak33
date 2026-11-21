package com.sfi.matrix.iam.repository;

import com.sfi.matrix.iam.entity.SysUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户-角色关联 Repository。
 *
 * @author matrix
 */
@Repository
public interface SysUserRoleRepository extends JpaRepository<SysUserRole, Long> {

    List<SysUserRole> findByUserIdAndDeletedFalse(Long userId);

    /** 按用户+租户查角色关联（登录/切租户无态阶段显式隔离） */
    List<SysUserRole> findByUserIdAndTenantIdAndDeletedFalse(Long userId, String tenantId);

    void deleteByUserId(Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SysUserRole r where r.userId = :userId and r.tenantId = :tenantId")
    void deleteByUserIdAndTenantId(@Param("userId") Long userId, @Param("tenantId") String tenantId);
}
