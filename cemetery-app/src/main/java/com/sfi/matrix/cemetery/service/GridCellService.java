package com.sfi.matrix.cemetery.service;

import com.sfi.matrix.cemetery.entity.CemeteryArea;
import com.sfi.matrix.cemetery.entity.GridCell;
import com.sfi.matrix.cemetery.repository.GridCellRepository;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.starter.data.jpa.service.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 格位服务：按 rows×cols 在叶子墓区批量铺规划格位，幂等可重复执行。
 *
 * @author matrix
 */
@Service
public class GridCellService extends BaseService<GridCell> {

    private final GridCellRepository gridCellRepository;
    private final CemeteryAreaService areaService;

    public GridCellService(GridCellRepository gridCellRepository, CemeteryAreaService areaService) {
        super(gridCellRepository);
        this.gridCellRepository = gridCellRepository;
        this.areaService = areaService;
    }

    /**
     * 铺格位：已存在的(墓区,排,列)跳过，重复执行不产生重复数据。
     *
     * @return 本次新建格位数
     */
    @Transactional(rollbackFor = Exception.class)
    public int generate(Long areaId, int rows, int cols) {
        if (rows < 1 || cols < 1) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "排数和列数必须大于 0");
        }
        CemeteryArea area = areaService.get(areaId);
        if (area == null) {
            throw BizException.of(GlobalErrorCode.NOT_FOUND, "墓区不存在：" + areaId);
        }
        String tenantId = area.getTenantId() != null ? area.getTenantId() : TenantIdProvider.current();
        int created = 0;
        for (int r = 1; r <= rows; r++) {
            for (int c = 1; c <= cols; c++) {
                Optional<GridCell> exists =
                        gridCellRepository.findByAreaIdAndRowNoAndColNoAndDeletedFalse(areaId, r, c);
                if (exists.isPresent()) continue;
                GridCell cell = new GridCell();
                cell.setAreaId(areaId);
                cell.setRowNo(r);
                cell.setColNo(c);
                cell.setInitialized(Boolean.FALSE);
                cell.setTenantId(tenantId);
                gridCellRepository.save(cell);
                created++;
            }
        }
        return created;
    }

    public List<GridCell> listByArea(Long areaId) {
        return gridCellRepository.findByAreaIdAndDeletedFalse(areaId);
    }

    public List<GridCell> listUninitialized(Long areaId) {
        return gridCellRepository.findByAreaIdAndInitializedFalseAndDeletedFalse(areaId);
    }
}
