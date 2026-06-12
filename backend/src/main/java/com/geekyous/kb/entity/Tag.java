package com.geekyous.kb.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 标签实体，映射 kb_tags 表。用于文档的分类标签管理。
 *
 * @author Geekyous Guo
 */
@Entity
@Table(name = "kb_tags")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 标签名称，全局唯一 */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /** 标签颜色，十六进制格式（如 "#ef4444"） */
    @Column(length = 7)
    private String color;

    /** 使用次数，反范式化字段，避免实时 COUNT 聚合 */
    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
