package com.sfi.matrix.support.service;

import com.sfi.matrix.support.entity.SysFilePart;
import com.sfi.matrix.support.repository.SysFilePartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 分片明细服务：临时数据的读写与清理。
 *
 * <p>不继承 BaseService（SysFilePart 非 BaseEntity、逻辑删除不适用）：
 * 分片明细在 complete/abort 后按会话物理删除。</p>
 *
 * @author matrix
 */
@Service
public class SysFilePartService {

    private final SysFilePartRepository partRepository;

    public SysFilePartService(SysFilePartRepository partRepository) {
        this.partRepository = partRepository;
    }

    /** 按会话查全部分片（升序），complete 时按序组装合并请求 */
    public List<SysFilePart> listByUploadId(String multipartUploadId) {
        return partRepository.findByMultipartUploadIdOrderByPartNumberAsc(multipartUploadId);
    }

    /** 查某一片是否已传（断点续传跳过用） */
    public List<SysFilePart> findPart(String multipartUploadId, Integer partNumber) {
        return partRepository.findByMultipartUploadIdAndPartNumber(multipartUploadId, partNumber);
    }

    /** 记录一个分片 */
    @Transactional(rollbackFor = Exception.class)
    public SysFilePart save(SysFilePart part) {
        return partRepository.save(part);
    }

    /** complete/abort 后清理该会话的全部分片明细 */
    @Transactional(rollbackFor = Exception.class)
    public void clearByUploadId(String multipartUploadId) {
        partRepository.deleteByMultipartUploadId(multipartUploadId);
    }
}
