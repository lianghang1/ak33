package com.sfi.matrix.support.service;

import com.sfi.conflux.file.engine.FileGateway;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 文件访问服务：把 sys_file 记录翻译成可访问地址。
 *
 * <p>公开文件（{@code private_access=false}）直接返回入库时记下的未签名原始地址
 * （raw_url / th_raw_url）；私有文件才走 conflux 门面按对象键（path + filename）
 * 生成限时签名 URL。原图与缩略图是独立对象，各自用自己的坐标签名。</p>
 *
 * <p>签名用的 basePath/密钥来自平台配置（FileConfigProvider），不落库；这里只负责
 * 把记录里的 path + filename 喂给官方 API，不自己拼对象键、不碰签名算法。</p>
 *
 * @author matrix
 */
@Service
public class FileAccessService {

    /** 签名 URL 默认有效期（秒），可用 matrix.file.presign-seconds 覆盖 */
    @Value("${matrix.file.presign-seconds:1800}")
    private int presignSeconds = 1800;

    private final FileGateway fileGateway;
    private final SysFileService fileService;

    public FileAccessService(FileGateway fileGateway, SysFileService fileService) {
        this.fileGateway = fileGateway;
        this.fileService = fileService;
    }

    /**
     * 取访问地址。
     *
     * @param fileId    文件记录 id
     * @param thumbnail true 取缩略图，false 取原图
     * @return 可访问 URL（公开为原始地址，私有为签名地址）
     */
    public String url(Long fileId, boolean thumbnail) {
        SysFile file = fileService.get(fileId);
        if (file == null) {
            throw BizException.of(GlobalErrorCode.NOT_FOUND, "文件记录不存在：{}", fileId);
        }
        if (!Boolean.TRUE.equals(file.getPrivateAccess())) {
            return thumbnail ? file.getThRawUrl() : file.getRawUrl();
        }
        FileInfo info = toFileInfo(file, thumbnail);
        String signed = fileGateway.generatePresignedUrl(
                file.getConfigId(), file.getChannelCode(), info, expireAt());
        if (signed == null) {
            throw BizException.of(GlobalErrorCode.SYSTEM_ERROR, "签名 URL 生成失败（平台可能不支持预签名）");
        }
        return signed;
    }

    /** 用记录里的对象坐标构造 FileInfo；平台字段带上，供网关选平台 */
    private static FileInfo toFileInfo(SysFile file, boolean thumbnail) {
        String path = thumbnail ? file.getThPath() : file.getPath();
        String filename = thumbnail ? file.getThFilename() : file.getFilename();
        FileInfo info = new FileInfo();
        info.setPlatform(file.getChannelCode());
        info.setPath(path);
        info.setFilename(filename);
        return info;
    }

    private Date expireAt() {
        return new Date(System.currentTimeMillis() + presignSeconds * 1000L);
    }
}
