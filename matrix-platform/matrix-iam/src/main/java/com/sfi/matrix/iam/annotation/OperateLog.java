package com.sfi.matrix.iam.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计标记：标注在需要落库审计的 controller 方法上（opt-in，不全局拦截）。
 *
 * @author lianghang
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperateLog {

    /** 模块名，如"用户管理" */
    String module() default "";

    /** 操作名，如"新增用户" */
    String operation() default "";
}
