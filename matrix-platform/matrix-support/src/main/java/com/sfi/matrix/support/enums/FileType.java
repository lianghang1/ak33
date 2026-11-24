package com.sfi.matrix.support.enums;

/**
 * 文件媒体大类。
 *
 * <p>回答"这个文件是什么"，与两个正交维度区分：
 * <ul>
 *   <li>{@code extension/contentType}：原始技术属性，可能被改或缺失</li>
 *   <li>{@code objectType}：业务归属，回答"挂在哪个业务对象上"</li>
 * </ul>
 * 入库时由 {@link FileTypeResolver} 按扩展名或 MIME 归类一次，无法识别归 {@link #OTHER}。
 *
 * @author matrix
 */
public enum FileType {

    /** 图片：jpg/png/gif/webp 等 */
    IMAGE,
    /** 视频：mp4/avi/mov 等 */
    VIDEO,
    /** 音频：mp3/wav/flac 等 */
    AUDIO,
    /** 文档：pdf/doc/xls/ppt/txt 等 */
    DOCUMENT,
    /** 压缩包：zip/rar/7z/tar/gz 等 */
    ARCHIVE,
    /** 其他：无法识别或未归类 */
    OTHER;

    /** 按名称解析，容错大小写与 null，失败返回 OTHER。 */
    public static FileType of(String name) {
        if (name == null || name.trim().isEmpty()) {
            return OTHER;
        }
        for (FileType t : values()) {
            if (t.name().equalsIgnoreCase(name.trim())) {
                return t;
            }
        }
        return OTHER;
    }
}
