package com.geekyous.kb.service;

import com.geekyous.kb.entity.Document;
import com.geekyous.kb.entity.Document.DocumentStatus;
import com.geekyous.kb.exception.BusinessException;
import com.geekyous.kb.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * 文档服务层 — 处理文档相关的业务逻辑
 *
 * @author Geekyous Guo
 * @since 1.0.0
 */
@Slf4j
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * 分页查询文档列表，支持关键词搜索、分类过滤、状态过滤。
     *
     * 查询优先级（互斥）：keyword > categoryId > status > 全部
     *
     * @param page       页码（1-based）
     * @param size       每页大小
     * @param keyword    搜索关键词（可选）
     * @param categoryId 分类 ID（可选）
     * @param status     文档状态（可选）
     * @return 分页结果
     */
    public Page<Document> listDocuments(int page, int size, String keyword, Long categoryId, String status) {
        // page-1: 将前端 1-based 页码转为 Spring Data 0-based 索引
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        if (keyword != null && !keyword.isBlank()) {
            return documentRepository.searchByKeyword(keyword, pageRequest);
        }
        if (categoryId != null) {
            return documentRepository.findByCategoryId(categoryId, pageRequest);
        }
        if (status != null) {
            return documentRepository.findByStatus(DocumentStatus.valueOf(status), pageRequest);
        }
        return documentRepository.findAll(pageRequest);
    }

    /**
     * 根据 ID 获取文档详情。
     *
     * @param id 文档 ID
     * @return 文档实体
     */
    public Document getDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));
    }

    /**
     * 创建新文档。
     *
     * @param document 文档实体
     * @return 保存后的文档实体（含自动生成的 ID 和审计时间戳）
     */
    public Document createDocument(Document document) {
        Document saved = documentRepository.save(document);
        log.info("文档创建: id={}, title={}", saved.getId(), saved.getTitle());
        return saved;
    }

    /**
     * 部分更新文档 — 只更新非 null 的字段（合并策略）。
     *
     * @param id      文档 ID
     * @param updates 包含更新字段的文档对象（null 字段表示不更新）
     * @return 更新后的文档实体
     */
    public Document updateDocument(Long id, Document updates) {
        Document doc = getDocument(id);
        if (updates.getTitle() != null) doc.setTitle(updates.getTitle());
        if (updates.getContent() != null) doc.setContent(updates.getContent());
        if (updates.getSummary() != null) doc.setSummary(updates.getSummary());
        if (updates.getCategoryId() != null) doc.setCategoryId(updates.getCategoryId());
        Document saved = documentRepository.save(doc);
        log.info("文档更新: id={}", id);
        return saved;
    }

    /**
     * 软删除文档 — 设置 deletedAt 时间戳，数据可恢复且保留审计记录。
     *
     * @param id 文档 ID
     */
    public void deleteDocument(Long id) {
        Document doc = getDocument(id);
        doc.setDeletedAt(java.time.LocalDateTime.now());
        documentRepository.save(doc);
        log.info("文档删除(软删除): id={}", id);
    }

    /**
     * 获取精选文档列表（isFeatured = true 的已发布文档）。
     *
     * @param page 页码（1-based）
     * @param size 每页大小
     * @return 精选文档分页结果
     */
    public Page<Document> getFeaturedDocuments(int page, int size) {
        return documentRepository.findFeaturedDocuments(PageRequest.of(page - 1, size));
    }

    /**
     * 获取热门文档列表 — 按浏览量降序。
     *
     * @param page 页码（1-based）
     * @param size 每页大小
     * @return 热门文档分页结果
     */
    public Page<Document> getPopularDocuments(int page, int size) {
        return documentRepository.findPopularDocuments(PageRequest.of(page - 1, size));
    }
}
