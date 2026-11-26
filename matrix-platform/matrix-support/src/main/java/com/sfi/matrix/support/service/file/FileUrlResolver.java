package com.sfi.matrix.support.service.file;

import com.sfi.conflux.common.util.SignUtil;
import com.sfi.conflux.file.config.FileChannelConfig;
import com.sfi.conflux.file.config.FileConfigProvider;
import com.sfi.conflux.file.engine.FileGateway;
import com.sfi.matrix.support.dto.FileConfigInfo;
import com.sfi.matrix.support.entity.SysFile;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.util.StringUtils;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.platform.LocalFileStorage;
import org.dromara.x.file.storage.core.platform.LocalPlusFileStorage;
import org.springframework.stereotype.Service;

import java.util.Date;

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

    private static final String NGINX_INTERNAL_PREFIX = "/__self_internal__/";
    private static final String MODE_NGINX = "nginx";
    private static final String SELF_PREFIX = "/self/";
    private static final String ACTION_NGINX = "file";
    private static final String ACTION_STREAM = "access";
    private static final int DEFAULT_PRESIGN_SECONDS = 1800;

    private final FileGateway fileGateway;
    private final FileConfigProvider configProvider;
    private final FileConfigInfoProvider configInfoProvider;

    public FileUrlResolver(FileGateway fileGateway, FileConfigProvider configProvider,
                           FileConfigInfoProvider configInfoProvider) {
        this.fileGateway = fileGateway;
        this.configProvider = configProvider;
        this.configInfoProvider = configInfoProvider;
    }

    /** 基于记录解析可访问地址 */
    public String url(SysFile file, boolean thumbnail) {
        String raw = thumbnail ? file.getThRawUrl() : file.getRawUrl();
        if (!Boolean.TRUE.equals(file.getPrivateAccess())) {
            return raw;
        }
        if (isLocal(file.getConfigId(), file.getChannelCode())) {
            return signedLocalUrl(file, raw, thumbnail);
        }
        FileInfo info = file.toFileInfo(thumbnail);
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
        if (!isLocal(file.getConfigId(), file.getChannelCode())) {
            return;
        }
        String base = concat(domainOf(file), SELF_PREFIX + action());
        file.setRawUrl(localBase(base, file.getId(), false));
        if (file.getThFilename() != null) {
            file.setThRawUrl(localBase(base, file.getId(), true));
        }
    }

    /** 生成本地签名串：HMAC-SHA256(fileId|t|thumb)，hex 小写 */
    public String sign(Long fileId, long t, boolean thumbnail) {
        return SignUtil.hmacSha256Hex(fileId + "|" + t + "|" + (thumbnail ? 1 : 0), effectiveSecret());
    }

    /**
     * 本地文件访问准入：公开文件直接放行，私有文件校验限时签名。
     *
     * <p>供 /self/file（nginx）与 /self/access（stream）两个端点共用。文件路径一律由 fileId
     * 查库得出，不接受客户端传 path。nginx 配置见 {@link #fillLocalUrls}。</p>
     *
     * @param file 按 fileId 查出的文件记录
     * @return 本地文件且（公开，或签名有效且未过期）返回 true，否则 false
     */
    public boolean checkAccess(SysFile file, long t, boolean thumbnail, String sign) {
        if (file == null || !isLocal(file.getConfigId(), file.getChannelCode())) {
            return false;
        }
        if (!Boolean.TRUE.equals(file.getPrivateAccess())) {
            return true;
        }
        return verify(file.getId(), t, thumbnail, sign);
    }

    /** 取 nginx 内部访问前缀，默认 {@link #NGINX_INTERNAL_PREFIX} */
    public String getNginxInternalPrefix() {
        return StringUtils.defaultIfBlank(fileConfig().getNginxInternalPrefix(), NGINX_INTERNAL_PREFIX);
    }

    /** 拼本地限时签名地址：基础地址 + t/s；有效期与 OSS 预签名共用 presignSeconds */
    private String signedLocalUrl(SysFile file, String raw, boolean thumbnail) {
        long t = System.currentTimeMillis() / 1000L + presignSeconds();
        return raw + "&t=" + t + "&s=" + sign(file.getId(), t, thumbnail);
    }

    /** 拼本地基础地址：{@code {base}?fileId={id}&thumb={0|1}} */
    private static String localBase(String base, Long fileId, boolean thumbnail) {
        return base + "?fileId=" + fileId + "&thumb=" + (thumbnail ? 1 : 0);
    }

    /** 校验本地签名有效性 */
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
        FileConfigInfo cfg = configInfoProvider.getConfig();
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

    private boolean isLocal(String configId, String channelCode) {
        FileStorageService storage = fileGateway.nativeService(configId, channelCode);
        FileStorage fileStorage = storage.getFileStorage(channelCode);
        return fileStorage instanceof LocalPlusFileStorage || fileStorage instanceof LocalFileStorage;
    }

    private static String concat(String domain, String path) {
        if (StringUtils.isBlank(domain)) {
            return path;
        }
        return domain.endsWith("/") ? domain.substring(0, domain.length() - 1) + path : domain + path;
    }
}
