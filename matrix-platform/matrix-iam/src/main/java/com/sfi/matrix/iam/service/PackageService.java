package com.sfi.matrix.iam.service;

import com.sfi.matrix.iam.entity.SysTenantPackage;
import com.sfi.matrix.iam.repository.SysTenantPackageRepository;
import com.sfi.scaffold.starter.data.jpa.service.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 套餐单实体服务：get/list/page/save/delete 骨架继承 BaseService，本类只承载套餐的业务规则。
 *
 * @author lianghang
 */
@Service
public class PackageService extends BaseService<SysTenantPackage> {

    private final SysTenantPackageRepository packageRepository;

    public PackageService(SysTenantPackageRepository packageRepository) {
        super(packageRepository);
        this.packageRepository = packageRepository;
    }

    /** 新增套餐：编码唯一校验。 */
    @Transactional(rollbackFor = Exception.class)
    public SysTenantPackage create(String packageCode, String packageName, String remark) {
        packageRepository.findByPackageCodeAndDeletedFalse(packageCode).ifPresent(p -> {
            throw new IllegalArgumentException("套餐编码已存在：" + packageCode);
        });
        SysTenantPackage pkg = new SysTenantPackage();
        pkg.setPackageCode(packageCode);
        pkg.setPackageName(packageName);
        pkg.setRemark(remark);
        pkg.setStatus(1);
        return packageRepository.save(pkg);
    }

    /** 修改套餐。查不到由 require 抛 NOT_FOUND。 */
    @Transactional(rollbackFor = Exception.class)
    public SysTenantPackage update(Long id, String packageName, String remark, Integer status) {
        SysTenantPackage p = require(id);
        if (packageName != null && !packageName.trim().isEmpty()) {
            p.setPackageName(packageName);
        }
        if (remark != null) {
            p.setRemark(remark);
        }
        if (status != null) {
            p.setStatus(status);
        }
        return packageRepository.save(p);
    }
}
