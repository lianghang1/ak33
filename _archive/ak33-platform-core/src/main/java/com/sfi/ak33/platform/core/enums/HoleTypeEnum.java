package com.sfi.ak33.platform.core.enums;

/**
 * 穴位类型：单穴 / 多穴（双穴及以上）。
 *
 * <p>一个格位初始化为一个墓位，墓位含 1 个或多个穴位；穴位数是墓位的物理属性，
 * 影响定价与后续合葬规则。</p>
 *
 * @author ak33
 */
public enum HoleTypeEnum {

    SINGLE(1, "单穴"),
    DOUBLE(2, "双穴"),
    MULTI(3, "多穴");

    private final int holes;
    private final String desc;

    HoleTypeEnum(int holes, String desc) {
        this.holes = holes;
        this.desc = desc;
    }

    public int getHoles() {
        return holes;
    }

    public String getDesc() {
        return desc;
    }

    public static HoleTypeEnum of(int holes) {
        for (HoleTypeEnum e : values()) {
            if (e.holes == holes) {
                return e;
            }
        }
        return null;
    }
}
