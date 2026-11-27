package com.sfi.matrix.iam.repository;

import com.sfi.matrix.iam.entity.SysOperateLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 操作审计日志 Repository（租户过滤由 @Filter 自动完成）。 */
@Repository
public interface SysOperateLogRepository extends JpaRepository<SysOperateLog, Long> {
}
