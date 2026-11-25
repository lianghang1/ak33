package com.sfi.matrix.support.service.file;

import com.sfi.matrix.support.dto.FileConfigInfo;
import com.sfi.matrix.support.dto.FileTypeInfo;
import com.sfi.matrix.support.enums.FileType;
import com.sfi.matrix.support.exception.FileErrorCode;
import com.sfi.scaffold.common.exception.BizException;
import org.apache.tika.Tika;
import org.dromara.x.file.storage.core.tika.DefaultTikaFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.sfi.matrix.support.exception.FileErrorCode.FILE_READ_FAILED;
import static com.sfi.matrix.support.exception.FileErrorCode.FILE_TYPE_NOT_ALLOWED;

/**
 * 文件类型统一门禁：所有上传入口（multipart / base64 / 分片初始化）在写入前过这一关。
 *
 * <p>读取（{@link #detect}）与校验（{@link #check}）分离：读取只做 Tika 检测、不依赖配置，
 * 无论白名单是否配置都得拿到真实类型；校验依赖 config 表 group=matrix.file / key=config
 * （平台租户），白名单是安全策略不允许租户覆盖，配置缺失或任何不匹配一律拒绝（fail-closed）。</p>
 *
 * <p>检测结果封装为 {@link FileTypeInfo}，门禁、MIME、归类、落库共用这一份，避免多处检测不一致。</p>
 *
 * @author lianghang
 */
@Component
public class FileTypeGuard {

    /** MIME 检测只读文件头：64KB 兼顾容错与开销（比 8KB 每次多约 0.03ms，可忽略），
     *  避免整文件读入内存导致大文件 OOM */
    private static final int HEADER_BYTES = 65536;

    /** 复用 x-file-storage 传递进来的 Tika，懒加载单例、线程安全 */
    private static final Tika TIKA = new DefaultTikaFactory().getTika();

    private final FileConfigProvider fileConfigProvider;

    public FileTypeGuard(FileConfigProvider fileConfigProvider) {
        this.fileConfigProvider = fileConfigProvider;
    }

    /**
     * 读取：检测真实 MIME 并归类，不依赖配置——无论白名单是否配置都先读出类型。
     * 只读文件头前 {@link #HEADER_BYTES} 字节，不把整文件读入内存。流由调用方关闭，本方法不重置流位置。
     */
    public FileTypeInfo detect(InputStream in, String filename) {
        if (in == null) {
            throw new BizException(FILE_TYPE_NOT_ALLOWED, "文件内容为空");
        }
        try {
            String mime = detectMime(readHeader(in));
            return new FileTypeInfo(mime, extension(filename), FileType.fromMime(mime));
        } catch (IOException e) {
            throw new BizException(FILE_READ_FAILED, "读取文件头失败", e);
        }
    }

    /**
     * 门禁：依赖配置，用检测结果里的真实 MIME + 后缀查白名单，不通过直接抛异常。
     */
    public void check(FileTypeInfo info) {
        Set<String> exts = loadAllowed().get(info.getMime());
        String ext = info.getExtension();
        if (ext == null || exts == null || !exts.contains(ext)) {
            throw new BizException(FILE_TYPE_NOT_ALLOWED,
                    "文件类型不允许: mime={}, ext={}", info.getMime(), ext);
        }
    }

    /** 读 + 校验一次完成，返回检测结果供 MIME/归类/落库复用 */
    public FileTypeInfo detectAndCheck(InputStream in, String filename) {
        FileTypeInfo info = detect(in, filename);
        check(info);
        return info;
    }

    /** 读文件头：最多 64KB，不足则实际长度 */
    private static byte[] readHeader(InputStream in) throws IOException {
        byte[] buf = new byte[HEADER_BYTES];
        int total = 0;
        while (total < HEADER_BYTES) {
            int n = in.read(buf, total, HEADER_BYTES - total);
            if (n < 0) {
                break;
            }
            total += n;
        }
        return total == HEADER_BYTES ? buf : Arrays.copyOf(buf, total);
    }

    /** 读平台租户的白名单配置；缺失或为空按 fail-closed 拒绝 */
    private Map<String, Set<String>> loadAllowed() {
        FileConfigInfo config = fileConfigProvider.getConfig();
        if (config == null || config.getAllowed() == null || config.getAllowed().isEmpty()) {
            throw new BizException(FileErrorCode.FILE_TYPE_CONFIG_MISSING);
        }
        return config.allowedMap();
    }

    /** Tika 纯文件头检测，统一小写 */
    private static String detectMime(byte[] bytes) {
        String mime = TIKA.detect(bytes);
        return mime == null ? "" : mime.toLowerCase(Locale.ROOT);
    }

    /** 取文件名最后一个点后的后缀，统一小写；无后缀返回 null */
    private static String extension(String filename) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
