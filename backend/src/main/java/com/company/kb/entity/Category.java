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
 * 分类实体，映射 kb_categories 表。通过自引用 parent 字段实现树形结构。
 *
 * @author Geekyous Guo
 */
@Entity
@Table(name = "kb_categories")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户可见的分类名称，如 "Java"、"Spring Boot" */
    @Column(nullable = false, length = 50)
    private String name;

    /** URL 友好标识，如 "Spring Boot" → "spring-boot"，全局唯一 */
    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    /** 可选的分类说明 */
    @Column(length = 255)
    private String description;

    /** 父分类，null 表示顶层分类（根节点） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    /** 前端展示用的图标名称或 CSS 类名 */
    @Column(length = 50)
    private String icon;

    /** 同级分类的显示顺序，值越小越靠前 */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    /** 分类状态 */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private CategoryStatus status = CategoryStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 分类状态枚举 */
    public enum CategoryStatus {
        ACTIVE,
        INACTIVE
    }
}
