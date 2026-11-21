package com.sfi.matrix.iam.entity;

import com.sfi.scaffold.starter.data.jpa.entity.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 用户资料（展示/业务用，1:1 对应 {@link SysUser}）。
 *
 * <p>只放低频的"人"资料，不参与登录鉴权；与社交平台返回的原始资料
 * （在 {@link SysUserSocial}.extInfo）分离——本表是系统的权威资料。</p>
 *
 * @author matrix
 */
@Entity
@Table(name = "sys_user_profile")
public class SysUserProfile extends BaseEntity {

    /** 对应 sys_user.id（1:1，唯一） */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "nickname", length = 64)
    private String nickname;

    @Column(name = "avatar", length = 255)
    private String avatar;

    /** 性别：0未知 1男 2女 */
    @Column(name = "sex")
    private Integer sex = 0;

    @Column(name = "birthday", length = 10)
    private String birthday;

    @Column(name = "id_card", length = 32)
    private String idCard;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "remark", length = 500)
    private String remark;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getSex() {
        return sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
