package com.sfi.ak33.business.cemetery.service;

import com.sfi.ak33.platform.core.enums.HoleTypeEnum;
import com.sfi.ak33.platform.core.enums.TombStatusEnum;
import com.sfi.ak33.platform.core.strategy.CodeContext;
import com.sfi.ak33.platform.core.strategy.CodeResult;
import com.sfi.ak33.platform.core.strategy.TombCodeStrategy;
import com.sfi.ak33.business.cemetery.entity.CemeteryAreaEntity;
import com.sfi.ak33.business.cemetery.entity.GridCellEntity;
import com.sfi.ak33.business.cemetery.entity.TombEntity;
import com.sfi.ak33.business.cemetery.repository.CemeteryAreaRepository;
import com.sfi.ak33.business.cemetery.repository.GridCellRepository;
import com.sfi.ak33.business.cemetery.repository.TombRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 墓位服务：实现"规划格位 → 选择格位 → 初始化为墓位 → 生成编号"的两步流程。
 *
 * <p>关键业务语义：格位（规划位置）与墓位（建成实体）数量可不等。
 * 规划时只生成 GridCell；只有选中格子并初始化，才生成 Tomb 并回写格位的 tombId。</p>
 *
 * @author ak33
 */
@Service
public class TombServiceImpl {

    private final GridCellRepository gridCellRepository;
    private final TombRepository tombRepository;
    private final CemeteryAreaRepository areaRepository;
    private final TombCodeStrategy codeStrategy;

    public TombServiceImpl(GridCellRepository gridCellRepository,
                           TombRepository tombRepository,
                           CemeteryAreaRepository areaRepository,
                           TombCodeStrategy codeStrategy) {
        this.gridCellRepository = gridCellRepository;
        this.tombRepository = tombRepository;
        this.areaRepository = areaRepository;
        this.codeStrategy = codeStrategy;
    }

    /**
     * 第一步：铺格位。按 rows × cols 在指定墓区生成规划格位（此时无墓位、不可售）。
     * 已存在的（墓区+排+列）跳过，保证可重复执行不产生重复数据。
     */
    @Transactional(rollbackFor = Exception.class)
    public int generateGridCells(Long areaId, int rows, int cols) {
        int created = 0;
        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= cols; c++) {
                Optional<GridCellEntity> exists =
                        gridCellRepository.findByAreaIdAndRowNoAndColNoAndDeletedFalse(areaId, r, c);
                if (exists.isPresent()) {
                    continue;
                }
                GridCellEntity cell = new GridCellEntity();
                cell.setAreaId(areaId);
                cell.setRowNo(r);
                cell.setColNo(c);
                cell.setInitialized(Boolean.FALSE);
                gridCellRepository.save(cell);
                created++;
            }
        }
        return created;
    }

    /**
     * 第二步：把选中的格位初始化为墓位，生成编号（区-排-号策略）。
     *
     * @param cellIds 选中的格位ID（支持单选/多选/全选后传入）
     * @param holes   穴位数：1=单穴，2=双穴
     * @return 成功初始化的墓位数
     */
    @Transactional(rollbackFor = Exception.class)
    public int initializeTombs(List<Long> cellIds, int holes) {
        HoleTypeEnum holeType = HoleTypeEnum.of(holes);
        if (holeType == null) {
            throw new IllegalArgumentException("不支持的穴位数：" + holes);
        }

        int count = 0;
        for (Long cellId : cellIds) {
            GridCellEntity cell = gridCellRepository.findById(cellId)
                    .orElseThrow(() -> new IllegalArgumentException("格位不存在：" + cellId));
            if (Boolean.TRUE.equals(cell.getInitialized())) {
                continue; // 已初始化，跳过
            }
            CemeteryAreaEntity area = areaRepository.findById(cell.getAreaId())
                    .orElseThrow(() -> new IllegalArgumentException("墓区不存在：" + cell.getAreaId()));

            CodeResult result = codeStrategy.generate(buildContext(area, cell, holes));

            TombEntity tomb = new TombEntity();
            tomb.setGridCellId(cell.getId());
            tomb.setAreaId(area.getId());
            tomb.setCode(result.getCode());
            tomb.setBasePath(result.getBasePath());
            tomb.setFullPath(result.getFullPath());
            tomb.setCodeStrategy(codeStrategy.name());
            tomb.setHoles(holes);
            // 初始化完成后状态为"初始化"，后续由业务动作流转到可售
            tomb.setStatus(TombStatusEnum.INITIALIZED);
            tombRepository.save(tomb);

            cell.setInitialized(Boolean.TRUE);
            cell.setTombId(tomb.getId());
            gridCellRepository.save(cell);
            count++;
        }
        return count;
    }

    private CodeContext buildContext(CemeteryAreaEntity area, GridCellEntity cell, int holes) {
        // 区级节点的 areaCode/areaName 作为区码/区名；园信息从 full_path 首段解析
        String gardenName = "";
        String fullPath = area.getFullPath();
        if (fullPath != null && fullPath.contains("/")) {
            gardenName = fullPath.split("/")[0];
        }

        return new CodeContext()
                .setTenantId("default")
                .setGardenName(gardenName)
                .setZoneCode(area.getAreaCode())
                .setZoneName(area.getAreaName())
                .setRowNo(cell.getRowNo())
                .setColNo(cell.getColNo())
                .setHoles(holes);
    }

    public List<TombEntity> listByArea(Long areaId) {
        return tombRepository.findByAreaIdAndDeletedFalse(areaId);
    }

    public List<GridCellEntity> listCellsByArea(Long areaId) {
        return gridCellRepository.findByAreaIdAndDeletedFalse(areaId);
    }
}
