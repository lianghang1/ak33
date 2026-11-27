package com.sfi.matrix.iam.repository;

import com.sfi.matrix.iam.entity.SysLoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 登录审计日志 Repository。 */
@Repository
public interface SysLoginLogRepository extends JpaRepository<SysLoginLog, Long> {
}
