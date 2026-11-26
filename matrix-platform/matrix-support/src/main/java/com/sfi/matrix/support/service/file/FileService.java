package com.sfi.matrix.support.service.file;

import com.sfi.conflux.file.engine.FileGateway;
import com.sfi.matrix.support.dto.FileDTO;
import com.sfi.matrix.support.dto.FileTypeInfo;
import com.sfi.matrix.support.dto.MultipartInitCommand;
import com.sfi.matrix.support.dto.MultipartInitResult;
import com.sfi.matrix.support.dto.MultipartPartCommand;
import com.sfi.matrix.support.dto.UploadCommand;
import com.sfi.matrix.support.dto.UploadContext;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.matrix.support.entity.SysFilePart;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.dromara.x.file.storage.core.upload.InitiateMultipartUploadPretreatment;
import org.dromara.x.file.storage.core.upload.MultipartUploadSupportInfo;
import org.dromara.x.file.storage.core.upload.UploadPretreatment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static com.sfi.matrix.support.exception.FileErrorCode.FILE_MULTIPART_NOT_SUPPORTED;
import static com.sfi.matrix.support.exception.FileErrorCode.FILE_READ_FAILED;
import static com.sfi.matrix.support.exception.FileErrorCode.FILE_TYPE_NOT_ALLOWED;

/**
 * 文件业务门面：上传、分片、查询统一入口。
 *
 * @author lianghang
 */
@Service
public class FileService {

    private static final String STATE_INITIATED = "INITIATED";
    private static final String STATE_UPLOADING = "UPLOADING";
    private static final String STATE_COMPLETED = "COMPLETED";
    private static final String STATE_ABORTED = "ABORTED";

    private final FileGateway fileGateway;
    private final SysFileService fileService;
    private final SysFilePartService partService;
    private final FileTypeGuard fileTypeGuard;
    private final FileUploadSupport support;
    private final FileUrlResolver urlResolver;

    public FileService(FileGateway fileGateway, SysFileService fileService, SysFilePartService partService,
                       FileTypeGuard fileTypeGuard, FileUploadSupport support, FileUrlResolver urlResolver) {
        this.fileGateway = fileGateway;
        this.fileService = fileService;
        this.partService = partService;
        this.fileTypeGuard = fileTypeGuard;
        this.support = support;
        this.urlResolver = urlResolver;
    }

    // ---- 上传 ----

    /** 上传文件并落库，返回带可访问地址的 DTO */
    @Transactional(rollbackFor = Exception.class)
    public FileDTO upload(UploadCommand cmd) {
        FileTypeInfo type;
        try (InputStream in = toStream(cmd.getSource())) {
            type = fileTypeGuard.detectAndCheck(in, cmd.getOriginalFilename());
        } catch (IOException e) {
            throw new BizException(FILE_READ_FAILED, "读取上传文件失败", e);
        }

        UploadContext ctx = support.resolve(cmd.getConfigId(), cmd.getChannelCode(), cmd.getOriginalFilename(),
                cmd.getObjectType(), cmd.getObjectId(), cmd.getModule(), cmd.getMetadata());
        support.applyType(ctx, type);

        FileInfo info = uploadWithThumbnail(ctx, cmd);
        SysFile record = fileService.save(support.persist(ctx, info));
        urlResolver.fillLocalUrls(record);
        return toDto(record);
    }

    /** 上传文件并按需生成缩略图 */
    private FileInfo uploadWithThumbnail(UploadContext ctx, UploadCommand cmd) {
        FileStorageService storage = fileGateway.nativeService(ctx.getConfigId(), ctx.getChannelCode());
        boolean supportAcl = storage.isSupportAcl(ctx.getChannelCode());

        UploadPretreatment pre = storage
                .of(normalize(cmd.getSource()), cmd.getOriginalFilename(), ctx.getMime())
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
            pre.thumbnailOf(normalize(cmd.getThumbnailSource()));
        } else if (cmd.getThumbWidth() != null && cmd.getThumbHeight() != null) {
            pre.thumbnail(cmd.getThumbWidth(), cmd.getThumbHeight());
        }
        return pre.upload();
    }

    // ---- 分片上传 ----

    /** 查询平台是否支持手动分片上传 */
    public MultipartUploadSupportInfo multipartSupport(String configId, String channelCode) {
        UploadContext ctx = support.resolve(configId, channelCode, null, null, null, null, null);
        return fileGateway.nativeService(ctx.getConfigId(), ctx.getChannelCode())
                .isSupportMultipartUpload(ctx.getChannelCode());
    }

    /** 初始化会话：建 FileInfo → 平台 initiate → 落一条 INITIATED 记录 */
    @Transactional(rollbackFor = Exception.class)
    public MultipartInitResult initiate(MultipartInitCommand cmd) {
        UploadContext ctx = support.resolve(cmd.getConfigId(), cmd.getChannelCode(), cmd.getOriginalFilename(),
                cmd.getObjectType(), cmd.getObjectId(), cmd.getModule(), cmd.getMetadata());

        FileStorageService storage = fileGateway.nativeService(ctx.getConfigId(), ctx.getChannelCode());
        MultipartUploadSupportInfo capability = storage.isSupportMultipartUpload(ctx.getChannelCode());
        if (capability == null || !Boolean.TRUE.equals(capability.getIsSupport())) {
            throw new BizException(FILE_MULTIPART_NOT_SUPPORTED, "存储平台不支持分片上传");
        }

        FileTypeInfo naive = fileTypeGuard.detectByName(cmd.getOriginalFilename());
        support.applyType(ctx, naive);

        boolean supportAcl = storage.isSupportAcl(ctx.getChannelCode());
        InitiateMultipartUploadPretreatment pre = storage.initiateMultipartUpload()
                .setPlatform(ctx.getChannelCode())
                .setOriginalFilename(cmd.getOriginalFilename())
                .setSize(cmd.getSize())
                .setContentType(ctx.getMime())
                .setPath(ctx.getPath())
                .setFileAcl(supportAcl, ctx.getAcl())
                .setNotSupportMetadataThrowException(false)
                .setNotSupportAclThrowException(false);
        if (!ctx.getMetadata().isEmpty()) {
            pre.putMetadataAll(ctx.getMetadata());
        }
        FileInfo info = pre.init();

        SysFile record = support.persist(ctx, info);
        record.setMultipartUploadId(info.getUploadId());
        record.setMultipartState(STATE_INITIATED);
        record = fileService.save(record);

        MultipartInitResult result = new MultipartInitResult();
        result.setFileId(record.getId());
        result.setUploadId(info.getUploadId());
        return result;
    }

    /** 上传一片：首片门禁 → 断点续传跳过 → 上传 → 记分片明细 */
    @Transactional(rollbackFor = Exception.class)
    public SysFilePart uploadPart(MultipartPartCommand cmd) {
        SysFile record = requireActive(cmd.getFileId());
        if (cmd.getPartNumber() == null || cmd.getPartNumber() < 1 || cmd.getPartNumber() > 10000) {
            throw new BizException(FILE_READ_FAILED, "分片序号错误");
        }

        if (cmd.getPartNumber() == 1) {
            checkFirstPart(record, cmd.getSource());
        }

        List<SysFilePart> existing = partService.findPart(record.getMultipartUploadId(), cmd.getPartNumber());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        FileInfo info = support.rebuild(record);
        FileStorageService storage = fileGateway.nativeService(record.getConfigId(), record.getChannelCode());
        FilePartInfo partInfo = storage.uploadPart(info, cmd.getPartNumber(), normalize(cmd.getSource())).upload();

        SysFilePart part = new SysFilePart();
        part.setMultipartUploadId(record.getMultipartUploadId());
        part.setPartNumber(cmd.getPartNumber());
        part.setETag(partInfo.getETag());
        part.setPartSize(partInfo.getPartSize());
        part = partService.save(part);

        if (STATE_INITIATED.equals(record.getMultipartState())) {
            record.setMultipartState(STATE_UPLOADING);
            fileService.save(record);
        }
        return part;
    }

    /** 完成：按序组装分片 → 合并 → 回填记录 → 物理删除分片明细 */
    @Transactional(rollbackFor = Exception.class)
    public SysFile complete(Long fileId) {
        SysFile record = requireActive(fileId);
        FileInfo info = support.rebuild(record);
        List<SysFilePart> parts = partService.listByUploadId(record.getMultipartUploadId());
        if (parts.isEmpty()) {
            throw new BizException(FILE_READ_FAILED, "没有已上传的分片");
        }
        List<FilePartInfo> partInfos = parts.stream().map(p -> {
            FilePartInfo fp = new FilePartInfo();
            fp.setPartNumber(p.getPartNumber());
            fp.setETag(p.getETag());
            fp.setPartSize(p.getPartSize());
            return fp;
        }).collect(Collectors.toList());

        FileStorageService storage = fileGateway.nativeService(record.getConfigId(), record.getChannelCode());
        FileInfo merged = storage.completeMultipartUpload(info).setPartInfoList(partInfos).complete();

        support.fillStorageFields(record, merged);
        record.setMultipartState(STATE_COMPLETED);
        record = fileService.save(record);
        urlResolver.fillLocalUrls(record);
        partService.clearByUploadId(record.getMultipartUploadId());
        return record;
    }

    /** 取消：平台 abort → 标记 ABORTED → 清理分片明细 */
    @Transactional(rollbackFor = Exception.class)
    public void abort(Long fileId) {
        SysFile record = requireActive(fileId);
        FileInfo info = support.rebuild(record);
        FileStorageService storage = fileGateway.nativeService(record.getConfigId(), record.getChannelCode());
        storage.abortMultipartUpload(info).abort();
        record.setMultipartState(STATE_ABORTED);
        fileService.save(record);
        partService.clearByUploadId(record.getMultipartUploadId());
    }

    /** 已上传分片列表 */
    public List<SysFilePart> listParts(Long fileId) {
        SysFile record = fileService.get(fileId);
        if (record == null) {
            throw new BizException(FILE_READ_FAILED, "文件记录不存在");
        }
        return partService.listByUploadId(record.getMultipartUploadId());
    }

    // ---- 地址与查询 ----

    /** 按 id 查询成功文件并返回 DTO */
    public FileDTO get(Long fileId) {
        SysFile file = fileService.get(fileId);
        if (file == null || !isSuccessful(file)) {
            throw new BizException(GlobalErrorCode.NOT_FOUND, "文件记录不存在：{}", fileId);
        }
        return toDto(file);
    }

    /** 按业务对象查询成功文件（按 id 升序）并返回 DTO */
    public List<FileDTO> listByObject(String objectType, String objectId) {
        return fileService.listByObject(objectType, objectId).stream()
                .filter(FileService::isSuccessful)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /** 实体转 DTO，附带可访问地址与原始地址 */
    public FileDTO toDto(SysFile f) {
        FileDTO dto = new FileDTO();
        dto.setId(f.getId());
        dto.setFilename(f.getFilename());
        dto.setOriginalFilename(f.getOriginalFilename());
        dto.setSize(f.getSize());
        dto.setContentType(f.getContentType());
        dto.setFileType(f.getFileType());
        dto.setRawUrl(f.getRawUrl());
        dto.setRawThumbUrl(f.getThRawUrl());
        dto.setUrl(urlResolver.url(f, false));
        dto.setThumbUrl(urlResolver.url(f, true));
        dto.setMd5(f.getHashMd5());
        return dto;
    }

    /** 成功文件：非分片（state 为空）或分片已完成 */
    private static boolean isSuccessful(SysFile f) {
        String state = f.getMultipartState();
        return state == null || STATE_COMPLETED.equals(state);
    }

    // ---- 内部工具 ----

    /** 取处于 INITIATED/UPLOADING 的会话记录 */
    private SysFile requireActive(Long fileId) {
        SysFile record = fileService.get(fileId);
        if (record == null) {
            throw new BizException(FILE_READ_FAILED, "文件记录不存在");
        }
        if (!STATE_INITIATED.equals(record.getMultipartState())
                && !STATE_UPLOADING.equals(record.getMultipartState())) {
            throw new BizException(FILE_READ_FAILED, "分片会话已结束");
        }
        return record;
    }

    /** 首片门禁：检测真实类型并回填记录 */
    private void checkFirstPart(SysFile record, Object source) {
        try (InputStream in = toStream(source)) {
            FileTypeInfo type = fileTypeGuard.detectAndCheck(in, record.getOriginalFilename());
            record.setContentType(type.getMime());
            record.setFileType(type.getFileType().name());
            fileService.save(record);
        } catch (IOException e) {
            throw new BizException(FILE_READ_FAILED, "读取首片失败", e);
        }
    }

    private static InputStream toStream(Object source) throws IOException {
        if (source instanceof MultipartFile) {
            return ((MultipartFile) source).getInputStream();
        }
        if (source instanceof byte[]) {
            return new ByteArrayInputStream((byte[]) source);
        }
        throw new BizException(FILE_TYPE_NOT_ALLOWED, "不支持的上传来源");
    }

    /** 归一化上传来源为 InputStream，读取失败转业务异常 */
    private static InputStream normalize(Object source) {
        try {
            return toStream(source);
        } catch (IOException e) {
            throw new BizException(FILE_READ_FAILED, "读取上传文件失败", e);
        }
    }
}
