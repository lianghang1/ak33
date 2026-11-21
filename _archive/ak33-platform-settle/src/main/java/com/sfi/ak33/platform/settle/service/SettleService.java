package com.sfi.ak33.platform.settle.service;

import com.sfi.ak33.platform.settle.entity.SettleBill;
import com.sfi.ak33.platform.settle.repository.SettleBillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 结算服务骨架：创建结算单、收款、退款。
 *
 * <p>刻意不依赖任何业务模块——通过 bizType/bizId 关联业务单据，
 * 因此公墓/寄存/殡仪系统均可复用同一套结算能力。</p>
 *
 * @author ak33
 */
@Service
public class SettleService {

    private final SettleBillRepository billRepository;

    public SettleService(SettleBillRepository billRepository) {
        this.billRepository = billRepository;
    }

    /** 创建结算单（业务系统调用，传入业务类型与单据ID） */
    @Transactional(rollbackFor = Exception.class)
    public SettleBill createBill(String bizType, Long bizId, BigDecimal amount) {
        SettleBill bill = new SettleBill();
        bill.setBillNo("BL" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        bill.setBizType(bizType);
        bill.setBizId(bizId);
        bill.setAmount(amount);
        bill.setStatus("UNPAID");
        return billRepository.save(bill);
    }

    /** 收款 */
    @Transactional(rollbackFor = Exception.class)
    public SettleBill pay(Long billId, String payMethod) {
        SettleBill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("结算单不存在：" + billId));
        bill.setPayMethod(payMethod);
        bill.setStatus("PAID");
        bill.setPaidAt(LocalDateTime.now());
        return billRepository.save(bill);
    }

    /** 退款 */
    @Transactional(rollbackFor = Exception.class)
    public SettleBill refund(Long billId) {
        SettleBill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("结算单不存在：" + billId));
        bill.setStatus("REFUNDED");
        return billRepository.save(bill);
    }
}
