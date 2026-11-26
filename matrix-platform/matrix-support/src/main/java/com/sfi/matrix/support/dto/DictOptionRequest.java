package com.sfi.matrix.support.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 字典选项增改请求。id 为空=新增/覆盖，非空=修改。 */
public class DictOptionRequest {

    private Long id;

    @NotNull(message = "所属字典ID不能为空")
    private Long dictId;

    @NotBlank(message = "选项编码不能为空")
    private String optionCode;

    @NotBlank(message = "选项显示名不能为空")
    private String optionLabel;

    private Integer sortNo;
    private Integer status;
    private String tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDictId() { return dictId; }
    public void setDictId(Long dictId) { this.dictId = dictId; }
    public String getOptionCode() { return optionCode; }
    public void setOptionCode(String optionCode) { this.optionCode = optionCode; }
    public String getOptionLabel() { return optionLabel; }
    public void setOptionLabel(String optionLabel) { this.optionLabel = optionLabel; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
