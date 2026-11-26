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
 * 文件类型统一门禁：所有上传入口在写入前检测真实 MIME 并校验后缀白名单。
 *
 * <p>白名单读取 config 表 group=matrix.file / key=config（平台租户），配置缺失或不匹配一律拒绝。
 * 检测结果封装为 {@link FileTypeInfo}，供门禁、MIME、归类、落库共用。</p>
 *
 * @author lianghang
 */
@Component
public class FileTypeGuard {

    /** MIME 检测读取的文件头字节数 */
    private static final int HEADER_BYTES = 65536;

    /** Tika 实例，线程安全 */
    private static final Tika TIKA = new DefaultTikaFactory().getTika();

    private final FileConfigInfoProvider fileConfigProvider;

    public FileTypeGuard(FileConfigInfoProvider fileConfigProvider) {
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

    /**
     * 按文件名粗判类型：无文件内容时用（如分片 initiate 阶段），只归类不校验白名单。
     * 真实门禁延迟到首片上传时用文件头检测。
     */
    public FileTypeInfo detectByName(String filename) {
        String mime = TIKA.detect(filename);
        mime = mime == null ? "" : mime.toLowerCase(Locale.ROOT);
        return new FileTypeInfo(mime, extension(filename), FileType.fromMime(mime));
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
