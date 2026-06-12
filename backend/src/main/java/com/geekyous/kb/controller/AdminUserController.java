package com.geekyous.kb.controller;

import com.geekyous.kb.dto.request.CreateUserRequest;
import com.geekyous.kb.dto.request.ResetPasswordRequest;
import com.geekyous.kb.dto.request.UpdateUserRequest;
import com.geekyous.kb.dto.response.UserAdminResponse;
import com.geekyous.kb.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理控制器 — 管理后台用户增删改查、禁用、解锁、重置密码等操作
 *
 * @author Geekyous Guo
 * @see AdminUserService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "管理后台-用户管理", description = "管理后台用户增删改查、禁用、解锁、重置密码等接口")
@Validated
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "获取用户列表", description = "支持分页、关键词搜索、角色和状态过滤")
    public Page<UserAdminResponse> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        log.info("管理后台-查询用户列表: page={}, size={}, keyword={}, role={}, status={}", page, size, keyword, role, status);
        return adminUserService.listUsers(page, size, keyword, role, status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情")
    public UserAdminResponse getUser(@PathVariable Long id) {
        log.info("管理后台-查询用户详情: id={}", id);
        return adminUserService.getUser(id);
    }

    @PostMapping
    @Operation(summary = "创建用户")
    public UserAdminResponse createUser(@RequestBody @Valid CreateUserRequest request) {
        log.info("管理后台-创建用户: username={}", request.getUsername());
        return adminUserService.createUser(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户信息")
    public UserAdminResponse updateUser(@PathVariable Long id, @RequestBody @Valid UpdateUserRequest request) {
        log.info("管理后台-更新用户: id={}", id);
        return adminUserService.updateUser(id, request);
    }

    @PutMapping("/{id}/reset-password")
    @Operation(summary = "重置用户密码")
    public Map<String, String> resetPassword(@PathVariable Long id, @RequestBody @Valid ResetPasswordRequest request) {
        log.info("管理后台-重置密码: id={}", id);
        adminUserService.resetPassword(id, request);
        return Map.of("message", "密码重置成功");
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用用户")
    public Map<String, String> disableUser(@PathVariable Long id) {
        log.info("管理后台-禁用用户: id={}", id);
        adminUserService.disableUser(id);
        return Map.of("message", "用户已禁用");
    }

    @PutMapping("/{id}/unlock")
    @Operation(summary = "解锁用户")
    public Map<String, String> unlockUser(@PathVariable Long id) {
        log.info("管理后台-解锁用户: id={}", id);
        adminUserService.unlockUser(id);
        return Map.of("message", "用户已解锁");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    public Map<String, String> deleteUser(@PathVariable Long id) {
        log.info("管理后台-删除用户: id={}", id);
        adminUserService.deleteUser(id);
        return Map.of("message", "用户已删除");
    }
}
