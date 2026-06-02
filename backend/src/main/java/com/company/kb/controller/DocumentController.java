package com.company.kb.controller;

import com.company.kb.dto.ApiResponse;
import com.company.kb.entity.Document;
import com.company.kb.service.DocumentService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 文档控制器（Document Controller）— 处理文档的 CRUD 和查询请求
 *
 * <h2>架构角色 — RESTful CRUD Controller</h2>
 * <p>本控制器实现了文档资源的完整 CRUD（Create, Read, Update, Delete）操作，
 * 是 RESTful API 设计的典型范例。</p>
 *
 * <h2>RESTful API 设计原则</h2>
 * <p>使用 HTTP 方法表达操作语义，URL 标识资源：
 * <table>
 *   <tr><th>HTTP 方法</th><th>路径</th><th>功能</th><th>幂等性</th></tr>
 *   <tr><td>GET</td><td>/api/v1/documents</td><td>获取文档列表</td><td>是</td></tr>
 *   <tr><td>GET</td><td>/api/v1/documents/{id}</td><td>获取单个文档</td><td>是</td></tr>
 *   <tr><td>POST</td><td>/api/v1/documents</td><td>创建文档</td><td>否</td></tr>
 *   <tr><td>PUT</td><td>/api/v1/documents/{id}</td><td>更新文档</td><td>是</td></tr>
 *   <tr><td>DELETE</td><td>/api/v1/documents/{id}</td><td>删除文档</td><td>是</td></tr>
 *   <tr><td>GET</td><td>/api/v1/documents/featured</td><td>获取精选文档</td><td>是</td></tr>
 *   <tr><td>GET</td><td>/api/v1/documents/popular</td><td>获取热门文档</td><td>是</td></tr>
 * </table>
 * </p>
 *
 * <h3>幂等性说明</h3>
 * <ul>
 *   <li><b>幂等（Idempotent）</b>: 多次执行同一请求，效果与一次相同。
 *       GET、PUT、DELETE 天然幂等。</li>
 *   <li><b>非幂等</b>: POST 每次调用可能创建新资源，不是幂等的。</li>
 * </ul>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>{@code @PathVariable}</b>: 从 URL 路径中提取变量值。
 *       如 {@code /documents/123} 中的 123 会绑定到 {@code Long id} 参数。</li>
 *   <li><b>{@code @RequestParam}</b>: 从 URL 查询参数中提取值。
 *       如 {@code ?page=1&size=20} 中的值会绑定到对应参数。
 *       {@code defaultValue} 设置默认值，{@code required = false} 表示参数可选。</li>
 *   <li><b>路径冲突避免</b>: {@code /featured} 和 {@code /popular} 必须在
 *       {@code /{id}} 之前声明，否则 Spring 会将 "featured" 当作 id 参数解析。</li>
 *   <li><b>{@code Page<T>}</b>: Spring Data 的分页结果包含丰富的元数据：
 *       内容列表、总页数、总记录数、当前页码、是否首页/末页等。</li>
 * </ul>
 *
 * @author Knowledge Base Team
 * @since 1.0.0
 * @see DocumentService
 * @see Document
 */
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    /** 文档服务层 — 处理业务逻辑 */
    private final DocumentService documentService;

    /**
     * 构造器注入 DocumentService。
     *
     * @param documentService 文档服务实例
     */
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 获取文档列表 — 支持分页、搜索、分类过滤、状态过滤。
     *
     * <h3>请求示例</h3>
     * <pre>
     * GET /api/v1/documents?page=1&size=20
     * GET /api/v1/documents?keyword=Spring&categoryId=2
     * GET /api/v1/documents?status=PUBLISHED
     * </pre>
     *
     * @param page       页码（默认第 1 页）
     * @param size       每页大小（默认 20 条）
     * @param keyword    搜索关键词（可选）
     * @param categoryId 分类 ID（可选）
     * @param status     文档状态（可选，如 "DRAFT"、"PUBLISHED"）
     * @return 分页文档列表
     */
    @GetMapping
    public ApiResponse<Page<Document>> list(
            @RequestParam(defaultValue = "1") int page,      // 页码，默认第 1 页
            @RequestParam(defaultValue = "20") int size,      // 每页大小，默认 20 条
            @RequestParam(required = false) String keyword,   // 可选的搜索关键词
            @RequestParam(required = false) Long categoryId,  // 可选的分类 ID
            @RequestParam(required = false) String status) {  // 可选的状态过滤
        return ApiResponse.success(documentService.listDocuments(page, size, keyword, categoryId, status));
    }

    /**
     * 获取单个文档详情。
     *
     * <p>{@code @PathVariable Long id} 从 URL 路径中提取文档 ID。
     * 例如 {@code /api/v1/documents/42} 中的 42 会被绑定到 id 参数。</p>
     *
     * @param id 文档 ID（从 URL 路径提取）
     * @return 文档详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Document> get(@PathVariable Long id) {
        return ApiResponse.success(documentService.getDocument(id));
    }

    /**
     * 创建新文档。
     *
     * <p>{@code @RequestBody Document document}: Spring 将请求体中的 JSON
     * 反序列化为 Document 对象。例如：
     * <pre>
     * POST /api/v1/documents
     * {
     *   "title": "Spring Boot 入门",
     *   "content": "...",
     *   "categoryId": 1
     * }
     * </pre></p>
     *
     * @param document 从请求体映射的文档对象
     * @return 创建成功的文档（包含自动生成的 ID）
     */
    @PostMapping
    public ApiResponse<Document> create(@RequestBody Document document) {
        return ApiResponse.success("创建成功", documentService.createDocument(document));
    }

    /**
     * 更新文档 — 支持部分更新（只更新非 null 字段）。
     *
     * <p>{@code @PutMapping("/{id}")} 映射 HTTP PUT 请求。
     * 结合 {@code @PathVariable} 获取要更新的文档 ID，
     * {@code @RequestBody} 获取更新内容。</p>
     *
     * @param id       文档 ID
     * @param document 包含更新字段的文档对象
     * @return 更新后的完整文档
     */
    @PutMapping("/{id}")
    public ApiResponse<Document> update(@PathVariable Long id, @RequestBody Document document) {
        return ApiResponse.success("更新成功", documentService.updateDocument(id, document));
    }

    /**
     * 删除文档（软删除）。
     *
     * <p>{@code @DeleteMapping("/{id}")} 映射 HTTP DELETE 请求。
     * 实际执行的是软删除（设置 deletedAt 时间戳），而非物理删除。</p>
     *
     * @param id 要删除的文档 ID
     * @return 删除成功提示
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ApiResponse.success("删除成功", null);
    }

    /**
     * 获取精选/推荐文档列表。
     *
     * <p>注意：此方法的路径 {@code /featured} 是固定字符串，不是变量。
     * 必须在 {@code /{id}} 之前声明，否则 "featured" 会被当作 id 解析。</p>
     *
     * @param page 页码（默认第 1 页）
     * @param size 每页大小（默认 10 条）
     * @return 精选文档分页列表
     */
    @GetMapping("/featured")
    public ApiResponse<Page<Document>> featured(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(documentService.getFeaturedDocuments(page, size));
    }

    /**
     * 获取热门文档列表 — 按浏览量降序。
     *
     * @param page 页码（默认第 1 页）
     * @param size 每页大小（默认 10 条）
     * @return 热门文档分页列表
     */
    @GetMapping("/popular")
    public ApiResponse<Page<Document>> popular(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(documentService.getPopularDocuments(page, size));
    }
}
