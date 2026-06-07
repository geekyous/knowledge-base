package com.geekyous.kb.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 用户实体，映射 kb_users 表。支持软删除和审计时间自动填充。
 *
 * @author Geekyous Guo
 */
@Entity
@Table(name = "kb_users")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 系统登录标识，全局唯一 */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** BCrypt 加密后的密码哈希 */
    @Column(nullable = false)
    private String password;

    /** 可选，用于通知和找回密码 */
    @Column(unique = true, length = 100)
    private String email;

    /** 可选的联系方式 */
    @Column(length = 20)
    private String phone;

    /** 头像图片链接地址 */
    @Column
    private String avatar;

    /** 用户角色，默认 USER */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    /** 账号状态，非 ACTIVE 用户不允许登录 */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    /** 每次成功登录时更新 */
    @Column
    private LocalDateTime lastLoginAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 软删除标记，非空表示用户已被"删除" */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** RBAC 角色枚举，权限从低到高：USER < EDITOR < ADMIN */
    public enum Role {
        USER,
        EDITOR,
        ADMIN
    }

    /** 账号状态枚举 */
    public enum UserStatus {
        ACTIVE,
        INACTIVE,
        LOCKED
    }
}
