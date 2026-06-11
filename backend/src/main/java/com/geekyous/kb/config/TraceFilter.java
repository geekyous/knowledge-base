package com.geekyous.kb.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求追踪过滤器 — 为每个 HTTP 请求生成唯一 traceId 并注入 MDC
 * <p>
 * 日志格式中的 [%X{traceId}] 由本过滤器填充，用于跨层追踪单个请求的完整链路。
 * traceId 同时写入响应头 X-Request-Id，方便前端/调用方反馈问题。
 * <p>
 * 执行顺序：SecurityHeadersFilter(HIGHEST_PRECEDENCE) → TraceFilter(+1) → Spring Security → 业务
 *
 * @author Geekyous
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TraceFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String HEADER_NAME = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 生成 8 位 traceId（UUID 去横线取前 8 位，42 亿种组合足够单机追踪）
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        MDC.put(TRACE_ID, traceId);
        response.setHeader(HEADER_NAME, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 必须清理，防止线程池复用时 MDC 泄露到其他请求
            MDC.remove(TRACE_ID);
        }
    }
}
