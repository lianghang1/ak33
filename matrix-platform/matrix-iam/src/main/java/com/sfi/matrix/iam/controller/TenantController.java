package com.sfi.matrix.iam.controller;

import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.matrix.iam.controller.support.IamPageQuery;
import com.sfi.matrix.iam.dto.TenantRequest;
import com.sfi.matrix.iam.entity.SysTenant;
import com.sfi.matrix.iam.service.IamService;
import com.sfi.scaffold.common.request.IdRequest;
import com.sfi.scaffold.common.result.PageResult;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.starter.security.policy.PasswordPolicyValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 租户接口：租户维护与平台租户 ID。
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/iam")
public class TenantController {

    private final IamService iamService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;

    public TenantController(IamService iamService, PasswordEncoder passwordEncoder,
                            PasswordPolicyValidator passwordPolicyValidator) {
        this.iamService = iamService;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    /** 返回唯一平台租户 ID（供字典/参数做默认配置兜底） */
    @GetMapping("/platform-tenant-id")
    public R<String> platformTenantId() {
        return R.ok(iamService.getPlatformTenantId());
    }

    @OperateLog(module = "租户管理", operation = "新增/修改租户")
    @PostMapping("/tenant")
    public R<SysTenant> saveTenant(@Valid @RequestBody TenantRequest req) {
        if (req.getId() == null) {
            if (req.getAdminPassword() != null && !req.getAdminPassword().isEmpty()) {
                passwordPolicyValidator.validate(req.getAdminPassword());
            }
            return R.ok(iamService.createTenant(req.getTenantCode(), req.getTenantName(), req.getPackageId(),
                    req.getExpireTime(), req.getRemark(), req.getAdminUsername(),
                    req.getAdminPassword() == null || req.getAdminPassword().isEmpty()
                            ? null : passwordEncoder.encode(req.getAdminPassword()), req.getAdminName()));
        }
        return R.ok(iamService.updateTenant(req.getId(), req.getTenantName(), req.getPackageId(),
                req.getExpireTime(), req.getStatus(), req.getRemark()));
    }

    @GetMapping("/tenant")
    public R<SysTenant> getTenant(@RequestParam Long id) {
        return R.ok(iamService.getTenant(id));
    }

    @OperateLog(module = "租户管理", operation = "删除租户")
    @PostMapping("/tenant/delete")
    public R<Void> deleteTenant(@Valid @RequestBody IdRequest req) {
        iamService.deleteTenant(req.getId());
        return R.ok();
    }

    @GetMapping("/tenants")
    public R<PageResult<SysTenant>> tenants(HttpServletRequest request) {
        return R.ok(IamPageQuery.query(request, SysTenant.class));
    }
}
