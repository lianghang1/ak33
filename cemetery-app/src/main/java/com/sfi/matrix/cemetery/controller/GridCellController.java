package com.sfi.matrix.cemetery.controller;

import com.sfi.matrix.cemetery.dto.GridCellSaveDto;
import com.sfi.matrix.cemetery.dto.GridGenerateDto;
import com.sfi.matrix.cemetery.entity.GridCell;
import com.sfi.matrix.cemetery.service.GridCellService;
import com.sfi.matrix.iam.audit.annotation.OperateLog;
import com.sfi.scaffold.common.request.IdRequest;
import com.sfi.scaffold.common.result.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 格位接口：批量铺设（业务动作，不接基类）+ 查询。
 *
 * @author lianghang
 */
@RestController
@RequestMapping("/api/cemetery/grid")
public class GridCellController {

    private final GridCellService gridCellService;

    public GridCellController(GridCellService gridCellService) {
        this.gridCellService = gridCellService;
    }

    /** 铺格位：按 rows×cols 生成规划格位，幂等。 */
    @OperateLog(module = "墓区管理", operation = "规划格位")
    @PostMapping("/generate")
    public R<Integer> generate(@Valid @RequestBody GridGenerateDto dto) {
        return R.ok(gridCellService.generate(dto.getAreaId(), dto.getRows(), dto.getCols()));
    }

    /** 某墓区下全部格位。 */
    @GetMapping("/list")
    public R<List<GridCell>> list(@RequestParam Long areaId) {
        return R.ok(gridCellService.listByArea(areaId));
    }

    /** 某墓区下待建（未初始化）格位。 */
    @GetMapping("/uninitialized")
    public R<List<GridCell>> uninitialized(@RequestParam Long areaId) {
        return R.ok(gridCellService.listUninitialized(areaId));
    }

    /** 修改空格位坐标（已初始化格位禁改）。 */
    @OperateLog(module = "墓区管理", operation = "修改格位")
    @PostMapping("/save")
    public R<GridCell> save(@Valid @RequestBody GridCellSaveDto dto) {
        return R.ok(gridCellService.update(dto.getId(), dto.getRowNo(), dto.getColNo()));
    }

    /** 删除空格位（已初始化格位禁删）。 */
    @OperateLog(module = "墓区管理", operation = "删除格位")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdRequest req) {
        gridCellService.delete(req.getId());
        return R.ok();
    }
}
