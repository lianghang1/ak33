package com.sfi.matrix.cemetery.service;

import com.sfi.matrix.cemetery.entity.CemeteryArea;
import com.sfi.matrix.cemetery.repository.CemeteryAreaRepository;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.starter.data.jpa.service.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 墓区服务：任意深度的园/区层级树。
 *
 * <p>level 按父节点自动计算（根=1，子=父+1），fullPath 为根到当前节点的可读路径；
 * 新增/重命名/移动时级联刷新自身与全部后代的这两项派生字段。同 parent 下编码唯一。</p>
 *
 * @author lianghang
 */
@Service
public class CemeteryAreaService extends BaseService<CemeteryArea> {

    private final CemeteryAreaRepository areaRepository;

    public CemeteryAreaService(CemeteryAreaRepository areaRepository) {
        super(areaRepository);
        this.areaRepository = areaRepository;
    }

    /**
     * 覆盖基类 save：无论新增还是修改，都按当前 parent 重算 level/fullPath 并级联刷新子树。
     * 这样通用 /save 端点无需特化即可正确维护派生字段。租户为空时注入当前租户。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CemeteryArea save(CemeteryArea area) {
        if (area.getTenantId() == null || area.getTenantId().isEmpty()) {
            area.setTenantId(TenantIdProvider.current());
        }
        // 编码唯一（排除自身，新增时 id 为 null）
        checkCodeUnique(area.getParentId(), requireCode(area.getAreaCode()), area.getId());
        CemeteryArea parent = area.getParentId() == null ? null : areaRepository.findById(area.getParentId()).orElse(null);
        refreshSubtree(area, parent);
        return area;
    }

    /** 新增墓区：注入租户、校验编码唯一、计算 level/fullPath。 */
    @Transactional(rollbackFor = Exception.class)
    public CemeteryArea create(String areaCode, String areaName, Long parentId, Integer sortNo, String tenantId) {
        CemeteryArea parent = null;
        if (parentId != null) {
            parent = require(parentId);
        }
        String code = requireCode(areaCode);
        checkCodeUnique(parentId, code, null);

        CemeteryArea area = new CemeteryArea();
        area.setAreaCode(code);
        area.setAreaName(requireName(areaName));
        area.setParentId(parentId);
        area.setSortNo(sortNo);
        area.setTenantId(tenantId != null ? tenantId : TenantIdProvider.current());
        applyDerived(area, parent);
        return areaRepository.save(area);
    }

    /** 修改名称/排序；改名后级联刷新自身与子树 fullPath。 */
    @Transactional(rollbackFor = Exception.class)
    public CemeteryArea update(Long id, String areaName, Integer sortNo) {
        CemeteryArea area = require(id);
        if (areaName != null && !areaName.trim().isEmpty()) {
            area.setAreaName(areaName.trim());
        }
        if (sortNo != null) area.setSortNo(sortNo);
        refreshSubtree(area);
        return areaRepository.save(area);
    }

    /**
     * 移动节点到新父节点：禁止移到自己或自己的后代下（防环）；移动后级联刷新
     * 自身与子树的 level/fullPath。newParentId=null 表示提升为根。
     */
    @Transactional(rollbackFor = Exception.class)
    public CemeteryArea move(Long id, Long newParentId) {
        CemeteryArea area = require(id);
        if (id.equals(newParentId)) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "墓区不能移动到自身");
        }
        CemeteryArea newParent = null;
        if (newParentId != null) {
            newParent = require(newParentId);
            if (descendantIds(id).contains(newParentId)) {
                throw BizException.of(GlobalErrorCode.BAD_REQUEST, "墓区不能移动到自己的子孙下");
            }
        }
        checkCodeUnique(newParentId, area.getAreaCode(), id);
        area.setParentId(newParentId);
        refreshSubtree(area, newParent);
        return areaRepository.save(area);
    }

    /** 树：从所有根节点递归组装。 */
    public List<CemeteryArea> tree() {
        List<CemeteryArea> roots = areaRepository.findByParentIdIsNullAndDeletedFalseOrderBySortNoAsc();
        List<CemeteryArea> result = new ArrayList<>();
        for (CemeteryArea root : roots) {
            result.add(root);
            result.addAll(descendants(root.getId()));
        }
        return result;
    }

    /** 直接子节点。 */
    public List<CemeteryArea> children(Long parentId) {
        return areaRepository.findByParentIdAndDeletedFalseOrderBySortNoAsc(parentId);
    }

    /** 逻辑删除：有子节点时禁止删除。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        require(id);
        if (!children(id).isEmpty()) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "存在子墓区，不能删除");
        }
        super.delete(id);
    }

    // ==================== 内部 ====================

    /** 计算并写入 level/fullPath。 */
    private void applyDerived(CemeteryArea area, CemeteryArea parent) {
        if (parent == null) {
            area.setLevel(1);
            area.setFullPath(area.getAreaName());
        } else {
            area.setLevel(parent.getLevel() + 1);
            area.setFullPath(join(parent.getFullPath(), area.getAreaName()));
        }
    }

    /** 刷新自身（用其当前 parent）+ 全部后代的 level/fullPath。 */
    private void refreshSubtree(CemeteryArea area) {
        CemeteryArea parent = area.getParentId() == null ? null : areaRepository.findById(area.getParentId()).orElse(null);
        refreshSubtree(area, parent);
    }

    private void refreshSubtree(CemeteryArea area, CemeteryArea parent) {
        applyDerived(area, parent);
        areaRepository.save(area);
        for (CemeteryArea child : children(area.getId())) {
            refreshSubtree(child, area);
        }
    }

    /** 收集某节点全部后代 id（含各级）。 */
    private Set<Long> descendantIds(Long rootId) {
        Set<Long> ids = new HashSet<>();
        collectDescendantIds(rootId, ids);
        return ids;
    }

    private void collectDescendantIds(Long parentId, Set<Long> acc) {
        for (CemeteryArea child : children(parentId)) {
            acc.add(child.getId());
            collectDescendantIds(child.getId(), acc);
        }
    }

    private List<CemeteryArea> descendants(Long rootId) {
        List<CemeteryArea> result = new ArrayList<>();
        for (CemeteryArea child : children(rootId)) {
            result.add(child);
            result.addAll(descendants(child.getId()));
        }
        return result;
    }

    private void checkCodeUnique(Long parentId, String code, Long excludeId) {
        boolean exists;
        if (parentId == null) {
            exists = areaRepository.findByParentIdIsNullAndDeletedFalseOrderBySortNoAsc().stream()
                    .anyMatch(a -> a.getAreaCode().equals(code) && !a.getId().equals(excludeId));
        } else {
            exists = areaRepository.findByParentIdAndAreaCodeAndDeletedFalse(parentId, code)
                    .filter(a -> !a.getId().equals(excludeId)).isPresent();
        }
        if (exists) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "同层级下墓区编码已存在：" + code);
        }
    }

    /** 取根（园）：沿 parent 链上溯。用于编号上下文的园名。 */
    public CemeteryArea rootOf(Long areaId) {
        CemeteryArea cur = require(areaId);
        while (cur.getParentId() != null) {
            cur = require(cur.getParentId());
        }
        return cur;
    }

    private static String requireCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "墓区编码不能为空");
        }
        return code.trim();
    }

    private static String requireName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "墓区名称不能为空");
        }
        return name.trim();
    }

    private static String join(String parent, String child) {
        return (parent == null || parent.isEmpty()) ? child : parent + "/" + child;
    }
}
