package com.sfi.matrix.support.service.file;

import com.sfi.conflux.common.util.JsonUtils;
import com.sfi.matrix.support.dto.FileConfigInfo;
import com.sfi.matrix.support.service.ConfigService;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>
 * TODO: 在此说明该类的核心业务逻辑或注意事项
 * </p>
 *
 * @author lianghang
 */
@Slf4j
@Component
public class FileConfigProvider {

    private static final String CONFIG_GROUP = "matrix.file";
    private static final String CONFIG_KEY = "config";

    private final ConfigService configService;

    public FileConfigProvider(ConfigService configService) {
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
