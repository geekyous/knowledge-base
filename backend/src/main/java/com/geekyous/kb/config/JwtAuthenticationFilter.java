package com.geekyous.kb.config;

import com.geekyous.kb.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器 — 从 HTTP 请求中提取并验证 JWT Token，检查黑名单
 * @author Geekyous Guo
 * @see JwtConfig
 * @see SecurityConfig
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtConfig jwtConfig;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(JwtConfig jwtConfig, TokenBlacklistService tokenBlacklistService) {
        this.jwtConfig = jwtConfig;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 从请求头提取 Bearer Token
        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                var claims = jwtConfig.parseToken(token);

                // 2. 检查 Token 是否已被吊销（黑名单）
                String jti = claims.get("jti", String.class);
                if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
                    log.warn("Token 已被吊销: jti={}", jti);
                    filterChain.doFilter(request, response);
                    return;
                }

                // 3. 从 Claims 中提取用户信息（缺字段或类型不符则视为非法 token，跳过认证）
                String username = claims.getSubject();
                String role = claims.get("role", String.class);
                Object rawUserId = claims.get("userId");
                if (username == null || role == null || !(rawUserId instanceof Number)) {
                    log.warn("JWT claims 缺失必要字段或类型不符，跳过认证: username={}, role={}, userId={}",
                            username, role, rawUserId);
                    filterChain.doFilter(request, response);
                    return;
                }
                Long userId = ((Number) rawUserId).longValue();

                // 4. 将角色映射为 Spring Security GrantedAuthority
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role));

                // 5. 创建认证对象并存入 SecurityContext
                var authentication = new UsernamePasswordAuthenticationToken(
                    new UserDetails(userId, username, role),
                    null,
                    authorities
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT 认证成功: username={}, role={}", username, role);

            } catch (Exception e) {
                // Token 无效，记录原因但不阻止请求（未认证状态继续）
                log.warn("JWT 验证失败: {}", e.getMessage());
            }
        }

        // 6. 继续过滤器链
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
