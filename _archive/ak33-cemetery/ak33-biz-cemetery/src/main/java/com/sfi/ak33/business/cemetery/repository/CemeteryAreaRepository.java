package com.sfi.ak33.business.cemetery.repository;

import com.sfi.ak33.business.cemetery.entity.CemeteryAreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 墓区 Repository。
 *
 * @author ak33
 */
@Repository
public interface CemeteryAreaRepository extends JpaRepository<CemeteryAreaEntity, Long> {

    /** 按上级查子墓区（未删除），用于园→区树 */
    List<CemeteryAreaEntity> findByParentIdAndDeletedFalseOrderBySortNoAsc(Long parentId);

    /** 顶层墓区（园） */
    List<CemeteryAreaEntity> findByParentIdIsNullAndDeletedFalseOrderBySortNoAsc();
}
