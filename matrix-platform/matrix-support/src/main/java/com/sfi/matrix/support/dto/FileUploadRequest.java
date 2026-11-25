package com.sfi.matrix.support.dto;

/**
 * 文件上传表单参数（随 multipart 一起提交）。
 *
 * <p>configId/channelCode 指定存储配置与平台；privateAccess 标记是否签名访问；
 * objectType/objectId 为可选业务归属。</p>
 *
 * @author matrix
 */
public class FileUploadRequest {

    /** 存储配置标识 */
    private String configId;

    /** 存储平台标识 */
    private String channelCode;

    /** 是否需要签名访问 */
    private Boolean privateAccess = Boolean.FALSE;

    /** 业务归属类型（可选） */
    private String objectType;

    /** 业务归属标识（可选） */
    private String objectId;

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

    public Boolean getPrivateAccess() {
        return privateAccess;
    }

    public void setPrivateAccess(Boolean privateAccess) {
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
}
