package com.company.kb.service;

import com.company.kb.entity.Document;
import com.company.kb.entity.Document.DocumentStatus;
import com.company.kb.repository.DocumentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * 文档服务层（Document Service）— 处理文档相关的业务逻辑
 *
 * <h2>架构角色 — Service Layer Pattern</h2>
 * <p>文档服务层是 Controller 和 Repository 之间的桥梁，负责：
 * <ul>
 *   <li>分页参数的处理和转换</li>
 *   <li>查询条件的分发逻辑（根据不同的筛选条件调用不同的 Repository 方法）</li>
 *   <li>软删除操作的实现</li>
 *   <li>部分更新（Patch Update）的逻辑</li>
 * </ul>
 * </p>
 *
 * <h2>分页查询设计</h2>
 * <p>本服务大量使用 Spring Data 的分页功能。分页是处理大数据集的核心技术：
 * <ul>
 *   <li><b>为什么需要分页？</b>: 如果一次返回所有数据（如 10 万条文档），
 *       会导致响应缓慢、内存溢出、前端渲染卡顿</li>
 *   <li><b>页码约定</b>: 前端传 1-based 页码（page=1 表示第一页），
 *       Spring Data 使用 0-based 索引（page=0 表示第一页），需要做 -1 转换</li>
 *   <li><b>排序</b>: 默认按更新时间降序排列，最新的文档排在前面</li>
 * </ul>
 * </p>
 *
 * <h2>部分更新（Partial Update）</h2>
 * <p>{@link #updateDocument(Long, Document)} 方法实现了部分更新：
 * 只更新客户端实际提供的字段（非 null 的字段），忽略未提供的字段。
 * 这是 PUT 请求中常见的处理方式，也称为"合并（Merge）"策略。</p>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>{@code PageRequest.of(page, size, sort)}</b>: 创建分页请求对象。
 *       page 是页码（0-based），size 是每页大小，sort 是排序规则。</li>
 *   <li><b>{@code Page<T>}</b>: Spring Data 的分页结果对象，包含：
 *       当前页数据列表、总记录数、总页数、当前页码等元数据。</li>
 *   <li><b>软删除</b>: {@code deleteDocument()} 不执行物理删除，而是设置 deletedAt 时间戳。
 *       配合查询中的 {@code deletedAt IS NULL} 条件实现"逻辑删除"。</li>
 *   <li><b>Service 层的职责边界</b>: 本类不处理 HTTP 相关逻辑（如请求参数绑定），
 *       也不处理 SQL 细节（如 JPQL 查询）。它专注于业务规则的编排。</li>
 * </ul>
 *
 * @author Geekyous Guo
 * @since 1.0.0
 * @see com.company.kb.controller.DocumentController
 * @see com.company.kb.repository.DocumentRepository
 */
@Service
public class DocumentService {

    /** 文档数据访问层 */
    private final DocumentRepository documentRepository;

    /**
     * 构造器注入 DocumentRepository。
     *
     * @param documentRepository 文档 Repository 实例
     */
    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * 分页查询文档列表 — 支持关键词搜索、分类过滤、状态过滤。
     *
     * <h3>查询优先级（互斥）</h3>
     * <p>如果同时提供了多个筛选条件，按以下优先级处理（第一个匹配的条件生效）：
     * <ol>
     *   <li>关键词搜索（keyword）</li>
     *   <li>分类过滤（categoryId）</li>
     *   <li>状态过滤（status）</li>
     *   <li>无过滤条件 → 查询全部</li>
     * </ol>
     * </p>
     *
     * <h3>分页参数转换</h3>
     * <p>{@code PageRequest.of(page - 1, size, ...)}: 前端传入 1-based 页码，
     * Spring Data 需要 0-based 索引，所以减 1。</p>
     *
     * @param page       页码（1-based，前端传入）
     * @param size       每页大小
     * @param keyword    搜索关键词（可选）
     * @param categoryId 分类 ID（可选）
     * @param status     文档状态（可选，如 "DRAFT"、"PUBLISHED"）
     * @return 分页结果
     */
    public Page<Document> listDocuments(int page, int size, String keyword, Long categoryId, String status) {
        // 创建分页请求：page-1 将 1-based 页码转为 0-based 索引
        // Sort.by(Direction.DESC, "updatedAt") 按更新时间降序排列
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        // 按优先级依次检查筛选条件
        if (keyword != null && !keyword.isBlank()) {
            // 关键词搜索：在标题和内容中搜索
            return documentRepository.searchByKeyword(keyword, pageRequest);
        }
        if (categoryId != null) {
            // 分类过滤：查询指定分类下的文档
            return documentRepository.findByCategoryId(categoryId, pageRequest);
        }
        if (status != null) {
            // 状态过滤：将字符串转为枚举值查询
            return documentRepository.findByStatus(DocumentStatus.valueOf(status), pageRequest);
        }
        // 无筛选条件：查询所有文档（按更新时间降序）
        return documentRepository.findAll(pageRequest);
    }

    /**
     * 根据 ID 获取单个文档详情。
     *
     * <p>使用 {@code orElseThrow()} 在文档不存在时抛出异常，
     * 由全局异常处理器（如果有）转换为 404 响应。</p>
     *
     * @param id 文档 ID
     * @return 文档实体
     * @throws RuntimeException 文档不存在时
     */
    public Document getDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文档不存在"));
    }

    /**
     * 创建新文档。
     *
     * <p>直接调用 Repository 的 save() 方法。JPA 的 save() 会：
     * <ul>
     *   <li>如果实体的 ID 为 null，执行 INSERT</li>
     *   <li>如果实体的 ID 不为 null 且存在于数据库，执行 UPDATE</li>
     * </ul>
     * </p>
     *
     * @param document 要创建的文档实体（通常由 Controller 从请求体映射）
     * @return 保存后的文档实体（包含自动生成的 ID 和审计时间戳）
     */
    public Document createDocument(Document document) {
        return documentRepository.save(document);
    }

    /**
     * 部分更新文档 — 只更新非 null 的字段。
     *
     * <p><b>部分更新（Partial Update / Patch）策略</b>:
     * 先从数据库加载完整文档，然后用请求中非 null 的字段覆盖对应字段。
     * 这样客户端可以只发送需要修改的字段，不必发送完整对象。</p>
     *
     * <p>与"全量替换"的区别：全量替换会用 null 覆盖未提供的字段，
     * 部分更新则保留原有值。本方法适合 PUT 请求场景（也可用于 PATCH）。</p>
     *
     * @param id      要更新的文档 ID
     * @param updates 包含更新字段的文档对象（null 字段表示不更新）
     * @return 更新后的文档实体
     */
    public Document updateDocument(Long id, Document updates) {
        // 先获取现有文档，如果不存在则抛出异常
        Document doc = getDocument(id);
        // 逐一检查并更新非 null 的字段
        if (updates.getTitle() != null) doc.setTitle(updates.getTitle());
        if (updates.getContent() != null) doc.setContent(updates.getContent());
        if (updates.getSummary() != null) doc.setSummary(updates.getSummary());
        if (updates.getCategoryId() != null) doc.setCategoryId(updates.getCategoryId());
        // 保存更新后的文档，JPA 检测到字段变化会生成 UPDATE SQL
        return documentRepository.save(doc);
    }

    /**
     * 软删除文档 — 设置 deletedAt 时间戳而非物理删除。
     *
     * <p><b>软删除的优势</b>:
     * <ul>
     *   <li>数据可恢复：通过清除 deletedAt 字段即可"恢复"文档</li>
     *   <li>审计追踪：保留了完整的历史数据</li>
     *   <li>避免级联问题：物理删除可能因外键约束导致复杂操作</li>
     * </ul>
     * </p>
     *
     * @param id 要删除的文档 ID
     */
    public void deleteDocument(Long id) {
        Document doc = getDocument(id);
        // 设置删除时间戳，标记为"已删除"
        doc.setDeletedAt(java.time.LocalDateTime.now());
        documentRepository.save(doc);
    }

    /**
     * 获取精选/推荐文档列表。
     *
     * <p>查询标记为 isFeatured = true 的已发布文档。
     * 通常用于首页"编辑推荐"模块。</p>
     *
     * @param page 页码（1-based）
     * @param size 每页大小
     * @return 精选文档分页结果
     */
    public Page<Document> getFeaturedDocuments(int page, int size) {
        return documentRepository.findFeaturedDocuments(PageRequest.of(page - 1, size));
    }

    /**
     * 获取热门文档列表 — 按浏览量降序排列。
     *
     * <p>通常用于首页"热门文章"模块，展示浏览量最高的文档。</p>
     *
     * @param page 页码（1-based）
     * @param size 每页大小
     * @return 热门文档分页结果
     */
    public Page<Document> getPopularDocuments(int page, int size) {
        return documentRepository.findPopularDocuments(PageRequest.of(page - 1, size));
    }
}
