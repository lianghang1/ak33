package com.sfi.matrix.support.service.file;

import com.sfi.conflux.common.util.SignUtil;
import com.sfi.conflux.file.config.FileChannelConfig;
import com.sfi.conflux.file.config.FileConfigProvider;
import com.sfi.conflux.file.engine.FileGateway;
import com.sfi.matrix.support.dto.FileConfigInfo;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.matrix.support.service.SysFileService;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.util.StringUtils;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.stereotype.Service;

import java.util.Date;

import static com.sfi.scaffold.common.exception.GlobalErrorCode.NOT_FOUND;
import static com.sfi.scaffold.common.exception.GlobalErrorCode.SYSTEM_ERROR;

/**
 * 文件访问地址解析：公开文件直出原始地址；私有文件按平台能力分流——
 * 对象存储走平台原生预签名，本地（local/local-plus）走自建 {@code /self/} 限时签名地址。
 *
 * <p>本地文件入库时 rawUrl/thRawUrl 即拼成 {@code /self/} 基础地址（含 fileId 与 thumb，
 * 不含动态签名，见 {@link #fillLocalUrls} 的 nginx 注意事项）。</p>
 *
 * @author lianghang
 */
@Service
public class FileUrlResolver {

    private static final String MODE_NGINX = "nginx";
    private static final String SELF_PREFIX = "/self/";
    private static final String ACTION_NGINX = "file";
    private static final String ACTION_STREAM = "access";
    private static final int DEFAULT_PRESIGN_SECONDS = 1800;

    private final FileGateway fileGateway;
    private final FileConfigProvider configProvider;
    private final com.sfi.matrix.support.service.file.FileConfigProvider matrixConfigProvider;
    private final SysFileService fileService;

    public FileUrlResolver(FileGateway fileGateway, FileConfigProvider configProvider,
                           com.sfi.matrix.support.service.file.FileConfigProvider matrixConfigProvider,
                           SysFileService fileService) {
        this.fileGateway = fileGateway;
        this.configProvider = configProvider;
        this.matrixConfigProvider = matrixConfigProvider;
        this.fileService = fileService;
    }

    /** 基于记录解析可访问地址 */
    public String url(SysFile file, boolean thumbnail) {
        String raw = thumbnail ? file.getThRawUrl() : file.getRawUrl();
        if (!Boolean.TRUE.equals(file.getPrivateAccess())) {
            return raw;
        }
        if (isLocal(file.getChannelCode())) {
            long t = System.currentTimeMillis() / 1000L + presignSeconds();
            return raw + "&t=" + t + "&s=" + sign(file.getId(), t, thumbnail);
        }
        FileInfo info = toFileInfo(file, thumbnail);
        Date expiration = new Date(System.currentTimeMillis() + presignSeconds() * 1000L);
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
     * <p><b>nginx 部署注意事项</b>：本地文件无论公开还是私有，都先到 Java 验签（公开放行、私有校验签名）。
     * nginx 模式验签通过后 Java 下发 X-Accel-Redirect 由 nginx 读盘，全程不经 Java 读文件；
     * stream 模式（/self/access）由 Java 读流写回。alias 必须等于平台 local-plus 的 storagePath
     * （物理硬盘根目录），internal 保证外部无法直连绕过验签：</p>
     * <pre>{@code
     * upstream app-server { server 127.0.0.1:8080; }
     *
     * server {
     *     listen 80;
     *     server_name file.example.com;
     *
     *     location /self/ {
     *         proxy_pass http://app-server;
     *         proxy_set_header Host $host;
     *         proxy_set_header X-Real-IP $remote_addr;
     *     }
     *
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

    /** 生成本地签名串：HMAC-SHA256(fileId|t|thumb)，hex 小写 */
    public String sign(Long fileId, long t, boolean thumbnail) {
        return SignUtil.hmacSha256Hex(fileId + "|" + t + "|" + (thumbnail ? 1 : 0), effectiveSecret());
    }

    /**
     * 本地文件访问准入：查库确认是本地文件后，公开文件直接放行，私有文件校验限时签名。
     *
     * <p>供 /self/file（nginx）与 /self/access（stream）两个端点共用。目标路径必须由 fileId
     * 查库得出，不接受客户端传 path。nginx 配置见 {@link #fillLocalUrls}。</p>
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

    private FileConfigInfo fileConfig() {
        FileConfigInfo cfg = matrixConfigProvider.getConfig();
        return cfg == null ? new FileConfigInfo() : cfg;
    }

    private int presignSeconds() {
        Integer v = fileConfig().getPresignSeconds();
        return v == null ? DEFAULT_PRESIGN_SECONDS : v;
    }

    private String effectiveSecret() {
        String s = fileConfig().getLocalAccessSecret();
        return StringUtils.isBlank(s) ? new FileConfigInfo().getLocalAccessSecret() : s;
    }

    private String action() {
        String m = fileConfig().getLocalAccessMode();
        return MODE_NGINX.equalsIgnoreCase(m) ? ACTION_NGINX : ACTION_STREAM;
    }

    /** 取平台基础配置里的 domain */
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

    private static String concat(String domain, String path) {
        if (StringUtils.isBlank(domain)) {
            return path;
        }
        return domain.endsWith("/") ? domain.substring(0, domain.length() - 1) + path : domain + path;
    }
}
