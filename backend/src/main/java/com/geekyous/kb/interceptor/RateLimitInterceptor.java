package com.geekyous.kb.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geekyous.kb.annotation.RateLimit;
import com.geekyous.kb.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 限流拦截器 — 基于 Redis 实现 IP + URI 级别的请求频率限制。
 *
 * <p>配合 {@link RateLimit} 注解使用，超限返回 HTTP 429。</p>
 *
 * @author Geekyous Guo
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String KEY_PREFIX = "rate_limit:";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final StringRedisTemplate redisTemplate;

    public RateLimitInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 方法级注解优先，其次类级注解
        RateLimit methodLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        RateLimit classLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        RateLimit rateLimit = methodLimit != null ? methodLimit : classLimit;

        if (rateLimit == null) {
            return true;
        }

        String clientIp = getClientIp(request);
        String redisKey = KEY_PREFIX + rateLimit.key() + ":" + clientIp + ":" + request.getRequestURI();

        Long count = redisTemplate.opsForValue().increment(redisKey);

        if (count != null && count == 1) {
            // 首次请求，设置过期时间
            redisTemplate.expire(redisKey, rateLimit.seconds(), TimeUnit.SECONDS);
        }

        if (count != null && count > rateLimit.permits()) {
            log.warn("接口限流: key={}, ip={}, uri={}, count={}", rateLimit.key(), clientIp, request.getRequestURI(), count);
            writeTooManyRequestsResponse(response);
            return false;
        }

        return true;
    }

    private void writeTooManyRequestsResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Void> body = ApiResponse.error(429, "请求过于频繁，请稍后再试");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
    }

    /** 从请求头中获取客户端真实 IP（支持代理场景） */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
