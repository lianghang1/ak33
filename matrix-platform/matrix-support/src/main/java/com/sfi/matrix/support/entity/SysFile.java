package com.sfi.matrix.support.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;
import com.sfi.scaffold.starter.data.jpa.logicdelete.JpaDeletedFilterAspect;
import com.sfi.scaffold.starter.tenant.aspect.JpaTenantFilterAspect;
import org.dromara.x.file.storage.core.FileInfo;
import org.hibernate.annotations.Filter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 文件记录：原图与缩略图各占一组对称字段，同存一行。
 *
 * <p>分片上传时用 {@code multipartUploadId/multipartState} 标识会话；分片明细
 * 存 {@link SysFilePart}，完成后物理删除。非分片上传这两列为空。</p>
 *
 * <p>三个"类型"维度彼此正交：{@code extension/contentType} 是原始技术属性，
 * {@code fileType} 是媒体大类（是什么），{@code objectType} 是业务归属（挂在哪）。</p>
 *
 * @author lianghang
 */
@Entity
@Table(name = "sys_file")
@Filter(name = JpaTenantFilterAspect.FILTER_NAME, condition = "tenant_id = :tenantId")
@Filter(name = JpaDeletedFilterAspect.FILTER_NAME, condition = "deleted = false")
public class SysFile extends BaseEntity {

    /** 逻辑删除标记，true=已删除；由字段名探测自动过滤 */
    @Column(name = "deleted")
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "tenant_id")
    private String tenantId;

    /** 存储配置标识，对应含密钥/basePath 的平台配置 */
    @Column(name = "config_id")
    private String configId;

    /** 存储平台实例标识 */
    @Column(name = "channel_code")
    private String channelCode;

    // ---- 原图 ----

    /** 原图对象路径 */
    @Column(name = "path")
    private String path;

    /** 原图存储文件名 */
    @Column(name = "filename")
    private String filename;

    /** 用户上传时的原始文件名 */
    @Column(name = "original_filename")
    private String originalFilename;

    /** 原图原始地址（未签名） */
    @Column(name = "raw_url")
    private String rawUrl;

    /** 原图字节数 */
    @Column(name = "size")
    private Long size;

    /** 原图扩展名 */
    @Column(name = "extension")
    private String extension;

    /** 原图 MIME */
    @Column(name = "content_type")
    private String contentType;

    /** 媒体大类：IMAGE/VIDEO/AUDIO/DOCUMENT/ARCHIVE/OTHER */
    @Column(name = "file_type")
    private String fileType;

    /** 原图 ACL 原始值，只存不用于过滤 */
    @Column(name = "acl")
    private String acl;

    /** 文件 md5，用于秒传/去重 */
    @Column(name = "hash_md5")
    private String hashMd5;

    // ---- 缩略图（独立对象，字段与原图对称）----

    /** 缩略图对象路径 */
    @Column(name = "th_path")
    private String thPath;

    /** 缩略图存储文件名 */
    @Column(name = "th_filename")
    private String thFilename;

    /** 缩略图原始地址（未签名） */
    @Column(name = "th_raw_url")
    private String thRawUrl;

    /** 缩略图字节数 */
    @Column(name = "th_size")
    private Long thSize;

    /** 缩略图 MIME */
    @Column(name = "th_content_type")
    private String thContentType;

    // ---- 访问与业务关联 ----

    /** 是否需要签名访问，唯一签名判断标志 */
    @Column(name = "private_access")
    private Boolean privateAccess = Boolean.FALSE;

    /** 关联业务对象标识 */
    @Column(name = "object_id")
    private String objectId;

    /** 关联业务对象类型（业务归属） */
    @Column(name = "object_type")
    private String objectType;

    // ---- 分片上传会话 ----

    /** 分片上传会话 ID，非分片为空 */
    @Column(name = "multipart_upload_id")
    private String multipartUploadId;

    /** 分片会话阶段：INITIATED/UPLOADING/COMPLETED/ABORTED，非分片为空 */
    @Column(name = "multipart_state")
    private String multipartState;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

    public String getRawUrl() {
        return rawUrl;
    }

    public void setRawUrl(String rawUrl) {
        this.rawUrl = rawUrl;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
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

    public String getAcl() {
        return acl;
    }

    public void setAcl(String acl) {
        this.acl = acl;
    }

    public String getHashMd5() {
        return hashMd5;
    }

    public void setHashMd5(String hashMd5) {
        this.hashMd5 = hashMd5;
    }

    public String getThPath() {
        return thPath;
    }

    public void setThPath(String thPath) {
        this.thPath = thPath;
    }

    public String getThFilename() {
        return thFilename;
    }

    public void setThFilename(String thFilename) {
        this.thFilename = thFilename;
    }

    public String getThRawUrl() {
        return thRawUrl;
    }

    public void setThRawUrl(String thRawUrl) {
        this.thRawUrl = thRawUrl;
    }

    public Long getThSize() {
        return thSize;
    }

    public void setThSize(Long thSize) {
        this.thSize = thSize;
    }

    public String getThContentType() {
        return thContentType;
    }

    public void setThContentType(String thContentType) {
        this.thContentType = thContentType;
    }

    public Boolean getPrivateAccess() {
        return privateAccess;
    }

    public void setPrivateAccess(Boolean privateAccess) {
        this.privateAccess = privateAccess;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getMultipartUploadId() {
        return multipartUploadId;
    }

    public void setMultipartUploadId(String multipartUploadId) {
        this.multipartUploadId = multipartUploadId;
    }

    public String getMultipartState() {
        return multipartState;
    }

    public void setMultipartState(String multipartState) {
        this.multipartState = multipartState;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    /** 从记录重建对象存储 FileInfo，thumbnail=true 时取缩略图坐标与元信息 */
    public FileInfo toFileInfo(boolean thumbnail) {
        FileInfo info = new FileInfo();
        info.setPlatform(channelCode);
        info.setPath(thumbnail ? thPath : path);
        info.setFilename(thumbnail ? thFilename : filename);
        info.setOriginalFilename(originalFilename);
        info.setContentType(thumbnail ? thContentType : contentType);
        info.setSize(thumbnail ? thSize : size);
        info.setUploadId(multipartUploadId);
        return info;
    }
}
