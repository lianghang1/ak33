package com.sfi.matrix.support.dto;

import java.util.Map;

/**
 * 基础文件上传入参：定位、业务归属、缩略图、存储层透传统一封装。
 *
 * <p>{@code source} 只接受 MultipartFile 或 byte[]（base64、分片统一解码为 byte[]），
 * 两者都可重复读流，门禁读头不消耗上传对象。</p>
 *
 * @author matrix
 */
public class UploadCommand {

    /** 存储配置标识，必填 */
    private String configId;
    /** 存储平台标识，必填 */
    private String channelCode;
    /** 原始文件名，必填；内部新文件名由 x-file 生成 */
    private String originalFilename;
    /** 上传来源：MultipartFile / byte[] */
    private Object source;

    /** 是否需要签名访问 */
    private boolean privateAccess;
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

    /** 存储层路径（OSS object key） */
    private String path;
    /** 存储层元数据 */
    private Map<String, String> metadata;

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public Object getSource() {
        return source;
    }

    public void setSource(Object source) {
        this.source = source;
    }

    public boolean isPrivateAccess() {
        return privateAccess;
    }

    public void setPrivateAccess(boolean privateAccess) {
        this.privateAccess = privateAccess;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public Object getThumbnailSource() {
        return thumbnailSource;
    }

    public void setThumbnailSource(Object thumbnailSource) {
        this.thumbnailSource = thumbnailSource;
    }

    public Integer getThumbWidth() {
        return thumbWidth;
    }

    public void setThumbWidth(Integer thumbWidth) {
        this.thumbWidth = thumbWidth;
    }

    public Integer getThumbHeight() {
        return thumbHeight;
    }

    public void setThumbHeight(Integer thumbHeight) {
        this.thumbHeight = thumbHeight;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
