package com.sfi.matrix.iam.controller;

import com.sfi.matrix.iam.annotation.OperateLog;
import com.sfi.matrix.iam.constant.IamConstants;
import com.sfi.matrix.iam.dto.AssignRolesRequest;
import com.sfi.matrix.iam.dto.AssignTenantRequest;
import com.sfi.matrix.iam.dto.ResetPasswordRequest;
import com.sfi.matrix.iam.dto.UserBatchRequest;
import com.sfi.matrix.iam.dto.UserDetailVO;
import com.sfi.matrix.iam.dto.UserRequest;
import com.sfi.matrix.iam.entity.SysUser;
import com.sfi.matrix.iam.entity.SysUserTenant;
import com.sfi.matrix.iam.plan.UserListPlan;
import com.sfi.matrix.iam.service.IamService;
import com.sfi.matrix.iam.service.UserService;
import com.sfi.scaffold.common.request.IdRequest;
import com.sfi.scaffold.common.result.PageResult;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.common.util.PageParams;
import com.sfi.scaffold.query.QueryUtilsX;
import com.sfi.scaffold.query.filter.OperatorX;
import com.sfi.scaffold.query.filter.QueryFilterX;
import com.sfi.scaffold.query.plan.annotation.QueryPlanX;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.security.policy.PasswordPolicyValidator;
import com.sfi.scaffold.starter.web.crud.BaseCrudController;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * 用户接口：list/get/delete 走 CRUD 基类；租户归属/角色分配与权限计算属跨实体编排，保留 IamService。
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/iam/user")
@QueryPlanX({UserListPlan.class})
public class UserController extends BaseCrudController<SysUser, UserRequest, UserService> {

    private final IamService iamService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;

    public UserController(UserService service, IamService iamService,
                          PasswordEncoder passwordEncoder,
                          PasswordPolicyValidator passwordPolicyValidator) {
        super(service, SysUser.class);
        this.iamService = iamService;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    /** 租户兜底：前端未传时用当前登录租户（芋道式单租户上下文）。 */
    private String tenantOf(String tenantId) {
        return (tenantId == null || tenantId.trim().isEmpty())
                ? LoginContext.currentTenantIdOrNull() : tenantId;
    }

    /**
     * 判断当前登录人是否为平台超管。
     *
     * <p>依据 LoginUser.roles 是否含 PLATFORM_ADMIN（登录装配时写入的角色编码）。
     * 仅平台超管可跨租户查看/分配用户；租户管理员及普通用户一律强制本租户隔离。</p>
     */
    private boolean isPlatformAdmin() {
        return LoginContext.currentOrNull() != null
                && LoginContext.current().getRoles() != null
                && LoginContext.current().getRoles().contains(IamConstants.PLATFORM_ADMIN_ROLE_CODE);
    }

    /** 非平台超管提交跨租户 tenantId 时拒绝，防止绕过前端直接调接口。 */
    private void assertTenantWritable(String tenantId) {
        if (isPlatformAdmin()) {
            return;
        }
        String current = LoginContext.currentTenantIdOrNull();
        if (current != null && tenantId != null && !current.equals(tenantId)) {
            throw new IllegalArgumentException("无权操作其他租户的数据");
        }
    }

    /**
     * 用户列表 plan 端点：覆盖基类。
     *
     * <p>平台超管：不注入 tenantId，可查看全部租户用户（列表带租户列，可按租户筛选）；
     * 租户管理员/普通用户：强制注入当前登录租户作为固定条件，前端无法篡改。</p>
     */
    @Override
    @SuppressWarnings("unchecked")
    public R<PageResult<?>> listByPlan(@PathVariable String plan, HttpServletRequest request) {
        List<QueryFilterX> filters = QueryUtilsX.parse(request.getParameterMap());
        if (!isPlatformAdmin()) {
            String tenantId = LoginContext.currentTenantIdOrNull();
            if (tenantId != null) {
                filters.add(new QueryFilterX("tenantId", OperatorX.EQ, tenantId));
            }
        }
        Class<?> target = QueryUtilsX.resolvePlan(org.springframework.util.ClassUtils.getUserClass(getClass()), plan);
        return R.ok((PageResult<?>) QueryUtilsX.queryPage(filters, PageParams.of(request), target));
    }

    @Override
    @OperateLog(module = "用户管理", operation = "新增/修改用户")
    @PostMapping("/save")
    public R<SysUser> save(@Valid @RequestBody UserRequest req) {
        String tenantId = tenantOf(req.getTenantId());
        assertTenantWritable(tenantId);
        if (req.getId() == null) {
            passwordPolicyValidator.validate(req.getPassword());
            return R.ok(iamService.createUserFull(
                    req.getUsername(), passwordEncoder.encode(req.getPassword()), req.getNickname(),
                    req.getMobile(), req.getEmail(), req.getStatus(), req.getMaxSessions(),
                    req.getSex(), req.getRemark(), tenantId, req.getDeptId(), req.getRoleIds()));
        }
        return R.ok(iamService.updateUserFull(
                req.getId(), req.getNickname(), req.getMobile(), req.getEmail(), req.getAccountType(),
                req.getStatus(), req.getMaxSessions(), req.getSex(), req.getRemark(),
                tenantId, req.getDeptId(), req.getRoleIds()));
    }

    /** 用户详情回显：账号 + 资料 + 归属部门 + 角色 ID（编辑抽屉用）。 */
    @GetMapping("/detail")
    public R<UserDetailVO> detail(@RequestParam Long id, @RequestParam(required = false) String tenantId) {
        String tid = tenantOf(tenantId);
        assertTenantWritable(tid);
        return R.ok(iamService.userDetail(id, tid));
    }

    /** 重置用户密码（管理员动作）。 */
    @OperateLog(module = "用户管理", operation = "重置密码")
    @PostMapping("/reset-password")
    public R<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        passwordPolicyValidator.validate(req.getPassword());
        iamService.resetPassword(req.getUserId(), passwordEncoder.encode(req.getPassword()));
        return R.ok();
    }

    /** 批量删除用户（逻辑删除）。 */
    @OperateLog(module = "用户管理", operation = "批量删除用户")
    @PostMapping("/delete-batch")
    public R<Void> deleteBatch(@Valid @RequestBody UserBatchRequest req) {
        for (Long id : req.getIds()) {
            service.delete(id);
        }
        return R.ok();
    }

    /** 批量改状态。 */
    @OperateLog(module = "用户管理", operation = "批量修改状态")
    @PostMapping("/change-status")
    public R<Void> changeStatus(@Valid @RequestBody UserBatchRequest req) {
        if (req.getStatus() == null) {
            throw new IllegalArgumentException("目标状态不能为空");
        }
        for (Long id : req.getIds()) {
            service.update(id, null, null, null, null, req.getStatus(), null);
        }
        return R.ok();
    }

    @Override
    @OperateLog(module = "用户管理", operation = "删除用户")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        return super.delete(req);
    }

    @OperateLog(module = "用户管理", operation = "分配用户租户")
    @PostMapping("/assign-tenant")
    public R<SysUserTenant> assignTenant(@Valid @RequestBody AssignTenantRequest req) {
        assertTenantWritable(req.getTenantId());
        return R.ok(iamService.assignTenant(req.getUserId(), req.getTenantId(), req.getDeptId()));
    }

    @GetMapping("/tenants")
    public R<List<SysUserTenant>> tenants(@RequestParam Long userId) {
        return R.ok(iamService.userTenants(userId));
    }

    @OperateLog(module = "用户管理", operation = "分配角色")
    @PostMapping("/assign-roles")
    public R<Void> assignRoles(@Valid @RequestBody AssignRolesRequest req) {
        assertTenantWritable(req.getTenantId());
        iamService.assignRoles(req.getUserId(), req.getRoleIds(), req.getTenantId());
        return R.ok();
    }

    // ===== 权限计算（带租户维度） =====
    @GetMapping("/roles")
    public R<List<String>> roles(@RequestParam Long userId, @RequestParam String tenantId) {
        return R.ok(iamService.userRoleCodes(userId, tenantId));
    }

    @GetMapping("/permissions")
    public R<Set<String>> permissions(@RequestParam Long userId, @RequestParam String tenantId) {
        return R.ok(iamService.userPermissions(userId, tenantId));
    }
}
