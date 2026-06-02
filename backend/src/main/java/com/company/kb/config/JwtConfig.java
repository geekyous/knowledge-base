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
 * JWT（JSON Web Token）配置与工具类 — 负责 Token 的生成、解析和验证
 *
 * <h2>什么是 JWT？</h2>
 * <p>JWT 是一种开放标准（RFC 7519），用于在各方之间安全地传输信息。
 * 它由三部分组成，用点号分隔：
 * <pre>
 *   Header.Payload.Signature
 *   eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.signature
 * </pre>
 * </p>
 *
 * <h3>JWT 三部分详解</h3>
 * <ul>
 *   <li><b>Header（头部）</b>: 指定令牌类型和签名算法，如 {"alg": "HS256", "typ": "JWT"}</li>
 *   <li><b>Payload（载荷）</b>: 包含声明（Claims），即用户身份数据。
 *       包括注册声明（iss, sub, exp 等）和自定义声明（role, userId 等）。</li>
 *   <li><b>Signature（签名）</b>: 用密钥对前两部分进行 HMAC 签名，确保数据不被篡改。</li>
 * </ul>
 *
 * <h2>JWT 在本系统中的工作流程</h2>
 * <ol>
 *   <li>用户登录 → AuthService 调用 {@link #generateToken(User)} 生成 JWT</li>
 *   <li>客户端收到 Token 并存储（通常在 localStorage 或 Cookie 中）</li>
 *   <li>后续请求在 Authorization 头部携带 Token：{@code Bearer &lt;token&gt;}</li>
 *   <li>{@link JwtAuthenticationFilter} 拦截请求，调用 {@link #parseToken(String)} 验证 Token</li>
 *   <li>验证通过后，将用户信息存入 Spring Security 的 {@code SecurityContext}</li>
 * </ol>
 *
 * <h2>HMAC 签名算法</h2>
 * <p>本项目使用 HMAC-SHA256（HS256）对称签名算法。同一把密钥既用于签名也用于验证。
 * 这是最常用的 JWT 签名方式，简单高效。在生产环境中，也可以使用 RSA/ECDSA 非对称算法，
 * 让认证服务用私钥签名，其他服务用公钥验证。</p>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>无状态认证（Stateless Auth）</b>: JWT 的核心优势。服务端不需要存储 Session，
 *       Token 本身就包含了所有身份信息，天然支持分布式部署和水平扩展。</li>
 *   <li><b>{@code @Value} 注解</b>: 从 application.properties/yml 中读取配置值。
 *       冒号后面是默认值（如 {@code default-secret-key-must-be-at-least-32-characters-long}）。</li>
 *   <li><b>密钥长度要求</b>: HMAC-SHA256 要求密钥至少 256 位（32 字节）。
 *       如果密钥太短，{@code Keys.hmacShaKeyFor()} 会抛出异常。</li>
 *   <li><b>{@code Claims}</b>: JWT 载荷中的声明集合。包含标准声明（sub, iat, exp）
 *       和自定义声明（role, userId）。通过 {@code claims.get("role")} 获取自定义声明。</li>
 *   <li><b>Token 过期机制</b>: {@code expiration} 设置为 24 小时（86400000 毫秒）。
 *       过期后需要重新登录。短期 Token + Refresh Token 是更安全的方案。</li>
 * </ul>
 *
 * @author Knowledge Base Team
 * @since 1.0.0
 * @see JwtAuthenticationFilter
 * @see com.company.kb.service.AuthService
 */
@Component
public class JwtConfig {

    /**
     * JWT 签名密钥 — 从配置文件读取，支持通过环境变量覆盖。
     *
     * <p>{@code @Value("${jwt.secret:default-secret-key-must-be-at-least-32-characters-long}")}:
     * 冒号前是配置 key，冒号后是默认值。生产环境中必须使用强随机密钥，且不能硬编码在代码中。</p>
     *
     * <p><b>安全警告</b>: 默认密钥仅用于开发环境！生产环境中应通过环境变量或密钥管理服务注入。</p>
     */
    @Value("${jwt.secret:default-secret-key-must-be-at-least-32-characters-long}")
    private String secret;

    /**
     * Token 过期时间（毫秒）— 默认 86400000ms = 24 小时。
     *
     * <p>可通配置文件 {@code jwt.expiration} 自定义。较短的有效期更安全，
     * 但用户需要更频繁地重新登录。</p>
     */
    @Value("${jwt.expiration:86400000}")
    private long expiration; // 24 hours default

    /**
     * 根据配置的密钥字符串构建 HMAC 签名密钥对象。
     *
     * <p>{@code Keys.hmacShaKeyFor()} 将字节数组转换为 {@code SecretKey} 对象。
     * 它会验证密钥长度是否满足 HMAC-SHA256 的要求（至少 32 字节 / 256 位）。</p>
     *
     * @return HMAC-SHA256 签名密钥
     */
    private SecretKey getSigningKey() {
        // 将密钥字符串转换为 UTF-8 字节数组。必须使用固定编码（如 UTF-8），
        // 避免平台默认编码不同导致密钥不一致的问题。
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT Token — 将用户身份信息编码为 Token。
     *
     * <h3>生成流程</h3>
     * <ol>
     *   <li>创建自定义声明（claims）：角色、用户 ID</li>
     *   <li>设置标准声明：subject（用户名）、签发时间、过期时间</li>
     *   <li>使用 HMAC-SHA256 密钥签名</li>
     *   <li>序列化为 Base64URL 编码的字符串</li>
     * </ol>
     *
     * <h3>生成的 Token 结构</h3>
     * <pre>
     * {
     *   "sub": "admin",           // subject = 用户名
     *   "role": "ADMIN",          // 自定义声明：角色
     *   "userId": 1,              // 自定义声明：用户ID
     *   "iat": 1700000000,        // issuedAt = 签发时间
     *   "exp": 1700086400         // expiration = 过期时间（iat + 24h）
     * }
     * </pre>
     *
     * @param user 已认证的用户实体
     * @return 生成的 JWT 字符串
     */
    public String generateToken(User user) {
        // 自定义声明（Claims）：存储额外的用户信息，后续可以从 Token 中提取
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());   // 角色名称字符串（如 "ADMIN"）
        claims.put("userId", user.getId());            // 用户 ID

        return Jwts.builder()
                .claims(claims)                        // 添加自定义声明
                .subject(user.getUsername())           // 标准声明：主题（通常是用户名）
                .issuedAt(new Date())                  // 标准声明：签发时间
                .expiration(new Date(System.currentTimeMillis() + expiration))  // 过期时间
                .signWith(getSigningKey())             // 使用 HMAC 密钥签名
                .compact();                            // 序列化为 Token 字符串
    }

    /**
     * 解析 JWT Token — 验证签名并提取载荷数据。
     *
     * <h3>解析流程</h3>
     * <ol>
     *   <li>使用密钥验证签名（如果签名不匹配则抛出异常）</li>
     *   <li>检查 Token 是否过期（过期则抛出 {@code ExpiredJwtException}）</li>
     *   <li>提取 Payload 中的所有声明（Claims）</li>
     * </ol>
     *
     * <p><b>异常处理</b>: 调用方应捕获以下异常：
     * <ul>
     *   <li>{@code ExpiredJwtException} — Token 已过期</li>
     *   <li>{@code MalformedJwtException} — Token 格式错误</li>
     *   <li>{@code SignatureException} — 签名验证失败（Token 被篡改）</li>
     * </ul>
     * </p>
     *
     * @param token JWT 字符串（不含 "Bearer " 前缀）
     * @return Token 中的声明（Claims）对象
     * @throws io.jsonwebtoken.JwtException Token 无效或已过期时抛出
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())   // 设置验证签名用的密钥
                .build()                       // 构建解析器
                .parseSignedClaims(token)       // 解析并验证 Token
                .getPayload();                  // 提取 Payload（即 Claims）
    }

    /**
     * 从 Token 中提取用户名。
     *
     * <p>用户名存储在 JWT 的 subject 标准声明中。</p>
     *
     * @param token JWT 字符串
     * @return Token 中存储的用户名
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 检查 Token 是否已过期。
     *
     * <p>比较 Token 中的过期时间（exp 声明）与当前时间。
     * 虽然 {@code parseToken()} 也会检查过期，但此方法提供了独立的检查能力。</p>
     *
     * @param token JWT 字符串
     * @return true 表示已过期，false 表示未过期
     */
    public boolean isTokenExpired(String token) {
        // getExpiration() 返回 exp 声明的 Date 对象
        // before() 判断过期时间是否在当前时间之前（即是否已过期）
        return parseToken(token).getExpiration().before(new Date());
    }
}
