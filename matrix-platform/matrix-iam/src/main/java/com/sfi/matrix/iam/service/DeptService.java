package com.sfi.matrix.iam.service;

import com.sfi.matrix.iam.entity.SysDept;
import com.sfi.matrix.iam.repository.SysDeptRepository;
import com.sfi.matrix.iam.repository.SysUserTenantRepository;
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
    private final SysUserTenantRepository userTenantRepository;

    public DeptService(SysDeptRepository deptRepository, SysUserTenantRepository userTenantRepository) {
        super(deptRepository);
        this.deptRepository = deptRepository;
        this.userTenantRepository = userTenantRepository;
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

    /**
     * 按租户查部门树（芋道式左侧部门树）。按 sortNo 排序后装配成树。
     * 显式带 tenantId 条件查询，不依赖 Hibernate 租户过滤（平台管理员可跨租户查看）。
     */
    @SuppressWarnings("unchecked")
    public java.util.List<java.util.Map<String, Object>> treeOfTenant(String tenantId) {
        java.util.List<SysDept> depts = deptRepository.findByTenantIdAndDeletedFalse(tenantId).stream()
                .sorted(java.util.Comparator.comparing(d -> d.getSortNo() == null ? 0 : d.getSortNo()))
                .collect(java.util.stream.Collectors.toList());
        // 部门用户数：按 deptId 聚合，供树节点直接显示
        java.util.Map<Long, Long> userCount = new java.util.HashMap<>();
        for (Object[] row : userTenantRepository.countUsersByDept(tenantId)) {
            if (row[0] != null && row[1] != null) {
                userCount.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
            }
        }

        java.util.List<java.util.Map<String, Object>> nodes = new java.util.ArrayList<>();
        for (SysDept d : depts) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("deptName", d.getDeptName());
            m.put("parentId", d.getParentId());
            m.put("sortNo", d.getSortNo());
            m.put("leaderId", d.getLeaderId());
            m.put("status", d.getStatus());
            // 本部门直属人数（不含子部门，前端如需含子部门可自行累加 children）
            m.put("userCount", userCount.getOrDefault(d.getId(), 0L));
            nodes.add(m);
        }
        return com.sfi.scaffold.common.util.TreeUtils.build(
                nodes,
                m -> (Long) m.get("id"),
                m -> (Long) m.get("parentId"),
                null,
                (m, children) -> m.put("children", children));
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
