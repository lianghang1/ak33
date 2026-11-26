package com.sfi.matrix.support.repository.file;

import com.sfi.matrix.support.entity.SysFilePart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 分片明细 Repository（临时数据，物理删除）。
 *
 * @author lianghang
 */
@Repository
public interface SysFilePartRepository extends JpaRepository<SysFilePart, Long> {

    /** 按会话查全部分片，按序号升序（合并时按序组装） */
    List<SysFilePart> findByMultipartUploadIdOrderByPartNumberAsc(String multipartUploadId);

    /** 按会话查某一片（断点续传跳过已传片用） */
    List<SysFilePart> findByMultipartUploadIdAndPartNumber(String multipartUploadId, Integer partNumber);

    /** complete/abort 后按会话物理删除全部分片明细 */
    void deleteByMultipartUploadId(String multipartUploadId);
}
