package com.campus.market.config;

import com.campus.market.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC 主配置类
 * <p>
 * 做两件事：
 * <ol>
 *   <li>将本地上传目录 {@code file.upload-dir} 映射到 URL {@code /uploads/**}，
 *       前端即可通过 {@code http://localhost:8080/api/uploads/xxx.jpg} 访问已上传图片。
 *       （报告 7.1 明确提到"图片上传路径映射"常见坑，这里提前规避）</li>
 *   <li>注册全局登录拦截器 {@link LoginInterceptor}，自动放行白名单（登录、注册、验证码等）。</li>
 * </ol>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.access-prefix}")
    private String accessPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 示例：file:D:/campus-market-uploads/xxx.jpg → http://xxx/api/uploads/xxx.jpg
        registry.addResourceHandler(accessPrefix + "**")
                .addResourceLocations("file:" + normalizeDir(uploadDir));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> whiteList = List.of(
                "/auth/login",
                "/auth/register/**",
                "/captcha",
                "/file/upload",     // 注册商家时需要先传营业执照/身份证
                "/hello/ping",      // 仅放 ping 匿名；/hello/me、/hello/admin 需走拦截器解析 Token
                "/uploads/**",
                "/home/**",
                "/products",        // 商品列表允许游客浏览
                "/products/**",     // 详情 + 评价列表等允许游客浏览
                "/categories",
                "/shops/*",
                "/error"
        );
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(whiteList);
    }

    private static String normalizeDir(String dir) {
        return dir.endsWith("/") || dir.endsWith("\\") ? dir : dir + "/";
    }
}
