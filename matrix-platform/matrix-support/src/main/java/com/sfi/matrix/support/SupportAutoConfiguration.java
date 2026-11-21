package com.sfi.matrix.support;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * support 模块自动配置：扫整个 support 包，未来新增的轻量功能包（如 file）无需改这里。
 *
 * @author matrix
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.sfi.matrix.support")
@EntityScan(basePackages = "com.sfi.matrix.support")
@EnableJpaRepositories(basePackages = "com.sfi.matrix.support")
public class SupportAutoConfiguration {
}
