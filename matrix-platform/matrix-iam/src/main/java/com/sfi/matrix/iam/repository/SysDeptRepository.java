package com.sfi.matrix.iam.repository;

import com.sfi.matrix.iam.entity.SysDept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 部门 Repository。
 *
 * @author matrix
 */
@Repository
public interface SysDeptRepository extends JpaRepository<SysDept, Long> {

    List<SysDept> findByDeletedFalseOrderBySortNoAsc();
}
