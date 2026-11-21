package com.sfi.ak33.business.cemetery.repository;

import com.sfi.ak33.business.cemetery.entity.TombEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 墓位 Repository。
 *
 * @author ak33
 */
@Repository
public interface TombRepository extends JpaRepository<TombEntity, Long> {

    /** 编号唯一性校验（租户内，第一阶段单租户直接按 code） */
    Optional<TombEntity> findByCodeAndDeletedFalse(String code);

    /** 某墓区下墓位 */
    List<TombEntity> findByAreaIdAndDeletedFalse(Long areaId);

    /** 按状态统计 */
    List<TombEntity> findByAreaIdAndStatusAndDeletedFalse(Long areaId, String status);
}
