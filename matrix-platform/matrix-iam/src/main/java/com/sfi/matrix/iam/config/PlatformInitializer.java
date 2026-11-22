package com.sfi.matrix.iam.config;

import com.sfi.matrix.iam.constant.IamConstants;
import com.sfi.matrix.iam.entity.SysMenu;
import com.sfi.matrix.iam.service.IamService;
import com.sfi.scaffold.starter.security.policy.PasswordPolicyValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 运营平台域启动初始化（幂等）。
 *
 * <p>每次启动都会执行，靠 service 层的幂等判断保证不重复建数据：
 * ① 建虚拟 PLATFORM 租户 + PLATFORM_ADMIN 角色；
 * ② 建平台超管账号并挂租户、授角色；
 * ③ 播种几条平台级菜单（租户/套餐/菜单管理），让平台超管开箱有可见菜单。</p>
 *
 * <p>账号密码走配置，可用 {@code matrix.platform.admin.enabled=false} 关闭账号初始化，
 * 仅保留租户域与菜单种子。密码由 PasswordEncoder 编码后传给 service。</p>
 *
 * @author matrix
 */
@Component
public class PlatformInitializer implements ApplicationRunner {

    private final IamService iamService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;

    @Value("${matrix.platform.admin.enabled:true}")
    private boolean adminEnabled;

    @Value("${matrix.platform.admin.username:admin}")
    private String adminUsername;

    @Value("${matrix.platform.admin.password:admin123}")
    private String adminPassword;

    @Value("${matrix.platform.admin.name:平台管理员}")
    private String adminName;

    @Value("${matrix.platform.seed-menus:true}")
    private boolean seedMenus;

    public PlatformInitializer(IamService iamService, PasswordEncoder passwordEncoder,
                               PasswordPolicyValidator passwordPolicyValidator) {
        this.iamService = iamService;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    @Override
    public void run(ApplicationArguments args) {
        iamService.initPlatformDomain();
        if (seedMenus) {
            seedPlatformMenus();
        }
        if (adminEnabled) {
            passwordPolicyValidator.validate(adminPassword);
            iamService.initPlatformAdmin(adminUsername, passwordEncoder.encode(adminPassword), adminName);
        }
    }

    /** 幂等播种平台菜单：以 permission 作为唯一判断，与管理接口的 @RequirePermission 一一对应 */
    private void seedPlatformMenus() {
        seed(IamConstants.PERM_TENANT, "租户管理", "C", 10, null);
        seed(IamConstants.PERM_PACKAGE, "套餐管理", "C", 20, null);
        seed(IamConstants.PERM_USER, "用户管理", "C", 30, null);
        seed(IamConstants.PERM_DEPT, "部门管理", "C", 40, null);
        seed(IamConstants.PERM_ROLE, "角色管理", "C", 50, null);
        seed(IamConstants.PERM_MENU, "菜单管理", "C", 60, null);
        seed(IamConstants.PERM_DICT, "字典管理", "C", 70, null);
        seed(IamConstants.PERM_DICT_SAVE, "字典维护", "C", 71, null);
        seed(IamConstants.PERM_CONFIG, "参数管理", "C", 80, null);
        seed(IamConstants.PERM_CONFIG_SAVE, "参数维护", "C", 81, null);
    }

    private void seed(String permission, String name, String type, int sortNo, Long parentId) {
        SysMenu exists = iamService.findMenuByPermission(permission);
        if (exists == null) {
            iamService.createMenu(name, type, parentId, permission,
                    IamConstants.MENU_SCOPE_PLATFORM, sortNo);
        }
    }
}
