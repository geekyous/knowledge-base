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
 * 分类实体类（Category Entity）— 映射到数据库中的 categories 表
 *
 * <h2>架构角色</h2>
 * <p>分类（Category）用于组织和管理文档，类似文件系统中的文件夹。
 * 通过自引用关联（Self-Referencing Relationship）实现树形分类结构。</p>
 *
 * <h2>核心设计：自引用关联 + 树形结构</h2>
 * <p>本实体最重要的设计是 {@code parent} 字段——它引用了另一个 Category 对象，
 * 形成了"自己引用自己"的关系。这是在关系数据库中实现树形结构的经典方法：</p>
 * <pre>
 *   技术文档 (id=1, parent=null)       ← 顶层分类（根节点）
 *   ├── Java (id=2, parent_id=1)        ← 二级分类
 *   │   ├── Spring Boot (id=5, parent_id=2)  ← 三级分类
 *   │   └── JVM (id=6, parent_id=2)
 *   └── Python (id=3, parent_id=1)
 *   产品文档 (id=4, parent=null)        ← 另一个顶层分类
 * </pre>
 *
 * <h3>邻接表模型（Adjacency List）</h3>
 * <p>这种设计称为"邻接表模型"——每条记录存储一个指向父记录的外键。
 * 优点：结构简单直观、增删改容易。缺点：查询整个子树需要递归查询，
 * 在深层嵌套时性能不如嵌套集（Nested Set）或物化路径（Materialized Path）模型。</p>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>自引用关联</b>: {@code @ManyToOne} 的目标实体就是自身（Category），
 *       这在 JPA 中完全合法。{@code parent_id} 列存储父分类的 ID，
 *       顶层分类的 {@code parent_id} 为 NULL。</li>
 *   <li><b>{@code FetchType.LAZY}</b>: 树形结构中如果使用 EAGER 加载，
 *       加载一个节点就会递归加载整个树，性能灾难。必须使用 LAZY。</li>
 *   <li><b>slug 字段</b>: URL 友好的标识符。例如分类名 "Spring Boot" 对应 slug "spring-boot"，
 *       用于生成 SEO 友好的 URL：{@code /categories/spring-boot}</li>
 *   <li><b>sortOrder 字段</b>: 控制同级分类的显示顺序，数值越小越靠前。</li>
 * </ul>
 *
 * @author Geekyous Guo
 * @since 1.0.0
 * @see com.company.kb.repository.CategoryRepository
 */
@Entity
@Table(name = "kb_categories")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    /**
     * 分类唯一标识（主键）。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 分类名称 — 用户可见的显示名称，如 "Java"、"Spring Boot"。
     */
    @Column(nullable = false, length = 50)
    private String name;

    /**
     * URL 友好的分类标识 — 用于 SEO 友好的 URL 路径。
     *
     * <p>例如：名称 "Spring Boot" → slug "spring-boot"。
     * 全局唯一约束确保不会产生 URL 冲突。</p>
     */
    @Column(nullable = false, unique = true, length = 50)
    private String slug;

    /**
     * 分类描述 — 可选的详细说明文本。
     */
    @Column(length = 255)
    private String description;

    /**
     * 父分类 — 自引用关联，实现树形结构的核心。
     *
     * <p><b>自引用关联详解</b>:
     * <ul>
     *   <li>当 {@code parent} 为 {@code null} 时，表示这是一个顶层分类（根节点）</li>
     *   <li>当 {@code parent} 不为 {@code null} 时，表示这是一个子分类</li>
     *   <li>{@code @JoinColumn(name = "parent_id")}: 数据库中的 parent_id 列存储父分类的 ID</li>
     *   <li>{@code FetchType.LAZY}: 延迟加载父分类，避免递归加载整棵树</li>
     * </ul>
     * </p>
     *
     * <p><b>注意</b>: 如果需要双向导航（从父分类获取所有子分类），
     * 可以添加 {@code @OneToMany(mappedBy = "parent")} 的 children 字段。
     * 本项目中未添加，通过 Repository 查询获取子分类。</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    /**
     * 分类图标 — 存储图标名称或 CSS 类名，用于前端展示。
     */
    @Column(length = 50)
    private String icon;

    /**
     * 排序序号 — 同级分类的显示顺序，值越小越靠前。
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * 分类状态 — 控制分类的可见性。
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private CategoryStatus status = CategoryStatus.ACTIVE;

    /**
     * 创建时间 — 由 JPA 审计自动填充。
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间 — 由 JPA 审计自动更新。
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 分类状态枚举。
     */
    public enum CategoryStatus {
        /** 活跃 — 正常显示 */
        ACTIVE,

        /** 停用 — 不显示但数据保留 */
        INACTIVE
    }
}
