package com.sfi.ak33.apps.cemetery.controller;

import com.sfi.ak33.business.cemetery.entity.CemeteryAreaEntity;
import com.sfi.ak33.business.cemetery.entity.GridCellEntity;
import com.sfi.ak33.business.cemetery.entity.TombEntity;
import com.sfi.ak33.business.cemetery.repository.CemeteryAreaRepository;
import com.sfi.ak33.business.cemetery.service.TombServiceImpl;
import com.sfi.scaffold.common.result.R;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 第一阶段墓区墓穴接口（冒烟验证用）。
 *
 * <p>覆盖：建墓区 → 铺格位 → 初始化墓位 → 查询。真实项目后续按 REST 规范拆分 DTO/VO。</p>
 *
 * @author ak33
 */
@RestController
@RequestMapping("/api/cemetery")
public class CemeteryController {

    private final CemeteryAreaRepository areaRepository;
    private final TombServiceImpl tombService;

    public CemeteryController(CemeteryAreaRepository areaRepository, TombServiceImpl tombService) {
        this.areaRepository = areaRepository;
        this.tombService = tombService;
    }

    /** 建一个墓区（园级），返回ID */
    @PostMapping("/area")
    public R<Long> createArea(@RequestParam String code, @RequestParam String name) {
        CemeteryAreaEntity area = new CemeteryAreaEntity();
        area.setAreaCode(code);
        area.setAreaName(name);
        area.setLevel(1);
        area.setFullPath(name);
        area.setSortNo(0);
        area = areaRepository.save(area);
        return R.ok(area.getId());
    }

    /** 铺格位：rows × cols */
    @PostMapping("/grid/generate")
    public R<Integer> generateGrid(@RequestParam Long areaId,
                                   @RequestParam int rows, @RequestParam int cols) {
        return R.ok(tombService.generateGridCells(areaId, rows, cols));
    }

    /** 初始化：把选中格位变成墓位，生成编号 */
    @PostMapping("/tomb/init")
    public R<Integer> initTombs(@RequestBody List<Long> cellIds, @RequestParam int holes) {
        return R.ok(tombService.initializeTombs(cellIds, holes));
    }

    /** 查某墓区墓位 */
    @GetMapping("/tombs")
    public R<List<TombEntity>> tombs(@RequestParam Long areaId) {
        return R.ok(tombService.listByArea(areaId));
    }

    /** 查某墓区格位 */
    @GetMapping("/cells")
    public R<List<GridCellEntity>> cells(@RequestParam Long areaId) {
        return R.ok(tombService.listCellsByArea(areaId));
    }

    /** 墓区列表 */
    @GetMapping("/areas")
    public R<List<CemeteryAreaEntity>> areas() {
        return R.ok(areaRepository.findByParentIdIsNullAndDeletedFalseOrderBySortNoAsc());
    }
}
