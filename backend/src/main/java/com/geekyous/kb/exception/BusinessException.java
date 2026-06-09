package com.geekyous.kb.exception;

/**
 * 业务异常 — 用于可预期的业务逻辑错误（如登录失败、参数校验失败等）。
 *
 * <p>与 RuntimeException 不同，BusinessException 携带 HTTP 状态码，
 * 便于 {@link GlobalExceptionHandler} 返回正确的 HTTP 响应。</p>
 *
 * @author Geekyous Guo
 */
public class BusinessException extends RuntimeException {

    private final int status;

    /**
     * @param status HTTP 状态码（如 401、403、423）
     * @param message 用户可读的错误描述
     */
    public BusinessException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
