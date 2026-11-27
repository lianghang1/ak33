package com.sfi.matrix.support.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 分片上传初始化入参：定位、业务归属、存储层透传。
 *
 * <p>initiate 阶段没有文件内容，门禁延迟到首片上传时用该片字节头检测。</p>
 *
 * @author lianghang
 */
@Getter
@Setter
public class MultipartInitCommand {

    /** 存储配置标识，空则取默认配置 */
    private String configId;
    /** 存储平台标识，空则取默认平台 */
    private String channelCode;
    /** 原始文件名，必填 */
    private String originalFilename;
    /** 文件总字节数 */
    private Long fileSize;

    /** 业务归属类型 */
    private String objectType;
    /** 业务归属标识 */
    private String objectId;

    /** 存储层模块 */
    private String module;
    /** 存储层元数据 */
    private Map<String, String> metadata;
}
