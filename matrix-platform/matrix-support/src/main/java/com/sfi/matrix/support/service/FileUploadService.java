package com.sfi.matrix.support.service;

import com.sfi.conflux.file.engine.FileGateway;
import com.sfi.matrix.support.dto.FileDTO;
import com.sfi.matrix.support.dto.FileTypeInfo;
import com.sfi.matrix.support.dto.UploadCommand;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.scaffold.common.exception.BizException;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.upload.UploadPretreatment;
import org.dromara.x.file.storage.core.hash.HashInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.sfi.matrix.support.exception.FileErrorCode.FILE_READ_FAILED;
import static com.sfi.matrix.support.exception.FileErrorCode.FILE_TYPE_NOT_ALLOWED;

/**
 * 文件上传编排：门禁 → 上传+缩略图 → 落库，只暴露一个上传方法，调用方零重复。
 *
 * <p>平台配置/签名/存储实现全在 conflux 与 x-file-storage 内部，这里只做编排和落库。</p>
 *
 * <p>原图与缩略图是同一平台上的独立对象：缩略图由本次上传产生（FileInfo 带 thFilename）
 * 时随记录一起落库，否则缩略图字段留空。</p>
 *
 * @author matrix
 */
@Service
public class FileUploadService {

    private final FileGateway fileGateway;
    private final SysFileService fileService;
    private final FileTypeGuard fileTypeGuard;

    public FileUploadService(FileGateway fileGateway, SysFileService fileService,
                             FileTypeGuard fileTypeGuard) {
        this.fileGateway = fileGateway;
        this.fileService = fileService;
        this.fileTypeGuard = fileTypeGuard;
    }

    /** 基础上传：读+门禁 → 上传+缩略图 → 落库 */
    @Transactional(rollbackFor = Exception.class)
    public SysFile upload(UploadCommand cmd) {
        // 1 读 + 门禁一次完成，检测结果供 MIME/归类/落库复用
        FileTypeInfo type;
        try (InputStream in = openStream(cmd.getSource())) {
            type = fileTypeGuard.detectAndCheck(in, cmd.getOriginalFilename());
        } catch (IOException e) {
            throw new BizException(FILE_READ_FAILED, "读取上传文件失败", e);
        }

        // 2 上传 + 缩略图（原生出口）
        FileInfo info = uploadWithThumbnail(cmd, type.getMime());

        // 3 落库
        return persist(cmd, type, info);
    }

    /** 上传 + 缩略图二选一，异常能力固定 fail-soft */
    private FileInfo uploadWithThumbnail(UploadCommand cmd, String mime) {
        UploadPretreatment pre = fileGateway.nativeService(cmd.getConfigId(), cmd.getChannelCode())
                .of(cmd.getSource(), cmd.getOriginalFilename(), mime)
                .setPlatform(cmd.getChannelCode())
                .setNotSupportMetadataThrowException(false)
                .setNotSupportAclThrowException(false)
                .setIgnoreThumbnailException(true, true);

        if (cmd.getPath() != null) {
            pre.setPath(cmd.getPath());
        }
        if (cmd.getMetadata() != null && !cmd.getMetadata().isEmpty()) {
            pre.putMetadataAll(cmd.getMetadata());
        }
        if (cmd.getThumbnailSource() != null) {
            pre.thumbnailOf(cmd.getThumbnailSource());              // 外部缩略图优先
        } else if (cmd.getThumbWidth() != null && cmd.getThumbHeight() != null) {
            pre.thumbnail(cmd.getThumbWidth(), cmd.getThumbHeight()); // 内部生成
        }
        return pre.upload();
    }

    /** 来源归一：只接受可重复读的两类，门禁读完不消耗上传对象 */
    private static InputStream openStream(Object source) throws IOException {
        if (source instanceof MultipartFile) {
            return ((MultipartFile) source).getInputStream();
        }
        if (source instanceof byte[]) {
            return new ByteArrayInputStream((byte[]) source);
        }
        throw new BizException(FILE_TYPE_NOT_ALLOWED, "不支持的上传来源");
    }

    /** FileInfo → SysFile 落库，MIME/归类用门禁检测结果 */
    private SysFile persist(UploadCommand cmd, FileTypeInfo type, FileInfo info) {
        SysFile record = new SysFile();
        record.setConfigId(cmd.getConfigId());
        record.setChannelCode(cmd.getChannelCode());
        record.setPath(info.getPath());
        record.setFilename(info.getFilename());
        record.setOriginalFilename(cmd.getOriginalFilename());
        record.setRawUrl(info.getUrl());
        record.setSize(info.getSize());
        record.setExtension(info.getExt());
        record.setContentType(type.getMime());
        record.setFileType(type.getFileType().name());
        record.setPrivateAccess(cmd.isPrivateAccess());
        record.setObjectType(cmd.getObjectType());
        record.setObjectId(cmd.getObjectId());

        HashInfo hash = info.getHashInfo();
        if (hash != null) {
            record.setHashMd5(hash.getMd5());
        }

        if (info.getThFilename() != null) {
            record.setThPath(info.getPath());
            record.setThFilename(info.getThFilename());
            record.setThRawUrl(info.getThUrl());
            record.setThSize(info.getThSize());
            record.setThContentType(info.getThContentType());
        }
        return fileService.save(record);
    }

    /** 实体转 DTO，使用方自选 */
    public FileDTO toDto(SysFile f) {
        FileDTO dto = new FileDTO();
        dto.setId(f.getId());
        dto.setFilename(f.getFilename());
        dto.setOriginalFilename(f.getOriginalFilename());
        dto.setSize(f.getSize());
        dto.setContentType(f.getContentType());
        dto.setFileType(f.getFileType());
        dto.setUrl(f.getRawUrl());
        dto.setThumbUrl(f.getThRawUrl());
        dto.setMd5(f.getHashMd5());
        return dto;
    }
}
