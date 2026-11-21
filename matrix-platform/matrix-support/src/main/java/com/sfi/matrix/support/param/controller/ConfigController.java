package com.sfi.matrix.support.param.controller;

import com.sfi.matrix.support.param.entity.SysConfig;
import com.sfi.matrix.support.param.service.ConfigService;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.starter.security.permission.RequirePermission;
import org.springframework.web.bind.annotation.*;

/**
 * 系统参数接口。
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

    /** 维护参数：需平台参数管理权限（权限点与 IamConstants.PERM_CONFIG 对应） */
    @RequirePermission("platform:config:list")
    @PostMapping
    public R<SysConfig> set(@RequestParam String configKey,
                            @RequestParam String configValue,
                            @RequestParam(required = false) String remark,
                            @RequestParam(required = false) String tenantId) {
        return R.ok(configService.set(configKey, configValue, remark, tenantId));
    }

    @GetMapping("/get")
    public R<String> get(@RequestParam String configKey) {
        return R.ok(configService.get(configKey));
    }
}
