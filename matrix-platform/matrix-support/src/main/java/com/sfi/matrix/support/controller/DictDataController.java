package com.sfi.matrix.support.controller;

import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.matrix.support.dto.DictDataRequest;
import com.sfi.matrix.support.entity.SysDictData;
import com.sfi.matrix.support.service.DictDataService;
import com.sfi.scaffold.common.request.IdRequest;
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
import java.util.List;

/**
 * 字典选项接口：标准 CRUD 由 {@link BaseCrudController} 提供，按类型取选项（带兜底）单独保留。
 *
 * <p>路径保持 {@code /api/dict}（前端已按此对接），底层数据为 sys_dict_data。
 * 基类端点：GET /list、GET /get?id=、POST /save、POST /delete；
 * 兼容取值端点避让到 GET /options?dictType=。</p>
 *
 * @author matrix
 */
@RestController
@RequestMapping("/api/dict")
public class DictDataController extends BaseCrudController<SysDictData, DictDataRequest, DictDataService> {

    public DictDataController(DictDataService service) {
        super(service, SysDictData.class);
    }

    @Override
    @OperateLog(module = "字典管理", operation = "维护字典选项")
    @PostMapping("/save")
    public R<SysDictData> save(@Valid @RequestBody DictDataRequest dto) {
        return super.save(dto);
    }

    @Override
    @OperateLog(module = "字典管理", operation = "删除字典选项")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        return super.delete(req);
    }

    /** 按类型取选项（带平台兜底）：避让基类 /list，保留前端兼容端点语义。 */
    @GetMapping("/options")
    public R<List<SysDictData>> options(@RequestParam String dictType) {
        LoginUser current = LoginContext.currentOrNull();
        String tenantId = current == null ? null : current.getTenantId();
        return R.ok(service.listByTypeWithPlatformFallback(dictType, tenantId));
    }
}
