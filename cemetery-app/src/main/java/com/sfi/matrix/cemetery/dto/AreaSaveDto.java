package com.sfi.matrix.cemetery.dto;

import javax.validation.constraints.NotBlank;

/**
 * 墓区新增/修改请求。
 *
 * @author matrix
 */
public class AreaSaveDto {

    /** 修改时传 id，新增不传 */
    private Long id;

    @NotBlank(message = "墓区编码不能为空")
    private String areaCode;

    @NotBlank(message = "墓区名称不能为空")
    private String areaName;

    /** 上级墓区ID，根不传 */
    private Long parentId;

    private Integer sortNo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAreaCode() { return areaCode; }
    public void setAreaCode(String areaCode) { this.areaCode = areaCode; }

    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
}
