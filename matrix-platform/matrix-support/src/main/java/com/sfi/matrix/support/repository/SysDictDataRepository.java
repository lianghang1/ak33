package com.sfi.matrix.support.repository;

import com.sfi.matrix.support.entity.SysDictData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 字典选项 Repository。
 *
 * @author lianghang
 */
@Repository
public interface SysDictDataRepository extends JpaRepository<SysDictData, Long> {

    List<SysDictData> findByDictTypeAndDeletedFalseOrderBySortNoAsc(String dictType);

    Optional<SysDictData> findByDictTypeAndDictCodeAndTenantIdAndDeletedFalse(
            String dictType, String dictCode, String tenantId);
}
