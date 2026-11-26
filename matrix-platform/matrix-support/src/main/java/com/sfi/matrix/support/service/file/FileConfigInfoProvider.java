package com.sfi.matrix.support.service.file;

import com.sfi.conflux.common.util.JsonUtils;
import com.sfi.matrix.support.dto.FileConfigInfo;
import com.sfi.matrix.support.service.ConfigService;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文件系统配置读取：从 config 中心读取 matrix.file/config。
 *
 * @author lianghang
 */
@Slf4j
@Component
public class FileConfigInfoProvider {

    private static final String CONFIG_GROUP = "matrix.file";
    private static final String CONFIG_KEY = "config";

    private final ConfigService configService;

    public FileConfigInfoProvider(ConfigService configService) {
        this.configService = configService;
    }

    public FileConfigInfo getConfig() {
        try {
            String json = configService.get(CONFIG_GROUP, CONFIG_KEY, LoginContext.currentTenantIdOrNull());
            return JsonUtils.fromJson(json, FileConfigInfo.class);
        } catch (Exception e) {
            log.error("Failed to get file config", e);
            return null;
        }
    }

}
