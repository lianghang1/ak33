package com.sfi.matrix.cemetery.config;

import com.sfi.matrix.cemetery.CemeteryConstants;
import com.sfi.matrix.iam.constant.IamConstants;
import com.sfi.matrix.iam.entity.SysMenu;
import com.sfi.matrix.iam.service.MenuService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 公墓业务 URL 权限菜单播种（幂等）。
 *
 * <p>把公墓端点的 URL 规则写入 sys_menu.api_pattern，交给 MenuUrlPermissionProvider
 * 加载为 URL 权限规则，生产开严格模式后这些端点才有规则命中、不至于被一律 403。</p>
 *
 * <p>读写分离（与平台 dict/config 一致）：GET 全部走 {@code cemetery:read}，
 * POST 全部走 {@code cemetery:write}。菜单范围用 COMMON——平台超管（PLATFORM+COMMON）
 * 天然可见，租户套餐也能装，避免平台超管访问公墓端点时被 403。</p>
 *
 * <p>可通过 {@code matrix.cemetery.seed-menus=false} 关闭播种。</p>
 *
 * @author lianghang
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class CemeteryMenuInitializer implements ApplicationRunner {

    private final MenuService menuService;

    @Value("${matrix.cemetery.seed-menus:true}")
    private boolean seedMenus;

    public CemeteryMenuInitializer(MenuService menuService) {
        this.menuService = menuService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedMenus) {
            return;
        }
        // 读：所有 GET 公墓端点
        seed(CemeteryConstants.PERM_CEMETERY_READ, "公墓查询", 10,
                "GET:/api/cemetery/**");
        // 写：所有 POST 公墓端点（墓区/格位/墓位的保存、删除、规划、初始化、移动）
        seed(CemeteryConstants.PERM_CEMETERY_WRITE, "公墓维护", 20,
                "POST:/api/cemetery/**");
    }

    private void seed(String permission, String name, int sortNo, String apiPattern) {
        SysMenu exists = menuService.findByPermission(permission);
        if (exists == null) {
            menuService.create(name, "C", null, permission,
                    IamConstants.MENU_SCOPE_COMMON, sortNo, apiPattern);
        } else {
            menuService.ensureApiPattern(permission, apiPattern);
        }
    }
}
