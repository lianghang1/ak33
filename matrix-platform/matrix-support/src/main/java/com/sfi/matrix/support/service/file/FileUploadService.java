package com.sfi.matrix.support.service.file;

import com.sfi.conflux.file.engine.FileGateway;
import com.sfi.matrix.support.dto.FileDTO;
import com.sfi.matrix.support.dto.FileTypeInfo;
import com.sfi.matrix.support.dto.UploadCommand;
import com.sfi.matrix.support.dto.UploadContext;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.matrix.support.service.SysFileService;
import com.sfi.scaffold.common.exception.BizException;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.upload.UploadPretreatment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.sfi.matrix.support.exception.FileErrorCode.FILE_READ_FAILED;
import static com.sfi.matrix.support.exception.FileErrorCode.FILE_TYPE_NOT_ALLOWED;

/**
 * 普通文件上传编排：门禁 → 上传+缩略图 → 落库，只暴露一个上传方法，调用方零重复。
 *
 * <p>决策/落库/重建的公共能力在 {@link FileUploadSupport}，分片上传复用同一套。</p>
 *
 * @author lianghang
 */
@Service
public class FileUploadService {

    private final FileGateway fileGateway;
    private final SysFileService fileService;
    private final FileTypeGuard fileTypeGuard;
    private final FileUploadSupport support;

    public FileUploadService(FileGateway fileGateway, SysFileService fileService,
                             FileTypeGuard fileTypeGuard, FileUploadSupport support) {
        this.fileGateway = fileGateway;
        this.fileService = fileService;
        this.fileTypeGuard = fileTypeGuard;
        this.support = support;
    }

    /** 基础上传：读+门禁 → 上传+缩略图 → 落库 */
    @Transactional(rollbackFor = Exception.class)
    public SysFile upload(UploadCommand cmd) {
        FileTypeInfo type;
        try (InputStream in = openStream(cmd.getSource())) {
            type = fileTypeGuard.detectAndCheck(in, cmd.getOriginalFilename());
        } catch (IOException e) {
            throw new BizException(FILE_READ_FAILED, "读取上传文件失败", e);
        }

        UploadContext ctx = support.resolve(cmd.getConfigId(), cmd.getChannelCode(), cmd.getOriginalFilename(),
                cmd.getObjectType(), cmd.getObjectId(), cmd.getModule(), cmd.getMetadata());
        support.applyType(ctx, type);

        FileInfo info = uploadWithThumbnail(ctx, cmd);
        return fileService.save(support.persist(ctx, info));
    }

    /** 上传 + 缩略图二选一，异常能力固定 fail-soft */
    private FileInfo uploadWithThumbnail(UploadContext ctx, UploadCommand cmd) {
        FileStorageService storage = fileGateway.nativeService(ctx.getConfigId(), ctx.getChannelCode());
        boolean supportAcl = storage.isSupportAcl(ctx.getChannelCode());

        UploadPretreatment pre = storage
                .of(cmd.getSource(), cmd.getOriginalFilename(), ctx.getMime())
                .setPlatform(ctx.getChannelCode())
                .setAcl(supportAcl, ctx.getAcl())
                .setPath(ctx.getPath())
                .setNotSupportMetadataThrowException(false)
                .setNotSupportAclThrowException(false)
                .setIgnoreThumbnailException(true, true);

        if (!ctx.getMetadata().isEmpty()) {
            pre.putMetadataAll(ctx.getMetadata());
        }
        if (cmd.getThumbnailSource() != null) {
            pre.thumbnailOf(cmd.getThumbnailSource());
        } else if (cmd.getThumbWidth() != null && cmd.getThumbHeight() != null) {
            pre.thumbnail(cmd.getThumbWidth(), cmd.getThumbHeight());
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
