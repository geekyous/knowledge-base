package com.geekyous.kb.service;

import com.geekyous.kb.config.JwtConfig;
import com.geekyous.kb.dto.LoginRequest;
import com.geekyous.kb.dto.LoginResponse;
import com.geekyous.kb.entity.User;
import com.geekyous.kb.exception.BusinessException;
import com.geekyous.kb.repository.UserRepository;
import com.geekyous.kb.utils.RsaUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.KeyPair;

/**
 * 认证服务 — 处理用户登录认证逻辑
 *
 * @author Geekyous Guo
 * @since 1.0.0
 * @see com.geekyous.kb.controller.AuthController
 * @see JwtConfig
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;
    private final KeyPair rsaKeyPair;
    private final LoginProtectionService loginProtectionService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtConfig jwtConfig, KeyPair rsaKeyPair, LoginProtectionService loginProtectionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtConfig = jwtConfig;
        this.rsaKeyPair = rsaKeyPair;
        this.loginProtectionService = loginProtectionService;
    }

    /**
     * 用户登录认证 — 验证凭证并返回 JWT Token。
     * 用户名不存在和密码错误返回相同提示，防止用户名枚举攻击。
     *
     * @param request 登录请求 DTO
     * @return LoginResponse 包含 JWT Token 和用户基本信息
     * @throws BusinessException 用户名不存在、密码错误(401)、账号锁定(423)或被禁用(403)
     */
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();

        // 0. 检查账号是否被锁定（防暴力破解）
        if (loginProtectionService.isLocked(username)) {
            long remaining = loginProtectionService.getRemainingLockTime(username);
            throw new BusinessException(423, "账号已锁定，请" + (remaining / 60 + 1) + "分钟后再试");
        }

        // 1. 查找用户（统一错误信息防止枚举）
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    loginProtectionService.recordFailure(username);
                    return new BusinessException(401, "用户名或密码错误");
                });

        // 2. RSA 解密密码（解密失败则当作明文）
        String rawPassword = RsaUtil.tryDecrypt(request.getPassword(), rsaKeyPair.getPrivate());

        // 3. 验证密码
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            loginProtectionService.recordFailure(username);
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 4. 检查账号状态
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new BusinessException(403, "账号已被禁用");
        }

        // 5. 登录成功，清除失败计数
        loginProtectionService.recordSuccess(username);

        // 6. 生成 JWT Token
        String token = jwtConfig.generateToken(user);

        // 6. 构建响应（email/phone 传原始值，@Sensitive 注解在序列化时自动脱敏）
        return LoginResponse.builder()
                .token(token)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole().name())
                        .avatar(user.getAvatar())
                        .build())
                .build();
    }
}
