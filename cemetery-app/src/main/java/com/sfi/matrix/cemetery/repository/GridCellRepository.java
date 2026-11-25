package com.sfi.matrix.cemetery.repository;

import com.sfi.matrix.cemetery.entity.GridCell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 格位 Repository。
 *
 * @author lianghang
 */
@Repository
public interface GridCellRepository extends JpaRepository<GridCell, Long> {

    List<GridCell> findByAreaIdAndDeletedFalse(Long areaId);

    List<GridCell> findByAreaIdAndInitializedFalseAndDeletedFalse(Long areaId);

    /** 唯一性校验：墓区+排+列（幂等铺格位用） */
    Optional<GridCell> findByAreaIdAndRowNoAndColNoAndDeletedFalse(Long areaId, Integer rowNo, Integer colNo);
}
