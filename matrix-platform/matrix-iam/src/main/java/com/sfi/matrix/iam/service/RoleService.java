package com.sfi.matrix.iam.service;

import com.sfi.matrix.iam.constant.IamConstants;
import com.sfi.matrix.iam.entity.SysRole;
import com.sfi.matrix.iam.repository.SysRoleRepository;
import com.sfi.matrix.iam.repository.SysUserRoleRepository;
import com.sfi.scaffold.common.datascope.DataScopeType;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.starter.data.jpa.service.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色单实体服务：get/list/page/save/delete 骨架继承 BaseService，本类只承载角色的业务规则。
 *
 * @author lianghang
 */
@Service
public class RoleService extends BaseService<SysRole> {

    private final SysRoleRepository roleRepository;
    private final SysUserRoleRepository userRoleRepository;

    public RoleService(SysRoleRepository roleRepository, SysUserRoleRepository userRoleRepository) {
        super(roleRepository);
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    /** 新增角色：内置编码保留校验、编码唯一、数据范围合法性。 */
    @Transactional(rollbackFor = Exception.class)
    public SysRole create(String roleCode, String roleName, String tenantId, String dataScope,
                          String dataScopeDeptIds, Integer sortNo, Integer status, String remark) {
        if (IamConstants.TENANT_ADMIN_ROLE_CODE.equals(roleCode)
                || IamConstants.PLATFORM_ADMIN_ROLE_CODE.equals(roleCode)) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "该角色编码为系统内置，不可手动创建：" + roleCode);
        }
        roleRepository.findByRoleCodeAndTenantIdAndDeletedFalse(roleCode, tenantId).ifPresent(r -> {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "角色编码已存在：" + roleCode);
        });
        String scope = (dataScope == null || dataScope.trim().isEmpty())
                ? DataScopeType.ALL.name() : dataScope.trim();
        if (!DataScopeType.isValid(scope)) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "数据范围非法：" + dataScope
                    + "，可选值：" + DataScopeType.CODES);
        }
        SysRole role = new SysRole();
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setTenantId(tenantId);
        role.setRoleType(IamConstants.ROLE_TYPE_NORMAL);
        role.setSortNo(sortNo == null ? 0 : sortNo);
        role.setBuiltIn(Boolean.FALSE);
        role.setDataScope(DataScopeType.of(scope).name());
        role.setDataScopeDeptIds(dataScopeDeptIds);
        role.setStatus(status == null ? 1 : status);
        role.setRemark(remark);
        return roleRepository.save(role);
    }

    /** 修改角色：内置超管保护 + 数据范围合法性。查不到由 require 抛 NOT_FOUND。 */
    @Transactional(rollbackFor = Exception.class)
    public SysRole update(Long id, String roleName, String dataScope, String dataScopeDeptIds,
                          Integer sortNo, Integer status, String remark) {
        SysRole r = require(id);
        if (isSuperRole(r)) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "内置超管角色不可修改");
        }
        if (roleName != null && !roleName.trim().isEmpty()) {
            r.setRoleName(roleName);
        }
        if (dataScope != null && !dataScope.trim().isEmpty()) {
            if (!DataScopeType.isValid(dataScope)) {
                throw BizException.of(GlobalErrorCode.BAD_REQUEST, "数据范围非法：" + dataScope);
            }
            r.setDataScope(DataScopeType.of(dataScope).name());
        }
        if (dataScopeDeptIds != null) {
            r.setDataScopeDeptIds(dataScopeDeptIds);
        }
        if (sortNo != null) {
            r.setSortNo(sortNo);
        }
        if (status != null) {
            r.setStatus(status);
        }
        if (remark != null) {
            r.setRemark(remark);
        }
        return roleRepository.save(r);
    }

    /** 批量改状态：内置超管角色跳过（不可停用），其余按 id 更新。 */
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(java.util.List<Long> ids, Integer status) {
        if (status == null) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "目标状态不能为空");
        }
        for (Long id : ids) {
            SysRole r = roleRepository.findById(id).orElse(null);
            if (r == null || isSuperRole(r)) {
                continue;
            }
            r.setStatus(status);
            roleRepository.save(r);
        }
    }

    /** 删除角色：内置超管保护 + 已分配校验。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole r = require(id);
        if (isSuperRole(r)) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "内置超管角色不可删除");
        }
        boolean inUse = userRoleRepository.findAll().stream()
                .anyMatch(ur -> id.equals(ur.getRoleId()) && !Boolean.TRUE.equals(ur.getDeleted()));
        if (inUse) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "角色已分配给用户，不可删除");
        }
        super.delete(id);
    }

    private boolean isSuperRole(SysRole role) {
        String type = role.getRoleType();
        return IamConstants.ROLE_TYPE_PLATFORM_ADMIN.equals(type)
                || IamConstants.ROLE_TYPE_TENANT_ADMIN.equals(type);
    }
}
