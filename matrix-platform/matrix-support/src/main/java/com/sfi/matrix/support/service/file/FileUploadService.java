package com.sfi.matrix.support.service.file;

import com.sfi.conflux.file.engine.FileGateway;
import com.sfi.matrix.support.dto.FileConfigInfo;
import com.sfi.matrix.support.dto.FileDTO;
import com.sfi.matrix.support.dto.FileTypeInfo;
import com.sfi.matrix.support.dto.UploadCommand;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.matrix.support.enums.FileType;
import com.sfi.matrix.support.service.SysFileService;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.util.CollUtils;
import com.sfi.scaffold.common.util.StringUtils;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.upload.UploadPretreatment;
import org.dromara.x.file.storage.core.hash.HashInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;

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
 * @author lianghang
 */
@Service
public class FileUploadService {

    private final FileGateway fileGateway;
    private final SysFileService fileService;
    private final FileTypeGuard fileTypeGuard;
    private final FileConfigProvider fileConfigProvider;

    public FileUploadService(FileGateway fileGateway, SysFileService fileService,
                             FileTypeGuard fileTypeGuard, FileConfigProvider fileConfigProvider) {
        this.fileGateway = fileGateway;
        this.fileService = fileService;
        this.fileTypeGuard = fileTypeGuard;
        this.fileConfigProvider = fileConfigProvider;
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

        FileConfigInfo fileConfigInfo = fileConfigProvider.getConfig();
        if (StringUtils.isBlank(cmd.getConfigId())) {
            cmd.setConfigId(fileConfigInfo.getDefaultConfigId());
        }

        if (StringUtils.isBlank(cmd.getChannelCode())) {
            cmd.setChannelCode(fileConfigInfo.getDefaultChannelCode());
        }

        if (cmd.getMetadata() == null) {
            cmd.setMetadata(new HashMap<>());
        }

        // 非图片类型默认 attachment，不覆盖调用方显式设置
        if (!FileType.IMAGE.name().equals(type.getFileType().name())) {
            cmd.getMetadata().putIfAbsent(Constant.Metadata.CONTENT_DISPOSITION, "attachment");
        }

        String module = resolveAndValidateModule(cmd.getModule());
        String acl = resolveFileAcl(fileConfigInfo, module);
        FileInfo info = uploadWithThumbnail(cmd, module, acl, type.getMime());

        return persist(cmd, type, info, acl);
    }

    /** 上传 + 缩略图二选一，异常能力固定 fail-soft */
    private FileInfo uploadWithThumbnail(UploadCommand cmd, String module, String acl, String mime) {

        String filePath = module + "/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM")) + "/";

        FileStorageService fileStorageService = fileGateway.nativeService(cmd.getConfigId(), cmd.getChannelCode());
        boolean supportAcl = fileStorageService.isSupportAcl(cmd.getChannelCode());

        UploadPretreatment pre = fileStorageService
                .of(cmd.getSource(), cmd.getOriginalFilename(), mime)
                .setPlatform(cmd.getChannelCode())
                .setAcl(supportAcl, acl)
                .setPath(filePath)
                .setNotSupportMetadataThrowException(false)
                .setNotSupportAclThrowException(false)
                .setIgnoreThumbnailException(true, true);

        if (cmd.getMetadata() != null && !cmd.getMetadata().isEmpty()) {
            pre.putMetadataAll(cmd.getMetadata());
        }
        // 外部缩略图优先
        if (cmd.getThumbnailSource() != null) {
            pre.thumbnailOf(cmd.getThumbnailSource());
        } else if (cmd.getThumbWidth() != null && cmd.getThumbHeight() != null) {
            // 内部生成
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

    /** FileInfo → SysFile 落库，MIME/归类用门禁检测结果，权限用上传时的决策值 */
    private SysFile persist(UploadCommand cmd, FileTypeInfo type, FileInfo info, String acl) {

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
        record.setObjectType(cmd.getObjectType());
        record.setObjectId(cmd.getObjectId());

        record.setAcl(acl);
        record.setPrivateAccess(Constant.ACL.PRIVATE.equals(acl));

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

    /**
     * 解析文件访问权限：模块在允许公开列表内返回 public-read，否则用配置的默认权限
     */
    private String resolveFileAcl(FileConfigInfo fileConfigInfo, String module) {
        if (CollUtils.isNotEmpty(fileConfigInfo.getAllowedModules())
                && fileConfigInfo.getAllowedModules().contains(module)) {
            return Constant.ACL.PUBLIC_READ;
        }
        String defaultAcl = fileConfigInfo.getDefaultAcl();
        return StringUtils.isBlank(defaultAcl) ? DEFAULT_FILE_ACL : defaultAcl;
    }

    /**
     * 解析、清洗并校验上传模块名称
     * @param rawModule 前端传入的原始模块名
     * @return 合法且标准化的模块名（全小写）
     * @throws BizException 校验不通过时抛出业务异常
     */
    private String resolveAndValidateModule(String rawModule) {
        String module = (rawModule != null) ? rawModule.trim().toLowerCase(Locale.ENGLISH) : null;
        if (StringUtils.isEmpty(module)) {
            return DEFAULT_MODULE;
        }

        // 1. 显式黑名单拦截路径遍历
        if (module.contains("..") || module.contains(".") || module.contains("/") || module.contains("\\")) {
            throw new BizException(FILE_READ_FAILED, "上传模块格式错误");
        }

        // 2. 底层白名单校验 (长度 + 纯ASCII安全字符)
        if (!isSafeAndValidModule(module)) {
            throw new BizException(FILE_READ_FAILED, "上传模块格式错误");
        }

        return module;
    }

    /**
     * 利用预转小写省去大写判断，利用 c >= 128 拦截一切非ASCII字符(中文/Emoji等)
     */
    private boolean isSafeAndValidModule(String str) {
        if (str.length() > MAX_MODULE_LENGTH) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= 128) {
                return false;
            }
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-')) {
                return false;
            }
        }
        return true;
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

    /** 默认模块常量 */
    private static final String DEFAULT_MODULE = "default";
    /** 模块名称最大允许长度，防止超长字符串攻击 */
    private static final int MAX_MODULE_LENGTH = 32;
    /** 默认文件访问权限 */
    private static final String DEFAULT_FILE_ACL = Constant.ACL.PRIVATE;
}
