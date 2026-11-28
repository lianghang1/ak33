package com.sfi.matrix.iam.controller;

import com.sfi.matrix.iam.annotation.OperateLog;
import com.sfi.matrix.iam.constant.IamConstants;
import com.sfi.matrix.iam.dto.DeptRequest;
import com.sfi.matrix.iam.entity.SysDept;
import com.sfi.matrix.iam.service.DeptService;
import com.sfi.scaffold.common.request.IdRequest;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.web.crud.BaseCrudController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 部门接口：list/get/delete/save 走 CRUD 基类，子部门校验在 DeptService.delete。
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/iam/dept")
public class DeptController extends BaseCrudController<SysDept, DeptRequest, DeptService> {

    public DeptController(DeptService service) {
        super(service, SysDept.class);
    }

    /** 平台超管：可跨租户；否则强制本租户。 */
    private boolean isPlatformAdmin() {
        return LoginContext.currentOrNull() != null
                && LoginContext.current().getRoles() != null
                && LoginContext.current().getRoles().contains(IamConstants.PLATFORM_ADMIN_ROLE_CODE);
    }

    /** 非平台超管提交跨租户 tenantId 时拒绝。 */
    private void assertTenantWritable(String tenantId) {
        if (isPlatformAdmin()) {
            return;
        }
        String current = LoginContext.currentTenantIdOrNull();
        if (current != null && tenantId != null && !current.equals(tenantId)) {
            throw new IllegalArgumentException("无权操作其他租户的数据");
        }
    }

    /** 部门树（芋道式左侧树）：按租户返回层级结构，供用户管理/部门管理使用。 */
    @GetMapping("/tree")
    public R<List<Map<String, Object>>> tree(@RequestParam(required = false) String tenantId) {
        String tid = (tenantId == null || tenantId.trim().isEmpty())
                ? LoginContext.currentTenantIdOrNull() : tenantId;
        assertTenantWritable(tid);
        return R.ok(service.treeOfTenant(tid));
    }

    @Override
    @OperateLog(module = "部门管理", operation = "新增/修改部门")
    @PostMapping("/save")
    public R<SysDept> save(@Valid @RequestBody DeptRequest req) {
        String tenantId = (req.getTenantId() == null || req.getTenantId().trim().isEmpty())
                ? LoginContext.currentTenantIdOrNull() : req.getTenantId();
        assertTenantWritable(tenantId);
        if (req.getId() == null) {
            return R.ok(service.create(req.getDeptName(), req.getParentId(), tenantId,
                    req.getSortNo() == null ? 0 : req.getSortNo(), req.getLeaderId()));
        }
        return R.ok(service.update(req.getId(), req.getDeptName(), req.getParentId(),
                req.getSortNo(), req.getLeaderId(), req.getStatus()));
    }

    @Override
    @OperateLog(module = "部门管理", operation = "删除部门")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        return super.delete(req);
    }
}
