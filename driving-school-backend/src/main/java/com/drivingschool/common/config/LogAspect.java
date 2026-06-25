package com.drivingschool.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.drivingschool.system.entity.SysLog;
import com.drivingschool.system.service.SysLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志切面类。
 * <p>
 * 使用 AOP（面向切面编程）自动记录用户操作日志。
 * 通过拦截带有 Swagger @Operation 注解的 Controller 方法，
 * 自动记录操作人、请求路径、请求方法、客户端 IP、请求参数和执行耗时等信息。
 * </p>
 * <p>
 * 切入点规则：所有标注了 @Operation 注解的方法（即 Swagger 文档化的 API 接口）。
 * 使用环绕通知（@Around），在方法执行前后分别采集信息。
 * </p>
 */
@Slf4j
@Aspect      // 声明这是一个切面类
@Component   // 注册为 Spring Bean
@RequiredArgsConstructor
public class LogAspect {

    /** 操作日志服务，用于持久化日志记录 */
    private final SysLogService logService;

    /** Jackson 对象映射器，用于将方法参数序列化为 JSON 字符串 */
    private final ObjectMapper objectMapper;

    /**
     * 定义切入点：匹配所有标注了 @Operation 注解的方法。
     * <p>
     * @Operation 是 Swagger/OpenAPI 3 的注解，用于描述接口功能。
     * 只有添加了此注解的 Controller 方法才会被记录操作日志。
     * </p>
     */
    @Pointcut("@annotation(io.swagger.v3.oas.annotations.Operation)")
    public void logPointcut() {}

    /**
     * 环绕通知：在目标方法执行前后记录操作日志。
     * <p>
     * 处理流程：
     * 1. 记录开始时间
     * 2. 从安全上下文中获取当前操作用户信息
     * 3. 从请求上下文中获取 HTTP 请求信息（方法、URI、IP）
     * 4. 获取被拦截方法的名称和参数
     * 5. 执行目标方法
     * 6. 计算执行耗时
     * 7. 异步保存操作日志到数据库
     * </p>
     *
     * @param joinPoint 连接点，包含被拦截方法的信息
     * @return 目标方法的执行结果
     * @throws Throwable 目标方法可能抛出的异常
     */
    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 记录方法执行开始时间
        long startTime = System.currentTimeMillis();

        SysLog sysLog = new SysLog();
        try {
            // 从 Spring Security 安全上下文中获取当前认证用户信息
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Long userId) {
                // 设置操作人 ID
                sysLog.setUserId(userId);
                // 设置操作人用户名（存储在 details 中）
                sysLog.setUsername(auth.getDetails() != null ? auth.getDetails().toString() : "");
            }

            // 从请求上下文中获取 HTTP 请求信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                // 记录请求方法和 URI，例如 "GET /api/students"
                sysLog.setMethod(request.getMethod() + " " + request.getRequestURI());
                // 记录客户端 IP 地址
                sysLog.setIp(getClientIp(request));
            }

            // 记录被调用方法的名称
            sysLog.setOperation(joinPoint.getSignature().getName());
            // 过滤掉不可序列化的参数（HttpServletRequest、HttpServletResponse、Authentication）
            Object[] args = joinPoint.getArgs();
            Object[] serializableArgs = java.util.Arrays.stream(args)
                    .filter(a -> !(a instanceof jakarta.servlet.http.HttpServletRequest)
                            && !(a instanceof jakarta.servlet.http.HttpServletResponse)
                            && !(a instanceof org.springframework.security.core.Authentication))
                    .toArray();
            // 将方法参数序列化为 JSON 字符串
            sysLog.setParams(objectMapper.writeValueAsString(serializableArgs));
        } catch (Exception e) {
            log.warn("记录日志参数异常", e);
        }

        // 执行目标方法
        Object result = joinPoint.proceed();

        // 计算方法执行耗时（毫秒）
        sysLog.setDuration(System.currentTimeMillis() - startTime);
        // 保存操作日志到数据库
        try {
            logService.save(sysLog);
        } catch (Exception e) {
            // 日志保存失败不影响业务流程
            log.warn("保存操作日志异常", e);
        }

        return result;
    }

    /**
     * 获取客户端真实 IP 地址。
     * <p>
     * 按优先级依次尝试从以下请求头中获取：
     * 1. X-Forwarded-For：经过代理服务器时记录的真实 IP（可能包含多个 IP，取第一个）
     * 2. X-Real-IP：Nginx 等反向代理设置的真实 IP
     * 3. getRemoteAddr：直接连接的客户端 IP（经过多层代理后可能不是真实 IP）
     * </p>
     *
     * @param request HTTP 请求对象
     * @return 客户端 IP 地址字符串
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个 IP（经过多层代理），取第一个即为真实客户端 IP
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
