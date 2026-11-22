package com.sfi.matrix.cemetery.service;

import com.sfi.matrix.cemetery.entity.CemeteryArea;
import com.sfi.matrix.cemetery.entity.GridCell;
import com.sfi.matrix.cemetery.entity.Tomb;
import com.sfi.matrix.cemetery.repository.GridCellRepository;
import com.sfi.matrix.cemetery.repository.TombRepository;
import com.sfi.matrix.cemetery.strategy.CodeContext;
import com.sfi.matrix.cemetery.strategy.CodeResult;
import com.sfi.matrix.cemetery.strategy.TombCodeStrategy;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.starter.data.jpa.service.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 墓位服务：把选中的空格位初始化为墓位，生成编号三元组落库为快照。
 *
 * <p>编号的区码/区名取墓位所在的叶子墓区，园名取墓区树根节点；穴位数为任意正整数；
 * P0 初始化后状态固定为 INITIALIZED，售价随初始化录入。整批在一个事务内完成，
 * 单个格位失败整批回滚。编号策略通过 {@link TombCodeStrategy} 可插拔。</p>
 *
 * @author matrix
 */
@Service
public class TombService extends BaseService<Tomb> {

    /** P0 初始化后的固定状态 */
    public static final String STATUS_INITIALIZED = "INITIALIZED";

    private final TombRepository tombRepository;
    private final GridCellRepository gridCellRepository;
    private final CemeteryAreaService areaService;
    private final TombCodeStrategy codeStrategy;

    public TombService(TombRepository tombRepository, GridCellRepository gridCellRepository,
                       CemeteryAreaService areaService, TombCodeStrategy codeStrategy) {
        super(tombRepository);
        this.tombRepository = tombRepository;
        this.gridCellRepository = gridCellRepository;
        this.areaService = areaService;
        this.codeStrategy = codeStrategy;
    }

    /**
     * 初始化墓位：批量把空格位建成墓位。
     *
     * @param cellIds   选中的格位ID
     * @param holes     穴位数（任意正整数）
     * @param salePrice 销售价
     * @return 成功初始化的墓位数
     */
    @Transactional(rollbackFor = Exception.class)
    public int initialize(List<Long> cellIds, int holes, BigDecimal salePrice) {
        if (holes < 1) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "穴位数必须大于 0");
        }
        if (cellIds == null || cellIds.isEmpty()) {
            throw BizException.of(GlobalErrorCode.BAD_REQUEST, "请至少选择一个格位");
        }
        int count = 0;
        for (Long cellId : cellIds) {
            GridCell cell = gridCellRepository.findById(cellId)
                    .filter(c -> !Boolean.TRUE.equals(c.getDeleted()))
                    .orElseThrow(() -> BizException.of(GlobalErrorCode.NOT_FOUND, "格位不存在：" + cellId));
            if (Boolean.TRUE.equals(cell.getInitialized())) {
                continue; // 已初始化，跳过（幂等）
            }
            CemeteryArea area = areaService.get(cell.getAreaId());
            if (area == null) {
                throw BizException.of(GlobalErrorCode.NOT_FOUND, "墓区不存在：" + cell.getAreaId());
            }
            CemeteryArea root = areaService.rootOf(area.getId());

            CodeResult result = codeStrategy.generate(new CodeContext()
                    .setTenantId(area.getTenantId() != null ? area.getTenantId() : TenantIdProvider.current())
                    .setGardenName(root.getAreaName())
                    .setZoneCode(area.getAreaCode())
                    .setZoneName(area.getAreaName())
                    .setRowNo(cell.getRowNo())
                    .setColNo(cell.getColNo())
                    .setHoles(holes));

            Tomb tomb = new Tomb();
            tomb.setGridCellId(cell.getId());
            tomb.setAreaId(area.getId());
            tomb.setCode(result.getCode());
            tomb.setBasePath(result.getBasePath());
            tomb.setFullPath(result.getFullPath());
            tomb.setCodeStrategy(codeStrategy.name());
            tomb.setHoles(holes);
            tomb.setSalePrice(salePrice);
            tomb.setStatus(STATUS_INITIALIZED);
            tomb.setTenantId(area.getTenantId() != null ? area.getTenantId() : TenantIdProvider.current());
            tombRepository.save(tomb);

            cell.setInitialized(Boolean.TRUE);
            cell.setTombId(tomb.getId());
            gridCellRepository.save(cell);
            count++;
        }
        return count;
    }

    public List<Tomb> listByArea(Long areaId) {
        return tombRepository.findByAreaIdAndDeletedFalse(areaId);
    }

    public List<Tomb> listByAreaAndStatus(Long areaId, String status) {
        return tombRepository.findByAreaIdAndStatusAndDeletedFalse(areaId, status);
    }
}
