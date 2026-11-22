package com.sfi.matrix.support.dto;

import javax.validation.constraints.NotBlank;

/** 字典类型增改请求。id 为空=新增/覆盖，非空=修改。 */
public class DictTypeRequest {

    private Long id;

    @NotBlank(message = "字典类型编码不能为空")
    private String dictType;

    @NotBlank(message = "字典类型名称不能为空")
    private String dictName;

    private String remark;
    private Integer status;
    private String tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDictType() { return dictType; }
    public void setDictType(String dictType) { this.dictType = dictType; }
    public String getDictName() { return dictName; }
    public void setDictName(String dictName) { this.dictName = dictName; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
