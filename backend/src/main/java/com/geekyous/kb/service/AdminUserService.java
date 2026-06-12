package com.geekyous.kb.service;

import com.geekyous.kb.dto.request.CreateUserRequest;
import com.geekyous.kb.dto.request.ResetPasswordRequest;
import com.geekyous.kb.dto.request.UpdateUserRequest;
import com.geekyous.kb.dto.response.UserAdminResponse;
import com.geekyous.kb.entity.User;
import com.geekyous.kb.entity.User.Role;
import com.geekyous.kb.entity.User.UserStatus;
import com.geekyous.kb.exception.BusinessException;
import com.geekyous.kb.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户管理服务 — 提供后台用户管理的业务逻辑，包括列表查询、
 * 创建、更新、密码重置、禁用/解锁、软删除等操作。
 *
 * @author Geekyous Guo
 */
@Slf4j
@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 分页查询用户列表，支持关键词搜索、角色过滤、状态过滤。
     *
     * 查询优先级（互斥）：keyword > role > status > 全部
     *
     * @param page    页码（1-based）
     * @param size    每页大小
     * @param keyword 搜索关键词（匹配用户名或邮箱）
     * @param role    角色过滤（可选）
     * @param status  状态过滤（可选）
     * @return 分页用户管理视图
     */
    public Page<UserAdminResponse> listUsers(int page, int size, String keyword, String role, String status) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (keyword != null && !keyword.isBlank()) {
            log.debug("用户列表查询: keyword={}", keyword);
            return userRepository.findByUsernameContainingOrEmailContaining(keyword, keyword, pageable)
                    .map(this::toResponse);
        }
        if (role != null && !role.isBlank()) {
            log.debug("用户列表查询: role={}", role);
            return userRepository.findByRole(Role.valueOf(role), pageable)
                    .map(this::toResponse);
        }
        if (status != null && !status.isBlank()) {
            log.debug("用户列表查询: status={}", status);
            return userRepository.findByStatus(UserStatus.valueOf(status), pageable)
                    .map(this::toResponse);
        }

        return userRepository.findAll(pageable)
                .map(this::toResponse);
    }

    /**
     * 获取用户详情。
     *
     * @param id 用户 ID
     * @return 用户管理视图
     */
    public UserAdminResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        return toResponse(user);
    }

    /**
     * 创建新用户 — 校验用户名唯一性，加密密码后持久化。
     *
     * @param req 创建用户请求
     * @return 创建后的用户管理视图
     */
    @Transactional
    public UserAdminResponse createUser(CreateUserRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BusinessException(409, "用户名已存在");
        }

        User user = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .email(req.getEmail())
                .phone(req.getPhone())
                .role(req.getRole())
                .build();

        User saved = userRepository.save(user);
        log.info("用户创建: username={}, role={}", saved.getUsername(), saved.getRole());
        return toResponse(saved);
    }

    /**
     * 更新用户信息 — 只更新请求中非 null 的字段。
     *
     * @param id  用户 ID
     * @param req 更新用户请求
     * @return 更新后的用户管理视图
     */
    @Transactional
    public UserAdminResponse updateUser(Long id, UpdateUserRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        if (req.getEmail() != null) {
            user.setEmail(req.getEmail());
        }
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone());
        }
        if (req.getRole() != null) {
            user.setRole(req.getRole());
        }
        if (req.getStatus() != null) {
            user.setStatus(req.getStatus());
        }

        User saved = userRepository.save(user);
        log.info("用户更新: id={}", id);
        return toResponse(saved);
    }

    /**
     * 重置用户密码 — 加密新密码后保存。
     *
     * @param id  用户 ID
     * @param req 重置密码请求
     */
    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        log.info("用户密码重置: id={}", id);
    }

    /**
     * 禁用用户 — 将状态设为 INACTIVE。
     *
     * @param id 用户 ID
     */
    @Transactional
    public void disableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        log.info("用户禁用: id={}", id);
    }

    /**
     * 解锁用户 — 将状态设为 ACTIVE（解除锁定或禁用）。
     *
     * @param id 用户 ID
     */
    @Transactional
    public void unlockUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("用户解锁: id={}", id);
    }

    /**
     * 软删除用户 — 设置 deletedAt 时间戳，数据可恢复。
     *
     * @param id 用户 ID
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        user.setDeletedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
        log.info("用户删除(软删除): id={}", id);
    }

    /**
     * 实体转响应 DTO — email 和 phone 通过 @Convert 自动解密。
     */
    private UserAdminResponse toResponse(User user) {
        return UserAdminResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
