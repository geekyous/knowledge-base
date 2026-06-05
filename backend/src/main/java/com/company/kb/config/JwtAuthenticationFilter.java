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
 * JWT 认证过滤器（JWT Authentication Filter）— 从 HTTP 请求中提取并验证 JWT Token
 *
 * <h2>架构角色</h2>
 * <p>本类是 Spring Security 过滤器链中的一个自定义过滤器，负责：
 * <ol>
 *   <li>从每个 HTTP 请求的 Authorization 头部提取 Bearer Token</li>
 *   <li>解析并验证 Token 的有效性（签名、过期时间）</li>
 *   <li>将解析出的用户信息封装为 Spring Security 认证对象</li>
 *   <li>将认证信息存入 {@code SecurityContext}，供后续授权检查使用</li>
 * </ol>
 * </p>
 *
 * <h2>为什么继承 {@code OncePerRequestFilter}？</h2>
 * <p>Spring 的过滤器可能被多次调用（如转发、包含等场景）。
 * {@code OncePerRequestFilter} 确保每个 HTTP 请求只经过一次此过滤器，
 * 避免重复认证处理。这是 Spring 推荐的自定义过滤器基类。</p>
 *
 * <h2>Spring Security 认证流程</h2>
 * <pre>
 *   1. 请求到达 → JwtAuthenticationFilter.doFilterInternal()
 *   2. 提取 Authorization 头部中的 Bearer Token
 *   3. 调用 JwtConfig.parseToken() 解析 Token
 *   4. 从 Claims 中提取用户名、角色、ID
 *   5. 创建 UsernamePasswordAuthenticationToken 认证对象
 *   6. 存入 SecurityContextHolder → 后续过滤器/控制器可通过 SecurityContext 获取用户信息
 *   7. 调用 filterChain.doFilter() 继续过滤器链
 * </pre>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>{@code SecurityContext}</b>: Spring Security 的安全上下文，存储当前线程的认证信息。
 *       基于 {@code ThreadLocal} 实现，每个请求线程有独立的 SecurityContext。</li>
 *   <li><b>Bearer Token 格式</b>: HTTP Authorization 头部格式为 "Bearer &lt;token&gt;"。
 *       "Bearer" 表示持有者令牌——谁持有这个 Token 谁就是认证用户。</li>
 *   <li><b>异常处理策略</b>: Token 解析失败时不抛出异常（不中断请求），而是继续过滤器链。
 *       这样未携带 Token 的请求可以正常访问公开接口。授权检查由后续的
 *       AuthorizationFilter 根据请求 URL 决定。</li>
 *   <li><b>Java Record</b>: {@code UserDetails} 使用 JDK 16+ 的 Record 类定义，
 *       自动生成构造器、getter、equals、hashCode、toString。比 Lombok 更轻量。</li>
 *   <li><b>空权限列表</b>: {@code new ArrayList<>()} 作为 authorities 参数传入，
 *       表示不使用基于权限的细粒度授权（本项目基于角色而非权限）。</li>
 * </ul>
 *
 * @author Geekyous Guo
 * @since 1.0.0
 * @see OncePerRequestFilter
 * @see JwtConfig
 * @see SecurityConfig
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT 配置与工具类，用于 Token 解析和验证 */
    private final JwtConfig jwtConfig;

    /**
     * 构造器注入 JwtConfig。
     *
     * @param jwtConfig JWT 配置类实例
     */
    public JwtAuthenticationFilter(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    /**
     * 过滤器核心方法 — 每个 HTTP 请求都会执行此方法。
     *
     * <h3>执行逻辑</h3>
     * <ol>
     *   <li>从请求头提取 Token</li>
     *   <li>如果 Token 存在，解析并提取用户信息</li>
     *   <li>将用户信息封装为 Spring Security 的 Authentication 对象</li>
     *   <li>存入 SecurityContext</li>
     *   <li>无论认证成功与否，继续执行后续过滤器</li>
     * </ol>
     *
     * @param request     当前 HTTP 请求
     * @param response    当前 HTTP 响应
     * @param filterChain  过滤器链，用于调用下一个过滤器
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 步骤 1: 从请求头中提取 Bearer Token
        String token = extractToken(request);

        // 步骤 2: 如果 Token 存在且非空，尝试验证
        if (StringUtils.hasText(token)) {
            try {
                // 解析 Token，如果签名无效或已过期会抛出异常
                var claims = jwtConfig.parseToken(token);

                // 从 Claims（载荷）中提取用户信息
                // getSubject() 返回标准声明 sub，本项目存储的是用户名
                String username = claims.getSubject();
                // get("role") 返回自定义声明，需要强制转换为 String
                String role = (String) claims.get("role");
                // get("userId") 返回自定义声明，JSON 数字解析为 Integer/Long，需要先转 Number 再转 Long
                Long userId = ((Number) claims.get("userId")).longValue();

                // 步骤 3: 创建 Spring Security 的认证对象
                // UsernamePasswordAuthenticationToken 是 Spring Security 最常用的认证实现
                // 参数：principal（用户身份）、credentials（凭证，已验证所以为 null）、authorities（权限列表）
                var authentication = new UsernamePasswordAuthenticationToken(
                    new UserDetails(userId, username, role),  // principal: 自定义用户详情对象
                    null,                                      // credentials: 已验证，不需要凭证
                    new ArrayList<>()                          // authorities: 空权限列表
                );

                // 步骤 4: 将认证信息存入 SecurityContext
                // SecurityContextHolder 基于 ThreadLocal，确保每个请求线程有独立的安全上下文
                // 后续的 AuthorizationFilter 和 Controller 可以通过 SecurityContext 获取当前用户信息
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                // Token 无效（过期、签名错误、格式错误等）
                // 不抛出异常，不设置认证信息 → 请求将以"未认证"状态继续
                // 如果请求的是受保护接口，后续的 AuthorizationFilter 会返回 403
            }
        }

        // 步骤 5: 继续过滤器链，无论认证是否成功
        // 这是关键：即使 Token 无效，也要继续执行后续过滤器和 Controller
        filterChain.doFilter(request, response);
    }

    /**
     * 从 HTTP 请求的 Authorization 头部提取 Bearer Token。
     *
     * <p>Authorization 头部格式：{@code Bearer eyJhbGciOiJIUzI1NiJ9...}
     * 本方法去除 "Bearer " 前缀，返回纯 Token 字符串。</p>
     *
     * @param request HTTP 请求对象
     * @return Token 字符串，如果不存在或格式不正确则返回 null
     */
    private String extractToken(HttpServletRequest request) {
        // 获取 Authorization 请求头的值
        String bearerToken = request.getHeader("Authorization");
        // 检查头部值非空，且以 "Bearer " 开头（注意 Bearer 后有空格）
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // 截取 "Bearer " 之后的部分（索引 7 = "Bearer ".length()）
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 用户详情 Record — 封装从 JWT 中提取的用户身份信息。
     *
     * <p>Java Record（JDK 16+）是一种特殊的类，专门用于不可变数据载体。
     * 与普通类相比，Record 自动生成：
     * <ul>
     *   <li>所有字段的 getter（通过字段名直接访问，如 {@code userDetails.username()}）</li>
     *   <li>equals()、hashCode()、toString() 方法</li>
     *   <li>全参构造器</li>
     * </ul>
     * 比 Lombok 的 @Data 更轻量、更安全（不可变）。</p>
     *
     * @param id       用户 ID
     * @param username 用户名
     * @param role     用户角色
     */
    public record UserDetails(Long id, String username, String role) {}
}
