package com.drivingschool.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.drivingschool.common.result.R;
import com.drivingschool.common.utils.RedisUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 登录接口频率限制过滤器
 * <p>
 * 对 /auth/login 接口进行IP级别的频率限制，防止暴力破解。
 * 每个IP每分钟最多允许10次登录尝试，超出后返回429错误。
 * 使用 Redis 实现分布式限流，支持多实例部署。
 * </p>
 *
 * @author drivingschool
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter implements Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }

    /** JSON 序列化器，用于将错误响应对象转换为 JSON 字符串 */
    private final ObjectMapper objectMapper;
    /** Redis 工具类，用于实现分布式频率限制计数器 */
    private final RedisUtils redisUtils;

    /** 每分钟最大请求数 */
    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    /** Redis key 前缀 */
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    /**
     * 执行频率限制过滤逻辑。
     * <p>
     * 对 /auth/login 和 /registrations 的 POST 请求进行 IP 级别限流，
     * 使用 Redis INCR 实现原子计数器，支持多实例部署。
     * </p>
     *
     * @param request     HTTP 请求对象
     * @param response    HTTP 响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // 对登录和注册接口进行频率限制
        boolean isLogin = uri.endsWith("/auth/login") && "POST".equalsIgnoreCase(request.getMethod());
        boolean isRegistration = uri.endsWith("/registrations") && "POST".equalsIgnoreCase(request.getMethod());
        if (!isLogin && !isRegistration) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String redisKey = RATE_LIMIT_PREFIX + clientIp;

        try {
            // 使用 Redis INCR 实现滑动窗口限流
            Long count = redisUtils.increment(redisKey);
            if (count != null && count == 1) {
                // 首次请求，设置 key 过期时间为 1 分钟
                redisUtils.expire(redisKey, 1, TimeUnit.MINUTES);
            }

            if (count != null && count > MAX_REQUESTS_PER_MINUTE) {
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(
                        R.fail(429, "登录尝试过于频繁，请1分钟后再试")));
                return;
            }
        } catch (Exception e) {
            // Redis 不可用时放行，保证系统可用性
            log.warn("Redis rate limit check failed, allowing request: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 获取客户端真实IP地址。
     * <p>
     * 优先使用 X-Real-IP（由可信反向代理设置，客户端不可伪造），
     * 不使用 X-Forwarded-For（客户端可伪造，导致频率限制被绕过），
     * 最后使用 TCP 连接的 remoteAddr。
     * </p>
     *
     * @param request HTTP 请求对象
     * @return 客户端 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        // 优先使用 Nginx 等反向代理设置的 X-Real-IP（需要在代理层配置 proxy_set_header X-Real-IP $remote_addr）
        String ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        // 不信任 X-Forwarded-For（客户端可伪造，绕过频率限制）
        // 直接使用 TCP 连接的 remoteAddr
        return request.getRemoteAddr();
    }
}
