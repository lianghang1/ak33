package com.sfi.ak33.platform.settle.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 结算模块自动配置：引入 ak33-platform-settle jar 即自动生效。
 *
 * @author ak33
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.sfi.ak33.platform.settle")
@EntityScan(basePackages = "com.sfi.ak33.platform.settle.entity")
@EnableJpaRepositories(basePackages = "com.sfi.ak33.platform.settle.repository")
public class SettleAutoConfiguration {
}
