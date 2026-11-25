package com.sfi.matrix.cemetery.strategy;

/**
 * 编号策略上下文：封装生成一个墓位标识所需的全部输入。策略只依赖本上下文，
 * 不依赖实体，便于独立测试与替换。
 *
 * <p>多级墓区下：区码/区名取墓位所在的叶子墓区，园名取根节点。基础路径与全路径
 * 为可读层级路径，由策略一并返回并落库为快照。</p>
 *
 * @author lianghang
 */
public class CodeContext {

    private String tenantId;
    private String gardenName;
    private String zoneCode;
    private String zoneName;
    private int rowNo;
    private int colNo;
    private int holes;

    public String getTenantId() { return tenantId; }
    public CodeContext setTenantId(String tenantId) { this.tenantId = tenantId; return this; }

    public String getGardenName() { return gardenName; }
    public CodeContext setGardenName(String gardenName) { this.gardenName = gardenName; return this; }

    public String getZoneCode() { return zoneCode; }
    public CodeContext setZoneCode(String zoneCode) { this.zoneCode = zoneCode; return this; }

    public String getZoneName() { return zoneName; }
    public CodeContext setZoneName(String zoneName) { this.zoneName = zoneName; return this; }

    public int getRowNo() { return rowNo; }
    public CodeContext setRowNo(int rowNo) { this.rowNo = rowNo; return this; }

    public int getColNo() { return colNo; }
    public CodeContext setColNo(int colNo) { this.colNo = colNo; return this; }

    public int getHoles() { return holes; }
    public CodeContext setHoles(int holes) { this.holes = holes; return this; }
}
