package com.sfi.matrix.iam.controller;

import com.sfi.matrix.iam.annotation.OperateLog;
import com.sfi.matrix.iam.constant.IamConstants;
import com.sfi.matrix.iam.dto.TenantRequest;
import com.sfi.matrix.iam.entity.SysTenant;
import com.sfi.matrix.iam.service.IamService;
import com.sfi.matrix.iam.service.TenantService;
import com.sfi.scaffold.common.request.IdRequest;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.starter.security.policy.PasswordPolicyValidator;
import com.sfi.scaffold.starter.web.crud.BaseCrudController;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 租户接口：list/get/delete/基础 save 走 CRUD 基类；新增租户的五步编排仍由 IamService 承担。
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/iam/tenant")
public class TenantController extends BaseCrudController<SysTenant, TenantRequest, TenantService> {

    private final IamService iamService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;

    public TenantController(TenantService service, IamService iamService,
                            PasswordEncoder passwordEncoder,
                            PasswordPolicyValidator passwordPolicyValidator) {
        super(service, SysTenant.class);
        this.iamService = iamService;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    /** 租户是平台级元数据：仅平台超管可增删改；租户管理员一律拒绝。 */
    private void assertPlatformAdmin() {
        boolean ok = com.sfi.scaffold.starter.security.identity.LoginContext.currentOrNull() != null
                && com.sfi.scaffold.starter.security.identity.LoginContext.current().getRoles() != null
                && com.sfi.scaffold.starter.security.identity.LoginContext.current()
                        .getRoles().contains(IamConstants.PLATFORM_ADMIN_ROLE_CODE);
        if (!ok) {
            throw new IllegalArgumentException("仅平台超管可管理租户");
        }
    }

    /** 返回唯一平台租户 ID（供字典/参数做默认配置兜底） */
    @GetMapping("/platform-tenant-id")
    public R<String> platformTenantId() {
        return R.ok(iamService.getPlatformTenantId());
    }

    /**
     * 新增走 IamService 五步初始化（含密码策略与管理员初始化），修改走 TenantService.update。
     */
    @Override
    @OperateLog(module = "租户管理", operation = "新增/修改租户")
    @PostMapping("/save")
    public R<SysTenant> save(@Valid @RequestBody TenantRequest req) {
        assertPlatformAdmin();
        if (req.getId() == null) {
            if (req.getAdminPassword() != null && !req.getAdminPassword().isEmpty()) {
                passwordPolicyValidator.validate(req.getAdminPassword());
            }
            return R.ok(iamService.createTenant(req.getTenantCode(), req.getTenantName(), req.getPackageId(),
                    req.getExpireTime(), req.getRemark(), req.getAdminUsername(),
                    req.getAdminPassword() == null || req.getAdminPassword().isEmpty()
                            ? null : passwordEncoder.encode(req.getAdminPassword()), req.getAdminName()));
        }
        return R.ok(service.update(req.getId(), req.getTenantName(), req.getPackageId(),
                req.getExpireTime(), req.getStatus(), req.getRemark()));
    }

    /** 删除走基类（TenantService.delete 含平台租户保护），补审计。 */
    @Override
    @OperateLog(module = "租户管理", operation = "删除租户")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        assertPlatformAdmin();
        return super.delete(req);
    }

    // list/get 白拿基类：list 走 TenantService.page（QueryUtilsX.queryPage），get 查不到返回 null。
}
