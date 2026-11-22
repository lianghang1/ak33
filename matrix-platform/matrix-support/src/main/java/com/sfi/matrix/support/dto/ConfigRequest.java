package com.sfi.matrix.support.dto;

import javax.validation.constraints.NotBlank;

/** 参数增改请求。id 为空=新增/覆盖，非空=修改。 */
public class ConfigRequest {

    private Long id;

    /** 配置分组，默认 DEFAULT */
    private String configGroup;

    @NotBlank(message = "参数键不能为空")
    private String configKey;

    @NotBlank(message = "参数值不能为空")
    private String configValue;

    /** 值类型：STRING（默认）/ INT / BOOL / JSON */
    private String valueType;

    /** 是否敏感值（写入加密落库、读取解密返回） */
    private Boolean sensitive;

    private String remark;
    private String tenantId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConfigGroup() { return configGroup; }
    public void setConfigGroup(String configGroup) { this.configGroup = configGroup; }
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public Boolean getSensitive() { return sensitive; }
    public void setSensitive(Boolean sensitive) { this.sensitive = sensitive; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
