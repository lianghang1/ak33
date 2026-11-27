package com.sfi.matrix.support.controller;

import com.sfi.matrix.iam.annotation.OperateLog;
import com.sfi.matrix.support.dto.ConfigCreateRequest;
import com.sfi.matrix.support.dto.ConfigSecretRequest;
import com.sfi.matrix.support.dto.ConfigUpdateRequest;
import com.sfi.matrix.support.dto.ConfigVO;
import com.sfi.matrix.support.entity.SysConfig;
import com.sfi.matrix.support.service.ConfigService;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
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
 * 系统参数接口。
 *
 * <p>读端点（基类 list/get + value/values/secret）敏感值出口一律经
 * {@link ConfigService#toVO} / {@link ConfigService#valueOf} 加密成密文对象；只有 GET /secret
 * 返回明文。写端点拆成 create（纯新增）/update（基础信息，不碰 sensitive）/secret（敏感值与
 * 标志，独立权限+审计）/delete，基类 save 已覆盖为拒绝。</p>
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController extends BaseCrudController<SysConfig, ConfigCreateRequest, ConfigService> {

    public ConfigController(ConfigService service) {
        super(service, SysConfig.class);
    }

    /** save 已停用：新增走 /create，修改走 /update 或 /secret。 */
    @Override
    @PostMapping("/save")
    public R<SysConfig> save(@Valid @RequestBody ConfigCreateRequest dto) {
        throw BizException.of(GlobalErrorCode.BAD_REQUEST,
                "未授权访问");
    }

    @PostMapping("/create")
    @OperateLog(module = "参数管理", operation = "新增系统参数")
    public R<ConfigVO> create(@Valid @RequestBody ConfigCreateRequest req) {
        return R.ok(service.toVO(service.create(req)));
    }

    @PostMapping("/update")
    @OperateLog(module = "参数管理", operation = "修改系统参数")
    public R<ConfigVO> update(@Valid @RequestBody ConfigUpdateRequest req) {
        return R.ok(service.toVO(service.update(req)));
    }

    /** 敏感值与标志的唯一写入口，独立权限点 + 审计。 */
    @PostMapping("/secret")
    @OperateLog(module = "参数管理", operation = "修改敏感参数")
    public R<ConfigVO> secret(@Valid @RequestBody ConfigSecretRequest req) {
        return R.ok(service.toVO(service.updateSecret(req)));
    }

    /** 敏感明文读取，独立权限点 + 审计。 */
    @GetMapping("/secret")
    @OperateLog(module = "参数管理", operation = "读取敏感参数明文")
    public R<String> secretPlain(@RequestParam("id") Long id) {
        return R.ok(service.secretPlain(id));
    }

    @Override
    @OperateLog(module = "参数管理", operation = "删除系统参数")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        return super.delete(req);
    }

    /** 详情出口加密：经基类 detailOf 钩子转对外视图，不覆盖 get 签名。 */
    @Override
    protected Object detailOf(SysConfig entity) {
        return service.toVO(entity);
    }

    /**
     * 列表出口加密：基类 page 走 QueryUtilsX 返回明文实体，这里整体转成对外视图。
     */
    @Override
    protected PageResult<?> rewriteList(PageResult<?> result) {
        List<Object> views = new ArrayList<>();
        for (Object item : result.getList()) {
            if (item instanceof SysConfig) {
                views.add(service.toVO((SysConfig) item));
            } else {
                views.add(item);
            }
        }
        return PageResult.of(views, result.getTotal(), result.getPageNum(), result.getPageSize());
    }

    /**
     * 单值取值：group 可选（空走 DEFAULT），敏感值返回密文对象、非敏感返回明文。
     * 例：/value?configKey=apiKey&group=wechat.pay
     */
    @GetMapping("/value")
    public R<Object> value(@RequestParam String configKey,
                           @RequestParam(required = false) String group) {
        SysConfig entry = service.valueEntry(group, configKey, LoginContext.currentTenantIdOrNull());
        return R.ok(service.valueOf(entry));
    }

    /**
     * 整组取值：group 可选（空走 DEFAULT），返回该组全部参数视图（带平台兜底）。
     * 例：/values?group=wechat.pay
     */
    @GetMapping("/values")
    public R<List<ConfigVO>> values(@RequestParam(required = false) String group) {
        List<ConfigVO> views = new ArrayList<>();
        for (SysConfig c : service.listByGroup(group, LoginContext.currentTenantIdOrNull())) {
            views.add(service.toVO(c));
        }
        return R.ok(views);
    }

}
