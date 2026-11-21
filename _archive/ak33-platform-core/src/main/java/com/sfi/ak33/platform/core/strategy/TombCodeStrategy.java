package com.sfi.ak33.platform.core.strategy;

/**
 * 墓位编号策略接口（可插拔）。
 *
 * <p>编号规则因客户而异，故抽象为策略：实现本接口并注册为 Spring Bean 即可替换默认规则，
 * 无需改动业务代码。接口放在 common 层，默认实现放 service 层，避免领域层反向依赖。</p>
 *
 * <p>默认策略见 service 层 ZoneRowNoStrategy（区-排-号，位数补零）。</p>
 *
 * @author ak33
 */
public interface TombCodeStrategy {

    /** 策略标识，用于配置与日志溯源 */
    default String name() {
        return this.getClass().getSimpleName();
    }

    /**
     * 根据上下文生成墓位标识三元组。
     *
     * @param ctx 编号上下文
     * @return 编号/基础路径/全路径
     */
    CodeResult generate(CodeContext ctx);
}
