package com.sfi.matrix.cemetery.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;
import com.sfi.scaffold.starter.tenant.aspect.JpaTenantFilterAspect;
import org.hibernate.annotations.Filter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 格位：图纸上规划出来的物理位置，不等于墓位。
 *
 * <p>规划 100 格、只建 50 位——100 条 GridCell，其中 50 条已初始化关联墓位，
 * 50 条仍是待建空格。initialized=false 表示待建。格位只挂叶子墓区。</p>
 *
 * @author matrix
 */
@Entity
@Table(name = "cem_grid_cell")
@Filter(name = JpaTenantFilterAspect.FILTER_NAME, condition = "tenant_id = :tenantId")
public class GridCell extends BaseEntity {

    /** 所属墓区ID（叶子节点） */
    @Column(name = "area_id", nullable = false)
    private Long areaId;

    /** 横排号，从 1 开始 */
    @Column(name = "row_no", nullable = false)
    private Integer rowNo;

    /** 竖列号，从 1 开始 */
    @Column(name = "col_no", nullable = false)
    private Integer colNo;

    /** 是否已初始化为墓位 */
    @Column(name = "initialized", nullable = false)
    private Boolean initialized = Boolean.FALSE;

    /** 初始化后关联的墓位ID，待建时为 null */
    @Column(name = "tomb_id")
    private Long tombId;

    @Column(name = "tenant_id")
    private String tenantId;

    public Long getAreaId() { return areaId; }
    public void setAreaId(Long areaId) { this.areaId = areaId; }

    public Integer getRowNo() { return rowNo; }
    public void setRowNo(Integer rowNo) { this.rowNo = rowNo; }

    public Integer getColNo() { return colNo; }
    public void setColNo(Integer colNo) { this.colNo = colNo; }

    public Boolean getInitialized() { return initialized; }
    public void setInitialized(Boolean initialized) { this.initialized = initialized; }

    public Long getTombId() { return tombId; }
    public void setTombId(Long tombId) { this.tombId = tombId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
