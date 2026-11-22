package com.sfi.matrix.cemetery.repository;

import com.sfi.matrix.cemetery.entity.CemeteryArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 墓区 Repository。租户过滤由 @Filter 全局处理，查询方法只写业务条件。
 *
 * @author matrix
 */
@Repository
public interface CemeteryAreaRepository extends JpaRepository<CemeteryArea, Long> {

    /** 按上级查子墓区，用于树 */
    List<CemeteryArea> findByParentIdAndDeletedFalseOrderBySortNoAsc(Long parentId);

    /** 顶层墓区（园） */
    List<CemeteryArea> findByParentIdIsNullAndDeletedFalseOrderBySortNoAsc();

    /** 同 parent 下编码唯一性校验 */
    Optional<CemeteryArea> findByParentIdAndAreaCodeAndDeletedFalse(Long parentId, String areaCode);

    /** 按名称精确查（测试/业务定位用） */
    Optional<CemeteryArea> findByAreaNameAndDeletedFalse(String areaName);
}
