package com.sfi.matrix.cemetery.strategy;

/**
 * 墓位编号策略接口（可插拔）。客户实现本接口并注册为 Spring Bean 即可替换默认规则，
 * 无需改动业务代码。
 *
 * @author matrix
 */
public interface TombCodeStrategy {

    /** 策略标识，用于快照与日志溯源 */
    default String name() {
        return this.getClass().getSimpleName();
    }

    /** 根据上下文生成编号三元组（编号/基础路径/全路径）。 */
    CodeResult generate(CodeContext ctx);
}
