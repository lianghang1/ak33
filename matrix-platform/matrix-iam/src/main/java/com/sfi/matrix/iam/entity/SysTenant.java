package com.sfi.matrix.iam.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;
import com.sfi.scaffold.starter.data.jpa.logicdelete.JpaDeletedFilterAspect;
import org.hibernate.annotations.Filter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDate;

/**
 * 租户（IAM 通用能力）。
 *
 * <p>租户表是平台级元数据，不加租户 @Filter——否则登录还没定位到租户时无从过滤。
 * packageId 指向该租户开通的套餐，决定租户能用哪些菜单；expireTime 到期后停用。</p>
 *
 * @author lianghang
 */
@Entity
@Table(name = "sys_tenant")
@Filter(name = JpaDeletedFilterAspect.FILTER_NAME, condition = "deleted = false")
public class SysTenant extends BaseEntity {

    /** 逻辑删除标记，true=已删除；由字段名探测自动过滤 */
    @Column(name = "deleted")
    private Boolean deleted = Boolean.FALSE;

    @Column(name = "tenant_code")
    private String tenantCode;

    @Column(name = "tenant_name")
    private String tenantName;

    /** 开通的套餐 ID（对应 sys_tenant_package.id） */
    @Column(name = "package_id")
    private Long packageId;

    /** 到期日，null 表示不过期 */
    @Column(name = "expire_time")
    private LocalDate expireTime;

    /** 状态：1启用 0停用 */
    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "remark")
    private String remark;

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public LocalDate getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDate expireTime) {
        this.expireTime = expireTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
