package com.sfi.matrix.support.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;

/**
 * 文件上传表单参数，随文件以表单字段提交。
 *
 * @author lianghang
 */
@Getter
@Setter
public class FileUploadForm {

    /** 存储配置标识，空则取默认配置 */
    private String configId;

    /** 存储平台标识，空则取默认平台 */
    private String channelCode;

    /** 业务归属类型 */
    private String objectType;

    /** 业务归属标识 */
    private String objectId;

    /** 存储层模块，影响路径与公开性判断 */
    private String module;

    /** 内部生成缩略图的宽 */
    @Min(value = 1, message = "缩略图宽度必须大于 0")
    private Integer thumbWidth;

    /** 内部生成缩略图的高 */
    @Min(value = 1, message = "缩略图高度必须大于 0")
    private Integer thumbHeight;
}
