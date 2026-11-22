package com.sfi.matrix.iam.service;

import com.sfi.matrix.iam.entity.SysMenu;
import com.sfi.matrix.iam.repository.SysMenuRepository;
import com.sfi.scaffold.starter.security.permission.UrlPermissionProvider;
import com.sfi.scaffold.starter.security.permission.UrlPermissionRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * URL 权限规则来源：从菜单表 sys_menu 读取 api_pattern 与 permission 生成规则。
 *
 * <p>api_pattern 允许多个模式用逗号分隔，每个模式可带 {@code METHOD:} 前缀限定 HTTP 方法
 * （如 {@code POST:/api/iam/user,GET:/api/iam/users}），无前缀表示不限方法。
 * 改菜单即改权限，无需改代码、不发版。</p>
 *
 * @author matrix
 */
@Component
public class MenuUrlPermissionProvider implements UrlPermissionProvider {

    private final SysMenuRepository menuRepository;

    public MenuUrlPermissionProvider(SysMenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    @Override
    public List<UrlPermissionRule> loadRules() {
        List<SysMenu> menus = menuRepository.findByDeletedFalseOrderBySortNoAsc();
        List<UrlPermissionRule> rules = new ArrayList<>();
        if (menus == null) {
            return rules;
        }
        for (SysMenu menu : menus) {
            String pattern = menu.getApiPattern();
            String permission = menu.getPermission();
            if (pattern == null || pattern.trim().isEmpty()
                    || permission == null || permission.trim().isEmpty()) {
                continue;
            }
            for (String raw : pattern.split(",")) {
                String item = raw.trim();
                if (item.isEmpty()) {
                    continue;
                }
                String method = "*";
                String path = item;
                int colon = item.indexOf(':');
                if (colon > 0) {
                    method = item.substring(0, colon).trim();
                    path = item.substring(colon + 1).trim();
                }
                if (path.isEmpty()) {
                    continue;
                }
                rules.add(new UrlPermissionRule(method, path, permission));
            }
        }
        return rules;
    }
}
