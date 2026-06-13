package com.geekyous.kb.controller;

import com.geekyous.kb.config.JwtAuthenticationFilter;
import com.geekyous.kb.entity.User;
import com.geekyous.kb.exception.BusinessException;
import com.geekyous.kb.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户控制器 — 当前用户信息查询与更新
 *
 * @author Geekyous
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "用户管理", description = "当前用户信息查询与更新")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 获取当前登录用户信息 — 前端登录后调用，恢复用户状态
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                "status", user.getStatus().name(),
                "createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
        ));
    }

    /**
     * 更新当前用户信息
     */
    @PutMapping("/me")
    @Operation(summary = "更新当前用户信息")
    public ResponseEntity<Map<String, Object>> updateCurrentUser(@RequestBody Map<String, Object> updates) {
        User user = getAuthenticatedUser();

        if (updates.containsKey("avatar")) {
            user.setAvatar((String) updates.get("avatar"));
        }
        userRepository.save(user);
        log.info("用户信息更新: username={}", user.getUsername());

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                "status", user.getStatus().name(),
                "createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
        ));
    }

    /** 从 SecurityContext 获取当前认证用户实体 */
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // 防御性校验：未认证或 principal 类型不符（如匿名 "anonymousUser"）时拒绝，避免 NPE/ClassCastException
        if (auth == null || !(auth.getPrincipal() instanceof JwtAuthenticationFilter.UserDetails details)) {
            throw new BusinessException(401, "未认证");
        }
        return userRepository.findById(details.id())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
