package com.company.kb.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT 认证过滤器 — 从 HTTP 请求中提取并验证 JWT Token
 * @author Geekyous Guo
 * @see JwtConfig
 * @see SecurityConfig
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtConfig jwtConfig;

    public JwtAuthenticationFilter(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 从请求头提取 Bearer Token
        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                var claims = jwtConfig.parseToken(token);

                // 2. 从 Claims 中提取用户信息
                String username = claims.getSubject();
                String role = (String) claims.get("role");
                Long userId = ((Number) claims.get("userId")).longValue();

                // 3. 创建认证对象并存入 SecurityContext
                var authentication = new UsernamePasswordAuthenticationToken(
                    new UserDetails(userId, username, role),
                    null,
                    new ArrayList<>()
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                // Token 无效，不设置认证信息，请求以未认证状态继续
            }
        }

        // 4. 继续过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 从 Authorization 头部提取 Bearer Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 用户详情 — 封装从 JWT 中提取的用户身份信息
     * @param id       用户 ID
     * @param username 用户名
     * @param role     用户角色
     */
    public record UserDetails(Long id, String username, String role) {}
}
