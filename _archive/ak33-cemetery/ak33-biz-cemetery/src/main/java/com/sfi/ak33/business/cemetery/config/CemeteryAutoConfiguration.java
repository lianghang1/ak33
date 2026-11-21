package com.sfi.ak33.business.cemetery.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 公墓业务模块自动配置：引入 ak33-biz-cemetery jar 即自动生效。
 *
 * @author ak33
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.sfi.ak33.business.cemetery")
@EntityScan(basePackages = "com.sfi.ak33.business.cemetery.entity")
@EnableJpaRepositories(basePackages = "com.sfi.ak33.business.cemetery.repository")
public class CemeteryAutoConfiguration {
}
