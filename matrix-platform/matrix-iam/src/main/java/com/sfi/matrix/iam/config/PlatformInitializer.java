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
 * @author lianghang
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

    /**
     * 幂等播种平台菜单：以 permission 作为唯一判断。
     *
     * <p>api_pattern 声明该权限点守护的后端 URL（Ant 风格，可带 METHOD: 前缀），
     * URL 权限拦截器从菜单表加载这些规则，按 URL + 方法裁决权限。
     * 读/写用 METHOD 区分，落到不同权限点（如字典的 list 与 save）。</p>
     */
    private void seedPlatformMenus() {
        // 每个资源独占一级路径，权限规则统一收敛为单条 /**；操作主体归入其资源（分配租户→用户）
        seed(IamConstants.PERM_TENANT, "租户管理", "C", 10,
                "/api/iam/tenant/**");
        seed(IamConstants.PERM_PACKAGE, "套餐管理", "C", 20,
                "/api/iam/package/**");
        seed(IamConstants.PERM_USER, "用户管理", "C", 30,
                "/api/iam/user/**,/api/audit/**");
        seed(IamConstants.PERM_DEPT, "部门管理", "C", 40,
                "/api/iam/dept/**");
        seed(IamConstants.PERM_ROLE, "角色管理", "C", 50,
                "/api/iam/role/**");
        seed(IamConstants.PERM_MENU, "菜单管理", "C", 60,
                "/api/iam/menu/**");
        seed(IamConstants.PERM_DICT, "字典管理", "C", 70,
                "GET:/api/dict/**");
        seed(IamConstants.PERM_DICT_SAVE, "字典维护", "C", 71,
                "POST:/api/dict/**");
        seed(IamConstants.PERM_CONFIG, "参数管理", "C", 80,
                "GET:/api/config/**");
        seed(IamConstants.PERM_CONFIG_SAVE, "参数维护", "C", 81,
                "POST:/api/config/**");
        // 敏感参数独立权限：精确 pattern 优先于 POST:/api/config/**，读明文与改值都受此点保护
        seed(IamConstants.PERM_CONFIG_SECRET, "敏感参数", "C", 82,
                "GET:/api/config/secret,POST:/api/config/secret");
        seed(IamConstants.PERM_FILE, "文件管理", "C", 90,
                "GET:/api/file/**");
        seed(IamConstants.PERM_FILE_SAVE, "文件维护", "C", 91,
                "POST:/api/file/**");
    }

    private void seed(String permission, String name, String type, int sortNo, String apiPattern) {
        SysMenu exists = iamService.findMenuByPermission(permission);
        if (exists == null) {
            iamService.createMenu(name, type, null, permission,
                    IamConstants.MENU_SCOPE_PLATFORM, sortNo, apiPattern);
        } else {
            iamService.ensureMenuApiPattern(permission, apiPattern);
        }
    }
}
