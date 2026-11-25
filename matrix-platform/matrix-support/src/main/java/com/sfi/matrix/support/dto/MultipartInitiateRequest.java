package com.sfi.matrix.support.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 分片初始化请求（POST JSON）。分片会话不含文件内容，门禁延迟到首片上传。
 *
 * @author lianghang
 */
@Getter
@Setter
public class MultipartInitiateRequest {

    /** 存储配置标识，空则取默认配置 */
    private String configId;

    /** 存储平台标识，空则取默认平台 */
    private String channelCode;

    @NotBlank(message = "原始文件名不能为空")
    private String originalFilename;

    @NotNull(message = "文件总字节数不能为空")
    private Long size;

    /** 业务归属类型 */
    private String objectType;

    /** 业务归属标识 */
    private String objectId;

    /** 存储层模块 */
    private String module;

    /** 存储层元数据 */
    private Map<String, String> metadata;
}
