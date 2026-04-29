package com.campus.market;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园二手交易平台 主启动类
 * <p>
 * 使用到的核心注解说明（答辩用）：
 * <ul>
 *   <li>{@code @SpringBootApplication}: 组合注解，等价于 {@code @Configuration + @EnableAutoConfiguration + @ComponentScan}，
 *       Spring Boot 根据类路径下的依赖（如 mybatis-plus-boot-starter）自动完成装配，体现"约定大于配置"。</li>
 *   <li>{@code @MapperScan}: 扫描 MyBatis Mapper 接口包，Spring 在启动时基于 JDK 动态代理为这些接口生成实现类并注入 IoC 容器。</li>
 *   <li>{@code @EnableScheduling}: 开启定时任务，用于"7 天自动确认收货"、"24h 后自动结算"、"封禁到期自动解封"。</li>
 *   <li>{@code @EnableAsync}: 开启异步方法支持，日志切面、消息通知可异步执行。</li>
 * </ul>
 */
@SpringBootApplication
@MapperScan("com.campus.market.mapper")
@EnableScheduling
@EnableAsync
public class CampusMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusMarketApplication.class, args);
    }
}
