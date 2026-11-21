package com.sfi.matrix.support.param.controller;

import com.sfi.matrix.support.dto.ConfigRequest;
import com.sfi.matrix.support.dto.IdRequest;
import com.sfi.matrix.support.param.entity.SysConfig;
import com.sfi.matrix.support.param.service.ConfigService;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.query.QueryUtilsX;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.security.identity.LoginUser;
import com.sfi.scaffold.starter.security.permission.RequirePermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 系统参数接口。列表走 QueryUtilsX 通路②；写操作 POST + JSON DTO。
 *
 * @author matrix
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @RequirePermission("platform:config:list")
    @PostMapping
    public R<SysConfig> save(@Valid @RequestBody ConfigRequest req) {
        return R.ok(configService.save(req.getId(), req.getConfigKey(), req.getConfigValue(),
                req.getRemark(), req.getTenantId()));
    }

    @RequirePermission("platform:config:list")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        configService.delete(req.getId());
        return R.ok();
    }

    @RequirePermission("platform:config:list")
    @GetMapping
    public R<SysConfig> get(@RequestParam Long id) {
        return R.ok(configService.get(id));
    }

    /** 参数分页列表（QueryBean 通路②） */
    @GetMapping("/page")
    public R<Page<SysConfig>> page(HttpServletRequest request) {
        int page = parseInt(request.getParameter("page"), 0);
        int size = Math.min(parseInt(request.getParameter("size"), 15), 200);
        return R.ok(QueryUtilsX.doQuery(QueryUtilsX.parse(request.getParameterMap()),
                PageRequest.of(page, size), SysConfig.class));
    }

    /** 取值：当前租户 → 平台兜底 */
    @GetMapping("/get")
    public R<String> get(@RequestParam String configKey) {
        LoginUser current = LoginContext.currentOrNull();
        String tenantId = current == null ? null : current.getTenantId();
        return R.ok(configService.get(configKey, tenantId));
    }

    private static int parseInt(String v, int def) {
        try { return v == null || v.isEmpty() ? def : Integer.parseInt(v); } catch (NumberFormatException e) { return def; }
    }
}
