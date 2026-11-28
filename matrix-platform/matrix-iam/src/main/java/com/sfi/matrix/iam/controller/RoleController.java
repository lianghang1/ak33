package com.sfi.matrix.iam.controller;

import com.sfi.matrix.iam.annotation.OperateLog;
import com.sfi.matrix.iam.constant.IamConstants;
import com.sfi.matrix.iam.dto.AssignMenusRequest;
import com.sfi.matrix.iam.dto.RoleRequest;
import com.sfi.matrix.iam.dto.UserBatchRequest;
import com.sfi.matrix.iam.entity.SysRole;
import com.sfi.matrix.iam.plan.RoleListPlan;
import com.sfi.matrix.iam.service.IamService;
import com.sfi.matrix.iam.service.RoleService;
import com.sfi.scaffold.common.request.IdRequest;
import com.sfi.scaffold.common.result.PageResult;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.common.util.PageParams;
import com.sfi.scaffold.query.QueryUtilsX;
import com.sfi.scaffold.query.filter.OperatorX;
import com.sfi.scaffold.query.filter.QueryFilterX;
import com.sfi.scaffold.query.plan.annotation.QueryPlanX;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.web.crud.BaseCrudController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 角色接口：list 走 RoleListPlan（关键表 join + 用户数/菜单数聚合），
 * get/delete 走 CRUD 基类；角色菜单授权属跨实体编排，保留 IamService。
 *
 * <p>租户隔离：非平台超管强制本租户；平台超管可跨租户查看/分配，写接口同样校验。</p>
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/iam/role")
@QueryPlanX({RoleListPlan.class})
public class RoleController extends BaseCrudController<SysRole, RoleRequest, RoleService> {

    private final IamService iamService;

    public RoleController(RoleService service, IamService iamService) {
        super(service, SysRole.class);
        this.iamService = iamService;
    }

    private String tenantOf(String tenantId) {
        return (tenantId == null || tenantId.trim().isEmpty())
                ? LoginContext.currentTenantIdOrNull() : tenantId;
    }

    /** 平台超管：roles 含 PLATFORM_ADMIN，可跨租户；其余强制本租户。 */
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
     * 角色列表 plan 端点：覆盖基类。
     * 平台超管不注入 tenantId（看全部，前端带租户列/筛选）；其余强制本租户。
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
    @OperateLog(module = "角色管理", operation = "新增/修改角色")
    @PostMapping("/save")
    public R<SysRole> save(@Valid @RequestBody RoleRequest req) {
        String tenantId = tenantOf(req.getTenantId());
        assertTenantWritable(tenantId);
        if (req.getId() == null) {
            return R.ok(service.create(req.getRoleCode(), req.getRoleName(), tenantId,
                    req.getDataScope() == null ? "ALL" : req.getDataScope(),
                    req.getDataScopeDeptIds(), req.getSortNo(), req.getStatus(), req.getRemark()));
        }
        return R.ok(service.update(req.getId(), req.getRoleName(), req.getDataScope(),
                req.getDataScopeDeptIds(), req.getSortNo(), req.getStatus(), req.getRemark()));
    }

    @OperateLog(module = "角色管理", operation = "批量改状态")
    @PostMapping("/change-status")
    public R<Void> changeStatus(@Valid @RequestBody UserBatchRequest req) {
        service.changeStatus(req.getIds(), req.getStatus());
        return R.ok();
    }

    @Override
    @OperateLog(module = "角色管理", operation = "删除角色")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        return super.delete(req);
    }

    @GetMapping("/menus")
    public R<List<Long>> menus(@RequestParam Long roleId, @RequestParam String tenantId) {
        String tid = tenantOf(tenantId);
        assertTenantWritable(tid);
        return R.ok(iamService.roleMenuIds(roleId, tid));
    }

    @OperateLog(module = "角色管理", operation = "分配角色菜单")
    @PostMapping("/assign-menus")
    public R<Void> assignMenus(@Valid @RequestBody AssignMenusRequest req) {
        String tid = tenantOf(req.getTenantId());
        assertTenantWritable(tid);
        iamService.assignMenus(req.getRoleId(), req.getMenuIds(), tid);
        return R.ok();
    }
}
