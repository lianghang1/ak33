package com.sfi.matrix.iam.audit.controller;

import com.sfi.matrix.iam.audit.entity.SysLoginLog;
import com.sfi.matrix.iam.audit.entity.SysOperateLog;
import com.sfi.matrix.iam.constant.IamConstants;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.query.QueryUtilsX;
import com.sfi.scaffold.starter.security.permission.RequirePermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 审计日志查询（QueryBean 通路②）。
 *
 * @author matrix
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @RequirePermission(IamConstants.PERM_USER)
    @GetMapping("/login-logs")
    public R<Page<SysLoginLog>> loginLogs(HttpServletRequest request) {
        return R.ok(QueryUtilsX.doQuery(QueryUtilsX.parse(request.getParameterMap()),
                PageRequest.of(page(request), size(request)), SysLoginLog.class));
    }

    @RequirePermission(IamConstants.PERM_USER)
    @GetMapping("/operate-logs")
    public R<Page<SysOperateLog>> operateLogs(HttpServletRequest request) {
        return R.ok(QueryUtilsX.doQuery(QueryUtilsX.parse(request.getParameterMap()),
                PageRequest.of(page(request), size(request)), SysOperateLog.class));
    }

    private static int page(HttpServletRequest request) {
        try {
            String v = request.getParameter("page");
            return v == null || v.isEmpty() ? 0 : Integer.parseInt(v);
        } catch (NumberFormatException e) { return 0; }
    }

    private static int size(HttpServletRequest request) {
        try {
            String v = request.getParameter("size");
            return v == null || v.isEmpty() ? 15 : Math.min(Integer.parseInt(v), 200);
        } catch (NumberFormatException e) { return 15; }
    }
}
