package com.sfi.ak33.business.cemetery.entity;

import com.sfi.ak33.platform.core.enums.HoleTypeEnum;
import com.sfi.ak33.platform.core.enums.TombStatusEnum;
import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * 墓位实体：格位初始化后生成的正式墓位，是购墓/安葬/售后的业务主体。
 *
 * <p>三标识（code/basePath/fullPath）由编号策略在初始化时一次性生成并落库为快照，
 * 策略变更不反向改写历史数据；status 由业务动作驱动流转。</p>
 *
 * @author ak33
 */
@Entity
@Table(name = "cem_tomb")
public class TombEntity extends BaseEntity {

    /** 来源格位ID */
    @Column(name = "grid_cell_id", nullable = false)
    private Long gridCellId;

    /** 所属墓区ID */
    @Column(name = "area_id", nullable = false)
    private Long areaId;

    /** 编号：如 A-01-01，租户内唯一 */
    @Column(name = "code", nullable = false, length = 64)
    private String code;

    /** 基础路径：如 玫瑰园/A区/1排 */
    @Column(name = "base_path", length = 255)
    private String basePath;

    /** 全路径：如 玫瑰园/A区/1排/01号 */
    @Column(name = "full_path", length = 512)
    private String fullPath;

    /** 生成编号所用的策略名（快照，便于溯源） */
    @Column(name = "code_strategy", length = 64)
    private String codeStrategy;

    /** 穴位数：1=单穴，2=双穴，3=多穴 */
    @Column(name = "holes", nullable = false)
    private Integer holes;

    /** 墓位状态 code，对应 TombStatusEnum */
    @Column(name = "status", nullable = false, length = 32)
    private String status;

    /** 销售价格（预留，后续接入墓型定价） */
    @Column(name = "sale_price", precision = 14, scale = 2)
    private BigDecimal salePrice;

    public Long getGridCellId() {
        return gridCellId;
    }

    public void setGridCellId(Long gridCellId) {
        this.gridCellId = gridCellId;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getFullPath() {
        return fullPath;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public String getCodeStrategy() {
        return codeStrategy;
    }

    public void setCodeStrategy(String codeStrategy) {
        this.codeStrategy = codeStrategy;
    }

    public Integer getHoles() {
        return holes;
    }

    public void setHoles(Integer holes) {
        this.holes = holes;
    }

    /** 以枚举方式读状态 */
    public TombStatusEnum statusEnum() {
        return TombStatusEnum.of(this.status);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setStatus(TombStatusEnum status) {
        this.status = status.getCode();
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }
}
