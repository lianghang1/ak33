package com.sfi.matrix.cemetery.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;
import com.sfi.scaffold.starter.data.jpa.logicdelete.JpaDeletedFilterAspect;
import com.sfi.scaffold.starter.tenant.aspect.JpaTenantFilterAspect;
import org.hibernate.annotations.Filter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 墓区：任意深度的园/区层级节点，靠 parentId 自关联表达层级。
 *
 * <p>level 由服务层按父节点自动计算（根=1，子=父+1）；fullPath 为根到当前节点的
 * 可读路径（如 玫瑰园/东区/二区），新增或移动节点时联动刷新。墓位编号的"区码/区名"
 * 取叶子墓区，"园名"取根节点。</p>
 *
 * @author lianghang
 */
@Entity
@Table(name = "cem_area")
@Filter(name = JpaTenantFilterAspect.FILTER_NAME, condition = "tenant_id = :tenantId")
@Filter(name = JpaDeletedFilterAspect.FILTER_NAME, condition = "deleted = false")
public class CemeteryArea extends BaseEntity {

    /** 逻辑删除标记，true=已删除；由字段名探测自动过滤 */
    @Column(name = "deleted")
    private Boolean deleted = Boolean.FALSE;

    /** 墓区编码，如 RY / A；同一 parent 下唯一 */
    @Column(name = "area_code", nullable = false, length = 32)
    private String areaCode;

    /** 墓区名称，如 玫瑰园 / A区 */
    @Column(name = "area_name", nullable = false, length = 64)
    private String areaName;

    /** 层级深度：根=1，子=父+1 */
    @Column(name = "level", nullable = false)
    private Integer level;

    /** 上级墓区ID，根节点为 null */
    @Column(name = "parent_id")
    private Long parentId;

    /** 根到当前节点的可读路径 */
    @Column(name = "full_path", length = 512)
    private String fullPath;

    /** 排序号 */
    @Column(name = "sort_no")
    private Integer sortNo;

    @Column(name = "tenant_id")
    private String tenantId;

    public String getAreaCode() { return areaCode; }
    public void setAreaCode(String areaCode) { this.areaCode = areaCode; }

    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getFullPath() { return fullPath; }
    public void setFullPath(String fullPath) { this.fullPath = fullPath; }

    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
