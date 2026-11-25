package com.sfi.matrix.cemetery.strategy;

/**
 * 编号策略产出：编号 + 基础路径 + 全路径，落库为快照。
 *
 * @author lianghang
 */
public class CodeResult {

    private final String code;
    private final String basePath;
    private final String fullPath;

    public CodeResult(String code, String basePath, String fullPath) {
        this.code = code;
        this.basePath = basePath;
        this.fullPath = fullPath;
    }

    public String getCode() { return code; }
    public String getBasePath() { return basePath; }
    public String getFullPath() { return fullPath; }
}
