package com.sfi.matrix.support.repository.dict;

import com.sfi.matrix.support.entity.SysDict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 字典 Repository。
 *
 * @author lianghang
 */
@Repository
public interface SysDictRepository extends JpaRepository<SysDict, Long> {

    Optional<SysDict> findByDictCodeAndTenantIdAndDeletedFalse(String dictCode, String tenantId);

    List<SysDict> findByDeletedFalseOrderByDictCodeAsc();
}
