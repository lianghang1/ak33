package com.sfi.matrix.support.dto;

import com.sfi.matrix.support.enums.FileType;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 上传决策上下文：一次解析出定位、类型、权限、路径、元数据，普通上传与分片上传共用。
 *
 * @author lianghang
 */
@Getter
@Setter
public class UploadContext {

    private String configId;
    private String channelCode;
    private String originalFilename;
    /** 真实 MIME，门禁检测得出 */
    private String mime;
    /** 文件大类 */
    private FileType fileType;
    /** 存储层模块，已校验标准化 */
    private String module;
    /** 存储路径：module/yyyy/MM/ */
    private String path;
    /** 本次上传决策的权限 */
    private String acl;
    private String objectType;
    private String objectId;
    private Map<String, String> metadata;
}
