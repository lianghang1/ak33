package com.sfi.matrix.cemetery;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 公墓业务自动配置：扫整个 cemetery 包，新增业务子包无需改这里。
 *
 * <p>宿主 CemeteryApplication 不带包扫描注解，本类负责把公墓业务的
 * Controller/Service/Repository/Entity 纳入 Spring 管理。</p>
 *
 * @author lianghang
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.sfi.matrix.cemetery")
@EntityScan(basePackages = "com.sfi.matrix.cemetery")
@EnableJpaRepositories(basePackages = "com.sfi.matrix.cemetery")
public class CemeteryAutoConfiguration {
}
