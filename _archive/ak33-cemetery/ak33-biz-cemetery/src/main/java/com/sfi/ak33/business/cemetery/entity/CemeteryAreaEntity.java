package com.sfi.ak33.business.cemetery.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 墓区实体：园 / 区 的层级节点（如 玫瑰园 / A区）。
 *
 * <p>用 parentId 自关联表达"园→区"层级，层级深度不写死，便于后续扩展。
 * 墓位是挂在叶子墓区（区）下的物理位置。</p>
 *
 * @author ak33
 */
@Entity
@Table(name = "cem_area")
public class CemeteryAreaEntity extends BaseEntity {

    /** 墓区编码，如 RY / A */
    @Column(name = "area_code", nullable = false, length = 32)
    private String areaCode;

    /** 墓区名称，如 玫瑰园 / A区 */
    @Column(name = "area_name", nullable = false, length = 64)
    private String areaName;

    /** 层级：1=园，2=区 */
    @Column(name = "level", nullable = false)
    private Integer level;

    /** 上级墓区ID，园级为 null */
    @Column(name = "parent_id")
    private Long parentId;

    /** 可读层级路径，如 玫瑰园/A区 */
    @Column(name = "full_path", length = 255)
    private String fullPath;

    /** 排序号 */
    @Column(name = "sort_no")
    private Integer sortNo;

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }
}
