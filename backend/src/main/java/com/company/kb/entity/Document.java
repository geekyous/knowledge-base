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
 * 文档实体类（Document Entity）— 映射到数据库中的 documents 表
 *
 * <h2>架构角色</h2>
 * <p>文档是知识库系统的核心业务实体。每个 Document 实例代表一篇知识文档，
 * 包含标题、内容、状态、统计信息等。这是系统中最重要、字段最多的实体。</p>
 *
 * <h2>设计模式与关键技术</h2>
 * <ul>
 *   <li><b>多对一关联（{@code @ManyToOne}）</b>: 文档与用户之间是"多对一"关系——
 *       多篇文档可以属于同一个作者（User）。这是 JPA 中最常见的关联关系。</li>
 *   <li><b>延迟加载（{@code FetchType.LAZY}）</b>: 查询文档时不自动加载关联的 User 对象，
 *       只有在调用 {@code getAuthor()} 时才触发 SQL 查询。避免 N+1 查询问题。</li>
 *   <li><b>软删除模式（Soft Delete）</b>: 通过 {@code deletedAt} 字段标记删除，
 *       保留数据用于审计和恢复。</li>
 *   <li><b>统计字段反范式化</b>: viewCount、likeCount、commentCount 直接存储在文档表中，
 *       避免每次查询都执行 COUNT 聚合，提升读取性能。</li>
 * </ul>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>{@code @ManyToOne(fetch = FetchType.LAZY)}</b>: 默认情况下 {@code @ManyToOne} 使用
 *       {@code EAGER}（立即加载），这意味着每次查询文档都会额外发一条 SQL 去加载用户。
 *       显式设置 {@code LAZY} 是最佳实践，但需要注意在 Session 关闭后访问懒加载属性会抛出
 *       {@code LazyInitializationException}。</li>
 *   <li><b>{@code @JoinColumn}</b>: 指定外键列名。{@code name = "author_id"} 表示在 documents 表中
 *       有一个 author_id 列作为外键指向 users 表。</li>
 *   <li><b>{@code columnDefinition = "LONGTEXT"}</b>: 文档内容可能很长，使用 MySQL 的 LONGTEXT 类型
 *       （最大 4GB），而非常规的 VARCHAR。</li>
 *   <li><b>反范式化（Denormalization）</b>: viewCount 等统计字段可以通过关联查询实时计算，
 *       但在高并发场景下直接 COUNT 性能差。将这些数值冗余存储在主表中是一种常见的优化手段。</li>
 * </ul>
 *
 * @author Geekyous Guo
 * @since 1.0.0
 * @see User
 * @see com.company.kb.repository.DocumentRepository
 */
@Entity
@Table(name = "kb_documents")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    /**
     * 文档唯一标识（主键）。
     *
     * <p>使用数据库自增策略（{@code IDENTITY}），主键由 MySQL 的 AUTO_INCREMENT 自动分配。</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文档标题 — 必填字段，最大 255 个字符。
     */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * 文档摘要 — 可选的简短描述，用于列表展示，最大 500 个字符。
     */
    @Column(length = 500)
    private String summary;

    /**
     * 文档正文内容 — 使用 LONGTEXT 类型支持大文本。
     *
     * <p>{@code columnDefinition = "LONGTEXT"} 直接指定数据库列的类型定义，
     * 适用于内容可能超过 VARCHAR 最大长度（65535 字节）的场景。</p>
     */
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    /**
     * 分类 ID — 文档所属分类的外键。
     *
     * <p>注意：这里使用的是 {@code Long categoryId} 而非 {@code @ManyToOne Category}，
     * 这是另一种常见的关联方式。好处是查询时不需要加载完整的 Category 实体，
     * 缺点是无法使用 JPA 的级联操作。实际项目中两种方式都常见。</p>
     */
    @Column(name = "category_id")
    private Long categoryId;

    /**
     * 文档作者 — 与 User 实体的多对一关联。
     *
     * <h3>关联关系详解</h3>
     * <p>{@code @ManyToOne}: 定义"多对一"关系。多篇文档（Many）属于一个用户（One）。</p>
     * <p>{@code fetch = FetchType.LAZY}: 延迟加载策略。查询文档时不会自动执行 JOIN 查询获取作者信息，
     * 只有当代码实际调用 {@code document.getAuthor()} 时才触发额外查询。</p>
     *
     * <h3>为什么选择 LAZY？</h3>
     * <ul>
     *   <li>如果用 EAGER，查询 20 条文档会额外发 20 条 SQL 查询用户（N+1 问题更严重）</li>
     *   <li>如果用 LAZY + JOIN FETCH，可以在需要时一次性加载所有关联数据</li>
     *   <li>LAZY 是{@code @ManyToOne} 的非默认行为（默认是 EAGER），需要显式声明</li>
     * </ul>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     * 文档状态 — 控制文档在生命周期中的阶段。
     *
     * <p>工作流：DRAFT(草稿) → PENDING(待审核) → PUBLISHED(已发布) → ARCHIVED(已归档)。
     * 每个状态决定了文档对用户的可见性和可操作性。</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.DRAFT;

    /**
     * 浏览次数 — 反范式化的统计字段。
     *
     * <p>每次用户查看文档详情时递增。直接存储在文档表中避免每次都 COUNT 浏览记录表。</p>
     */
    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    /**
     * 点赞次数 — 记录文档被点赞的总数。
     */
    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0;

    /**
     * 评论次数 — 记录文档下的评论总数。
     */
    @Column(name = "comment_count")
    @Builder.Default
    private Integer commentCount = 0;

    /**
     * 是否为推荐/精选文档 — 管理员可手动标记优质内容。
     *
     * <p>使用 Boolean 类型映射到数据库的 TINYINT(1)（MySQL）。</p>
     */
    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    /**
     * 发布时间 — 文档从 DRAFT 变为 PUBLISHED 状态的时间。
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * 创建时间 — 文档首次创建的时间，由 JPA 审计自动填充，且不可修改。
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间 — 文档最后一次修改的时间，由 JPA 审计自动更新。
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 软删除时间戳 — 非空表示文档已被"删除"。
     *
     * <p>查询时需要手动添加 {@code WHERE deleted_at IS NULL} 条件来过滤已删除的文档。
     * 也可以使用 Hibernate 的 {@code @SQLDelete} 和 {@code @Where} 注解来自动处理。</p>
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 文档状态枚举 — 定义文档的完整生命周期。
     *
     * <p>状态流转：
     * <pre>
     *   DRAFT → PENDING → PUBLISHED → ARCHIVED
     *                  ↘ (rejected) → DRAFT
     * </pre>
     * <ul>
     *   <li>{@code DRAFT} — 草稿：作者正在编辑中，仅作者可见</li>
     *   <li>{@code PENDING} — 待审核：已提交等待审核人员审批</li>
     *   <li>{@code PUBLISHED} — 已发布：审核通过，对所有用户可见</li>
     *   <li>{@code ARCHIVED} — 已归档：过期内容归档保留，不再出现在常规列表中</li>
     * </ul>
     * </p>
     */
    public enum DocumentStatus {
        /** 草稿 — 编辑中，未提交 */
        DRAFT,

        /** 待审核 — 已提交，等待审批 */
        PENDING,

        /** 已发布 — 审核通过，公开可见 */
        PUBLISHED,

        /** 已归档 — 过期归档，不在常规列表显示 */
        ARCHIVED
    }
}
