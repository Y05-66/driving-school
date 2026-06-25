package com.drivingschool.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.drivingschool.common.result.R;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置类。
 * <p>
 * 负责配置整个应用的安全策略，包括：
 * - 禁用 CSRF（因为使用 JWT 无状态认证，不需要 CSRF 保护）
 * - 配置无状态会话管理（不使用 HttpSession）
 * - 定义 URL 访问权限规则（哪些接口需要认证、哪些可以公开访问）
 * - 配置认证失败和权限不足时的自定义响应
 * - 注册 JWT 认证过滤器
 * - 提供密码编码器和认证管理器
 * </p>
 */
@Configuration
@EnableWebSecurity      // 启用 Spring Security Web 安全功能
@EnableMethodSecurity   // 启用方法级别的安全注解（如 @PreAuthorize）
// TODO: 如果 AuthorizationFilter 拒绝已认证请求，可能需要检查此注解的影响
@RequiredArgsConstructor
public class SecurityConfig {

    /** JWT 认证过滤器，在用户名密码过滤器之前执行 */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 登录频率限制过滤器，防止暴力破解 */
    private final RateLimitFilter rateLimitFilter;

    /** Jackson 对象映射器，用于将响应对象序列化为 JSON */
    private final ObjectMapper objectMapper;

    /**
     * 配置安全过滤器链，定义核心安全策略。
     * <p>
     * 主要配置内容：
     * 1. 禁用 CSRF —— 前后端分离架构使用 JWT，无需 CSRF 令牌
     * 2. 无状态会话 —— 不创建 HttpSession，每次请求都通过 JWT 认证
     * 3. URL 权限规则 —— 登录/刷新/Swagger/头像等接口公开，其余需认证
     * 4. 自定义异常处理 —— 未认证返回 401，无权限返回 403
     * 5. 添加 JWT 过滤器 —— 在标准认证过滤器之前执行 JWT 解析
     * </p>
     *
     * @param http HttpSecurity 构建器
     * @return 配置完成的 SecurityFilterChain
     * @throws Exception 配置过程中可能抛出的异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF 防护（JWT 无状态认证不需要）
                .csrf(AbstractHttpConfigurer::disable)
                // 配置安全响应头
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())  // 禁止 iframe 嵌入，防止点击劫持
                        .contentTypeOptions(opt -> {})         // 设置 X-Content-Type-Options: nosniff
                )
                // 设置会话管理策略为无状态，不创建 HttpSession
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置 URL 访问权限
                .authorizeHttpRequests(auth -> auth
                        // 登录和刷新令牌接口允许匿名访问
                        .requestMatchers("/auth/login", "/auth/refresh").permitAll()
                        // 报名提交和状态查询允许匿名访问（小程序端）
                        .requestMatchers(HttpMethod.POST, "/registrations").permitAll()
                        .requestMatchers(HttpMethod.GET, "/registrations/status").permitAll()
                        // Swagger 文档接口允许匿名访问
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // 头像静态资源允许匿名访问
                        .requestMatchers("/avatars/**").permitAll()
                        // 允许所有 OPTIONS 预检请求（CORS 预检）
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                )
                // 配置异常处理
                .exceptionHandling(exception -> exception
                        // 未认证处理器：返回 401 状态码和 JSON 格式的错误信息
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(objectMapper.writeValueAsString(R.fail(401, "未认证，请先登录")));
                        })
                        // 权限不足处理器：返回 403 状态码和 JSON 格式的错误信息
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(objectMapper.writeValueAsString(R.fail(403, "无权限访问")));
                        })
                )
                // 将 JWT 认证过滤器添加到用户名密码认证过滤器之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 将频率限制过滤器添加到 JWT 过滤器之前
                .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class)
                // 在 AuthorizationFilter 之前恢复 JWT filter 保存的 SecurityContext
                // Spring Security 6 的 SecurityContextHolderFilter 在 filter chain 结束后会清除 ThreadLocal SecurityContext，
                // 导致 AuthorizationFilter 看不到 JWT filter 设置的认证信息。通过 request attribute 持久化解决。
                .addFilterBefore(new org.springframework.web.filter.OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain chain) throws jakarta.servlet.ServletException, java.io.IOException {
                        var savedAuth = request.getAttribute("JWT_AUTHENTICATION");
                        if (savedAuth instanceof org.springframework.security.core.Authentication) {
                            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication((org.springframework.security.core.Authentication) savedAuth);
                        }
                        chain.doFilter(request, response);
                    }
                }, org.springframework.security.web.access.intercept.AuthorizationFilter.class);

        return http.build();
    }

    /**
     * 配置密码编码器。
     * <p>
     * 使用 BCrypt 强哈希算法对密码进行加密和验证，
     * BCrypt 会自动生成随机盐值并嵌入到哈希结果中。
     * </p>
     *
     * @return BCryptPasswordEncoder 密码编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置认证管理器。
     * <p>
     * AuthenticationManager 是 Spring Security 认证的核心接口，
     * 用于处理认证请求。在登录接口中需要注入使用。
     * </p>
     *
     * @param config 认证配置，由 Spring 自动注入
     * @return AuthenticationManager 认证管理器实例
     * @throws Exception 获取认证管理器时可能抛出的异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
