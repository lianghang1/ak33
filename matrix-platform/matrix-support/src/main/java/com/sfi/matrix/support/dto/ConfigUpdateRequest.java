package com.sfi.matrix.support.dto;

import javax.validation.constraints.NotNull;

/**
 * 参数基础信息修改请求。
 *
 * <p><b>不碰 sensitive 标志</b>——放宽或收紧保护都走 /secret。configValue 仅在目标配置
 * 非敏感时可改；敏感配置传 configValue 由 Service 拒绝并引导走 /secret。</p>
 *
 * @author lianghang
 */
public class ConfigUpdateRequest {

    @NotNull(message = "id 不能为空")
    private Long id;

    /** 明文；仅非敏感配置允许修改，敏感配置传此字段会报错 */
    private String configValue;

    /** 值类型：STRING / INT / BOOL / JSON */
    private String valueType;

    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
