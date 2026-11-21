package com.sfi.ak33.platform.core.strategy;

/**
 * 编号策略产出：编号 + 基础路径 + 全路径（方案A 可读层级路径）。
 *
 * @author ak33
 */
public class CodeResult {

    /** 编号：如 A-01-01，租户内唯一 */
    private final String code;

    /** 基础路径：如 玫瑰园/A区/1排 */
    private final String basePath;

    /** 全路径：如 玫瑰园/A区/1排/01号 */
    private final String fullPath;

    public CodeResult(String code, String basePath, String fullPath) {
        this.code = code;
        this.basePath = basePath;
        this.fullPath = fullPath;
    }

    public String getCode() {
        return code;
    }

    public String getBasePath() {
        return basePath;
    }

    public String getFullPath() {
        return fullPath;
    }
}
