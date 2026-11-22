package com.sfi.matrix.support.repository;

import com.sfi.matrix.support.entity.SysConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统参数 Repository。
 *
 * @author matrix
 */
@Repository
public interface SysConfigRepository extends JpaRepository<SysConfig, Long> {

    Optional<SysConfig> findByConfigKeyAndDeletedFalse(String configKey);

    Optional<SysConfig> findByTenantIdAndConfigKeyAndDeletedFalse(String tenantId, String configKey);

    Optional<SysConfig> findByTenantIdAndConfigGroupAndConfigKeyAndDeletedFalse(
            String tenantId, String configGroup, String configKey);

    List<SysConfig> findByTenantIdAndConfigGroupAndDeletedFalseOrderByConfigKeyAsc(
            String tenantId, String configGroup);
}
