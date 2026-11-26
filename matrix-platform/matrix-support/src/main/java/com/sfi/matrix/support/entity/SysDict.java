package com.sfi.matrix.support.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;
import com.sfi.scaffold.starter.data.jpa.logicdelete.JpaDeletedFilterAspect;
import com.sfi.scaffold.starter.tenant.aspect.JpaTenantFilterAspect;
import org.hibernate.annotations.Filter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 字典（主表）。
 *
 * <p>描述"有哪些字典"，如 sys_normal_disable（系统正常/停用）。
 * 具体选项在 {@link SysDictOption} 中按 dictId 关联。</p>
 *
 * @author lianghang
 */
@Entity
@Table(name = "sys_dict")
@Filter(name = JpaTenantFilterAspect.FILTER_NAME, condition = "tenant_id = :tenantId")
@Filter(name = JpaDeletedFilterAspect.FILTER_NAME, condition = "deleted = false")
public class SysDict extends BaseEntity {

    /** 逻辑删除标记，true=已删除；由字段名探测自动过滤 */
    @Column(name = "deleted")
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "tenant_id")
    private String tenantId;

    /** 字典编码 */
    @Column(name = "dict_code")
    private String dictCode;

    /** 字典名称 */
    @Column(name = "dict_name")
    private String dictName;

    @Column(name = "remark")
    private String remark;

    /** 状态：1启用 0停用 */
    @Column(name = "status")
    private Integer status = 1;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDictCode() {
        return dictCode;
    }

    public void setDictCode(String dictCode) {
        this.dictCode = dictCode;
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
