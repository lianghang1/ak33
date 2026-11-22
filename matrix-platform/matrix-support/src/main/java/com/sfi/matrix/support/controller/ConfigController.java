package com.sfi.matrix.support.controller;

import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.matrix.support.dto.ConfigRequest;
import com.sfi.matrix.support.entity.SysConfig;
import com.sfi.matrix.support.service.ConfigService;
import com.sfi.scaffold.common.request.IdRequest;
import com.sfi.scaffold.common.result.PageResult;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.security.identity.LoginUser;
import com.sfi.scaffold.starter.web.crud.BaseCrudController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统参数接口：标准 CRUD 由 {@link BaseCrudController} 提供，取值/分组端点单独保留。
 *
 * <p>基类端点：GET /list、GET /get?id=、POST /save、POST /delete。敏感值在列表与详情
 * 均经 {@link ConfigService#plainCopy} 脱敏后返回；取值端点避让到 /value 系列。</p>
 *
 * @author matrix
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController extends BaseCrudController<SysConfig, ConfigRequest, ConfigService> {

    public ConfigController(ConfigService service) {
        super(service, SysConfig.class);
    }

    @Override
    @OperateLog(module = "参数管理", operation = "维护系统参数")
    @PostMapping("/save")
    public R<SysConfig> save(@Valid @RequestBody ConfigRequest dto) {
        return super.save(dto);
    }

    @Override
    @OperateLog(module = "参数管理", operation = "删除系统参数")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        return super.delete(req);
    }

    /**
     * 列表结果脱敏：基类 page 走 QueryUtilsX 返回 JPA 实体，config_value 仍是密文。
     * 这里整体改写成明文副本（详情由 ConfigService.get 覆盖已脱敏，不经过这里）。
     */
    @Override
    protected PageResult<?> rewriteList(PageResult<?> result) {
        List<Object> plain = new ArrayList<>();
        for (Object item : result.getList()) {
            if (item instanceof SysConfig) {
                plain.add(service.plainCopy((SysConfig) item));
            } else {
                plain.add(item);
            }
        }
        return PageResult.of(plain, result.getTotal(), result.getPageNum(), result.getPageSize());
    }

    /** 取值（默认分组）：当前租户 → 平台兜底，敏感值解密返回。 */
    @GetMapping("/value")
    public R<String> value(@RequestParam String configKey) {
        return R.ok(service.get(configKey, currentTenantId()));
    }

    /** 分组取值：如 /value-by-group?group=wechat.pay&configKey=apiKey */
    @GetMapping("/value-by-group")
    public R<String> valueByGroup(@RequestParam String group, @RequestParam String configKey) {
        return R.ok(service.get(group, configKey, currentTenantId()));
    }

    /** 整组取值（带平台兜底，敏感值已解密）。 */
    @GetMapping("/values-by-group")
    public R<List<SysConfig>> valuesByGroup(@RequestParam String group) {
        return R.ok(service.listByGroup(group, currentTenantId()));
    }

    private static String currentTenantId() {
        LoginUser current = LoginContext.currentOrNull();
        return current == null ? null : current.getTenantId();
    }
}
