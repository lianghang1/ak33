package com.sfi.ak33.business.cemetery.repository;

import com.sfi.ak33.business.cemetery.entity.GridCellEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 格位 Repository。
 *
 * @author ak33
 */
@Repository
public interface GridCellRepository extends JpaRepository<GridCellEntity, Long> {

    /** 某墓区下全部格位 */
    List<GridCellEntity> findByAreaIdAndDeletedFalse(Long areaId);

    /** 某墓区下待建（未初始化）格位 */
    List<GridCellEntity> findByAreaIdAndInitializedFalseAndDeletedFalse(Long areaId);

    /** 唯一性校验：墓区+排+列 */
    Optional<GridCellEntity> findByAreaIdAndRowNoAndColNoAndDeletedFalse(Long areaId, Integer rowNo, Integer colNo);
}
