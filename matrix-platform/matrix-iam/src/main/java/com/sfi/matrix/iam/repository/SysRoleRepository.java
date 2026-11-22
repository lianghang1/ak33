package com.sfi.matrix.iam.repository;

import com.sfi.matrix.iam.entity.SysRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 角色 Repository。
 *
 * @author matrix
 */
@Repository
public interface SysRoleRepository extends JpaRepository<SysRole, Long> {

    Optional<SysRole> findByRoleCodeAndDeletedFalse(String roleCode);

    /** 按租户查角色（登录无态阶段显式隔离用） */
    Optional<SysRole> findByRoleCodeAndTenantIdAndDeletedFalse(String roleCode, String tenantId);

    List<SysRole> findByIdInAndDeletedFalse(List<Long> roleIds);

    /** 按租户 + 角色类型查（识别内置超管角色） */
    List<SysRole> findByTenantIdAndRoleTypeAndDeletedFalse(String tenantId, String roleType);
}
