package com.geekyous.kb.controller;

import com.geekyous.kb.annotation.RateLimit;
import com.geekyous.kb.config.JwtConfig;
import com.geekyous.kb.dto.ApiResponse;
import com.geekyous.kb.dto.LoginRequest;
import com.geekyous.kb.dto.LoginResponse;
import com.geekyous.kb.service.AuthService;
import com.geekyous.kb.service.TokenBlacklistService;
import com.geekyous.kb.utils.RsaUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证管理", description = "用户登录、登出等认证相关接口")
public class AuthController {

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtConfig jwtConfig;
    private final KeyPair rsaKeyPair;

    /**
     * @param authService          认证服务实例
     * @param tokenBlacklistService Token 黑名单服务
     * @param jwtConfig            JWT 工具类
     * @param rsaKeyPair            RSA 密钥对（由 RsaKeyConfig 生成）
     */
    public AuthController(AuthService authService, TokenBlacklistService tokenBlacklistService,
                          JwtConfig jwtConfig, KeyPair rsaKeyPair) {
        this.authService = authService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.jwtConfig = jwtConfig;
        this.rsaKeyPair = rsaKeyPair;
    }

    /**
     * 用户登录 — 验证凭证并返回 JWT Token。
     *
     * @param request 登录请求 DTO
     * @return 统一响应包装的登录结果
     */
    @PostMapping("/login")
    @RateLimit(key = "login", permits = 5, seconds = 60)
    @Operation(summary = "用户登录", description = "验证用户名和密码，返回 JWT Token 和用户信息")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    /**
     * 用户登出 — 将当前 JWT 加入黑名单，使其立即失效。
     *
     * <p>即使客户端未丢弃 Token，黑名单机制也能阻止已登出的 Token 被复用。</p>
     *
     * @param request HTTP 请求（用于提取 Authorization 头中的 Token）
     * @return 登出成功提示
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "将当前 Token 加入黑名单使其立即失效")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            try {
                String jti = jwtConfig.getTokenId(token);
                long remainingMillis = jwtConfig.getTokenRemainingMillis(token);
                tokenBlacklistService.blacklist(jti, remainingMillis);
            } catch (Exception e) {
                // Token 已无效（过期/格式错误），无需加入黑名单，直接视为登出成功
                log.debug("登出时 Token 已无效，跳过黑名单: {}", e.getMessage());
            }
        }
        return ApiResponse.success("登出成功", null);
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
