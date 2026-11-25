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

import static com.sfi.scaffold.common.exception.GlobalErrorCode.NOT_FOUND;
import static com.sfi.scaffold.common.exception.GlobalErrorCode.SYSTEM_ERROR;

/**
 * 文件访问地址解析：公开文件直出原始地址；私有文件按平台能力分流——
 * 对象存储走平台原生预签名，本地（local/local-plus）走自建 {@code /self/} 限时签名地址。
 *
 * <p>本地文件入库时即把 rawUrl/thRawUrl 拼成 {@code /self/} 基础地址（含 fileId 与 thumb，
 * 不含动态签名）。公开文件直接返回该地址；私有文件在取用时追加 {@code t}/{@code s} 签名。
 * {@code /self/} 是本地接口的固定标志，与对象存储直链区分，路径写死不配置化。</p>
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
     * 按 id 取可访问地址。
     *
     * @param fileId    文件记录 id
     * @param thumbnail true 取缩略图，false 取原图
     * @return 可访问 URL
     */
    public String url(Long fileId, boolean thumbnail) {
        SysFile file = fileService.get(fileId);
        if (file == null) {
            throw new BizException(NOT_FOUND, "文件记录不存在：{}", fileId);
        }
        return url(file, thumbnail);
    }

    /**
     * 基于记录解析地址，供上传/查询组装 DTO 时复用，避免重复查库。
     *
     * <p>公开文件原样返回 rawUrl/thRawUrl（本地在入库时已拼成 /self/ 基础地址，
     * 对象存储为平台原始直链）；私有本地追加动态签名，私有对象存储走平台预签名。</p>
     */
    public String url(SysFile file, boolean thumbnail) {
        String raw = thumbnail ? file.getThRawUrl() : file.getRawUrl();
        if (!Boolean.TRUE.equals(file.getPrivateAccess())) {
            return raw;
        }
        if (isLocal(file.getChannelCode())) {
            long t = System.currentTimeMillis() / 1000L + presignSeconds;
            return raw + "&t=" + t + "&s=" + sign(file.getId(), t, thumbnail);
        }
        FileInfo info = toFileInfo(file, thumbnail);
        Date expiration = new Date(System.currentTimeMillis() + presignSeconds * 1000L);
        String signed = fileGateway.generatePresignedUrl(file.getConfigId(), file.getChannelCode(), info, expiration);
        if (signed == null) {
            throw new BizException(SYSTEM_ERROR, "签名 URL 生成失败（平台可能不支持预签名）");
        }
        return signed;
    }

    /**
     * 本地文件入库后回填 rawUrl/thRawUrl 为 {@code /self/} 基础地址，须先 save 生成雪花 id 再调用。
     * 非本地文件（对象存储）保留 SDK 返回的原始地址，不改写。
     *
     * <p>nginx 部署配置：本地文件无论公开还是私有，都先到 Java 验签（公开放行、私有校验签名），
     * nginx 模式验签通过后 Java 下发 X-Accel-Redirect 由 nginx 读盘，全程不经 Java 读文件；
     * stream 模式（/self/access）则由 Java 读流写回。alias 必须等于平台 local-plus 的
     * storagePath（物理硬盘根目录），internal 保证外部无法直连绕过验签：</p>
     * <pre>{@code
     * upstream app-server { server 127.0.0.1:8080; }
     *
     * server {
     *     listen 80;
     *     server_name file.example.com;
     *
     *     # /self/file(nginx) 与 /self/access(stream) 都 proxy 到 Java：
     *     #   /self/file   验签后下发 X-Accel-Redirect，由 nginx 读盘返回
     *     #   /self/access 验签后由 Java 读流写回响应
     *     location /self/ {
     *         proxy_pass http://app-server;
     *         proxy_set_header Host $host;
     *         proxy_set_header X-Real-IP $remote_addr;
     *     }
     *
     *     # 内部读盘 location：仅接受 X-Accel-Redirect 跳转，外部直连返回 404
     *     # alias = 平台 storagePath，路径由 Java 按 fileId 查库得出，禁止接受客户端传 path
     *     location /__self_internal__/ {
     *         internal;
     *         alias /data/file-storage/;
     *     }
     * }
     * }</pre>
     */
    public void fillLocalUrls(SysFile file) {
        if (!isLocal(file.getChannelCode())) {
            return;
        }
        String base = concat(domainOf(file), SELF_PREFIX + action());
        file.setRawUrl(base + "?fileId=" + file.getId() + "&thumb=0");
        if (file.getThFilename() != null) {
            file.setThRawUrl(base + "?fileId=" + file.getId() + "&thumb=1");
        }
    }

    /**
     * 生成本地签名串：HMAC-SHA256(fileId|t|thumb)，hex 小写。
     *
     * <p>密钥来自配置，缺省用 {@link #DEFAULT_SECRET}；{@code t} 为秒级到期时间戳，
     * 参与签名以防篡改续期。nginx 模式下该签名由 /self/file 请求携带，Java 验签通过才下发
     * X-Accel-Redirect，部署配置见 {@link #fillLocalUrls}。</p>
     */
    public String sign(Long fileId, long t, boolean thumbnail) {
        return SignUtil.hmacSha256Hex(fileId + "|" + t + "|" + (thumbnail ? 1 : 0), effectiveSecret());
    }

    /**
     * 本地文件访问准入：查库确认是本地文件后，公开文件直接放行，私有文件校验限时签名。
     *
     * <p>供 /self/file（nginx）与 /self/access（stream）两个端点共用。nginx 模式验签通过后
     * 设置 X-Accel-Redirect 交 nginx 读盘（location 需标 internal，alias 指向 storagePath，
     * 配置示例见 {@link #fillLocalUrls}）；stream 模式验签通过后由 Java 读流写回。
     * 目标路径必须由 fileId 查库得出，禁止接受客户端传 path。</p>
     *
     * @return 本地文件且（公开，或签名有效且未过期）返回 true，否则 false
     */
    public boolean checkAccess(Long fileId, long t, boolean thumbnail, String sign) {
        SysFile file = fileService.get(fileId);
        if (file == null || !isLocal(file.getChannelCode())) {
            return false;
        }
        if (!Boolean.TRUE.equals(file.getPrivateAccess())) {
            return true;
        }
        return verify(fileId, t, thumbnail, sign);
    }

    /** 校验签名：先判是否过期，再常量时间比较 */
    private boolean verify(Long fileId, long t, boolean thumbnail, String sign) {
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

    private String action() {
        return MODE_NGINX.equalsIgnoreCase(mode) ? ACTION_NGINX : ACTION_STREAM;
    }

    /** 取平台基础配置里的 domain，不新增独立配置项 */
    private String domainOf(SysFile file) {
        FileChannelConfig cfg = configProvider.getConfig(file.getConfigId(), file.getChannelCode());
        if (cfg == null || cfg.getAttr() == null) {
            return "";
        }
        String domain = cfg.getAttr().get("domain");
        return StringUtils.isBlank(domain) ? "" : domain;
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
