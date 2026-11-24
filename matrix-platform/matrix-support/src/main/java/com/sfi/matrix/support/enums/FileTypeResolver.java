package com.sfi.matrix.support.enums;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 文件媒体大类归类器：按扩展名或 MIME 把文件归入 {@link FileType}。
 *
 * <p>归类只认技术属性（扩展名、MIME），不依赖业务归属。纯静态无 Spring 依赖，
 * 归类逻辑集中于此，避免字符串映射散落各处。判定时扩展名优先、MIME 兜底。</p>
 *
 * @author matrix
 */
public final class FileTypeResolver {

    private static final Map<String, FileType> EXTENSION_MAP = buildExtensionMap();

    private FileTypeResolver() {
    }

    /**
     * 归类：优先按扩展名，扩展名未命中再按 contentType 主类型，都不中归 OTHER。
     *
     * @param extension   扩展名（可带点、可大小写混用，可为 null）
     * @param contentType MIME（可带参数如 image/jpeg;charset=utf-8，可为 null）
     */
    public static FileType resolve(String extension, String contentType) {
        FileType byExt = byExtension(extension);
        if (byExt != null) {
            return byExt;
        }
        return byContentType(contentType);
    }

    /** 仅按扩展名归类，未命中返回 null（便于调用方决定是否走 MIME 兜底）。 */
    public static FileType byExtension(String extension) {
        if (extension == null || extension.trim().isEmpty()) {
            return null;
        }
        String ext = normalizeExt(extension);
        return EXTENSION_MAP.get(ext);
    }

    /** 仅按 MIME 主类型归类，未命中返回 OTHER。 */
    public static FileType byContentType(String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) {
            return FileType.OTHER;
        }
        String main = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (main.startsWith("image/")) {
            return FileType.IMAGE;
        }
        if (main.startsWith("video/")) {
            return FileType.VIDEO;
        }
        if (main.startsWith("audio/")) {
            return FileType.AUDIO;
        }
        if ("application/pdf".equals(main)
                || main.contains("msword")
                || main.contains("officedocument")
                || main.contains("ms-excel")
                || main.contains("ms-powerpoint")
                || main.startsWith("text/")) {
            return FileType.DOCUMENT;
        }
        if (main.contains("zip") || main.contains("compressed") || main.contains("rar") || main.contains("gzip")) {
            return FileType.ARCHIVE;
        }
        return FileType.OTHER;
    }

    private static String normalizeExt(String extension) {
        String ext = extension.trim().toLowerCase(Locale.ROOT);
        if (ext.startsWith(".")) {
            ext = ext.substring(1);
        }
        return ext;
    }

    private static Map<String, FileType> buildExtensionMap() {
        Map<String, FileType> map = new HashMap<String, FileType>();
        for (String e : new String[]{"jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "ico", "tif", "tiff"}) {
            map.put(e, FileType.IMAGE);
        }
        for (String e : new String[]{"mp4", "avi", "mov", "mkv", "flv", "wmv", "webm", "m4v"}) {
            map.put(e, FileType.VIDEO);
        }
        for (String e : new String[]{"mp3", "wav", "flac", "aac", "ogg", "m4a", "wma"}) {
            map.put(e, FileType.AUDIO);
        }
        for (String e : new String[]{"pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "rtf"}) {
            map.put(e, FileType.DOCUMENT);
        }
        for (String e : new String[]{"zip", "rar", "7z", "tar", "gz", "bz2", "xz"}) {
            map.put(e, FileType.ARCHIVE);
        }
        return Collections.unmodifiableMap(map);
    }
}
