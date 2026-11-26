package com.sfi.matrix.support.service.file;

import com.sfi.matrix.support.entity.SysFile;
import com.sfi.matrix.support.repository.SysFileRepository;
import com.sfi.scaffold.starter.data.jpa.service.BaseService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 文件记录服务：继承 BaseService 复用 get/save/delete 骨架，补充按业务对象、
 * md5、存储定位、分片会话的读取。
 *
 * <p>此处只提供基础数据访问，签名与访问地址、上传编排等接口逻辑后续接入。</p>
 *
 * @author lianghang
 */
@Service
public class SysFileService extends BaseService<SysFile> {

    private final SysFileRepository fileRepository;

    public SysFileService(SysFileRepository fileRepository) {
        super(fileRepository);
        this.fileRepository = fileRepository;
    }

    /** 按业务对象反查未删除文件 */
    public List<SysFile> listByObject(String objectType, String objectId) {
        return fileRepository.findByObjectTypeAndObjectIdAndDeletedFalseOrderByIdAsc(objectType, objectId);
    }

    /** 按 md5 查未删除文件（秒传/去重） */
    public Optional<SysFile> findByMd5(String hashMd5) {
        return fileRepository.findByHashMd5AndDeletedFalse(hashMd5);
    }

    /** 按存储定位查未删除文件 */
    public Optional<SysFile> findByLocation(String configId, String channelCode, String path) {
        return fileRepository.findByConfigIdAndChannelCodeAndPathAndDeletedFalse(configId, channelCode, path);
    }

    /** 按分片会话 ID 查记录 */
    public Optional<SysFile> findByMultipartUploadId(String multipartUploadId) {
        return fileRepository.findByMultipartUploadIdAndDeletedFalse(multipartUploadId);
    }
}
