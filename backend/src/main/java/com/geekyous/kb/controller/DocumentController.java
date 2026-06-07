package com.geekyous.kb.controller;

import com.geekyous.kb.dto.request.CreateDocumentRequest;
import com.geekyous.kb.dto.request.UpdateDocumentRequest;
import com.geekyous.kb.entity.Document;
import com.geekyous.kb.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 文档控制器 — 处理文档的 CRUD 和查询请求
 * @author Geekyous Guo
 * @see DocumentService
 */
@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "文档管理", description = "文档的增删改查、精选文档、热门文档等接口")
@Validated
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    @Operation(summary = "获取文档列表", description = "支持分页、关键词搜索、分类过滤、状态过滤")
    public Page<Document> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status) {
        return documentService.listDocuments(page, size, keyword, categoryId, status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取文档详情")
    public Document get(@PathVariable Long id) {
        return documentService.getDocument(id);
    }

    @PostMapping
    @Operation(summary = "创建文档")
    public Document create(@RequestBody @Valid CreateDocumentRequest request) {
        return documentService.createDocument(toEntity(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新文档", description = "支持部分更新，只更新非 null 字段")
    public Document update(@PathVariable Long id, @RequestBody @Valid UpdateDocumentRequest request) {
        return documentService.updateDocument(id, toEntity(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档", description = "软删除（设置 deletedAt 时间戳）")
    public void delete(@PathVariable Long id) {
        documentService.deleteDocument(id);
    }

    @GetMapping("/featured")
    @Operation(summary = "获取精选文档")
    public Page<Document> featured(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return documentService.getFeaturedDocuments(page, size);
    }

    @GetMapping("/popular")
    @Operation(summary = "获取热门文档", description = "按浏览量降序")
    public Page<Document> popular(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return documentService.getPopularDocuments(page, size);
    }

    private Document toEntity(CreateDocumentRequest req) {
        return Document.builder()
                .title(req.getTitle())
                .summary(req.getSummary())
                .content(req.getContent())
                .categoryId(req.getCategoryId())
                .build();
    }

    private Document toEntity(UpdateDocumentRequest req) {
        return Document.builder()
                .title(req.getTitle())
                .summary(req.getSummary())
                .content(req.getContent())
                .categoryId(req.getCategoryId())
                .build();
    }
}
