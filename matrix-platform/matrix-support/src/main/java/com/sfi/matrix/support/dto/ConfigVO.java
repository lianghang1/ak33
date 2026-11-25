package com.sfi.matrix.support.dto;

import java.time.LocalDateTime;

/**
 * 系统参数对外视图。
 *
 * <p>与 {@code SysConfig} 实体的唯一差异在 configValue 的类型：实体里恒为明文字符串（DB 存明文），
 * 对外视图里 sensitive=true 时是 {@link SecretCipher} 密文对象、否则是明文字符串。这样对外永不
 * 直接暴露敏感明文，且密文是结构化对象而非字符串，无法被误当明文提交。</p>
 *
 * @author matrix
 */
public class ConfigVO {

    private Long id;
    private String tenantId;
    private String configGroup;
    private String configKey;
    /** sensitive=false 时为明文 String；sensitive=true 时为 SecretCipher 密文对象 */
    private Object configValue;
    private String valueType;
    private Boolean sensitive;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    private String updateBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getConfigGroup() { return configGroup; }
    public void setConfigGroup(String configGroup) { this.configGroup = configGroup; }
    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public Object getConfigValue() { return configValue; }
    public void setConfigValue(Object configValue) { this.configValue = configValue; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public Boolean getSensitive() { return sensitive; }
    public void setSensitive(Boolean sensitive) { this.sensitive = sensitive; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
}
