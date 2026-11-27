package com.sfi.matrix.support.dto;

/**
 * 文件对外 DTO：从 {@link com.sfi.matrix.support.entity.SysFile} 转换，字段精简。
 *
 * @author lianghang
 */
public class FileDTO {

    private Long id;
    private String filename;
    private String originalFilename;
    private Long fileSize;
    private String contentType;
    private String fileType;
    /** 可访问地址（公开为原始地址，私有为限时签名地址） */
    private String url;
    /** 缩略图可访问地址 */
    private String thumbUrl;
    /** 入库原始地址（未签名） */
    private String rawUrl;
    /** 缩略图入库原始地址（未签名） */
    private String rawThumbUrl;
    private String md5;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public void setThumbUrl(String thumbUrl) {
        this.thumbUrl = thumbUrl;
    }

    public String getRawUrl() {
        return rawUrl;
    }

    public void setRawUrl(String rawUrl) {
        this.rawUrl = rawUrl;
    }

    public String getRawThumbUrl() {
        return rawThumbUrl;
    }

    public void setRawThumbUrl(String rawThumbUrl) {
        this.rawThumbUrl = rawThumbUrl;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }
}
