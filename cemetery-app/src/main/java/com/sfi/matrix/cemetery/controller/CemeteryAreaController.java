package com.sfi.matrix.cemetery.controller;

import com.sfi.matrix.cemetery.dto.AreaMoveDto;
import com.sfi.matrix.cemetery.dto.AreaSaveDto;
import com.sfi.matrix.cemetery.entity.CemeteryArea;
import com.sfi.matrix.cemetery.service.CemeteryAreaService;
import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.scaffold.common.request.IdRequest;
import com.sfi.scaffold.common.result.R;
import com.sfi.scaffold.starter.web.crud.BaseCrudController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 墓区接口：标准 CRUD 由 {@link BaseCrudController} 提供（/list /get /save /delete），
 * 树查询 /tree 与移动 /move 为特化端点。墓区 save 在 Service 覆盖中自动算 level/fullPath。
 *
 * @author matrix
 */
@RestController
@RequestMapping("/api/cemetery/area")
public class CemeteryAreaController
        extends BaseCrudController<CemeteryArea, AreaSaveDto, CemeteryAreaService> {

    public CemeteryAreaController(CemeteryAreaService service) {
        super(service, CemeteryArea.class);
    }

    @Override
    @OperateLog(module = "墓区管理", operation = "保存墓区")
    @PostMapping("/save")
    public R<CemeteryArea> save(@Valid @RequestBody AreaSaveDto dto) {
        return super.save(dto);
    }

    @Override
    @OperateLog(module = "墓区管理", operation = "删除墓区")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        return super.delete(req);
    }

    /** 墓区树：按 parentId 层级平铺（根到叶），前端自行组装树形。 */
    @GetMapping("/tree")
    public R<List<CemeteryArea>> tree() {
        return R.ok(service.tree());
    }

    /** 移动墓区到新父节点（级联刷新子树 level/fullPath，防环）。 */
    @OperateLog(module = "墓区管理", operation = "移动墓区")
    @PostMapping("/move")
    public R<CemeteryArea> move(@Valid @RequestBody AreaMoveDto dto) {
        return R.ok(service.move(dto.getId(), dto.getNewParentId()));
    }
}
