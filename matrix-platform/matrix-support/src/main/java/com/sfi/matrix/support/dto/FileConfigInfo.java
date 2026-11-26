package com.sfi.matrix.support.dto;

import lombok.Getter;
import lombok.Setter;

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
 * @author lianghang
 */
@Getter
@Setter
public class FileConfigInfo {

    /**
     * 默认配置 ID
     */
    private String defaultConfigId;

    /**
     * 默认渠道码
     * eg: aliyunOss、tencentCos
     */
    private String defaultChannelCode;

    /**
     * 允许公开访问的模块码列表，未列入的模块按默认权限处理
     * eg: ['portal']
     */
    private List<String> allowedModules;

    /** 默认文件访问权限，未配置时为 private */
    private String defaultAcl = "private";

    /** 本地文件访问模式：nginx / stream */
    private String localAccessMode = "nginx";

    /** nginx 内部访问前缀，X-Accel-Redirect 的目标路径前缀 */
    private String nginxInternalPrefix = "/__self_internal__/";

    /** 本地文件签名密钥 */
    private String localAccessSecret = "matrix-self-file-2026-default-secret";

    /** 临时访问 URL 有效期（秒），预签名与本地签名共用 */
    private Integer presignSeconds = 1800;

    /** MIME 白名单：一个 MIME 允许一组后缀 */
    private List<MimeRule> allowed;

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
    @Getter
    @Setter
    public static class MimeRule {
        private String mime;
        private List<String> exts;
    }
}
