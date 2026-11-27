package com.sfi.matrix.support.dto;

import javax.validation.constraints.NotBlank;

/**
 * 参数新增请求（纯新增，业务键重复则拒绝，不做覆盖）。
 *
 * <p>configValue 只收明文字符串；sensitive 在此定初值。敏感值也以明文传入，
 * 落库即明文，对外读取时才加密成 {@link SecretCipher}。</p>
 *
 * @author lianghang
 */
public class ConfigCreateRequest {

    /** 配置分组，默认 DEFAULT */
    private String configGroup;

    @NotBlank(message = "参数键不能为空")
    private String configKey;

    @NotBlank(message = "参数值不能为空")
    private String configValue;

    /** 值类型：STRING（默认）/ INT / BOOL / JSON */
    private String valueType;

    /** 是否敏感值（创建时定初值，后续只能通过 /secret 改） */
    private Boolean needEncrypt;

    private String remark;
    private String tenantId;

    public String getConfigGroup() { return configGroup; }
    public void setConfigGroup(String configGroup) { this.configGroup = configGroup; }
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public Boolean getNeedEncrypt() { return needEncrypt; }
    public void setNeedEncrypt(Boolean needEncrypt) { this.needEncrypt = needEncrypt; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
