package com.company.kb.controller;

import com.company.kb.dto.LoginRequest;
import com.company.kb.dto.LoginResponse;
import com.company.kb.service.AuthService;
import com.company.kb.utils.RsaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.KeyPair;
import java.util.Map;

/**
 * 认证控制器 — 处理用户登录、登出和 RSA 公钥获取请求。
 *
 * @author Geekyous Guo
 * @since 1.0.0
 * @see AuthService
 * @see LoginRequest
 * @see LoginResponse
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证管理", description = "用户登录、登出等认证相关接口")
public class AuthController {

    private final AuthService authService;

    private final KeyPair rsaKeyPair;

    /**
     * @param authService 认证服务实例
     * @param rsaKeyPair   RSA 密钥对（由 RsaKeyConfig 生成）
     */
    public AuthController(AuthService authService, KeyPair rsaKeyPair) {
        this.authService = authService;
        this.rsaKeyPair = rsaKeyPair;
    }

    /**
     * 用户登录 — 验证凭证并返回 JWT Token。
     *
     * @param request 登录请求 DTO
     * @return 统一响应包装的登录结果
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "验证用户名和密码，返回 JWT Token 和用户信息")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    /**
     * 用户登出 — JWT 无状态，客户端丢弃 Token 即可；本接口为语义补充。
     *
     * @return 登出成功提示
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "客户端丢弃 Token 即可实现登出，本接口为语义补充")
    public void logout() {
        // JWT 无状态登出，客户端丢弃 Token 即可
    }

    /**
     * 获取 RSA 公钥 — 前端用于加密密码等敏感字段。
     *
     * <p>加密传输流程：前端获取公钥 → 加密密码 → 发送到 /login → 后端私钥解密后 BCrypt 比对。</p>
     *
     * @return Base64 编码的 RSA 公钥
     */
    @GetMapping("/public-key")
    @Operation(summary = "获取 RSA 公钥", description = "前端获取公钥用于加密敏感字段（如密码）")
    public Map<String, String> getPublicKey() {
        return Map.of(
                "publicKey", RsaUtil.getPublicKeyBase64(rsaKeyPair),
                "algorithm", "RSA"
        );
    }
}
