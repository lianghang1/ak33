package com.sfi.matrix.support.service.file;

import com.sfi.matrix.support.dto.FileConfigInfo;
import com.sfi.matrix.support.dto.FileTypeInfo;
import com.sfi.matrix.support.dto.UploadContext;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.matrix.support.enums.FileType;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.util.CollUtils;
import com.sfi.scaffold.common.util.StringUtils;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.hash.HashInfo;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.sfi.matrix.support.exception.FileErrorCode.FILE_READ_FAILED;

/**
 * 上传决策与落库的公共能力。
 *
 * @author lianghang
 */
@Component
public class FileUploadSupport {

    private final FileConfigProvider fileConfigProvider;

    public FileUploadSupport(FileConfigProvider fileConfigProvider) {
        this.fileConfigProvider = fileConfigProvider;
    }

    /** 解析上传决策：配置/渠道兜底、module、路径、权限、元数据 */
    public UploadContext resolve(String configId, String channelCode, String originalFilename,
                                 String objectType, String objectId, String module, Map<String, String> metadata) {
        FileConfigInfo config = fileConfigProvider.getConfig();
        UploadContext ctx = new UploadContext();
        ctx.setConfigId(StringUtils.isBlank(configId) ? config.getDefaultConfigId() : configId);
        ctx.setChannelCode(StringUtils.isBlank(channelCode) ? config.getDefaultChannelCode() : channelCode);
        ctx.setOriginalFilename(originalFilename);
        ctx.setObjectType(objectType);
        ctx.setObjectId(objectId);

        String safeModule = resolveAndValidateModule(module);
        ctx.setModule(safeModule);
        ctx.setPath(safeModule + "/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM")) + "/");
        ctx.setAcl(resolveFileAcl(config, safeModule));
        ctx.setMetadata(metadata == null ? new HashMap<>() : metadata);
        return ctx;
    }

    /** 补全上下文的类型信息与默认元数据 */
    public void applyType(UploadContext ctx, FileTypeInfo type) {
        ctx.setMime(type.getMime());
        ctx.setFileType(type.getFileType());
        if (type.getFileType() != FileType.IMAGE) {
            ctx.getMetadata().putIfAbsent(Constant.Metadata.CONTENT_DISPOSITION, "attachment");
        }
    }

    /** 新建文件记录 */
    public SysFile persist(UploadContext ctx, FileInfo info) {
        SysFile record = new SysFile();
        record.setConfigId(ctx.getConfigId());
        record.setChannelCode(ctx.getChannelCode());
        record.setOriginalFilename(ctx.getOriginalFilename());
        record.setContentType(ctx.getMime());
        record.setFileType(ctx.getFileType().name());
        record.setAcl(ctx.getAcl());
        record.setPrivateAccess(Constant.ACL.PRIVATE.equals(ctx.getAcl()));
        record.setObjectType(ctx.getObjectType());
        record.setObjectId(ctx.getObjectId());
        fillStorageFields(record, info);
        return record;
    }

    /** 回填 FileInfo 的存储字段 */
    public void fillStorageFields(SysFile record, FileInfo info) {
        record.setPath(info.getPath());
        record.setFilename(info.getFilename());
        record.setRawUrl(info.getUrl());
        record.setSize(info.getSize());
        record.setExtension(info.getExt());

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
    }

    /** 从记录重建带 uploadId 的 FileInfo */
    public FileInfo rebuild(SysFile file) {
        FileInfo info = new FileInfo();
        info.setPlatform(file.getChannelCode());
        info.setPath(file.getPath());
        info.setFilename(file.getFilename());
        info.setOriginalFilename(file.getOriginalFilename());
        info.setContentType(file.getContentType());
        info.setSize(file.getSize());
        info.setUploadId(file.getMultipartUploadId());
        return info;
    }

    /** 解析文件访问权限 */
    private String resolveFileAcl(FileConfigInfo config, String module) {
        if (CollUtils.isNotEmpty(config.getAllowedModules()) && config.getAllowedModules().contains(module)) {
            return Constant.ACL.PUBLIC_READ;
        }
        String defaultAcl = config.getDefaultAcl();
        return StringUtils.isBlank(defaultAcl) ? Constant.ACL.PRIVATE : defaultAcl;
    }

    /** 解析、清洗并校验上传模块名称 */
    private String resolveAndValidateModule(String rawModule) {
        String module = (rawModule != null) ? rawModule.trim().toLowerCase(Locale.ENGLISH) : null;
        if (StringUtils.isEmpty(module)) {
            return "default";
        }
        if (module.contains("..") || module.contains(".") || module.contains("/") || module.contains("\\")) {
            throw new BizException(FILE_READ_FAILED, "上传模块格式错误");
        }
        if (!isSafeAndValidModule(module)) {
            throw new BizException(FILE_READ_FAILED, "上传模块格式错误");
        }
        return module;
    }

    private boolean isSafeAndValidModule(String str) {
        if (str.length() > 32) {
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
}
