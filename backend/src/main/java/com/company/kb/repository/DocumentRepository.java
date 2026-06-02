package com.company.kb.repository;

import com.company.kb.entity.Document;
import com.company.kb.entity.Document.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 文档数据访问层（Document Repository）— 文档实体的数据库操作接口
 *
 * <h2>架构角色</h2>
 * <p>属于数据访问层，负责所有与 Document 实体相关的数据库操作。
 * 本接口展示了 Spring Data JPA 的两种查询方式：派生查询和自定义 JPQL 查询。</p>
 *
 * <h2>两种查询方式对比</h2>
 * <table>
 *   <tr><th>方式</th><th>适用场景</th><th>示例</th></tr>
 *   <tr>
 *     <td>派生查询（Derived Query）</td>
 *     <td>简单条件查询，方法名能表达的条件</td>
 *     <td>{@code findByStatus(status, pageable)}</td>
 *   </tr>
 *   <tr>
 *     <td>自定义 {@code @Query}</td>
 *     <td>复杂查询、多条件组合、子查询等</td>
 *     <td>{@code @Query("SELECT d FROM Document d WHERE ...")}</td>
 *   </tr>
 * </table>
 *
 * <h2>JPQL（Java Persistence Query Language）简介</h2>
 * <p>JPQL 是 JPA 的查询语言，语法类似 SQL 但操作的是<b>实体对象和字段</b>，
 * 而非数据库表和列。例如用 {@code d.title} 而非 {@code title} 列名。
 * JPA 提供者（如 Hibernate）会将 JPQL 翻译成具体数据库的 SQL。</p>
 *
 * <h2>分页查询</h2>
 * <p>返回 {@code Page<Document>} 的方法接受 {@code Pageable} 参数，
 * Spring Data JPA 自动处理分页逻辑（LIMIT、OFFSET）和总数统计（COUNT）。</p>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>{@code @Query} 注解</b>: 当方法名无法表达复杂查询逻辑时，使用 {@code @Query}
 *       编写自定义 JPQL。value 属性中写 JPQL 语句，nativeQuery 默认 false。</li>
 *   <li><b>{@code @Param} 注解</b>: 将方法参数绑定到 JPQL 中的命名参数（如 {@code :keyword}）。
 *       Spring Boot 3.x 在编译时保留参数名时可以省略，但显式声明更清晰安全。</li>
 *   <li><b>{@code Pageable} 参数</b>: 由 Controller 层通过 {@code PageRequest.of()} 创建，
 *       包含页码、每页大小、排序信息。Spring Data 自动执行两条 SQL：
 *       一条查数据（带 LIMIT/OFFSET），一条查总数（COUNT）。</li>
 *   <li><b>软删除过滤</b>: 注意自定义查询中都有 {@code d.deletedAt IS NULL} 条件，
 *       这是软删除模式的要求——手动过滤"已删除"的记录。</li>
 * </ul>
 *
 * @author Knowledge Base Team
 * @since 1.0.0
 * @see JpaRepository
 * @see Document
 * @see Pageable
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * 按文档状态查询文档列表（分页）。
     *
     * <p>派生查询方式。Spring Data JPA 解析方法名 {@code findByStatus}，
     * 自动生成 SQL：{@code SELECT * FROM documents WHERE status = ?}</p>
     *
     * @param status   文档状态筛选条件
     * @param pageable 分页参数（页码、每页大小、排序规则）
     * @return 分页结果，包含当前页的文档列表和分页元数据（总页数、总记录数等）
     */
    Page<Document> findByStatus(DocumentStatus status, Pageable pageable);

    /**
     * 按分类 ID 查询文档列表（分页）。
     *
     * <p>注意这里使用的是 {@code categoryId}（Long 类型），而非 Category 实体对象。
     * Spring Data JPA 能正确解析并生成：{@code WHERE category_id = ?}</p>
     *
     * @param categoryId 分类 ID
     * @param pageable   分页参数
     * @return 该分类下的文档分页结果
     */
    Page<Document> findByCategoryId(Long categoryId, Pageable pageable);

    /**
     * 按作者 ID 查询文档列表（分页）。
     *
     * <p>这里的 {@code AuthorId} 对应 {@code author} 字段关联的 User 实体的 ID。
     * Spring Data JPA 自动解析为外键列条件：{@code WHERE author_id = ?}</p>
     *
     * @param authorId 作者（User）的 ID
     * @param pageable 分页参数
     * @return 该作者的文档分页结果
     */
    Page<Document> findByAuthorId(Long authorId, Pageable pageable);

    /**
     * 全文搜索 — 在已发布的文档中按关键词搜索标题或内容。
     *
     * <p>自定义 JPQL 查询，逻辑：
     * <ol>
     *   <li>只搜索已发布的文档（{@code d.status = 'PUBLISHED'}）</li>
     *   <li>排除已软删除的文档（{@code d.deletedAt IS NULL}）</li>
     *   <li>在标题和内容中搜索关键词（{@code LIKE %keyword%}）</li>
     * </ol>
     * </p>
     *
     * <p><b>性能提示</b>: LIKE '%keyword%' 无法使用索引，会导致全表扫描。
     * 在大数据量场景下，应该使用全文索引（MySQL FULLTEXT）或搜索引擎（Elasticsearch）。</p>
     *
     * @param keyword  搜索关键词
     * @param pageable 分页参数
     * @return 匹配的文档分页结果
     */
    @Query("SELECT d FROM Document d WHERE d.status = 'PUBLISHED' AND d.deletedAt IS NULL AND (d.title LIKE %:keyword% OR d.content LIKE %:keyword%)")
    Page<Document> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 获取热门文档 — 按浏览量降序排列（排除已删除文档）。
     *
     * <p>自定义 JPQL 查询。{@code ORDER BY d.viewCount DESC} 使浏览量最高的文档排在前面。
     * 常用于首页"热门文章"模块。</p>
     *
     * @param pageable 分页参数（通常只取前 10 条）
     * @return 按浏览量排序的文档分页结果
     */
    @Query("SELECT d FROM Document d WHERE d.deletedAt IS NULL ORDER BY d.viewCount DESC")
    Page<Document> findPopularDocuments(Pageable pageable);

    /**
     * 获取推荐/精选文档 — 管理员标记的高质量内容。
     *
     * <p>查询条件：已发布 + 已标记为精选 + 未删除。
     * 常用于首页"编辑推荐"模块。</p>
     *
     * @param pageable 分页参数
     * @return 精选文档分页结果
     */
    @Query("SELECT d FROM Document d WHERE d.status = 'PUBLISHED' AND d.isFeatured = true AND d.deletedAt IS NULL")
    Page<Document> findFeaturedDocuments(Pageable pageable);

    /**
     * 统计指定状态的文档数量。
     *
     * <p>派生查询。生成 SQL：{@code SELECT COUNT(*) FROM documents WHERE status = ?}</p>
     *
     * <p>使用场景：仪表盘统计、状态分布报表。</p>
     *
     * @param status 要统计的文档状态
     * @return 该状态下的文档总数
     */
    long countByStatus(DocumentStatus status);
}
