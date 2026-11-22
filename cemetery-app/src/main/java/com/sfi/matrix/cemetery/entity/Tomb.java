package com.sfi.matrix.cemetery.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;
import com.sfi.scaffold.starter.tenant.aspect.JpaTenantFilterAspect;
import org.hibernate.annotations.Filter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * 墓位：格位初始化后生成的正式墓位，是销售/安葬/售后的业务主体。
 *
 * <p>三标识（code/basePath/fullPath）由编号策略在初始化时一次性生成并落库为快照，
 * 策略或墓区树后续变更不反向改写历史数据。status 由业务动作驱动流转，P0 阶段
 * 初始化后固定为 INITIALIZED。</p>
 *
 * @author matrix
 */
@Entity
@Table(name = "cem_tomb")
@Filter(name = JpaTenantFilterAspect.FILTER_NAME, condition = "tenant_id = :tenantId")
public class Tomb extends BaseEntity {

    /** 来源格位ID */
    @Column(name = "grid_cell_id", nullable = false)
    private Long gridCellId;

    /** 所属墓区ID（叶子节点） */
    @Column(name = "area_id", nullable = false)
    private Long areaId;

    /** 编号，如 A-01-01，租户内唯一 */
    @Column(name = "code", nullable = false, length = 64)
    private String code;

    /** 基础路径：如 玫瑰园/东区/1排 */
    @Column(name = "base_path", length = 255)
    private String basePath;

    /** 全路径：如 玫瑰园/东区/1排/01号 */
    @Column(name = "full_path", length = 512)
    private String fullPath;

    /** 生成编号所用的策略名（快照，便于溯源） */
    @Column(name = "code_strategy", length = 64)
    private String codeStrategy;

    /** 穴位数：任意正整数（1=单穴，2=双穴，6/7/8 亦可） */
    @Column(name = "holes", nullable = false)
    private Integer holes;

    /** 墓位状态 code，P0 初始化固定为 INITIALIZED */
    @Column(name = "status", nullable = false, length = 32)
    private String status;

    /** 销售价格 */
    @Column(name = "sale_price", precision = 14, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "tenant_id")
    private String tenantId;

    public Long getGridCellId() { return gridCellId; }
    public void setGridCellId(Long gridCellId) { this.gridCellId = gridCellId; }

    public Long getAreaId() { return areaId; }
    public void setAreaId(Long areaId) { this.areaId = areaId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getBasePath() { return basePath; }
    public void setBasePath(String basePath) { this.basePath = basePath; }

    public String getFullPath() { return fullPath; }
    public void setFullPath(String fullPath) { this.fullPath = fullPath; }

    public String getCodeStrategy() { return codeStrategy; }
    public void setCodeStrategy(String codeStrategy) { this.codeStrategy = codeStrategy; }

    public Integer getHoles() { return holes; }
    public void setHoles(Integer holes) { this.holes = holes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
