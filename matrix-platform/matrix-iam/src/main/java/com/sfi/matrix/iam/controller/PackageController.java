package com.sfi.matrix.iam.controller;

import com.sfi.matrix.iam.annotation.OperateLog;
import com.sfi.matrix.iam.dto.AssignMenusRequest;
import com.sfi.matrix.iam.dto.PackageRequest;
import com.sfi.matrix.iam.entity.SysTenantPackage;
import com.sfi.matrix.iam.service.IamService;
import com.sfi.matrix.iam.service.PackageService;
import com.sfi.scaffold.common.request.IdRequest;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.starter.web.crud.BaseCrudController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 套餐接口：list/get/delete 走 CRUD 基类；套餐菜单授权属跨实体编排，保留 IamService。
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/iam/package")
public class PackageController extends BaseCrudController<SysTenantPackage, PackageRequest, PackageService> {

    private final IamService iamService;

    public PackageController(PackageService service, IamService iamService) {
        super(service, SysTenantPackage.class);
        this.iamService = iamService;
    }

    @Override
    @OperateLog(module = "套餐管理", operation = "新增/修改套餐")
    @PostMapping("/save")
    public R<SysTenantPackage> save(@Valid @RequestBody PackageRequest req) {
        if (req.getId() == null) {
            return R.ok(service.create(req.getPackageCode(), req.getPackageName(), req.getRemark()));
        }
        return R.ok(service.update(req.getId(), req.getPackageName(), req.getRemark(), req.getStatus()));
    }

    @Override
    @OperateLog(module = "套餐管理", operation = "删除套餐")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        return super.delete(req);
    }

    @OperateLog(module = "套餐管理", operation = "分配套餐菜单")
    @PostMapping("/assign-menus")
    public R<Void> assignMenus(@Valid @RequestBody AssignMenusRequest req) {
        iamService.assignPackageMenus(req.getPackageId(), req.getMenuIds());
        return R.ok();
    }
}
