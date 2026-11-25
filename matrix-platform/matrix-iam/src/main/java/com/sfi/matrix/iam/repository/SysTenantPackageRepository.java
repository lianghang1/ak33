package com.sfi.matrix.iam.repository;

import com.sfi.matrix.iam.entity.SysTenantPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 租户套餐 Repository（全局，不参与租户过滤）。
 *
 * @author lianghang
 */
@Repository
public interface SysTenantPackageRepository extends JpaRepository<SysTenantPackage, Long> {

    Optional<SysTenantPackage> findByPackageCodeAndDeletedFalse(String packageCode);
}
