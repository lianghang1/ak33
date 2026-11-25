package com.sfi.matrix.support.dto;

import com.sfi.matrix.support.enums.FileType;

/**
 * 文件类型检测结果：一次读头算全，门禁校验、上传 MIME、落库共用这一份。
 *
 * @author matrix
 */
public final class FileTypeInfo {

    /** Tika 检测出的真实 MIME（统一小写） */
    private final String mime;
    /** 文件名后缀（统一小写） */
    private final String extension;
    /** 按真实 MIME 归类的媒体大类 */
    private final FileType fileType;

    public FileTypeInfo(String mime, String extension, FileType fileType) {
        this.mime = mime;
        this.extension = extension;
        this.fileType = fileType;
    }

    public String getMime() {
        return mime;
    }

    public String getExtension() {
        return extension;
    }

    public FileType getFileType() {
        return fileType;
    }
}
