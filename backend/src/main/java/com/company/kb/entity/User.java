package com.company.kb.entity;

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
 * 用户实体类（User Entity）— 映射到数据库中的 users 表
 *
 * <h2>架构角色</h2>
 * <p>属于领域模型层（Domain Model / Entity Layer），是系统核心业务对象之一。
 * 使用 JPA（Java Persistence API，即 Jakarta Persistence）将 Java 对象映射到关系数据库表，
 * 实现 ORM（对象关系映射）。每个 User 实例对应数据库中的一行记录。</p>
 *
 * <h2>设计模式</h2>
 * <ul>
 *   <li><b>Active Record 风格（借助 Spring Data JPA）</b>: 实体类通过 Repository 进行持久化操作</li>
 *   <li><b>软删除模式（Soft Delete）</b>: 通过 {@code deletedAt} 字段标记删除，而非物理删除记录，
 *       保证数据可追溯、可恢复</li>
 *   <li><b>审计模式（Auditing）</b>: 通过 JPA Auditing 自动记录创建和修改时间</li>
 *   <li><b>Builder 模式</b>: 通过 Lombok 的 {@code @Builder} 提供链式构造对象的能力</li>
 * </ul>
 *
 * <h2>Lombok 注解说明</h2>
 * <ul>
 *   <li>{@code @Data} — 自动生成 getter、setter、equals、hashCode、toString 方法，
 *       大幅减少样板代码。注意：{@code @Data} 在继承场景下可能有坑，
 *       因为它生成的 equals/hashCode 默认包含所有字段</li>
 *   <li>{@code @Builder} — 生成建造者模式 API，例如：
 *       {@code User.builder().username("admin").password("xxx").build()}</li>
 *   <li>{@code @NoArgsConstructor} — 生成无参构造器。JPA 要求实体类必须有无参构造器，
 *       因为它通过反射创建实体实例</li>
 *   <li>{@code @AllArgsConstructor} — 生成全参构造器</li>
 * </ul>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>JPA 实体要求</b>: 必须有 {@code @Entity} 注解、无参构造器、主键字段（{@code @Id}），
 *       且不能用 final 类或 final 字段</li>
 *   <li><b>{@code @Table(name = "users")}</b>: 显式指定表名。因为 "user" 在某些数据库中是保留字，
 *       使用 "users" 可以避免 SQL 语法冲突</li>
 *   <li><b>枚举持久化</b>: {@code @Enumerated(EnumType.STRING)} 将枚举以字符串形式存储，
 *       比 {@code ORDINAL}（序号）更安全——新增枚举值不会打乱已有映射</li>
 *   <li><b>{@code @Builder.Default}</b>: 使用 Lombok {@code @Builder} 时，字段的 Java 默认值会被忽略。
 *       必须加上 {@code @Builder.Default} 注解才能使默认值生效</li>
 *   <li><b>审计字段</b>: {@code @CreatedDate} 和 {@code @LastModifiedDate} 由 Spring Data JPA
 *       自动填充，前提是主启动类上有 {@code @EnableJpaAuditing}</li>
 * </ul>
 *
 * @author Knowledge Base Team
 * @since 1.0.0
 * @see jakarta.persistence.Entity
 * @see org.springframework.data.jpa.domain.support.AuditingEntityListener
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * 用户唯一标识（主键）。
     *
     * <p>{@code @Id} 声明此字段为主键。
     * {@code @GeneratedValue(strategy = GenerationType.IDENTITY)} 表示主键由数据库自动生成
     * （即 MySQL 的 AUTO_INCREMENT）。其他策略还包括 SEQUENCE、TABLE、UUID 等。</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名 — 系统登录标识。
     *
     * <p>{@code nullable = false}: 数据库层面 NOT NULL 约束
     * {@code unique = true}: 唯一约束，确保用户名不重复
     * {@code length = 50}: 数据库字段长度 VARCHAR(50)</p>
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 密码 — 存储的是 BCrypt 加密后的密码哈希值，永远不能存储明文密码。
     *
     * <p>BCrypt 是一种自适应哈希算法，内置盐值（salt），每次加密结果都不同，
     * 能有效防止彩虹表攻击。加密后的字符串长度固定为 60 个字符。</p>
     */
    @Column(nullable = false)
    private String password;

    /**
     * 邮箱 — 可选字段，用于通知和找回密码。
     *
     * <p>{@code unique = true} 确保邮箱全局唯一，一个邮箱只能注册一个账号。</p>
     */
    @Column(unique = true, length = 100)
    private String email;

    /**
     * 手机号 — 可选的联系方式。
     */
    @Column(length = 20)
    private String phone;

    /**
     * 头像 URL — 存储用户头像图片的链接地址。
     */
    @Column
    private String avatar;

    /**
     * 用户角色 — 通过枚举定义系统中不同权限级别。
     *
     * <p>{@code @Enumerated(EnumType.STRING)}: 将枚举以字符串形式存入数据库（如 "ADMIN"），
     * 而非默认的整数序号（0, 1, 2）。这样做的好处：
     * <ul>
     *   <li>数据库中的值具有可读性</li>
     *   <li>新增枚举值不会影响已有数据的映射关系</li>
     * </ul>
     * </p>
     *
     * <p>{@code @Builder.Default}: Lombok Builder 模式下设置默认值。
     * 新用户的默认角色为 {@code Role.USER}。</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    /**
     * 用户账号状态 — 用于账号管理和安全控制。
     *
     * <p>ACTIVE(活跃)、INACTIVE(停用)、LOCKED(锁定，例如密码错误次数过多)。
     * 登录时需要检查此状态，非 ACTIVE 用户不允许登录。</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * 最后登录时间 — 每次用户成功登录时更新此字段，用于安全审计和用户活跃度统计。
     */
    @Column
    private LocalDateTime lastLoginAt;

    /**
     * 创建时间 — 记录用户注册的时间。
     *
     * <p>{@code @CreatedDate}: Spring Data JPA 审计注解，在实体首次持久化时自动设置为当前时间。
     * {@code updatable = false}: 该字段一旦设置就不允许修改，保证创建时间的不可变性。</p>
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间 — 记录用户信息最后一次被修改的时间。
     *
     * <p>{@code @LastModifiedDate}: 每次实体被更新并保存时，自动刷新为当前时间。
     * 配合 {@code @CreatedDate} 可以追踪任何一条记录的完整生命周期。</p>
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 软删除标记时间 — 记录"删除"操作的时间戳。
     *
     * <p><b>软删除 vs 硬删除</b>:
     * <ul>
     *   <li>硬删除：{@code DELETE FROM users WHERE id = ?}，数据从数据库中永久消失</li>
     *   <li>软删除：{@code UPDATE users SET deleted_at = NOW() WHERE id = ?}，数据仍在数据库中，
     *       但通过查询条件 {@code WHERE deleted_at IS NULL} 过滤掉"已删除"的记录</li>
     * </ul>
     * 软删除的优点：数据可恢复、支持审计追踪、避免外键级联删除问题。
     * </p>
     *
     * <p>当此字段为 {@code null} 时，表示用户正常存在；当有值时，表示用户已被"删除"。</p>
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 用户角色枚举 — 定义系统中的角色层级。
     *
     * <p>采用 RBAC（Role-Based Access Control，基于角色的访问控制）设计：
     * <ul>
     *   <li>{@code USER} — 普通用户：可以浏览文档、评论等基本操作</li>
     *   <li>{@code EDITOR} — 编辑者：拥有文档的创建、编辑、发布权限</li>
     *   <li>{@code ADMIN} — 管理员：拥有系统所有权限，包括用户管理</li>
     * </ul>
     * 权限从低到高：USER &lt; EDITOR &lt; ADMIN</p>
     */
    public enum Role {
        /** 普通用户 — 基础浏览权限 */
        USER,

        /** 编辑者 — 可以创建和编辑文档 */
        EDITOR,

        /** 管理员 — 拥有系统全部权限 */
        ADMIN
    }

    /**
     * 用户状态枚举 — 用于账号生命周期管理。
     *
     * <ul>
     *   <li>{@code ACTIVE} — 正常状态，可以登录和使用系统</li>
     *   <li>{@code INACTIVE} — 停用状态，通常由管理员手动设置</li>
     *   <li>{@code LOCKED} — 锁定状态，通常由系统自动设置（如密码错误次数过多）</li>
     * </ul>
     */
    public enum UserStatus {
        /** 活跃 — 正常可用状态 */
        ACTIVE,

        /** 停用 — 管理员手动禁用 */
        INACTIVE,

        /** 锁定 — 安全策略触发（如密码重试过多） */
        LOCKED
    }
}
