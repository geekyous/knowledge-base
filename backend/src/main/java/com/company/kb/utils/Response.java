package com.company.kb.utils;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 统一响应结果工具类
 *
 * @author Geekyous Guo
 * @see com.company.kb.dto.ApiResponse
 */
@Data
public class Response<T> {

    /** HTTP 风格的状态码 */
    private Integer code;

    /** 响应消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 响应时间戳（毫秒级 Unix 时间戳） */
    private Long timestamp;

    /** 默认构造器，自动设置当前时间戳 */
    public Response() {
        this.timestamp = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /** 全参构造器 */
    public Response(Integer code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功响应（无数据） */
    public static <T> Response<T> success() {
        return new Response<>(HttpStatus.OK.value(), "操作成功", null);
    }

    /** 成功响应（有数据） */
    public static <T> Response<T> success(T data) {
        return new Response<>(HttpStatus.OK.value(), "操作成功", data);
    }

    /** 成功响应（自定义消息） */
    public static <T> Response<T> success(String message, T data) {
        return new Response<>(HttpStatus.OK.value(), message, data);
    }

    /** 错误响应（默认 500） */
    public static <T> Response<T> error(String message) {
        return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, null);
    }

    /** 错误响应（自定义状态码） */
    public static <T> Response<T> error(Integer code, String message) {
        return new Response<>(code, message, null);
    }

    /** 错误响应（带数据） */
    public static <T> Response<T> error(Integer code, String message, T data) {
        return new Response<>(code, message, data);
    }

    /** 参数校验失败响应（400） */
    public static <T> Response<T> validationFailed(List<String> errors) {
        return new Response<T>(HttpStatus.BAD_REQUEST.value(), "参数校验失败", (T) errors);
    }

    /** 未授权响应（401） */
    public static <T> Response<T> unauthorized(String message) {
        return new Response<>(HttpStatus.UNAUTHORIZED.value(), message, null);
    }

    /** 禁止访问响应（403） */
    public static <T> Response<T> forbidden(String message) {
        return new Response<>(HttpStatus.FORBIDDEN.value(), message, null);
    }

    /** 资源不存在响应（404） */
    public static <T> Response<T> notFound(String message) {
        return new Response<>(HttpStatus.NOT_FOUND.value(), message, null);
    }
}
