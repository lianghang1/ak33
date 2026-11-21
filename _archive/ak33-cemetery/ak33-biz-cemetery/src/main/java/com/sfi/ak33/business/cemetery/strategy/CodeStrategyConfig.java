package com.sfi.ak33.business.cemetery.strategy;

import com.sfi.ak33.platform.core.strategy.TombCodeStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 编号策略装配：默认注册"区-排-号"策略。
 *
 * <p>采用脚手架同款范式（{@code @Configuration} + {@code @Bean} +
 * {@code @ConditionalOnMissingBean}）：当客户在容器中注册了任意自定义
 * {@link TombCodeStrategy} Bean 时，本默认策略自动退让，无需改动任何业务代码——
 * 编号规则可插拔由此落地。</p>
 *
 * @author ak33
 */
@Configuration
public class CodeStrategyConfig {

    @Bean
    @ConditionalOnMissingBean(TombCodeStrategy.class)
    public TombCodeStrategy tombCodeStrategy() {
        return new ZoneRowNoStrategy();
    }
}
