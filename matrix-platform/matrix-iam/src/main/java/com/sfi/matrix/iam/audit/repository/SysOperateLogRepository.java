package com.sfi.matrix.iam.audit.repository;

import com.sfi.matrix.iam.audit.entity.SysOperateLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 操作审计日志 Repository（租户过滤由 @Filter 自动完成）。 */
@Repository
public interface SysOperateLogRepository extends JpaRepository<SysOperateLog, Long> {
}
