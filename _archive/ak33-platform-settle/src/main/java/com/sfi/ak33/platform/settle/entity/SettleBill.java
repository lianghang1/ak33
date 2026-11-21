package com.sfi.ak33.platform.settle.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 结算单（平台结算能力骨架，跨系统复用）。
 *
 * <p>bizType 标识业务来源（tomb购墓 / deposit寄存 / funeral殡仪），
 * 结算能力不依赖任何具体业务，靠 bizType + bizId 关联业务单据——这是复用的关键设计。</p>
 *
 * @author ak33
 */
@Entity
@Table(name = "settle_bill")
public class SettleBill extends BaseEntity {

    /** 结算单号 */
    @Column(name = "bill_no", nullable = false, length = 64)
    private String billNo;

    /** 业务来源：tomb / deposit / funeral */
    @Column(name = "biz_type", nullable = false, length = 32)
    private String bizType;

    /** 业务单据ID */
    @Column(name = "biz_id")
    private Long bizId;

    /** 应收金额 */
    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /** 支付方式：cash/pos/transfer/online */
    @Column(name = "pay_method", length = 32)
    private String payMethod;

    /** 状态：UNPAID已创建 PAID已收款 REFUNDED已退 */
    @Column(name = "status", nullable = false, length = 32)
    private String status = "UNPAID";

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public Long getBizId() {
        return bizId;
    }

    public void setBizId(Long bizId) {
        this.bizId = bizId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPayMethod() {
        return payMethod;
    }

    public void setPayMethod(String payMethod) {
        this.payMethod = payMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
