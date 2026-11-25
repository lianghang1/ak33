package com.sfi.matrix.cemetery.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

/**
 * 墓位初始化请求：勾选若干空格位 + 穴位数 + 销售价，批量建成墓位。
 *
 * @author lianghang
 */
public class TombInitDto {

    @NotEmpty(message = "请至少选择一个格位")
    private List<Long> cellIds;

    @NotNull(message = "穴位数不能为空")
    @Positive(message = "穴位数必须大于 0")
    private Integer holes;

    private BigDecimal salePrice;

    public List<Long> getCellIds() { return cellIds; }
    public void setCellIds(List<Long> cellIds) { this.cellIds = cellIds; }

    public Integer getHoles() { return holes; }
    public void setHoles(Integer holes) { this.holes = holes; }

    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
}
