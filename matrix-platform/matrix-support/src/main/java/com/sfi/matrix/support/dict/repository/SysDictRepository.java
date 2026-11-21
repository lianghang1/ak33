package com.sfi.matrix.support.dict.repository;

import com.sfi.matrix.support.dict.entity.SysDict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 数据字典 Repository。
 *
 * @author matrix
 */
@Repository
public interface SysDictRepository extends JpaRepository<SysDict, Long> {

    List<SysDict> findByDictTypeAndDeletedFalseOrderBySortNoAsc(String dictType);
}
