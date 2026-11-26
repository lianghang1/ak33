package com.sfi.matrix.support.repository.dict;

import com.sfi.matrix.support.entity.SysDictOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 字典选项 Repository。
 *
 * @author lianghang
 */
@Repository
public interface SysDictOptionRepository extends JpaRepository<SysDictOption, Long> {

    List<SysDictOption> findByDictIdAndDeletedFalseOrderBySortNoAsc(Long dictId);

    Optional<SysDictOption> findByDictIdAndOptionCodeAndTenantIdAndDeletedFalse(
            Long dictId, String optionCode, String tenantId);
}
