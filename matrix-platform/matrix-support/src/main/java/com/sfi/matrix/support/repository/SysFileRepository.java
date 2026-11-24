package com.sfi.matrix.support.repository;

import com.sfi.matrix.support.entity.SysFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文件记录 Repository。
 *
 * @author matrix
 */
@Repository
public interface SysFileRepository extends JpaRepository<SysFile, Long> {

    /** 按业务对象反查未删除文件 */
    List<SysFile> findByObjectTypeAndObjectIdAndDeletedFalseOrderByIdAsc(String objectType, String objectId);

    /** 按 md5 查未删除文件（秒传/去重） */
    Optional<SysFile> findByHashMd5AndDeletedFalse(String hashMd5);

    /** 按存储定位查未删除文件 */
    Optional<SysFile> findByConfigIdAndChannelCodeAndPathAndDeletedFalse(
            String configId, String channelCode, String path);

    /** 按分片会话 ID 查记录（分片上传过程用） */
    Optional<SysFile> findByMultipartUploadIdAndDeletedFalse(String multipartUploadId);
}
