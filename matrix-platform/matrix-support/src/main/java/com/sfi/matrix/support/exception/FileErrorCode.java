package com.sfi.matrix.support.exception;

import com.sfi.scaffold.common.exception.ErrorCode;

/**
 * 文件系统业务错误码（6xxx 段）。
 *
 * @author lianghang
 */
public enum FileErrorCode implements ErrorCode {

    /** 文件类型不在白名单内 */
    FILE_TYPE_NOT_ALLOWED(6001, "文件类型不允许", 400),

    /** 文件系统白名单配置缺失或为空（fail-closed） */
    FILE_TYPE_CONFIG_MISSING(6002, "文件类型白名单配置缺失", 500),

    /** 读取上传文件字节失败 */
    FILE_READ_FAILED(6003, "读取上传文件失败", 400),

    /** 存储平台不支持分片上传 */
    FILE_MULTIPART_NOT_SUPPORTED(6004, "存储平台不支持分片上传", 400);

    private final int code;
    private final String message;
    private final int httpStatus;

    FileErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }
}
