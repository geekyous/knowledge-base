package com.geekyous.kb.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geekyous.kb.annotation.RateLimit;
import com.geekyous.kb.config.ClientIpResolver;
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
    private final ClientIpResolver clientIpResolver;

    public RateLimitInterceptor(StringRedisTemplate redisTemplate, ClientIpResolver clientIpResolver) {
        this.redisTemplate = redisTemplate;
        this.clientIpResolver = clientIpResolver;
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

        // 正常情况下 getRemoteAddr() 兜底总能拿到客户端 IP；若仍为空（异常网络环境/容器配置），
        // 不能直接拼进 key——字面量 "null" 会让所有匿名请求塌缩到同一桶，互相挤占配额。
        // 这里改为按 "anonymous" 维度限流：既不让匿名请求绕过限流，又能通过日志暴露配置异常。
        if (clientIp == null || clientIp.isEmpty()) {
            log.warn("无法识别客户端 IP，按匿名维度限流: uri={}", request.getRequestURI());
            clientIp = "anonymous";
        }

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

    /**
     * 委托 {@link ClientIpResolver} 解析真实客户端 IP。
     *
     * <p>仅在 TCP 对端是可信代理时才信任 X-Forwarded-For，并从右往左取首个非可信 IP，
     * 抵御客户端伪造 XFF（旧实现取 XFF 最左值，可被任意伪造绕过限流）。
     */
    private String getClientIp(HttpServletRequest request) {
        return clientIpResolver.resolve(request);
    }
}
