package com.sfi.matrix.iam.repository;

import com.sfi.matrix.iam.entity.SysRoleMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色-菜单关联 Repository。
 *
 * @author lianghang
 */
@Repository
public interface SysRoleMenuRepository extends JpaRepository<SysRoleMenu, Long> {

    List<SysRoleMenu> findByRoleIdAndDeletedFalse(Long roleId);

    /** 按角色+租户查菜单关联（登录/切租户无态阶段显式隔离） */
    List<SysRoleMenu> findByRoleIdAndTenantIdAndDeletedFalse(Long roleId, String tenantId);

    List<SysRoleMenu> findByRoleIdInAndDeletedFalse(List<Long> roleIds);

    void deleteByRoleId(Long roleId);

    /**
     * 物理删除后立即 flush，避免同事务内「先删后插」时 Hibernate 把 insert 排在 delete 前，
     * 撞上 (tenant_id, role_id, menu_id) 唯一约束。
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SysRoleMenu r where r.roleId = :roleId and r.tenantId = :tenantId")
    void deleteByRoleIdAndTenantId(@Param("roleId") Long roleId, @Param("tenantId") String tenantId);
}
