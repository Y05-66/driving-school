package com.drivingschool.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果封装类
 * 所有 REST API 接口统一返回此对象
 *
 * 响应格式：{ "code": 200, "message": "success", "data": {...} }
 *
 * 状态码约定：
 * - 200: 成功
 * - 400: 参数校验失败
 * - 401: 未认证/Token过期
 * - 403: 无权限
 * - 500: 业务错误/系统错误
 */
@Data
public class R<T> implements Serializable {

    /** 状态码 */
    private int code;

    /** 响应消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /**
     * 成功响应（无数据）
     */
    public static <T> R<T> ok() {
        return ok(null);
    }

    /**
     * 成功响应（带数据）
     * @param data 响应数据
     */
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    /**
     * 失败响应（默认500）
     * @param message 错误消息
     */
    public static <T> R<T> fail(String message) {
        return fail(500, message);
    }

    /**
     * 失败响应（自定义状态码）
     * @param code 状态码
     * @param message 错误消息
     */
    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
