package com.drivingschool.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域资源共享（CORS）配置类。
 * <p>
 * 前后端分离架构中，前端和后端通常部署在不同的域名或端口下，
 * 浏览器的同源策略会阻止跨域请求。此配置通过注册全局 CORS 过滤器，
 * 允许前端应用跨域访问后端 API。
 * </p>
 * <p>
 * 配置说明：
 * - 允许携带凭证（Cookie、Authorization 头等）
 * - 允许所有来源的请求（生产环境建议限制为具体域名）
 * - 允许所有请求头
 * - 允许所有 HTTP 方法（GET、POST、PUT、DELETE 等）
 * - 预检请求缓存时间为 3600 秒（1 小时）
 * </p>
 */
@Configuration
public class CorsConfig {

    /** 允许的跨域来源，可通过 application.yml 的 cors.allowed-origins 配置，默认仅允许本地开发 */
    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    /**
     * 注册全局 CORS 过滤器。
     * <p>
     * 对所有 URL 路径生效，统一处理跨域请求。
     * 生产环境请在 application-prod.yml 中配置 cors.allowed-origins 为具体域名。
     * </p>
     *
     * @return CorsFilter 跨域过滤器实例
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许携带凭证（如 Cookie、Authorization 请求头）
        config.setAllowCredentials(true);
        // 允许的来源模式，支持配置多个（逗号分隔）或通配符
        for (String origin : allowedOrigins.split(",")) {
            config.addAllowedOriginPattern(origin.trim());
        }
        // 允许所有请求头
        config.addAllowedHeader("*");
        // 允许所有 HTTP 方法
        config.addAllowedMethod("*");
        // 预检请求（OPTIONS）的缓存时间，单位为秒
        config.setMaxAge(3600L);

        // 基于 URL 的 CORS 配置源，对所有路径应用上述配置
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
