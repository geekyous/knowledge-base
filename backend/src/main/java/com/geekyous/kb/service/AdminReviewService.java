package com.geekyous.kb.service;

import com.geekyous.kb.entity.Document;
import com.geekyous.kb.entity.Document.DocumentStatus;
import com.geekyous.kb.exception.BusinessException;
import com.geekyous.kb.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档审核服务 — 处理后台文档审核相关的业务逻辑，包括
 * 待审核列表查询、审核通过、审核驳回、批量通过等操作。
 *
 * @author Geekyous Guo
 */
@Slf4j
@Service
public class AdminReviewService {

    private final DocumentRepository documentRepository;

    public AdminReviewService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * 分页查询待审核文档列表。
     *
     * @param page 页码（1-based）
     * @param size 每页大小
     * @return 待审核文档分页结果
     */
    public Page<Document> listPending(int page, int size) {
        return documentRepository.findByStatus(DocumentStatus.PENDING, PageRequest.of(page - 1, size));
    }

    /**
     * 分页查询指定状态的已审核文档。
     *
     * @param status 文档状态字符串（如 PUBLISHED、REJECTED）
     * @param page   页码（1-based）
     * @param size   每页大小
     * @return 指定状态的文档分页结果
     */
    public Page<Document> listReviewed(String status, int page, int size) {
        return documentRepository.findByStatus(DocumentStatus.valueOf(status), PageRequest.of(page - 1, size));
    }

    /**
     * 审核通过 — 将文档状态设为 PUBLISHED 并记录发布时间。
     *
     * @param id 文档 ID
     */
    @Transactional
    public void approve(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));

        doc.setStatus(DocumentStatus.PUBLISHED);
        doc.setPublishedAt(LocalDateTime.now());
        documentRepository.save(doc);
        log.info("文档审核通过: id={}", id);
    }

    /**
     * 审核驳回 — 将文档状态设为 REJECTED。
     *
     * @param id     文档 ID
     * @param reason 驳回原因
     */
    @Transactional
    public void reject(Long id, String reason) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));

        doc.setStatus(DocumentStatus.REJECTED);
        documentRepository.save(doc);
        log.info("文档审核驳回: id={}, reason={}", id, reason);
    }

    /**
     * 批量审核通过 — 将指定文档全部设为 PUBLISHED 并记录发布时间。
     *
     * @param ids 文档 ID 列表
     */
    @Transactional
    public void batchApprove(List<Long> ids) {
        List<Document> docs = documentRepository.findAllById(ids);
        LocalDateTime now = LocalDateTime.now();
        for (Document doc : docs) {
            doc.setStatus(DocumentStatus.PUBLISHED);
            doc.setPublishedAt(now);
        }
        documentRepository.saveAll(docs);
        log.info("文档批量审核通过: count={}", docs.size());
    }
}
