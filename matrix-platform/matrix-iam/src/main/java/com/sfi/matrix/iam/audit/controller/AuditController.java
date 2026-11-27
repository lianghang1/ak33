package com.sfi.matrix.iam.audit.controller;

import com.sfi.matrix.iam.audit.entity.SysLoginLog;
import com.sfi.matrix.iam.audit.entity.SysOperateLog;
import com.sfi.scaffold.common.request.PageParam;
import com.sfi.scaffold.common.result.PageResult;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.query.QueryUtilsX;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 审计日志查询（QueryBean 通路②，统一 1 基 PageResult）。
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @GetMapping("/login-logs")
    public R<PageResult<SysLoginLog>> loginLogs(HttpServletRequest request) {
        return R.ok(QueryUtilsX.queryPage(QueryUtilsX.parse(request.getParameterMap()),
                pageParamOf(request), SysLoginLog.class));
    }

    @GetMapping("/operate-logs")
    public R<PageResult<SysOperateLog>> operateLogs(HttpServletRequest request) {
        return R.ok(QueryUtilsX.queryPage(QueryUtilsX.parse(request.getParameterMap()),
                pageParamOf(request), SysOperateLog.class));
    }

    /** 从 request 解析 1 基分页参数：pageNum/page（默认1）、pageSize/size（默认15）。 */
    private static PageParam pageParamOf(HttpServletRequest request) {
        PageParam p = new PageParam();
        int pageNum = toInt(first(request, "pageNum", "page"), 1);
        int pageSize = toInt(first(request, "pageSize", "size"), 15);
        p.setPageNum(pageNum < 1 ? 1 : pageNum);
        p.setPageSize(pageSize > 0 ? pageSize : 15);
        return p;
    }

    private static String first(HttpServletRequest request, String... keys) {
        for (String key : keys) {
            String v = request.getParameter(key);
            if (v != null && !v.isEmpty()) {
                return v;
            }
        }
        return null;
    }

    private static int toInt(String v, int def) {
        try {
            return v == null || v.isEmpty() ? def : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
