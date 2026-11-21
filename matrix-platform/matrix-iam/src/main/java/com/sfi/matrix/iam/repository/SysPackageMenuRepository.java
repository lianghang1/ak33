package com.sfi.matrix.iam.repository;

import com.sfi.matrix.iam.entity.SysPackageMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 套餐-菜单 Repository（全局，不参与租户过滤）。
 *
 * @author matrix
 */
@Repository
public interface SysPackageMenuRepository extends JpaRepository<SysPackageMenu, Long> {

    List<SysPackageMenu> findByPackageIdAndDeletedFalse(Long packageId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SysPackageMenu p where p.packageId = :packageId")
    void deleteByPackageId(@Param("packageId") Long packageId);
}
