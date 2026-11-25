package com.sfi.matrix.cemetery.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 墓位修改请求：只允许改穴位数与售价。编号/路径/状态是快照或由业务动作驱动，不在此改。
 *
 * @author lianghang
 */
public class TombSaveDto {

    /** 墓位ID（必填） */
    @NotNull(message = "墓位ID不能为空")
    private Long id;

    /** 穴位数，任意正整数 */
    @NotNull(message = "穴位数不能为空")
    @Min(value = 1, message = "穴位数必须大于 0")
    private Integer holes;

    /** 销售价格 */
    private BigDecimal salePrice;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getHoles() { return holes; }
    public void setHoles(Integer holes) { this.holes = holes; }

    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
}
