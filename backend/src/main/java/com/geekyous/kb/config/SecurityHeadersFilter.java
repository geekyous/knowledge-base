package com.geekyous.kb.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 安全响应头过滤器 — 为所有 HTTP 响应添加安全相关的 Header。
 *
 * <p>防御 clickjacking、MIME 嗅探、XSS 等常见 Web 攻击。</p>
 *
 * @author Geekyous Guo
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 禁止浏览器猜测 MIME 类型，防止上传漏洞
        response.setHeader("X-Content-Type-Options", "nosniff");

        // 禁止页面被嵌入 iframe，防止点击劫持
        response.setHeader("X-Frame-Options", "DENY");

        // 启用浏览器内置 XSS 过滤器
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // 内容安全策略：限制资源加载来源
        response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'");

        // HTTPS 强制跳转（生产环境生效，HTTP 下忽略）
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // 控制引用来源泄露
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 禁用浏览器缓存敏感页面（API 响应不应被缓存）
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");

        filterChain.doFilter(request, response);
    }
}
