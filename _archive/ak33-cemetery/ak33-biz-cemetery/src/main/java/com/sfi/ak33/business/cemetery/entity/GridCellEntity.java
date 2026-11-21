package com.sfi.ak33.business.cemetery.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 格位实体：规划出来的物理位置（图纸上的格子），不等于墓位。
 *
 * <p>规划 100 个格子、只建好 50 个——100 条 GridCell 记录，其中 50 条已初始化关联墓位，
 * 50 条仍是待建空格。格位承载"规划了多少 / 建好了多少"的对齐关系。</p>
 *
 * @author ak33
 */
@Entity
@Table(name = "cem_grid_cell")
public class GridCellEntity extends BaseEntity {

    /** 所属墓区ID（叶子区） */
    @Column(name = "area_id", nullable = false)
    private Long areaId;

    /** 横排号，从 1 开始 */
    @Column(name = "row_no", nullable = false)
    private Integer rowNo;

    /** 竖列号，从 1 开始 */
    @Column(name = "col_no", nullable = false)
    private Integer colNo;

    /** 是否已初始化为墓位：false=待建空格，true=已建墓位 */
    @Column(name = "initialized", nullable = false)
    private Boolean initialized = Boolean.FALSE;

    /** 初始化后关联的墓位ID，待建时为 null */
    @Column(name = "tomb_id")
    private Long tombId;

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
    }

    public Integer getColNo() {
        return colNo;
    }

    public void setColNo(Integer colNo) {
        this.colNo = colNo;
    }

    public Boolean getInitialized() {
        return initialized;
    }

    public void setInitialized(Boolean initialized) {
        this.initialized = initialized;
    }

    public Long getTombId() {
        return tombId;
    }

    public void setTombId(Long tombId) {
        this.tombId = tombId;
    }
}
