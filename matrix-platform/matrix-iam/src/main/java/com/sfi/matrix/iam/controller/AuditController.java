package com.sfi.matrix.iam.controller;

import com.sfi.matrix.iam.entity.SysLoginLog;
import com.sfi.matrix.iam.entity.SysOperateLog;
import com.sfi.scaffold.common.result.PageResult;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.common.util.PageParams;
import com.sfi.scaffold.query.QueryUtilsX;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 审计日志查询（QueryBean 通路②，统一 1 基 PageResult）。
 *
 * <p>审计日志只读、双实体，不接 BaseCrudController；分页参数走公共
 * {@link PageParams}，与全平台保持同一套解析规则。</p>
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @GetMapping("/login-logs")
    public R<PageResult<SysLoginLog>> loginLogs(HttpServletRequest request) {
        return R.ok(QueryUtilsX.queryPage(QueryUtilsX.parse(request.getParameterMap()),
                PageParams.of(request), SysLoginLog.class));
    }

    @GetMapping("/operate-logs")
    public R<PageResult<SysOperateLog>> operateLogs(HttpServletRequest request) {
        return R.ok(QueryUtilsX.queryPage(QueryUtilsX.parse(request.getParameterMap()),
                PageParams.of(request), SysOperateLog.class));
    }
}
