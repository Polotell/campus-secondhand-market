package com.campus.market.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置（CORS）
 * <p>
 * 项目有两个前端工程：
 * <ul>
 *   <li>{@code frontend-user}  运行在 http://localhost:5173</li>
 *   <li>{@code frontend-admin} 运行在 http://localhost:5174</li>
 * </ul>
 * 均不同源，需要统一开启 CORS。这里用 {@link CorsFilter} 方式是因为它优先级最高，
 * 保证在 Spring Security / 拦截器 前就完成跨域预检响应。
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.addAllowedOriginPattern("*");        // 开发期允许所有来源；生产请改成白名单
        cfg.addAllowedHeader("*");
        cfg.addAllowedMethod("*");
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return new CorsFilter(source);
    }
}
