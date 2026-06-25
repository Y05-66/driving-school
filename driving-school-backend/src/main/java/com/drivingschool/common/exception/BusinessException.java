package com.drivingschool.common.exception;

import lombok.Getter;

/**
 * 业务异常类。
 * <p>
 * 用于表示业务逻辑处理过程中发生的异常，如：
 * - 数据校验失败（如用户名已存在、手机号已注册）
 * - 业务规则违反（如学员状态不允许报名、课程时间冲突）
 * - 数据不存在（如查询的记录不存在）
 * </p>
 * <p>
 * 与系统异常（NullPointerException 等）不同，业务异常是可预期的，
 * 应该返回友好的错误信息给前端，而不是暴露系统内部错误。
 * 由 GlobalExceptionHandler 统一捕获并转换为标准响应格式。
 * </p>
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码，用于标识异常类型。
     * <p>
     * 默认为 500（服务器内部错误），
     * 也可以自定义为其他业务错误码（如 400 参数错误、404 资源不存在等）。
     * </p>
     */
    private final int code;

    /**
     * 构造业务异常（使用默认错误码 500）。
     *
     * @param message 错误描述信息，会返回给前端展示
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 构造业务异常（自定义错误码）。
     *
     * @param code    错误码，用于标识异常类型
     * @param message 错误描述信息，会返回给前端展示
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
