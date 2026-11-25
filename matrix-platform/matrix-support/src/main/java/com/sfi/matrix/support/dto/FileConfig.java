package com.sfi.matrix.support.dto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 文件系统模块配置：对应 config 表 group=matrix.file / key=config 的 JSON。
 *
 * <p>目前只有 {@link #allowed}（MIME→后缀白名单），后续文件系统配置（缩略图尺寸、
 * 分片大小等）作为同级字段往本对象加即可，读取入口不变。</p>
 *
 * @author matrix
 */
public class FileConfig {

    /** MIME 白名单：一个 MIME 允许一组后缀 */
    private List<MimeRule> allowed;

    public List<MimeRule> getAllowed() {
        return allowed;
    }

    public void setAllowed(List<MimeRule> allowed) {
        this.allowed = allowed;
    }

    /**
     * 归一倍对表：MIME 与后缀统一小写，供门禁直接 contains 判断。
     * 返回新 Map，调用方可安全缓存。
     */
    public Map<String, Set<String>> allowedMap() {
        Map<String, Set<String>> map = new HashMap<>();
        if (allowed == null) {
            return map;
        }
        for (MimeRule rule : allowed) {
            if (rule == null || rule.getMime() == null) {
                continue;
            }
            String mime = rule.getMime().trim().toLowerCase(Locale.ROOT);
            Set<String> exts = new HashSet<>();
            if (rule.getExts() != null) {
                for (String ext : rule.getExts()) {
                    if (ext != null && !ext.trim().isEmpty()) {
                        exts.add(ext.trim().toLowerCase(Locale.ROOT));
                    }
                }
            }
            map.put(mime, exts);
        }
        return map;
    }

    /** 一条白名单规则：{@code image/jpeg -> [jpg, jpeg, JPG, png]} */
    public static class MimeRule {
        private String mime;
        private List<String> exts;

        public String getMime() {
            return mime;
        }

        public void setMime(String mime) {
            this.mime = mime;
        }

        public List<String> getExts() {
            return exts;
        }

        public void setExts(List<String> exts) {
            this.exts = exts;
        }
    }
}
