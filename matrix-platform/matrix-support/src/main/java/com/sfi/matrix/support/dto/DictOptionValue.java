package com.sfi.matrix.support.dto;

/** 字典选项取值：下拉渲染所需的精简字段。 */
public class DictOptionValue {

    private String code;
    private String label;
    private Integer sortNo;

    public DictOptionValue() {
    }

    public DictOptionValue(String code, String label, Integer sortNo) {
        this.code = code;
        this.label = label;
        this.sortNo = sortNo;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
}
