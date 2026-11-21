package com.sfi.matrix.iam.repository;

import com.sfi.matrix.iam.entity.SysMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 菜单 Repository。
 *
 * @author matrix
 */
@Repository
public interface SysMenuRepository extends JpaRepository<SysMenu, Long> {

    List<SysMenu> findByDeletedFalseOrderBySortNoAsc();

    List<SysMenu> findByParentIdIsNullAndDeletedFalseOrderBySortNoAsc();

    List<SysMenu> findByIdInAndDeletedFalse(List<Long> ids);

    /** 按范围取菜单（平台超管取 PLATFORM+COMMON） */
    List<SysMenu> findByMenuScopeInAndDeletedFalseOrderBySortNoAsc(List<String> scopes);

    Optional<SysMenu> findByPermissionAndDeletedFalse(String permission);
}
