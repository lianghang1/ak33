package com.sfi.matrix.support.param.repository;

import com.sfi.matrix.support.param.entity.SysConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
