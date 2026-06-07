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
 * 文档实体，映射 kb_documents 表。知识库系统的核心业务对象。
 *
 * @author Geekyous Guo
 */
@Entity
@Table(name = "kb_documents")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 文档标题 */
    @Column(nullable = false, length = 255)
    private String title;

    /** 可选的简短描述，用于列表展示 */
    @Column(length = 500)
    private String summary;

    /** 文档正文，使用 LONGTEXT 支持大文本 */
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    /** 所属分类的外键 ID */
    @Column(name = "category_id")
    private Long categoryId;

    /** 文档作者，多对一关联，延迟加载 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /** 文档生命周期状态，默认 DRAFT */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.DRAFT;

    /** 反范式化的统计字段，避免实时 COUNT 聚合 */
    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "like_count")
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "comment_count")
    @Builder.Default
    private Integer commentCount = 0;

    /** 管理员标记的精选文档 */
    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    /** 从 DRAFT 变为 PUBLISHED 的时间 */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 软删除时间戳，非空表示文档已被"删除" */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 文档状态：DRAFT → PENDING → PUBLISHED → ARCHIVED */
    public enum DocumentStatus {
        DRAFT,
        PENDING,
        PUBLISHED,
        ARCHIVED
    }
}
