package com.geekyous.kb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.geekyous.kb.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 安全配置 — 定义认证、授权规则和 CORS 策略
 *
 * @author Geekyous Guo
 * @since 1.0.0
 * @see JwtAuthenticationFilter
 * @see SecurityFilterChain
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitInterceptor rateLimitInterceptor) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor);
    }

    /**
     * 安全过滤器链 — 定义 HTTP 请求的安全规则。
     *
     * @param http HttpSecurity 构建器
     * @return 构建完成的 SecurityFilterChain
     * @throws Exception 配置过程中的异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF：REST API 使用 JWT 而非 Cookie 认证，不面临 CSRF 攻击风险
            .csrf(AbstractHttpConfigurer::disable)

            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 无状态会话：不创建 HttpSession，每个请求通过 JWT 自带身份信息
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // 公开接口：认证（登录、注册）
                .requestMatchers("/api/v1/auth/**").permitAll()
                // 公开接口：健康检查
                .requestMatchers("/actuator/health").permitAll()
                // 公开接口：文档只读（列表、详情、精选、热门）
                .requestMatchers(HttpMethod.GET, "/api/v1/documents", "/api/v1/documents/**").permitAll()
                // 文档写操作需要认证
                .requestMatchers(HttpMethod.POST, "/api/v1/documents", "/api/v1/documents/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/documents/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/documents/**").authenticated()
                // 公开接口：搜索
                .requestMatchers("/api/v1/search/**").permitAll()
                // 公开接口：分类浏览
                .requestMatchers("/api/v1/categories/**").permitAll()
                // 公开接口：Swagger UI 和 OpenAPI 文档
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-resources/**").permitAll()
                .anyRequest().authenticated()
            )

            // JWT 过滤器插入到标准认证过滤器之前
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** 密码编码器 — BCrypt 自带随机盐值，是当前推荐的密码哈希算法 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 跨域配置。
     * 注意：allowedOrigins("*") 仅用于开发环境，生产环境应替换为具体的前端域名。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
