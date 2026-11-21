package com.sfi.matrix.support.dict.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;
import com.sfi.scaffold.starter.tenant.aspect.JpaTenantFilterAspect;
import org.hibernate.annotations.Filter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 数据字典项。
 *
 * <p>type + code 唯一定位一个字典项，type 相同的若干项构成一个下拉选项集。</p>
 *
 * @author matrix
 */
@Entity
@Table(name = "sys_dict")
@Filter(name = JpaTenantFilterAspect.FILTER_NAME, condition = "tenant_id = :tenantId")
public class SysDict extends BaseEntity {

    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @Column(name = "dict_type", nullable = false, length = 64)
    private String dictType;

    @Column(name = "dict_code", nullable = false, length = 64)
    private String dictCode;

    @Column(name = "dict_label", nullable = false, length = 128)
    private String dictLabel;

    @Column(name = "sort_no")
    private Integer sortNo;

    /** 状态：1启用 0停用 */
    @Column(name = "status", nullable = false)
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
}
