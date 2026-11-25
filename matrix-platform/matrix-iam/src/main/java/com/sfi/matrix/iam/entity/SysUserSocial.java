package com.sfi.matrix.iam.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;
import com.sfi.scaffold.starter.data.jpa.logicdelete.JpaDeletedFilterAspect;
import org.hibernate.annotations.Filter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 用户社交绑定（1:N：一个用户可绑多个第三方身份）。
 *
 * <p>只存绑定关系 + 第三方原始标识，不存任何资料快照（资料在
 * {@link SysUserProfile}）。openId 是平台内主标识（唯一键 provider+openId），
 * unionId 是跨应用统一标识（微信等有，其他平台可空），extInfo 兜住其余
 * 所有第三方返回字段——加新平台零改表。</p>
 *
 * @author lianghang
 */
@Entity
@Table(name = "sys_user_social")
@Filter(name = JpaDeletedFilterAspect.FILTER_NAME, condition = "deleted = false")
public class SysUserSocial extends BaseEntity {

    /** 逻辑删除标记，true=已删除；由字段名探测自动过滤 */
    @Column(name = "deleted")
    private Boolean deleted = Boolean.FALSE;

    /** 对应 sys_user.id */
    @Column(name = "user_id")
    private Long userId;

    /** 平台编码：wechat-mp / wechat-mini / alipay / ... */
    @Column(name = "provider")
    private String provider;

    /** 平台内主唯一标识（openId） */
    @Column(name = "open_id")
    private String openId;

    /** 跨应用统一标识（unionId，可空） */
    @Column(name = "union_id")
    private String unionId;

    /** 第三方原始返回快照（JSON），兜住不确定字段 */
    @Column(name = "ext_info")
    private String extInfo;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public String getUnionId() {
        return unionId;
    }

    public void setUnionId(String unionId) {
        this.unionId = unionId;
    }

    public String getExtInfo() {
        return extInfo;
    }

    public void setExtInfo(String extInfo) {
        this.extInfo = extInfo;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
