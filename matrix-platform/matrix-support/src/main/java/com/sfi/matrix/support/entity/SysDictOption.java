package com.sfi.matrix.support.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;
import com.sfi.scaffold.starter.data.jpa.logicdelete.JpaDeletedFilterAspect;
import com.sfi.scaffold.starter.tenant.aspect.JpaTenantFilterAspect;
import org.hibernate.annotations.Filter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 字典选项（子表）。
 *
 * <p>通过 dictId 关联 {@link SysDict}；同一字典下 optionCode 唯一定位一个选项，
 * 平台兜底按 optionCode 合并租户与平台两份。</p>
 *
 * @author lianghang
 */
@Entity
@Table(name = "sys_dict_option")
@Filter(name = JpaTenantFilterAspect.FILTER_NAME, condition = "tenant_id = :tenantId")
@Filter(name = JpaDeletedFilterAspect.FILTER_NAME, condition = "deleted = false")
public class SysDictOption extends BaseEntity {

    /** 逻辑删除标记，true=已删除；由字段名探测自动过滤 */
    @Column(name = "deleted")
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "tenant_id")
    private String tenantId;

    /** 所属字典ID，对应 sys_dict.id */
    @Column(name = "dict_id")
    private Long dictId;

    /** 选项编码 */
    @Column(name = "option_code")
    private String optionCode;

    /** 选项显示名 */
    @Column(name = "option_label")
    private String optionLabel;

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

    public Long getDictId() {
        return dictId;
    }

    public void setDictId(Long dictId) {
        this.dictId = dictId;
    }

    public String getOptionCode() {
        return optionCode;
    }

    public void setOptionCode(String optionCode) {
        this.optionCode = optionCode;
    }

    public String getOptionLabel() {
        return optionLabel;
    }

    public void setOptionLabel(String optionLabel) {
        this.optionLabel = optionLabel;
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
