package com.sfi.matrix.support.service.file;

import com.sfi.conflux.common.util.SignUtil;
import com.sfi.conflux.file.config.FileChannelConfig;
import com.sfi.conflux.file.config.FileConfigProvider;
import com.sfi.conflux.file.engine.FileGateway;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.matrix.support.service.SysFileService;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.util.StringUtils;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

import static com.sfi.matrix.support.exception.FileErrorCode.FILE_ACCESS_SIGN_INVALID;

/**
 * 文件访问地址解析：公开文件直出原始地址；私有文件按平台能力分流——
 * 对象存储走平台原生预签名，本地（local/local-plus）走自建 {@code /self/} 限时签名地址。
 *
 * <p>{@code /self/} 是本地文件接口的固定标志，与对象存储直链区分；路径写死不配置化。
 * 两种模式统一 {@code fileId} 参数与签名串 {@code fileId|t|thumb}，仅动作段不同：
 * nginx 模式 {@code /self/file}，stream 模式 {@code /self/access}。</p>
 *
 * @author lianghang
 */
@Service
public class FileUrlResolver {

    private static final String MODE_NGINX = "nginx";

    /** 本地访问路径固定前缀，带 self 标志，一眼识别非 oss 直链 */
    private static final String SELF_PREFIX = "/self/";
    private static final String ACTION_NGINX = "file";
    private static final String ACTION_STREAM = "access";

    /** 默认签名密钥：风险低（仅本地文件临时访问），可经配置覆盖 */
    private static final String DEFAULT_SECRET = "matrix-self-file-2026-default-secret";

    /** 有效期复用对象存储预签名配置，语义统一为"临时访问 URL 窗口" */
    @Value("${matrix.file.presign-seconds:1800}")
    private int presignSeconds = 1800;

    /** 本地访问模式：nginx（默认）/ stream */
    @Value("${matrix.file.local-access.mode:nginx}")
    private String mode = MODE_NGINX;

    /** 本地签名密钥，缺省用内置默认值 */
    @Value("${matrix.file.local-access.secret:}")
    private String secret;

    private final FileGateway fileGateway;
    private final FileConfigProvider configProvider;
    private final SysFileService fileService;

    public FileUrlResolver(FileGateway fileGateway, FileConfigProvider configProvider, SysFileService fileService) {
        this.fileGateway = fileGateway;
        this.configProvider = configProvider;
        this.fileService = fileService;
    }

    /**
     * 取访问地址：公开直出原始地址，私有按平台能力生成限时地址。
     *
     * @param fileId    文件记录 id
     * @param thumbnail true 取缩略图，false 取原图
     * @return 可访问 URL
     */
    public String url(Long fileId, boolean thumbnail) {
        SysFile file = fileService.get(fileId);
        if (file == null) {
            throw new BizException(com.sfi.scaffold.common.exception.GlobalErrorCode.NOT_FOUND,
                    "文件记录不存在：{}", fileId);
        }
        return url(file, thumbnail);
    }

    /** 基于文件记录解析地址，供上传/查询组装 DTO 时复用，避免重复查库 */
    public String url(SysFile file, boolean thumbnail) {
        if (!Boolean.TRUE.equals(file.getPrivateAccess())) {
            return thumbnail ? file.getThRawUrl() : file.getRawUrl();
        }
        if (isLocal(file.getChannelCode())) {
            return localUrl(file, thumbnail);
        }
        FileInfo info = toFileInfo(file, thumbnail);
        Date expiration = new Date(System.currentTimeMillis() + presignSeconds * 1000L);
        String signed = fileGateway.generatePresignedUrl(file.getConfigId(), file.getChannelCode(), info, expiration);
        if (signed == null) {
            throw new BizException(com.sfi.scaffold.common.exception.GlobalErrorCode.SYSTEM_ERROR,
                    "签名 URL 生成失败（平台可能不支持预签名）");
        }
        return signed;
    }

    /**
     * 生成本地文件限时签名地址。
     *
     * <p>nginx 模式部署要求（{@code /self/file}）：请求先经 Java 验签，验签通过后由 Java 下发
     * 响应头 {@code X-Accel-Redirect: /__self_internal__/{path}{filename}} 交 nginx 读盘；
     * nginx 需配置内部 location（外部直连返回 404），alias 指向平台 storagePath：</p>
     * <pre>{@code
     * location /self/file { proxy_pass http://app-server; }        # 先到 Java 验签
     * location /__self_internal__/ {
     *     internal;                                                 # 仅允许内部跳转
     *     alias /data/file-storage/;                                # = storagePath
     * }
     * }</pre>
     * <p>stream 模式（{@code /self/access}）无 nginx，由 Java 读流写回响应。</p>
     *
     * @param fileId    文件记录 id
     * @param thumbnail true 取缩略图
     * @return 本地签名 URL
     */
    public String localUrl(SysFile file, boolean thumbnail) {
        long t = System.currentTimeMillis() / 1000L + presignSeconds;
        Long fileId = file.getId();
        String sign = sign(fileId, t, thumbnail);
        String path = SELF_PREFIX + (MODE_NGINX.equalsIgnoreCase(mode) ? ACTION_NGINX : ACTION_STREAM);
        FileChannelConfig cfg = configProvider.getConfig(file.getConfigId(), file.getChannelCode());
        String domain = cfg.getAttr().get("domain");
        return concat(domain, path)
                + "?fileId=" + fileId
                + "&t=" + t
                + "&thumb=" + (thumbnail ? 1 : 0)
                + "&s=" + sign;
    }

    /**
     * 生成本地签名串：HMAC-SHA256(fileId|t|thumb)，hex 小写。
     *
     * <p>密钥来自配置，缺省用 {@link #DEFAULT_SECRET}；{@code t} 为秒级到期时间戳，
     * 参与签名以防篡改续期。nginx 模式下本签名由 {@code /self/file} 请求携带，
     * Java 验签通过才下发 X-Accel-Redirect，部署配置见 {@link #localUrl}。</p>
     */
    public String sign(Long fileId, long t, boolean thumbnail) {
        return SignUtil.hmacSha256Hex(fileId + "|" + t + "|" + (thumbnail ? 1 : 0), effectiveSecret());
    }

    /**
     * 校验本地签名：先判是否过期，再常量时间比较签名串。
     *
     * <p>供两种模式的验签入口共用：nginx 模式（{@code /self/file}）验签通过后设置
     * {@code X-Accel-Redirect} 交 nginx 读盘，nginx 侧 location 需标 {@code internal}
     * 且 alias 指向平台 storagePath（配置示例见 {@link #localUrl}）；stream 模式
     * （{@code /self/access}）验签通过后由 Java 读流写回。</p>
     *
     * @return 签名有效且未过期返回 true，否则 false
     */
    public boolean verify(Long fileId, long t, boolean thumbnail, String sign) {
        if (t < System.currentTimeMillis() / 1000L) {
            return false;
        }
        if (StringUtils.isBlank(sign)) {
            return false;
        }
        return SignUtil.constantTimeEquals(sign(fileId, t, thumbnail), sign);
    }

    private String effectiveSecret() {
        return StringUtils.isBlank(secret) ? DEFAULT_SECRET : secret;
    }

    private static boolean isLocal(String channelCode) {
        return "local".equalsIgnoreCase(channelCode) || "local-plus".equalsIgnoreCase(channelCode);
    }

    private static FileInfo toFileInfo(SysFile file, boolean thumbnail) {
        FileInfo info = new FileInfo();
        info.setPlatform(file.getChannelCode());
        info.setPath(thumbnail ? file.getThPath() : file.getPath());
        info.setFilename(thumbnail ? file.getThFilename() : file.getFilename());
        return info;
    }

    /** domain 可能带尾斜杠，统一拼出 /self/... */
    private static String concat(String domain, String path) {
        if (StringUtils.isBlank(domain)) {
            return path;
        }
        return domain.endsWith("/") ? domain.substring(0, domain.length() - 1) + path : domain + path;
    }
}
