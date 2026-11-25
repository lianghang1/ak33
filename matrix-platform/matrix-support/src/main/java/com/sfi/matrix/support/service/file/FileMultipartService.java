package com.sfi.matrix.support.service.file;

import com.sfi.conflux.file.engine.FileGateway;
import com.sfi.matrix.support.dto.FileTypeInfo;
import com.sfi.matrix.support.dto.MultipartInitCommand;
import com.sfi.matrix.support.dto.MultipartInitResult;
import com.sfi.matrix.support.dto.MultipartPartCommand;
import com.sfi.matrix.support.dto.UploadContext;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.matrix.support.entity.SysFilePart;
import com.sfi.matrix.support.service.SysFilePartService;
import com.sfi.matrix.support.service.SysFileService;
import com.sfi.scaffold.common.exception.BizException;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.dromara.x.file.storage.core.upload.InitiateMultipartUploadPretreatment;
import org.dromara.x.file.storage.core.upload.MultipartUploadSupportInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.sfi.matrix.support.exception.FileErrorCode.FILE_MULTIPART_NOT_SUPPORTED;
import static com.sfi.matrix.support.exception.FileErrorCode.FILE_READ_FAILED;
import static com.sfi.matrix.support.exception.FileErrorCode.FILE_TYPE_NOT_ALLOWED;

/**
 * 手动分片上传：初始化、逐片上传、完成、取消。
 *
 * @author lianghang
 */
@Service
public class FileMultipartService {

    private static final String STATE_INITIATED = "INITIATED";
    private static final String STATE_UPLOADING = "UPLOADING";
    private static final String STATE_COMPLETED = "COMPLETED";
    private static final String STATE_ABORTED = "ABORTED";

    private final FileGateway fileGateway;
    private final SysFileService fileService;
    private final SysFilePartService partService;
    private final FileTypeGuard fileTypeGuard;
    private final FileUploadSupport support;

    public FileMultipartService(FileGateway fileGateway, SysFileService fileService,
                                SysFilePartService partService, FileTypeGuard fileTypeGuard,
                                FileUploadSupport support) {
        this.fileGateway = fileGateway;
        this.fileService = fileService;
        this.partService = partService;
        this.fileTypeGuard = fileTypeGuard;
        this.support = support;
    }

    /** 查询平台是否支持手动分片上传 */
    public MultipartUploadSupportInfo support(String configId, String channelCode) {
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

        // 按文件名粗判类型
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

        // 首片用文件头做真实门禁，并把真实类型回填记录
        if (cmd.getPartNumber() == 1) {
            checkFirstPart(record, cmd.getSource());
        }

        List<SysFilePart> existing = partService.findPart(record.getMultipartUploadId(), cmd.getPartNumber());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        FileInfo info = support.rebuild(record);
        FileStorageService storage = fileGateway.nativeService(record.getConfigId(), record.getChannelCode());
        FilePartInfo partInfo = storage.uploadPart(info, cmd.getPartNumber(), cmd.getSource()).upload();

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
        }).toList();

        FileStorageService storage = fileGateway.nativeService(record.getConfigId(), record.getChannelCode());
        FileInfo merged = storage.completeMultipartUpload(info).setPartInfoList(partInfos).complete();

        support.fillStorageFields(record, merged);
        record.setMultipartState(STATE_COMPLETED);
        record = fileService.save(record);
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
}
