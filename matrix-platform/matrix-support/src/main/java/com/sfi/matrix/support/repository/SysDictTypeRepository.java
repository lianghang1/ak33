package com.sfi.matrix.support.repository;

import com.sfi.matrix.support.entity.SysDictType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 字典类型 Repository。
 *
 * @author matrix
 */
@Repository
public interface SysDictTypeRepository extends JpaRepository<SysDictType, Long> {

    Optional<SysDictType> findByDictTypeAndTenantIdAndDeletedFalse(String dictType, String tenantId);

    List<SysDictType> findByDeletedFalseOrderByDictTypeAsc();
}
