package com.sfi.matrix.support.controller;

import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.matrix.support.dto.DictTypeRequest;
import com.sfi.matrix.support.entity.SysDictType;
import com.sfi.matrix.support.service.DictTypeService;
import com.sfi.scaffold.common.request.IdRequest;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.web.crud.BaseCrudController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 字典类型接口：标准 CRUD 由 {@link BaseCrudController} 提供，平台兜底列表单独保留。
 *
 * <p>基类端点：GET /list、GET /get?id=、POST /save、POST /delete；
 * 兜底列表避让到 GET /options（与字典选项的 /api/dict/options 对称）。
 * 写操作仍走 DictTypeService.save(E) 的幂等+缓存逻辑。</p>
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/dict-type")
public class DictTypeController extends BaseCrudController<SysDictType, DictTypeRequest, DictTypeService> {

    public DictTypeController(DictTypeService service) {
        super(service, SysDictType.class);
    }

    /** 覆盖基类 save 仅为叠加审计；映射注解需重新声明，业务仍走基类 → Service.save(E)。 */
    @Override
    @OperateLog(module = "字典管理", operation = "维护字典类型")
    @PostMapping("/save")
    public R<SysDictType> save(@Valid @RequestBody DictTypeRequest dto) {
        return super.save(dto);
    }

    @Override
    @OperateLog(module = "字典管理", operation = "删除字典类型")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        return super.delete(req);
    }

    /** 类型下拉列表（带平台兜底）：避让基类 /list，命名与 /api/dict/options 对称。 */
    @GetMapping("/options")
    public R<List<SysDictType>> options() {
        return R.ok(service.listWithPlatformFallback(LoginContext.currentTenantIdOrNull()));
    }
}
