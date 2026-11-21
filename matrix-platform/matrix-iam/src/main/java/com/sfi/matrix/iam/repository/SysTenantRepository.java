package com.sfi.matrix.iam.repository;

import com.sfi.matrix.iam.entity.SysTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 租户 Repository。
 *
 * @author matrix
 */
@Repository
public interface SysTenantRepository extends JpaRepository<SysTenant, Long> {

    Optional<SysTenant> findByTenantCodeAndDeletedFalse(String tenantCode);

    List<SysTenant> findByIdInAndDeletedFalse(List<Long> tenantIds);
}
