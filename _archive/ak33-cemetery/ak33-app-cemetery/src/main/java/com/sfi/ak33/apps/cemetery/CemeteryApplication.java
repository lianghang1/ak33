package com.sfi.ak33.apps.cemetery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 公墓系统启动类（可独立部署的 app）。
 *
 * <p>关键：本类没有任何 @ComponentScan/@EntityScan/@EnableJpaRepositories——
 * 因为各能力模块（iam/settle/cemetery）通过自己的自动配置声明了各自包的扫描。
 * app 只需在 pom 引入需要的 jar，能力即自动生效——这就是"按需组装"。</p>
 *
 * @author ak33
 */
@SpringBootApplication
public class CemeteryApplication {

    public static void main(String[] args) {
        SpringApplication.run(CemeteryApplication.class, args);
    }
}
