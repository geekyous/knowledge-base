package com.company.kb.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.company.kb.entity.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类 — 负责 Token 的生成、解析和验证。
 *
 * @author Geekyous Guo
 * @since 1.0.0
 * @see JwtAuthenticationFilter
 * @see com.company.kb.service.AuthService
 */
@Component
public class JwtConfig {

    /** 签名密钥，从配置文件读取，默认值仅用于开发环境。 */
    @Value("${jwt.secret:default-secret-key-must-be-at-least-32-characters-long}")
    private String secret;

    /** Token 过期时间（毫秒），默认 24 小时。 */
    @Value("${jwt.expiration:86400000}")
    private long expiration;

    /** 将密钥字符串转换为 HMAC-SHA256 SecretKey。 */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT：自定义声明(role, userId) + 标准声明(subject, iat, exp) → HMAC 签名。
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("userId", user.getId());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析并验证 Token：验证签名 → 检查过期 → 提取 Claims。
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 从 Token 的 subject 声明中提取用户名。 */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /** 检查 Token 是否已过期。 */
    public boolean isTokenExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }
}
