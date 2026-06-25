package com.drivingschool.common.config;

import com.drivingschool.common.utils.JwtUtils;
import com.drivingschool.common.utils.RedisUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器。
 * <p>
 * 继承 OncePerRequestFilter，确保每个请求只被过滤一次。
 * 负责拦截每个 HTTP 请求，从请求头中提取 JWT 令牌，验证其有效性，
 * 并将用户信息设置到 Spring Security 的安全上下文中。
 * </p>
 * <p>
 * 处理流程：
 * 1. 从请求头 Authorization 字段提取 Bearer Token
 * 2. 验证 Token 的签名和有效期
 * 3. 检查 Token 类型是否为 access（访问令牌）
 * 4. 检查 Token 是否已被加入黑名单（登出场景）
 * 5. 解析用户信息并设置到 SecurityContext 中
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter implements Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    /** JWT 工具类，用于解析和验证令牌 */
    private final JwtUtils jwtUtils;

    /** Redis 工具类，用于检查令牌黑名单 */
    private final RedisUtils redisUtils;

    /**
     * 执行 JWT 认证过滤逻辑。
     * <p>
     * 每个请求都会经过此方法。如果请求携带有效的 JWT 令牌，
     * 则解析用户信息并设置到安全上下文中；否则直接放行，
     * 由后续的安全配置决定是否拒绝访问。
     * </p>
     *
     * @param request     HTTP 请求对象
     * @param response    HTTP 响应对象
     * @param filterChain 过滤器链，用于继续执行后续过滤器
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 步骤1：从请求头中提取 JWT 令牌
        String token = jwtUtils.extractToken(request);

        // 如果没有令牌，直接放行（由后续安全配置决定是否拒绝）
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 步骤2：验证令牌的签名和有效期
        if (!jwtUtils.validateToken(token)) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(response, "令牌无效或已过期");
            return;
        }

        // 步骤3：解析令牌声明，检查令牌类型
        // 只接受 access 类型的令牌（refresh 令牌不能用于接口认证）
        io.jsonwebtoken.Claims claims = jwtUtils.parseAllClaims(token);
        String tokenType = claims.get("type", String.class);
        if (!"access".equals(tokenType)) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(response, "令牌类型无效");
            return;
        }

        // 步骤4：检查令牌是否在黑名单中（用户登出后令牌会被加入黑名单）
        try {
            String blacklistKey = "token:blacklist:" + token;
            if (Boolean.TRUE.equals(redisUtils.hasKey(blacklistKey))) {
                SecurityContextHolder.clearContext();
                writeUnauthorized(response, "令牌已被注销，请重新登录");
                return;
            }
        } catch (Exception e) {
            // Redis 不可用时跳过黑名单检查，保证系统可用性
            log.warn("Redis check failed, skipping blacklist check: {}", e.getMessage());
        }

        // 步骤5：从令牌中解析用户信息并设置到安全上下文
        try {
            Object userIdObj = claims.get("userId");
            Long userId = userIdObj instanceof Number ? ((Number) userIdObj).longValue() : null;
            if (userId == null) {
                log.error("Invalid userId claim type: {}", userIdObj);
                writeUnauthorized(response, "令牌用户信息无效");
                return;
            }
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);

            // 构建权限对象，角色名需要加 "ROLE_" 前缀（Spring Security 约定）
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
            // 创建认证令牌：principal 为 userId，credentials 为 null（已认证无需密码），authorities 为角色
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.singletonList(authority));
            // 将用户名存入 details，方便后续日志记录使用
            authentication.setDetails(username);
            // 将认证信息设置到安全上下文中，后续可通过 SecurityContextHolder 获取当前用户
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // 同时保存到 request attribute，防止 SecurityContextHolderFilter 的 finally 块清除后丢失
            request.setAttribute("JWT_AUTHENTICATION", authentication);
        } catch (Exception e) {
            log.error("Failed to set SecurityContext from JWT token", e);
        }

        // 继续执行过滤器链中的下一个过滤器
        filterChain.doFilter(request, response);
    }

    /**
     * 直接写入401 JSON响应（绕过Spring Security的异常处理链）
     * 用于在过滤器阶段拦截无效请求，避免进入后续安全处理流程
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}");
    }
}
