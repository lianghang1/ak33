package com.sfi.matrix.support.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 分片上传单片表单参数（multipart 豁免，分片文件以 @RequestPart 单独提交）。
 *
 * @author lianghang
 */
@Getter
@Setter
public class MultipartPartForm {

    @NotNull(message = "文件记录 ID 不能为空")
    private Long fileId;

    @NotNull(message = "分片序号不能为空")
    @Min(value = 1, message = "分片序号从 1 开始")
    @Max(value = 10000, message = "分片序号不能大于 10000")
    private Integer partNumber;
}
