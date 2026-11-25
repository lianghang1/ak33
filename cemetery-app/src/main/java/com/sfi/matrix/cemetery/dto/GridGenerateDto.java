package com.sfi.matrix.cemetery.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * 铺格位请求：在指定墓区按 rows×cols 生成规划格位。
 *
 * @author lianghang
 */
public class GridGenerateDto {

    @NotNull(message = "墓区ID不能为空")
    private Long areaId;

    @NotNull(message = "排数不能为空")
    @Positive(message = "排数必须大于 0")
    private Integer rows;

    @NotNull(message = "列数不能为空")
    @Positive(message = "列数必须大于 0")
    private Integer cols;

    public Long getAreaId() { return areaId; }
    public void setAreaId(Long areaId) { this.areaId = areaId; }

    public Integer getRows() { return rows; }
    public void setRows(Integer rows) { this.rows = rows; }

    public Integer getCols() { return cols; }
    public void setCols(Integer cols) { this.cols = cols; }
}
