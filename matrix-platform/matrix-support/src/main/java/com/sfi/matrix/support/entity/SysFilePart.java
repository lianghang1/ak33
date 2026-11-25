package com.sfi.matrix.support.entity;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 分片上传明细（临时数据）。
 *
 * <p>每片一条，记录合并所需的 partNumber + eTag。complete 成功后按
 * multipartUploadId 物理删除全部明细，不做逻辑删除、不留 deleted 列。</p>
 *
 * <p>不继承 BaseEntity：分片是短生命周期的过程数据，只需要雪花主键与创建时间，
 * 创建时间同时供兜底清理任务判断会话是否过期。</p>
 *
 * @author lianghang
 */
@Entity
@Table(name = "sys_file_part")
@EntityListeners(AuditingEntityListener.class)
public class SysFilePart {

    @Id
    @GeneratedValue(generator = "snowflake_file_part")
    @GenericGenerator(name = "snowflake_file_part", strategy = "com.sfi.scaffold.starter.data.jpa.id.SnowflakeIdentifierGenerator")
    private Long id;

    /** 关联 sys_file.multipart_upload_id */
    @Column(name = "multipart_upload_id")
    private String multipartUploadId;

    /** 分片序号，合并请求必需 */
    @Column(name = "part_number")
    private Integer partNumber;

    /** 平台返回的分片 eTag，合并请求必需 */
    @Column(name = "e_tag")
    private String eTag;

    /** 分片字节数，断点续传校验用 */
    @Column(name = "part_size")
    private Long partSize;

    /** 创建时间，兜底清理判断过期用 */
    @CreatedDate
    @Column(name = "create_time")
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMultipartUploadId() {
        return multipartUploadId;
    }

    public void setMultipartUploadId(String multipartUploadId) {
        this.multipartUploadId = multipartUploadId;
    }

    public Integer getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(Integer partNumber) {
        this.partNumber = partNumber;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    public Long getPartSize() {
        return partSize;
    }

    public void setPartSize(Long partSize) {
        this.partSize = partSize;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
