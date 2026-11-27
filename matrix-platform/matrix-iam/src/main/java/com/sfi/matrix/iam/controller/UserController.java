package com.sfi.matrix.iam.controller;

import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.matrix.iam.controller.support.IamPageQuery;
import com.sfi.matrix.iam.dto.AssignRolesRequest;
import com.sfi.matrix.iam.dto.AssignTenantRequest;
import com.sfi.matrix.iam.dto.UserRequest;
import com.sfi.matrix.iam.entity.SysUser;
import com.sfi.matrix.iam.entity.SysUserTenant;
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
import java.util.List;
import java.util.Set;

/**
 * 用户接口：用户维护、租户归属、角色/权限查询。
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/iam")
public class UserController {

    private final IamService iamService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;

    public UserController(IamService iamService, PasswordEncoder passwordEncoder,
                          PasswordPolicyValidator passwordPolicyValidator) {
        this.iamService = iamService;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    @OperateLog(module = "用户管理", operation = "新增/修改用户")
    @PostMapping("/user")
    public R<SysUser> saveUser(@Valid @RequestBody UserRequest req) {
        if (req.getId() == null) {
            passwordPolicyValidator.validate(req.getPassword());
            return R.ok(iamService.createUser(req.getUsername(), passwordEncoder.encode(req.getPassword()),
                    req.getNickname()));
        }
        return R.ok(iamService.updateUser(req.getId(), req.getNickname(), req.getMobile(), req.getEmail(),
                req.getAccountType(), req.getStatus(), req.getMaxSessions()));
    }

    @GetMapping("/user")
    public R<SysUser> getUser(@RequestParam Long id) {
        return R.ok(iamService.getUser(id));
    }

    @OperateLog(module = "用户管理", operation = "删除用户")
    @PostMapping("/user/delete")
    public R<Void> deleteUser(@Valid @RequestBody IdRequest req) {
        iamService.deleteUser(req.getId());
        return R.ok();
    }

    @GetMapping("/users")
    public R<PageResult<SysUser>> users(HttpServletRequest request) {
        return R.ok(IamPageQuery.query(request, SysUser.class));
    }

    @OperateLog(module = "用户管理", operation = "分配用户租户")
    @PostMapping("/assign-tenant")
    public R<SysUserTenant> assignTenant(@Valid @RequestBody AssignTenantRequest req) {
        return R.ok(iamService.assignTenant(req.getUserId(), req.getTenantId(), req.getDeptId()));
    }

    @GetMapping("/user-tenants")
    public R<List<SysUserTenant>> userTenants(@RequestParam Long userId) {
        return R.ok(iamService.userTenants(userId));
    }

    @OperateLog(module = "用户管理", operation = "分配角色")
    @PostMapping("/assign-roles")
    public R<Void> assignRoles(@Valid @RequestBody AssignRolesRequest req) {
        iamService.assignRoles(req.getUserId(), req.getRoleIds(), req.getTenantId());
        return R.ok();
    }

    // ===== 权限计算（带租户维度） =====
    @GetMapping("/user-roles")
    public R<List<String>> userRoles(@RequestParam Long userId, @RequestParam String tenantId) {
        return R.ok(iamService.userRoleCodes(userId, tenantId));
    }

    @GetMapping("/user-permissions")
    public R<Set<String>> userPermissions(@RequestParam Long userId, @RequestParam String tenantId) {
        return R.ok(iamService.userPermissions(userId, tenantId));
    }
}
