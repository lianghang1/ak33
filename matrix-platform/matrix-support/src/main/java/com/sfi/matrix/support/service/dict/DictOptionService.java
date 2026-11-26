package com.sfi.matrix.support.service.dict;

import com.sfi.matrix.support.constant.SupportConstants;
import com.sfi.matrix.support.dto.DictOptionValue;
import com.sfi.matrix.support.entity.SysDictOption;
import com.sfi.matrix.support.repository.dict.SysDictOptionRepository;
import com.sfi.scaffold.common.exception.BizException;
import com.sfi.scaffold.common.exception.GlobalErrorCode;
import com.sfi.scaffold.starter.data.jpa.service.BaseService;
import com.sfi.scaffold.starter.security.identity.LoginContext;
import com.sfi.scaffold.starter.tenant.annotation.TenantIgnore;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典选项服务：CRUD + 批量取值。
 *
 * <p>单值取值由 {@link DictOptionQueryService} 提供，批量取值按字典编码逐个取选项。
 * 写入时全量失效选项缓存。</p>
 *
 * @author lianghang
 */
@Service
public class DictOptionService extends BaseService<SysDictOption> {

    private final SysDictOptionRepository optionRepository;
    private final DictOptionQueryService queryService;

    public DictOptionService(SysDictOptionRepository optionRepository, DictOptionQueryService queryService) {
        super(optionRepository);
        this.optionRepository = optionRepository;
        this.queryService = queryService;
    }

    /** 覆盖基类 save：id 为空按 dictId+optionCode 幂等，非空按 id 修改。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = SupportConstants.CACHE_DICT_OPTION, allEntries = true)
    public SysDictOption save(SysDictOption incoming) {
        SysDictOption entity;
        if (incoming.getId() != null) {
            entity = require(incoming.getId());
            if (incoming.getOptionCode() != null && !incoming.getOptionCode().equals(entity.getOptionCode())) {
                String tid = entity.getTenantId();
                boolean exists = optionRepository
                        .findByDictIdAndOptionCodeAndTenantIdAndDeletedFalse(
                                entity.getDictId(), incoming.getOptionCode(), tid)
                        .filter(o -> !o.getId().equals(entity.getId()))
                        .isPresent();
                if (exists) {
                    throw BizException.of(GlobalErrorCode.CONFLICT, "选项编码已存在：{}", incoming.getOptionCode());
                }
            }
        } else {
            String tid = incoming.getTenantId() != null && !incoming.getTenantId().isEmpty()
                    ? incoming.getTenantId() : LoginContext.currentTenantIdOrNull();
            entity = optionRepository.findByDictIdAndOptionCodeAndTenantIdAndDeletedFalse(
                    incoming.getDictId(), incoming.getOptionCode(), tid).orElseGet(SysDictOption::new);
            if (entity.getId() == null) {
                entity.setDictId(incoming.getDictId());
                entity.setOptionCode(incoming.getOptionCode());
                entity.setTenantId(tid);
            }
        }
        if (incoming.getOptionCode() != null) entity.setOptionCode(incoming.getOptionCode());
        if (incoming.getOptionLabel() != null) entity.setOptionLabel(incoming.getOptionLabel());
        entity.setSortNo(incoming.getSortNo() == null ? 0 : incoming.getSortNo());
        if (incoming.getStatus() != null) entity.setStatus(incoming.getStatus());
        return optionRepository.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = SupportConstants.CACHE_DICT_OPTION, allEntries = true)
    @Override
    public void delete(Long id) {
        super.delete(id);
    }

    /** 批量取：按编码逐个取值，组装 Map。 */
    @TenantIgnore
    public Map<String, List<DictOptionValue>> valuesBatch(List<String> dictCodes, String tenantId) {
        Map<String, List<DictOptionValue>> result = new LinkedHashMap<>();
        if (dictCodes == null) return result;
        for (String code : dictCodes) {
            if (code != null && !code.isEmpty()) result.put(code, queryService.values(code, tenantId));
        }
        return result;
    }
}
