package com.company.kb.controller;

import com.company.kb.dto.ApiResponse;
import com.company.kb.entity.Document;
import com.company.kb.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 文档控制器 — 处理文档的 CRUD 和查询请求
 *
 * @author Geekyous Guo
 * @since 1.0.0
 * @see DocumentService
 * @see Document
 */
@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "文档管理", description = "文档的增删改查、精选文档、热门文档等接口")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /** 获取文档列表 — 支持分页、搜索、分类和状态过滤 */
    @GetMapping
    @Operation(summary = "获取文档列表", description = "支持分页、关键词搜索、分类过滤、状态过滤")
    public ApiResponse<Page<Document>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(documentService.listDocuments(page, size, keyword, categoryId, status));
    }

    /** 获取单个文档详情 */
    @GetMapping("/{id}")
    @Operation(summary = "获取文档详情", description = "根据文档 ID 获取完整的文档内容")
    public ApiResponse<Document> get(@PathVariable Long id) {
        return ApiResponse.success(documentService.getDocument(id));
    }

    /** 创建新文档 */
    @PostMapping
    @Operation(summary = "创建文档", description = "提交 JSON 创建新文档，返回包含自动生成 ID 的文档对象")
    public ApiResponse<Document> create(@RequestBody Document document) {
        return ApiResponse.success("创建成功", documentService.createDocument(document));
    }

    /** 更新文档 — 支持部分更新（只更新非 null 字段） */
    @PutMapping("/{id}")
    @Operation(summary = "更新文档", description = "根据 ID 更新文档内容，支持部分更新（只更新非 null 字段）")
    public ApiResponse<Document> update(@PathVariable Long id, @RequestBody Document document) {
        return ApiResponse.success("更新成功", documentService.updateDocument(id, document));
    }

    /** 删除文档（软删除） */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档", description = "根据 ID 软删除文档（设置 deletedAt 时间戳）")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ApiResponse.success("删除成功", null);
    }

    /** 获取精选/推荐文档列表 */
    @GetMapping("/featured")
    @Operation(summary = "获取精选文档", description = "获取标记为精选/推荐的文档列表")
    public ApiResponse<Page<Document>> featured(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(documentService.getFeaturedDocuments(page, size));
    }

    /** 获取热门文档列表 — 按浏览量降序 */
    @GetMapping("/popular")
    @Operation(summary = "获取热门文档", description = "按浏览量降序获取热门文档列表")
    public ApiResponse<Page<Document>> popular(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(documentService.getPopularDocuments(page, size));
    }
}
