package com.sfi.matrix.cemetery.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 格位修改请求：只允许改坐标（排/列），墓区归属与初始化状态不可改。
 *
 * @author lianghang
 */
public class GridCellSaveDto {

    /** 格位ID（必填，修改现有格位） */
    @NotNull(message = "格位ID不能为空")
    private Long id;

    /** 横排号，从 1 开始 */
    @NotNull(message = "排号不能为空")
    @Min(value = 1, message = "排号必须大于 0")
    private Integer rowNo;

    /** 竖列号，从 1 开始 */
    @NotNull(message = "列号不能为空")
    @Min(value = 1, message = "列号必须大于 0")
    private Integer colNo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getRowNo() { return rowNo; }
    public void setRowNo(Integer rowNo) { this.rowNo = rowNo; }

    public Integer getColNo() { return colNo; }
    public void setColNo(Integer colNo) { this.colNo = colNo; }
}
