package com.sfi.matrix.cemetery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 公墓系统启动类（可独立部署的 app）。
 *
 * <p>本类不带任何 @ComponentScan / @EntityScan / @EnableJpaRepositories——
 * 各能力模块（matrix-iam / matrix-support）通过自己的自动配置声明包扫描，
 * app 只需在 pom 引入对应 jar，能力即自动生效（按需组装）。</p>
 *
 * <p>当前阶段公墓核心业务（墓区 / 墓位 / 寄存）尚未实现，本 app 提供平台基座：
 * 登录、租户、用户、角色、菜单、数据权限、字典参数、审计日志。</p>
 *
 * @author matrix
 */
@SpringBootApplication
public class CemeteryApplication {

    public static void main(String[] args) {
        SpringApplication.run(CemeteryApplication.class, args);
    }
}
