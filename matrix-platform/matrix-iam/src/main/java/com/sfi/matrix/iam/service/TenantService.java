package com.sfi.matrix.iam.service;

import com.sfi.matrix.iam.constant.IamConstants;
import com.sfi.matrix.iam.entity.SysTenant;
import com.sfi.matrix.iam.repository.SysTenantRepository;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.starter.data.jpa.service.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 租户单实体服务：get/list/page/save/delete 骨架继承 BaseService，本类只承载租户的业务规则。
 *
 * <p>跨实体的建租户（租户+角色+用户+归属+授权五步）属于聚合编排，保留在 {@link IamService#createTenant}；
 * 本服务的 save 走通用骨架，平台租户保护在 delete 与 update 中覆盖。</p>
 *
 * @author lianghang
 */
@Service
public class TenantService extends BaseService<SysTenant> {

    private final SysTenantRepository tenantRepository;

    public TenantService(SysTenantRepository tenantRepository) {
        super(tenantRepository);
        this.tenantRepository = tenantRepository;
    }

    /** 修改租户：字段更新 + 平台租户不可停用保护。查不到由 require 抛 NOT_FOUND。 */
    @Transactional(rollbackFor = Exception.class)
    public SysTenant update(Long id, String tenantName, Long packageId, LocalDate expireTime,
                            Integer status, String remark) {
        SysTenant t = require(id);
        if (tenantName != null && !tenantName.trim().isEmpty()) {
            t.setTenantName(tenantName);
        }
        t.setPackageId(packageId);
        t.setExpireTime(expireTime);
        if (status != null) {
            t.setStatus(status);
        }
        if (remark != null) {
            t.setRemark(remark);
        }
        if (IamConstants.PLATFORM_TENANT_CODE.equals(t.getTenantCode()) && Integer.valueOf(0).equals(status)) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "平台租户不可停用");
        }
        return tenantRepository.save(t);
    }

    /** 删除租户：平台租户不可删除。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysTenant t = require(id);
        if (IamConstants.PLATFORM_TENANT_CODE.equals(t.getTenantCode())) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "平台租户不可删除");
        }
        super.delete(id);
    }
}
