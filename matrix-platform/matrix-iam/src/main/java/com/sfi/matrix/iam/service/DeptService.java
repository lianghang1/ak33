package com.sfi.matrix.iam.service;

import com.sfi.matrix.iam.entity.SysDept;
import com.sfi.matrix.iam.repository.SysDeptRepository;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.starter.data.jpa.service.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 部门单实体服务：get/list/page/save/delete 骨架继承 BaseService，本类只承载部门的业务规则。
 *
 * @author lianghang
 */
@Service
public class DeptService extends BaseService<SysDept> {

    private final SysDeptRepository deptRepository;

    public DeptService(SysDeptRepository deptRepository) {
        super(deptRepository);
        this.deptRepository = deptRepository;
    }

    /** 新增部门。 */
    @Transactional(rollbackFor = Exception.class)
    public SysDept create(String deptName, Long parentId, String tenantId, Integer sortNo, Long leaderId) {
        SysDept dept = new SysDept();
        dept.setDeptName(deptName);
        dept.setParentId(parentId);
        dept.setTenantId(tenantId);
        dept.setSortNo(sortNo == null ? 0 : sortNo);
        dept.setLeaderId(leaderId);
        dept.setStatus(1);
        return deptRepository.save(dept);
    }

    /** 修改部门。查不到由 require 抛 NOT_FOUND。 */
    @Transactional(rollbackFor = Exception.class)
    public SysDept update(Long id, String deptName, Long parentId, Integer sortNo,
                          Long leaderId, Integer status) {
        SysDept d = require(id);
        if (deptName != null && !deptName.trim().isEmpty()) {
            d.setDeptName(deptName);
        }
        d.setParentId(parentId);
        if (sortNo != null) {
            d.setSortNo(sortNo);
        }
        d.setLeaderId(leaderId);
        if (status != null) {
            d.setStatus(status);
        }
        return deptRepository.save(d);
    }

    /** 删除部门：存在子部门时拒绝。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysDept d = require(id);
        List<SysDept> children = deptRepository.findByTenantIdAndDeletedFalse(d.getTenantId()).stream()
                .filter(x -> id.equals(x.getParentId()))
                .collect(java.util.stream.Collectors.toList());
        if (!children.isEmpty()) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "存在子部门，不可删除");
        }
        super.delete(id);
    }
}
