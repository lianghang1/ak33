package com.sfi.matrix.cemetery.controller;

import com.sfi.matrix.cemetery.dto.TombInitDto;
import com.sfi.matrix.cemetery.entity.Tomb;
import com.sfi.matrix.cemetery.service.TombService;
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
 * 墓位接口：初始化（业务动作，不接基类）+ 查询。
 *
 * @author matrix
 */
@RestController
@RequestMapping("/api/cemetery/tomb")
public class TombController {

    private final TombService tombService;

    public TombController(TombService tombService) {
        this.tombService = tombService;
    }

    /** 初始化墓位：批量把空格位建成墓位，生成编号快照。 */
    @OperateLog(module = "墓位管理", operation = "初始化墓位")
    @PostMapping("/init")
    public R<Integer> init(@Valid @RequestBody TombInitDto dto) {
        return R.ok(tombService.initialize(dto.getCellIds(), dto.getHoles(), dto.getSalePrice()));
    }

    /** 某墓区下全部墓位。 */
    @GetMapping("/list")
    public R<List<Tomb>> list(@RequestParam Long areaId) {
        return R.ok(tombService.listByArea(areaId));
    }

    /** 某墓区下按状态查墓位。 */
    @GetMapping("/by-status")
    public R<List<Tomb>> byStatus(@RequestParam Long areaId, @RequestParam String status) {
        return R.ok(tombService.listByAreaAndStatus(areaId, status));
    }
}
