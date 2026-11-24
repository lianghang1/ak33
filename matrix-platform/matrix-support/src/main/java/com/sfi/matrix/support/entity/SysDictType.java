package com.sfi.matrix.support.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;
import com.sfi.scaffold.starter.data.jpa.logicdelete.JpaDeletedFilterAspect;
import com.sfi.scaffold.starter.tenant.aspect.JpaTenantFilterAspect;
import org.hibernate.annotations.Filter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 字典类型（主表）。
 *
 * <p>描述"有哪些字典"，如 sys_normal_disable（系统正常/停用）。
 * 具体选项在 {@link SysDictData} 中按 dictType 关联。类型本身可独立命名、
 * 备注、整组启停，这是单表设计无法表达的。</p>
 *
 * @author matrix
 */
@Entity
@Table(name = "sys_dict_type")
@Filter(name = JpaTenantFilterAspect.FILTER_NAME, condition = "tenant_id = :tenantId")
@Filter(name = JpaDeletedFilterAspect.FILTER_NAME, condition = "deleted = false")
public class SysDictType extends BaseEntity {

    /** 逻辑删除标记，true=已删除；由字段名探测自动过滤 */
    @Column(name = "deleted")
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "tenant_id")
    private String tenantId;

    /** 字典类型编码，如 sys_normal_disable */
    @Column(name = "dict_type")
    private String dictType;

    /** 字典类型名称，如"系统正常/停用" */
    @Column(name = "dict_name")
    private String dictName;

    @Column(name = "remark")
    private String remark;

    /** 状态：1启用 0停用（整组选项的开关） */
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

    public String getDictName() {
        return dictName;
    }

    public void setDictName(String dictName) {
        this.dictName = dictName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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
