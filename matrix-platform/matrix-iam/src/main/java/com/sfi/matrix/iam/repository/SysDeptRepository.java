package com.sfi.matrix.iam.repository;

import com.sfi.matrix.iam.entity.SysDept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 部门 Repository。
 *
 * @author lianghang
 */
@Repository
public interface SysDeptRepository extends JpaRepository<SysDept, Long> {

    /** 按租户查未删除部门（数据权限部门子树构树用）。 */
    List<SysDept> findByTenantIdAndDeletedFalse(String tenantId);
}
