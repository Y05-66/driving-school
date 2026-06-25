package com.drivingschool.common.exception;

import com.drivingschool.common.result.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * <p>
 * 使用 @RestControllerAdvice 统一拦截和处理 Controller 层抛出的异常，
 * 将各种异常转换为统一的 JSON 响应格式（R 对象），避免将异常堆栈信息暴露给前端。
 * </p>
 * <p>
 * 处理的异常类型（按优先级排列）：
 * 1. BusinessException - 业务异常（自定义业务错误）
 * 2. MethodArgumentNotValidException - 参数校验异常（@Valid 注解触发）
 * 3. BindException - 参数绑定异常（表单数据绑定失败）
 * 4. BadCredentialsException - 认证失败（用户名或密码错误）
 * 5. AccessDeniedException - 权限不足（已认证但无权访问）
 * 6. HttpRequestMethodNotSupportedException - 请求方法不支持
 * 7. NoHandlerFoundException - 接口不存在（404）
 * 8. IllegalArgumentException - 非法参数异常
 * 9. Exception - 兜底处理所有未被上述处理器捕获的异常
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常。
     * <p>
     * 业务异常是可预期的，使用 warn 级别记录日志，
     * 返回业务异常中定义的错误码和错误信息。
     * </p>
     *
     * @param e       业务异常
     * @param request HTTP 请求对象，用于记录请求路径
     * @return 包含错误码和错误信息的统一响应对象
     */
    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: {} - {}", request.getRequestURI(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理请求体参数校验异常（@Valid + @RequestBody 触发）。
     * <p>
     * 当使用 @Valid 注解校验请求体参数失败时抛出此异常，
     * 提取所有字段的校验错误信息，用逗号拼接后返回。
     * </p>
     *
     * @param e 参数校验异常
     * @return 包含 400 错误码和校验错误信息的统一响应对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return R.fail(400, message);
    }

    /**
     * 处理表单参数绑定异常（@Valid + 表单参数触发）。
     * <p>
     * 当表单数据绑定到对象失败时抛出此异常，
     * 提取所有字段的错误信息，用逗号拼接后返回。
     * </p>
     *
     * @param e 参数绑定异常
     * @return 包含 400 错误码和绑定错误信息的统一响应对象
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return R.fail(400, message);
    }

    /**
     * 处理认证失败异常（用户名或密码错误）。
     * <p>
     * 由 Spring Security 的 AuthenticationManager 在认证失败时抛出。
     * </p>
     *
     * @param e 认证失败异常
     * @return 包含 401 错误码和统一错误信息的响应对象
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<?> handleBadCredentialsException(BadCredentialsException e) {
        return R.fail(401, "用户名或密码错误");
    }

    /**
     * 处理权限不足异常。
     * <p>
     * 用户已通过认证，但访问的资源超出其权限范围时抛出此异常。
     * </p>
     *
     * @param e 权限不足异常
     * @return 包含 403 错误码和统一错误信息的响应对象
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<?> handleAccessDeniedException(AccessDeniedException e) {
        return R.fail(403, "无权限访问");
    }

    /**
     * 处理 HTTP 请求方法不支持异常。
     * <p>
     * 例如对只支持 GET 的接口发送 POST 请求时触发。
     * </p>
     *
     * @param e 请求方法不支持异常
     * @return 包含 405 错误码和错误信息的响应对象
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public R<?> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return R.fail(405, "不支持的请求方法: " + e.getMethod());
    }

    /**
     * 处理接口不存在异常（404）。
     * <p>
     * 当请求的 URL 没有对应的 Handler 时触发。
     * 需要在配置中设置 throw-exception-if-no-handler-found 为 true 才能捕获。
     * </p>
     *
     * @param e 未找到处理器异常
     * @return 包含 404 错误码和统一错误信息的响应对象
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<?> handleNoHandlerFound(NoHandlerFoundException e) {
        return R.fail(404, "接口不存在");
    }

    /**
     * 处理非法参数异常。
     * <p>
     * 当方法接收到不合法的参数时抛出，使用 warn 级别记录日志。
     * </p>
     *
     * @param e 非法参数异常
     * @return 包含 400 错误码和异常信息的响应对象
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数错误: {}", e.getMessage());
        return R.fail(400, e.getMessage());
    }

    /**
     * 处理请求体解析异常（JSON格式错误或类型不匹配）。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return R.fail(400, "请求参数格式错误");
    }

    /**
     * 处理缺少必填请求参数异常。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getMessage());
        return R.fail(400, "缺少必填参数: " + e.getParameterName());
    }

    /**
     * 兜底异常处理器，处理所有未被上述处理器捕获的异常。
     * <p>
     * 使用 error 级别记录完整的异常堆栈（方便排查系统 Bug），
     * 但返回给前端的是统一的友好错误信息，不暴露系统内部细节。
     * </p>
     *
     * @param e       未知异常
     * @param request HTTP 请求对象
     * @return 包含 500 错误码和统一错误信息的响应对象
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<?> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: {} - {}", request.getRequestURI(), e.getMessage(), e);
        return R.fail(500, "系统内部错误，请联系管理员");
    }
}
