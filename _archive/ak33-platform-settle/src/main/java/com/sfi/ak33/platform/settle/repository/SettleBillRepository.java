package com.sfi.ak33.platform.settle.repository;

import com.sfi.ak33.platform.settle.entity.SettleBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 结算单 Repository。
 *
 * @author ak33
 */
@Repository
public interface SettleBillRepository extends JpaRepository<SettleBill, Long> {

    Optional<SettleBill> findByBillNoAndDeletedFalse(String billNo);

    List<SettleBill> findByBizTypeAndBizIdAndDeletedFalse(String bizType, Long bizId);
}
