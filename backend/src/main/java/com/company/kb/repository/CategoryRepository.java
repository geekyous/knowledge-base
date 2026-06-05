package com.company.kb.repository;

import com.company.kb.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 分类数据访问层（Category Repository）— 分类实体的数据库操作接口
 *
 * <h2>架构角色</h2>
 * <p>属于数据访问层，负责 Category 实体的 CRUD 和树形结构查询。
 * 配合 {@link com.company.kb.entity.Category} 的自引用关联设计，
 * 本接口提供了构建分类树所需的查询方法。</p>
 *
 * <h2>树形结构查询策略</h2>
 * <p>本项目的分类树采用<b>两级查询策略</b>：
 * <ol>
 *   <li>查询所有顶层分类（parent 为 null 的记录）— 对应 {@link #findByParentIdIsNull()}</li>
 *   <li>对每个顶层分类，查询其直接子分类 — 对应 {@link #findByParentId(Long)}</li>
 * </ol>
 * 前端拿到这些数据后，在客户端组装成完整的树形结构。</p>
 *
 * <h2>Repository 模式</h2>
 * <p>本接口体现了 Repository 模式的核心思想：
 * <ul>
 *   <li>每个聚合根（Aggregate Root）对应一个 Repository</li>
 *   <li>Repository 只提供集合式接口（类似 Map：get/put/remove）</li>
 *   <li>不暴露底层 SQL 或 ORM 细节</li>
 * </ul>
 * </p>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>{@code findByParentIdIsNull()}</b>: Spring Data JPA 支持 {@code IsNull} / {@code IsNotNull}
 *       关键字。生成的 SQL 包含 {@code WHERE parent_id IS NULL}，用于查找根节点。</li>
 *   <li><b>树形数据的加载策略</b>: 本项目使用"逐层查询"方式。另一种方案是一次性查出所有分类，
 *       然后在内存中构建树。后者减少数据库查询次数，适合分类数量不多的场景。</li>
 *   <li><b>{@code List} vs {@code Page} 返回值</b>: 分类通常数量不多，
 *       不需要分页。文档等大量数据才需要 {@code Page<T>} 返回值。</li>
 * </ul>
 *
 * @author Geekyous Guo
 * @since 1.0.0
 * @see JpaRepository
 * @see Category
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * 查询所有顶层分类（根节点）。
     *
     * <p>Spring Data JPA 解析方法名 {@code findByParentIdIsNull}，生成 SQL：
     * {@code SELECT * FROM categories WHERE parent_id IS NULL}</p>
     *
     * <p>在自引用关联模型中，{@code parent_id} 为 NULL 的记录就是树的根节点。
     * 这是构建分类树的入口——先获取所有根节点，再逐个加载子节点。</p>
     *
     * <p>使用场景：{@code /api/v1/categories/tree} 接口返回分类树结构。</p>
     *
     * @return 所有顶层分类列表，如果没有顶层分类则返回空列表
     */
    List<Category> findByParentIdIsNull();

    /**
     * 查询指定父分类下的所有直接子分类。
     *
     * <p>生成 SQL：{@code SELECT * FROM categories WHERE parent_id = ?}</p>
     *
     * <p>注意：这只返回<b>直接子分类</b>（一级子节点），不会递归返回孙子节点。
     * 如果需要整棵子树，需要递归调用此方法，或改用 CTE（Common Table Expression）查询。</p>
     *
     * <p>使用场景：{@code /api/v1/categories/{id}/children} 接口。</p>
     *
     * @param parentId 父分类的 ID
     * @return 该父分类下的直接子分类列表
     */
    List<Category> findByParentId(Long parentId);

    /**
     * 根据 slug（URL 标识）查找分类。
     *
     * <p>生成 SQL：{@code SELECT * FROM categories WHERE slug = ? LIMIT 1}</p>
     *
     * <p>使用场景：通过 URL 路径查找分类，如 {@code /categories/spring-boot}。
     * slug 作为 URL 友好的标识，比使用数字 ID 更有利于 SEO 和用户体验。</p>
     *
     * @param slug 分类的 URL 标识（如 "java"、"spring-boot"）
     * @return 包含匹配分类的 Optional，可能为空
     */
    Optional<Category> findBySlug(String slug);
}
