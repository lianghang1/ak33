package com.sfi.matrix.iam.repository;

import com.sfi.matrix.iam.entity.SysUserTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户-租户归属 Repository（不参与租户过滤，登录时要跨租户查全部归属）。
 *
 * @author lianghang
 */
@Repository
public interface SysUserTenantRepository extends JpaRepository<SysUserTenant, Long> {

    /** 查账号挂的所有租户（登录选租户用，跨租户） */
    List<SysUserTenant> findByUserIdAndDeletedFalse(Long userId);

    /** 查账号在指定租户下的归属（切换/建身份用） */
    Optional<SysUserTenant> findByUserIdAndTenantIdAndDeletedFalse(Long userId, String tenantId);

    void deleteByUserId(Long userId);

    /**
     * 按部门统计用户数（部门树节点显示人数用）。
     * 返回每行 [deptId, count]，deptId 为 null 表示未分配部门的用户。
     */
    @Query("select ut.deptId, count(distinct ut.userId) from SysUserTenant ut " +
            "where ut.tenantId = :tenantId and ut.deleted = false and ut.status = 1 " +
            "group by ut.deptId")
    List<Object[]> countUsersByDept(@Param("tenantId") String tenantId);
}
