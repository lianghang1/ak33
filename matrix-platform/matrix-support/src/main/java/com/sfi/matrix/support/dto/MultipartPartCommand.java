package com.sfi.matrix.support.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 分片上传单片入参。
 *
 * <p>source 只接受 MultipartFile 或 byte[]，均可重复读流：门禁读头不消耗上传对象。</p>
 *
 * @author lianghang
 */
@Getter
@Setter
public class MultipartPartCommand {

    /** 初始化返回的文件记录 ID */
    private Long fileId;
    /** 分片序号，从 1 开始 */
    private Integer partNumber;
    /** 分片内容：MultipartFile / byte[] */
    private Object source;
}
