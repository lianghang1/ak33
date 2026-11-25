package com.sfi.matrix.cemetery.config;

import com.sfi.matrix.cemetery.strategy.TombCodeStrategy;
import com.sfi.matrix.cemetery.strategy.ZoneRowNoStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 编号策略装配：默认注册"区-排-号"策略。客户注册任意自定义 {@link TombCodeStrategy}
 * Bean 时默认策略自动退让，编号规则可插拔由此落地。
 *
 * @author lianghang
 */
@Configuration
public class CodeStrategyConfig {

    @Bean
    @ConditionalOnMissingBean(TombCodeStrategy.class)
    public TombCodeStrategy tombCodeStrategy() {
        return new ZoneRowNoStrategy();
    }
}
