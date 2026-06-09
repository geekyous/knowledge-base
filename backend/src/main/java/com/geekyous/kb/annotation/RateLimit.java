package com.geekyous.kb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解 — 标注在 Controller 类或方法上，限制单位时间内的请求次数。
 *
 * <p>基于 Redis INCR + EXPIRE 实现，按 IP + key 计数。
 * 超限返回 HTTP 429 Too Many Requests。</p>
 *
 * @author Geekyous Guo
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 限流标识，用于 Redis key 前缀（如 "login"、"document"） */
    String key() default "global";

    /** 时间窗口内允许的最大请求数 */
    int permits() default 60;

    /** 时间窗口（秒） */
    int seconds() default 60;
}
