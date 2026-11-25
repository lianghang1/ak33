package com.sfi.matrix.cemetery.repository;

import com.sfi.matrix.cemetery.entity.Tomb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 墓位 Repository。租户过滤由 @Filter 全局处理，编号唯一为租户内（配合租户过滤器）。
 *
 * @author lianghang
 */
@Repository
public interface TombRepository extends JpaRepository<Tomb, Long> {

    /** 编号校验（租户由 @Filter 隔离，唯一键为 tenant_id+code+deleted） */
    Optional<Tomb> findByCodeAndDeletedFalse(String code);

    List<Tomb> findByAreaIdAndDeletedFalse(Long areaId);

    List<Tomb> findByAreaIdAndStatusAndDeletedFalse(Long areaId, String status);
}
