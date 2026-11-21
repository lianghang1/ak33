package com.sfi.ak33.platform.core.enums;

/**
 * 墓位状态枚举。
 *
 * <p>主链：未初始化 → 初始化 → 可售 → 保留 → 洽谈 → 已售 → 落葬；
 * 分支：退购、迁出。状态由业务动作驱动，任何一次变更都必须留痕关联单据。</p>
 *
 * @author ak33
 */
public enum TombStatusEnum {

    UNINITIALIZED("uninitialized", "未初始化", false),
    INITIALIZED("initialized", "初始化", false),
    AVAILABLE("available", "可售", true),
    RESERVED("reserved", "保留", false),
    NEGOTIATING("negotiating", "洽谈", false),
    SOLD("sold", "已售", false),
    BURIED("buried", "落葬", false),
    REFUNDED("refunded", "退购", false),
    MOVED_OUT("moved_out", "迁出", false);

    private final String code;
    private final String desc;
    private final boolean sellable;

    TombStatusEnum(String code, String desc, boolean sellable) {
        this.code = code;
        this.desc = desc;
        this.sellable = sellable;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public boolean isSellable() {
        return sellable;
    }

    public static TombStatusEnum of(String code) {
        for (TombStatusEnum e : values()) {
            if (e.code.equalsIgnoreCase(code)) {
                return e;
            }
        }
        return null;
    }
}
