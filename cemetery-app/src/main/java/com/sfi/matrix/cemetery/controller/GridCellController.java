package com.sfi.matrix.cemetery.controller;

import com.sfi.matrix.cemetery.dto.GridGenerateDto;
import com.sfi.matrix.cemetery.entity.GridCell;
import com.sfi.matrix.cemetery.service.GridCellService;
import com.sfi.matrix.iam.audit.annotation.OperateLog;
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
 * @author matrix
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
}
