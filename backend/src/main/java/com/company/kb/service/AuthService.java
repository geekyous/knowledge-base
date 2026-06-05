package com.company.kb.service;

import com.company.kb.config.JwtConfig;
import com.company.kb.dto.LoginRequest;
import com.company.kb.dto.LoginResponse;
import com.company.kb.entity.User;
import com.company.kb.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务层（Authentication Service）— 处理用户登录认证逻辑
 *
 * <h2>架构角色 — Service Layer Pattern（服务层模式）</h2>
 * <p>Service 层是三层架构（Controller → Service → Repository）的中间层，负责：
 * <ul>
 *   <li><b>业务逻辑编排</b>: 协调多个 Repository 和外部组件完成业务操作</li>
 *   <li><b>事务管理</b>: 控制数据库事务的边界（本类中未显式使用，可加 {@code @Transactional}）</li>
 *   <li><b>异常处理</b>: 将底层异常转换为业务异常</li>
 *   <li><b>DTO 转换</b>: 在 Entity 和 DTO 之间进行数据映射</li>
 * </ul>
 * </p>
 *
 * <h2>登录认证流程</h2>
 * <pre>
 *   1. 接收 LoginRequest（用户名 + 明文密码）
 *   2. 根据用户名查询数据库 → UserRepository.findByUsername()
 *   3. 验证密码 → BCryptPasswordEncoder.matches()
 *   4. 检查账号状态 → User.UserStatus == ACTIVE
 *   5. 生成 JWT Token → JwtConfig.generateToken()
 *   6. 构建响应 DTO → LoginResponse（Token + UserInfo）
 * </pre>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>BCrypt 密码验证</b>: {@code passwordEncoder.matches(明文, 密文)} 是安全的密码验证方式。
 *       BCrypt 自动从密文中提取盐值进行哈希比对，不需要单独存储盐。</li>
 *   <li><b>统一错误信息</b>: 用户名不存在和密码错误返回相同的提示"用户名或密码错误"，
 *       防止攻击者通过不同的错误信息枚举有效用户名。</li>
 *   <li><b>{@code @Service}</b>: Spring 的服务层组件注解。被 {@code @ComponentScan} 扫描后
 *       注册为 Spring Bean，可以被 Controller 通过依赖注入使用。</li>
 *   <li><b>构造器注入</b>: 所有依赖通过构造器注入（而非 {@code @Autowired} 字段注入），
 *       这是 Spring 官方推荐的最佳实践。当一个类只有一个构造器时，
 *       Spring 会自动使用它进行注入，无需 {@code @Autowired} 注解。</li>
 * </ul>
 *
 * @author Geekyous Guo
 * @since 1.0.0
 * @see com.company.kb.controller.AuthController
 * @see JwtConfig
 */
@Service
public class AuthService {

    /** 用户数据访问层 — 用于查询用户信息 */
    private final UserRepository userRepository;

    /** 密码编码器 — 用于验证密码（BCrypt） */
    private final PasswordEncoder passwordEncoder;

    /** JWT 配置类 — 用于生成认证 Token */
    private final JwtConfig jwtConfig;

    /**
     * 构造器注入所有依赖。
     *
     * <p>Spring 在创建 AuthService Bean 时，会自动从容器中找到并注入
     * UserRepository、PasswordEncoder、JwtConfig 的实现。
     * 这是<b>依赖注入（Dependency Injection, DI）</b>的核心原理：
     * 对象不自己创建依赖，而是由容器提供。</p>
     *
     * @param userRepository  用户 Repository
     * @param passwordEncoder 密码编码器（由 SecurityConfig 中的 @Bean 方法提供）
     * @param jwtConfig       JWT 工具类
     */
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtConfig jwtConfig) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtConfig = jwtConfig;
    }

    /**
     * 用户登录认证 — 验证凭证并返回 JWT Token。
     *
     * <h3>完整认证流程</h3>
     * <ol>
     *   <li><b>查找用户</b>: 根据用户名查询数据库。如果用户不存在则抛出异常</li>
     *   <li><b>验证密码</b>: 使用 BCrypt 将用户输入的明文密码与数据库中的哈希值比对</li>
     *   <li><b>检查状态</b>: 确保用户账号处于活跃状态（未被禁用或锁定）</li>
     *   <li><b>生成 Token</b>: 将用户信息编码为 JWT</li>
     *   <li><b>构建响应</b>: 使用 Builder 模式组装 LoginResponse</li>
     * </ol>
     *
     * <h3>安全设计</h3>
     * <p>注意用户名不存在和密码错误返回相同的错误信息——"用户名或密码错误"。
     * 这是防止<b>用户名枚举攻击</b>的标准做法。如果分别提示"用户不存在"和"密码错误"，
     * 攻击者就可以通过反复尝试来发现系统中存在哪些用户名。</p>
     *
     * @param request 登录请求 DTO，包含用户名和密码
     * @return LoginResponse 包含 JWT Token 和用户基本信息
     * @throws RuntimeException 当用户名不存在、密码错误或账号被禁用时
     */
    public LoginResponse login(LoginRequest request) {
        // 步骤 1: 根据用户名查找用户
        // orElseThrow() 如果 Optional 为空则抛出异常
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        // 步骤 2: 验证密码
        // BCrypt.matches(明文密码, 哈希密码) 返回 true 表示匹配
        // 注意参数顺序：第一个是用户输入的明文，第二个是数据库中的密文
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 步骤 3: 检查账号状态
        // 只有 ACTIVE 状态的用户才允许登录
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new RuntimeException("账号已被禁用");
        }

        // 步骤 4: 生成 JWT Token
        String token = jwtConfig.generateToken(user);

        // 步骤 5: 构建并返回登录响应
        // 使用 Lombok @Builder 提供的链式构造 API
        return LoginResponse.builder()
                .token(token)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole().name())   // 枚举转字符串，如 "ADMIN"
                        .avatar(user.getAvatar())
                        .build())
                .build();
    }
}
