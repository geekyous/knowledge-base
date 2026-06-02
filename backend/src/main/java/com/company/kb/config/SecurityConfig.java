package com.company.kb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 安全配置类 — 定义整个应用的认证和授权规则
 *
 * <h2>架构角色</h2>
 * <p>本类是 Spring Security 框架的核心配置，控制着整个应用的 HTTP 安全策略。
 * 它定义了：哪些 URL 需要认证、哪些可以公开访问、使用什么认证方式、
 * 如何处理 CORS 和 CSRF 等。所有 HTTP 请求都会经过这里配置的安全过滤器链。</p>
 *
 * <h2>Spring Security 过滤器链（Filter Chain）</h2>
 * <p>Spring Security 基于过滤器（Filter）模式工作。每个 HTTP 请求会依次经过一系列过滤器：
 * <pre>
 *   HTTP Request
 *     → CorsFilter（跨域处理）
 *     → CsrfFilter（CSRF 防护，本项目已禁用）
 *     → JwtAuthenticationFilter（自定义 JWT 认证过滤器）
 *     → UsernamePasswordAuthenticationFilter（Spring 内置认证过滤器）
 *     → AuthorizationFilter（授权检查：判断是否有权限访问）
 *     → Controller（业务处理）
 * </pre>
 * </p>
 *
 * <h2>关键安全决策</h2>
 * <ul>
 *   <li><b>无状态会话</b>: 不使用 HttpSession，每次请求都通过 JWT 独立认证</li>
 *   <li><b>CSRF 禁用</b>: 使用 Token 认证时 CSRF 攻击不再适用（因为 Token 不在 Cookie 中）</li>
 *   <li><b>公开接口</b>: 登录、注册、文档浏览、分类查询不需要认证</li>
 *   <li><b>保护接口</b>: 文档创建/编辑/删除等操作需要认证</li>
 * </ul>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>{@code @Configuration}</b>: 声明本类是 Spring 配置类，其中的 {@code @Bean} 方法
 *       会由 Spring 容器管理。{@code @EnableWebSecurity} 启用 Spring Security 的 Web 安全支持。</li>
 *   <li><b>CSRF（Cross-Site Request Forgery）</b>: 跨站请求伪造攻击。
 *       传统 Session + Cookie 方式容易受 CSRF 攻击，需要 CSRF Token 防护。
 *       本项目使用 JWT（存在 HTTP 头部而非 Cookie），天然免疫 CSRF，因此可以禁用。</li>
 *   <li><b>CORS（Cross-Origin Resource Sharing）</b>: 跨域资源共享。
 *       浏览器的同源策略限制了前端（如 localhost:3000）访问后端（如 localhost:8080）。
 *       CORS 配置告诉浏览器允许哪些源、方法、头部的跨域请求。</li>
 *   <li><b>{@code SessionCreationPolicy.STATELESS}</b>: 告诉 Spring Security 不创建或使用
 *       HttpSession。每个请求必须通过 JWT 自带身份信息。这是 REST API 的标准做法。</li>
 *   <li><b>{@code addFilterBefore()}</b>: 将自定义的 JWT 过滤器插入到 Spring Security
 *       过滤器链中。在 UsernamePasswordAuthenticationFilter 之前执行，
 *       这样 JWT 解析出的认证信息可以被后续的授权过滤器使用。</li>
 * </ul>
 *
 * @author Knowledge Base Team
 * @since 1.0.0
 * @see JwtAuthenticationFilter
 * @see SecurityFilterChain
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** JWT 认证过滤器 — 从请求头提取并验证 JWT Token */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 构造器注入 — Spring 自动注入 JwtAuthenticationFilter。
     *
     * <p>推荐使用构造器注入而非 {@code @Autowired} 字段注入，因为：
     * <ul>
     *   <li>明确声明依赖关系</li>
     *   <li>字段可以声明为 final（不可变）</li>
     *   <li>便于单元测试（直接传参而非依赖反射）</li>
     * </ul>
     * </p>
     *
     * @param jwtAuthenticationFilter JWT 认证过滤器实例
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 配置安全过滤器链 — 定义 HTTP 请求的安全规则。
     *
     * <p>这是 Spring Security 6.x 的新式配置方式（Lambda DSL），
     * 替代了旧版的 {@code WebSecurityConfigurerAdapter} 继承方式。</p>
     *
     * <h3>配置详解</h3>
     * <ol>
     *   <li><b>CSRF 禁用</b>: REST API 使用 JWT 认证，不需要 CSRF 保护</li>
     *   <li><b>CORS 启用</b>: 允许前端跨域访问</li>
     *   <li><b>无状态会话</b>: 不使用服务器端 Session</li>
     *   <li><b>URL 授权规则</b>: 定义哪些 URL 需要认证</li>
     *   <li><b>JWT 过滤器</b>: 在标准认证过滤器之前插入自定义 JWT 过滤器</li>
     * </ol>
     *
     * @param http HttpSecurity 构建器，Spring 自动注入
     * @return 构建完成的 SecurityFilterChain
     * @throws Exception 配置过程中的异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（Cross-Site Request Forgery）防护
            // REST API 使用 JWT 而非 Cookie 进行认证，不面临 CSRF 攻击风险
            .csrf(AbstractHttpConfigurer::disable)

            // 配置 CORS（Cross-Origin Resource Sharing）跨域策略
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 会话管理：STATELESS 模式
            // Spring Security 不会创建 HttpSession，也不使用 Session 进行认证
            // 每个请求必须通过 JWT Token 自带身份信息
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // URL 授权规则配置
            .authorizeHttpRequests(auth -> auth
                // 公开接口：认证相关（登录、注册）
                .requestMatchers("/api/v1/auth/**").permitAll()
                // 公开接口：健康检查（监控告警系统需要访问）
                .requestMatchers("/actuator/health").permitAll()
                // 公开接口：文档浏览和搜索（SEO 和用户体验考虑）
                .requestMatchers("/api/v1/documents", "/api/v1/documents/**").permitAll()
                // 公开接口：搜索
                .requestMatchers("/api/v1/search/**").permitAll()
                // 公开接口：分类浏览
                .requestMatchers("/api/v1/categories/**").permitAll()
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )

            // 在 Spring Security 的标准认证过滤器之前插入自定义的 JWT 过滤器
            // 这样 JWT 解析出的认证信息可以被后续的 AuthorizationFilter 使用
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码编码器 Bean — 使用 BCrypt 算法对密码进行哈希。
     *
     * <p><b>BCrypt 简介</b>:
     * <ul>
     *   <li>基于 Blowfish 加密算法</li>
     *   <li>自动生成随机盐值（Salt），每次加密结果不同</li>
     *   <li>支持强度因子（cost factor），可以随硬件升级提高加密强度</li>
     *   <li>是当前推荐的密码哈希算法之一（其他还有 Argon2、scrypt）</li>
     * </ul>
     * </p>
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 跨域配置 — 定义允许跨域请求的规则。
     *
     * <p><b>为什么需要 CORS？</b>
     * 浏览器有同源策略（Same-Origin Policy），默认不允许跨域请求。
     * 当前端（如 http://localhost:3000）和后端（如 http://localhost:8080）不同源时，
     * 需要后端返回 CORS 头部告诉浏览器"允许此跨域请求"。</p>
     *
     * <p><b>配置说明</b>:
     * <ul>
     *   <li>{@code allowedOrigins("*")}: 允许所有来源。生产环境应限制为具体域名</li>
     *   <li>{@code allowedMethods}: 允许的 HTTP 方法</li>
     *   <li>{@code allowedHeaders("*")}: 允许所有请求头（包括 Authorization）</li>
     * </ul>
     * </p>
     *
     * <p><b>安全提示</b>: {@code allowedOrigins("*")} 在生产环境中不安全，
     * 应该替换为具体的前端域名列表。</p>
     *
     * @return CORS 配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有来源的跨域请求。生产环境应改为具体域名，如：
        // config.setAllowedOrigins(List.of("https://www.company.com", "https://admin.company.com"));
        config.setAllowedOrigins(List.of("*"));
        // 允许的 HTTP 方法。OPTIONS 是预检请求（Preflight）使用的方法
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许所有请求头。必须包含 Authorization，否则 JWT Token 无法发送
        config.setAllowedHeaders(List.of("*"));

        // 将 CORS 配置注册到 URL 路径模式
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // "/**" 表示所有路径都应用此 CORS 配置
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
