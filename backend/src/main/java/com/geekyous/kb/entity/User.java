package com.geekyous.kb.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.geekyous.kb.converter.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

/**
 * 用户实体，映射 kb_users 表。支持软删除和审计时间自动填充。
 *
 * <p>敏感字段（email、phone）通过 {@link EncryptedStringConverter} 在数据库中加密存储，
 * email_hash 列自动维护 SHA-256 哈希用于查询匹配。
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

    /** BCrypt 加密后的密码哈希，禁止序列化到 API 响应 */
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    /** 邮箱（AES 加密存储，解密后为明文），唯一性通过 emailHash 列保障 */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 255)
    private String email;

    /** 邮箱 SHA-256 哈希，用于查询匹配和唯一性校验（自动维护，无需手动赋值） */
    @Column(name = "email_hash", unique = true, length = 64)
    private String emailHash;

    /** 手机号（AES 加密存储，解密后为明文） */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 255)
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

    /**
     * 持久化前回调 — 自动计算 email_hash，保持与 email 字段同步。
     * 仅在 email 非空时计算，email 为空则 hash 也置空。
     */
    @PrePersist
    @PreUpdate
    private void computeEmailHash() {
        if (email != null && !email.isBlank()) {
            this.emailHash = sha256Hex(email);
        } else {
            this.emailHash = null;
        }
    }

    /**
     * SHA-256 哈希计算 — 将明文转为 64 字符十六进制字符串。
     * 静态方法避免依赖 Spring Bean（Entity 回调在 Bean 初始化阶段可能尚未就绪）。
     */
    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
