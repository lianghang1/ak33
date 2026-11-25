package com.sfi.matrix.support.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 分片上传初始化结果。
 *
 * @author lianghang
 */
@Getter
@Setter
public class MultipartInitResult {

    /** 文件记录 ID，后续上传分片/完成/取消都凭它定位 */
    private Long fileId;
    /** 平台返回的分片上传会话 ID */
    private String uploadId;
}
