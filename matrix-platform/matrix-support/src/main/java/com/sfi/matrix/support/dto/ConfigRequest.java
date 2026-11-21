package com.sfi.matrix.support.dto;

import javax.validation.constraints.NotBlank;

/** 参数增改请求。id 为空=新增/覆盖，非空=修改。 */
public class ConfigRequest {

    private Long id;

    @NotBlank(message = "参数键不能为空")
    private String configKey;

    @NotBlank(message = "参数值不能为空")
    private String configValue;

    private String remark;
    private String tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
