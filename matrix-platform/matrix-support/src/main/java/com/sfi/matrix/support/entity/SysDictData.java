package com.sfi.matrix.support.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;
import com.sfi.scaffold.starter.data.jpa.logicdelete.JpaDeletedFilterAspect;
import com.sfi.scaffold.starter.tenant.aspect.JpaTenantFilterAspect;
import org.hibernate.annotations.Filter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 字典选项（明细表）。
 *
 * <p>type + code 唯一定位一个选项；type 相同的若干项构成一个下拉选项集，
 * 通过 dictType 关联 {@link SysDictType}。平台兜底按 code 合并租户与平台两份。</p>
 *
 * @author lianghang
 */
@Entity
@Table(name = "sys_dict_data")
@Filter(name = JpaTenantFilterAspect.FILTER_NAME, condition = "tenant_id = :tenantId")
@Filter(name = JpaDeletedFilterAspect.FILTER_NAME, condition = "deleted = false")
public class SysDictData extends BaseEntity {

    /** 逻辑删除标记，true=已删除；由字段名探测自动过滤 */
    @Column(name = "deleted")
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "tenant_id")
    private String tenantId;

    /** 所属字典类型编码 */
    @Column(name = "dict_type")
    private String dictType;

    /** 选项编码（存库值） */
    @Column(name = "dict_code")
    private String dictCode;

    /** 选项显示名 */
    @Column(name = "dict_label")
    private String dictLabel;

    @Column(name = "sort_no")
    private Integer sortNo;

    /** 状态：1启用 0停用 */
    @Column(name = "status")
    private Integer status = 1;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDictType() {
        return dictType;
    }

    public void setDictType(String dictType) {
        this.dictType = dictType;
    }

    public String getDictCode() {
        return dictCode;
    }

    public void setDictCode(String dictCode) {
        this.dictCode = dictCode;
    }

    public String getDictLabel() {
        return dictLabel;
    }

    public void setDictLabel(String dictLabel) {
        this.dictLabel = dictLabel;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
