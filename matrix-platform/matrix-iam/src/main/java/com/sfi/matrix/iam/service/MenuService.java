package com.sfi.matrix.iam.service;

import com.sfi.matrix.iam.constant.IamConstants;
import com.sfi.matrix.iam.entity.SysMenu;
import com.sfi.matrix.iam.repository.SysMenuRepository;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.starter.data.jpa.service.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单单实体服务：get/list/page/save/delete 骨架继承 BaseService，本类只承载菜单定义的业务规则。
 *
 * @author lianghang
 */
@Service
public class MenuService extends BaseService<SysMenu> {

    private final SysMenuRepository menuRepository;

    public MenuService(SysMenuRepository menuRepository) {
        super(menuRepository);
        this.menuRepository = menuRepository;
    }

    /** 新增菜单（默认 TENANT 范围）：平台种子初始化用。 */
    @Transactional(rollbackFor = Exception.class)
    public SysMenu create(String menuName, String menuType, Long parentId, String permission, int sortNo) {
        return create(menuName, menuType, parentId, permission,
                IamConstants.MENU_SCOPE_TENANT, sortNo, null);
    }

    /** 新增菜单：带范围（URL 权限种子初始化用）。 */
    @Transactional(rollbackFor = Exception.class)
    public SysMenu create(String menuName, String menuType, Long parentId, String permission,
                          String menuScope, int sortNo) {
        return create(menuName, menuType, parentId, permission, menuScope, sortNo, null);
    }

    /** 新增菜单：带范围与 URL 权限模式。 */
    @Transactional(rollbackFor = Exception.class)
    public SysMenu create(String menuName, String menuType, Long parentId, String permission,
                          String menuScope, int sortNo, String apiPattern) {
        SysMenu menu = new SysMenu();
        menu.setMenuName(menuName);
        menu.setMenuType(menuType);
        menu.setParentId(parentId);
        menu.setPermission(permission);
        menu.setMenuScope(menuScope);
        menu.setSortNo(sortNo);
        menu.setApiPattern(apiPattern);
        menu.setVisible(Boolean.TRUE);
        return menuRepository.save(menu);
    }

    /** 新增菜单（界面全字段）：含路由/组件/图标/可见/URL规则。 */
    @Transactional(rollbackFor = Exception.class)
    public SysMenu createFull(String menuName, String menuType, Long parentId, String path,
                              String component, String icon, String permission, String apiPattern,
                              String menuScope, Integer sortNo, Boolean visible) {
        SysMenu menu = new SysMenu();
        menu.setMenuName(menuName);
        menu.setMenuType(menuType);
        menu.setParentId(parentId);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setIcon(icon);
        menu.setPermission(permission);
        menu.setApiPattern(apiPattern);
        menu.setMenuScope(menuScope == null || menuScope.isEmpty() ? IamConstants.MENU_SCOPE_TENANT : menuScope);
        menu.setSortNo(sortNo == null ? 0 : sortNo);
        menu.setVisible(visible == null ? Boolean.TRUE : visible);
        return menuRepository.save(menu);
    }

    /** 按权限标识查菜单（种子幂等判断）。 */
    public SysMenu findByPermission(String permission) {
        return menuRepository.findByPermissionAndDeletedFalse(permission).orElse(null);
    }

    /** 已存在的菜单补齐 api_pattern（种子幂等回填）。 */
    @Transactional(rollbackFor = Exception.class)
    public void ensureApiPattern(String permission, String apiPattern) {
        SysMenu menu = findByPermission(permission);
        if (menu != null && (menu.getApiPattern() == null || menu.getApiPattern().isEmpty())) {
            menu.setApiPattern(apiPattern);
            menuRepository.save(menu);
        }
    }

    /** 修改菜单。查不到由 require 抛 NOT_FOUND。 */
    @Transactional(rollbackFor = Exception.class)
    public SysMenu update(Long id, String menuName, String menuType, Long parentId, String path,
                          String component, String icon, String permission, String apiPattern,
                          String menuScope, Integer sortNo, Boolean visible) {
        SysMenu m = require(id);
        if (menuName != null && !menuName.trim().isEmpty()) {
            m.setMenuName(menuName);
        }
        if (menuType != null && !menuType.trim().isEmpty()) {
            m.setMenuType(menuType);
        }
        m.setParentId(parentId);
        if (path != null) {
            m.setPath(path);
        }
        if (component != null) {
            m.setComponent(component);
        }
        if (icon != null) {
            m.setIcon(icon);
        }
        if (permission != null) {
            m.setPermission(permission);
        }
        if (apiPattern != null) {
            m.setApiPattern(apiPattern);
        }
        if (menuScope != null && !menuScope.trim().isEmpty()) {
            m.setMenuScope(menuScope);
        }
        if (sortNo != null) {
            m.setSortNo(sortNo);
        }
        if (visible != null) {
            m.setVisible(visible);
        }
        return menuRepository.save(m);
    }

    /** 删除菜单：存在子菜单时拒绝。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysMenu m = require(id);
        List<SysMenu> children = menuRepository.findByDeletedFalseOrderBySortNoAsc().stream()
                .filter(x -> id.equals(x.getParentId()))
                .collect(Collectors.toList());
        if (!children.isEmpty()) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "存在子菜单，不可删除");
        }
        super.delete(id);
    }
}
