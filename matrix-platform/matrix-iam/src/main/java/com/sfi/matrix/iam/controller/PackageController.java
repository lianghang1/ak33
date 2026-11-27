package com.sfi.matrix.iam.controller;

import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.matrix.iam.controller.support.IamPageQuery;
import com.sfi.matrix.iam.dto.AssignMenusRequest;
import com.sfi.matrix.iam.dto.PackageRequest;
import com.sfi.matrix.iam.entity.SysTenantPackage;
import com.sfi.matrix.iam.service.IamService;
import com.sfi.scaffold.common.request.IdRequest;
import com.sfi.scaffold.common.result.PageResult;
import com.sfi.scaffold.common.result.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 套餐接口：套餐维护、套餐菜单授权。
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/iam")
public class PackageController {

    private final IamService iamService;

    public PackageController(IamService iamService) {
        this.iamService = iamService;
    }

    @OperateLog(module = "套餐管理", operation = "新增/修改套餐")
    @PostMapping("/package")
    public R<SysTenantPackage> savePackage(@Valid @RequestBody PackageRequest req) {
        if (req.getId() == null) {
            return R.ok(iamService.createPackage(req.getPackageCode(), req.getPackageName(), req.getRemark()));
        }
        return R.ok(iamService.updatePackage(req.getId(), req.getPackageName(), req.getRemark(), req.getStatus()));
    }

    @GetMapping("/package")
    public R<SysTenantPackage> getPackage(@RequestParam Long id) {
        return R.ok(iamService.getPackage(id));
    }

    @OperateLog(module = "套餐管理", operation = "删除套餐")
    @PostMapping("/package/delete")
    public R<Void> deletePackage(@Valid @RequestBody IdRequest req) {
        iamService.deletePackage(req.getId());
        return R.ok();
    }

    @GetMapping("/packages")
    public R<PageResult<SysTenantPackage>> packages(HttpServletRequest request) {
        return R.ok(IamPageQuery.query(request, SysTenantPackage.class));
    }

    @OperateLog(module = "套餐管理", operation = "分配套餐菜单")
    @PostMapping("/assign-package-menus")
    public R<Void> assignPackageMenus(@Valid @RequestBody AssignMenusRequest req) {
        iamService.assignPackageMenus(req.getPackageId(), req.getMenuIds());
        return R.ok();
    }
}
