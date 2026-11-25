package com.sfi.matrix.support.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 基础文件上传入参：定位、业务归属、缩略图、存储层透传统一封装。
 *
 * <p>{@code source} 只接受 MultipartFile 或 byte[]（base64、分片统一解码为 byte[]），
 * 两者都可重复读流，门禁读头不消耗上传对象。</p>
 *
 * @author lianghang
 */
@Getter
@Setter
public class UploadCommand {

    /** 存储配置标识，必填 */
    private String configId;
    /** 存储平台标识，必填 */
    private String channelCode;
    /** 原始文件名，必填；内部新文件名由 x-file 生成 */
    private String originalFilename;
    /** 上传来源：MultipartFile / byte[] */
    private Object source;

    /** 业务归属类型 */
    private String objectType;
    /** 业务归属标识 */
    private String objectId;

    /** 外部传入的缩略图文件，优先采用 */
    private Object thumbnailSource;
    /** 内部生成缩略图的宽 */
    private Integer thumbWidth;
    /** 内部生成缩略图的高 */
    private Integer thumbHeight;
    /** 存储层模块 */
    private String module;
    /** 存储层元数据 */
    private Map<String, String> metadata;

}
