package com.sfi.matrix.iam.config;

import com.sfi.scaffold.starter.security.config.SecurityProperties;
import com.sfi.scaffold.starter.security.policy.PasswordPolicyValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * IAM 模块自动配置：引入 matrix-iam jar 即自动生效，app 无需任何手动扫描配置。
 *
 * <p>各声明只覆盖本模块包，不碰 com.sfi.scaffold（框架走自己的自动配置）。</p>
 *
 * @author lianghang
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.sfi.matrix.iam")
@EntityScan(basePackages = {"com.sfi.matrix.iam.entity", "com.sfi.matrix.iam.audit.entity"})
@EnableJpaRepositories(basePackages = {"com.sfi.matrix.iam.repository", "com.sfi.matrix.iam.audit.repository"})
public class IamAutoConfiguration {

    /**
     * scaffold 只提供 PasswordPolicyValidator 类、未自动装配，业务侧补一个 Bean。
     * 密码强度规则仍由 scaffold 的 SecurityProperties.password-policy 驱动。
     */
    @Bean
    @ConditionalOnMissingBean(PasswordPolicyValidator.class)
    public PasswordPolicyValidator passwordPolicyValidator(SecurityProperties securityProperties) {
        return new PasswordPolicyValidator(securityProperties.getPasswordPolicy());
    }
}
