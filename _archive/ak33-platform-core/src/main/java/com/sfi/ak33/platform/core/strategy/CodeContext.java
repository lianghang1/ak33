package com.sfi.ak33.platform.core.strategy;

/**
 * 编号策略上下文：封装生成一个墓位标识所需的全部输入参数。
 *
 * <p>策略只依赖本上下文，不依赖实体，便于独立测试与替换。不同租户可注册不同策略，
 * 上下文预留 tenantId 供策略区分规则；基础路径/全路径按用户约定为可读层级路径（方案A），
 * 由策略一并返回并落库为快照——策略后续变更不反向改写历史墓位。</p>
 *
 * @author ak33
 */
public class CodeContext {

    /** 租户标识，供多租户差异化策略使用 */
    private String tenantId;

    /** 园区码，如 A */
    private String gardenCode;

    /** 园区名称，如 玫瑰园 */
    private String gardenName;

    /** 区码，如 A */
    private String zoneCode;

    /** 区名称，如 A区 */
    private String zoneName;

    /** 排号，从 1 开始 */
    private int rowNo;

    /** 位号，从 1 开始 */
    private int colNo;

    /** 穴位数，1=单穴，2=双穴 */
    private int holes;

    public String getTenantId() {
        return tenantId;
    }

    public CodeContext setTenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public String getGardenCode() {
        return gardenCode;
    }

    public CodeContext setGardenCode(String gardenCode) {
        this.gardenCode = gardenCode;
        return this;
    }

    public String getGardenName() {
        return gardenName;
    }

    public CodeContext setGardenName(String gardenName) {
        this.gardenName = gardenName;
        return this;
    }

    public String getZoneCode() {
        return zoneCode;
    }

    public CodeContext setZoneCode(String zoneCode) {
        this.zoneCode = zoneCode;
        return this;
    }

    public String getZoneName() {
        return zoneName;
    }

    public CodeContext setZoneName(String zoneName) {
        this.zoneName = zoneName;
        return this;
    }

    public int getRowNo() {
        return rowNo;
    }

    public CodeContext setRowNo(int rowNo) {
        this.rowNo = rowNo;
        return this;
    }

    public int getColNo() {
        return colNo;
    }

    public CodeContext setColNo(int colNo) {
        this.colNo = colNo;
        return this;
    }

    public int getHoles() {
        return holes;
    }

    public CodeContext setHoles(int holes) {
        this.holes = holes;
        return this;
    }
}
