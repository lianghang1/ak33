package com.sfi.matrix.support.dto;

import javax.validation.constraints.NotNull;

/**
 * 敏感参数专用修改请求（改值和/或 sensitive 标志）。
 *
 * <p>唯一能改 sensitive 标志、以及唯一能改敏感配置值的入口，受独立权限点 + 审计保护。
 * configValue 只收明文字符串——对外密文是结构化对象，无法直接塞进这个字段（类型隔离）。
 * 两个字段都可选，但至少提供一个，否则无意义。</p>
 *
 * @author lianghang
 */
public class ConfigSecretRequest {

    @NotNull(message = "id 不能为空")
    private Long id;

    /** 新明文值；为空表示不改值 */
    private String configValue;

    /** 敏感标志；为 null 表示不改标志 */
    private Boolean needEncrypt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
    public Boolean getNeedEncrypt() { return needEncrypt; }
    public void setNeedEncrypt(Boolean needEncrypt) { this.needEncrypt = needEncrypt; }
}
