package com.geekyous.kb.service;

import com.geekyous.kb.dto.response.DashboardStatsResponse;
import com.geekyous.kb.entity.Document;
import com.geekyous.kb.entity.Document.DocumentStatus;
import com.geekyous.kb.repository.DocumentRepository;
import com.geekyous.kb.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 仪表盘服务 — 提供后台首页统计数据，包括文档数、用户数、
 * 最近文档、待审核文档等。
 *
 * @author Geekyous Guo
 */
@Slf4j
@Service
public class DashboardService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public DashboardService(DocumentRepository documentRepository, UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    /**
     * 获取仪表盘统计数据 — 文档总数（排除已软删除）、用户总数、
     * 问答总数（默认 0，对话/消息实体待建设）、准确率（默认 95.0）。
     *
     * @return 仪表盘统计响应
     */
    public DashboardStatsResponse getStats() {
        long totalDocs = documentRepository.countByDeletedAtIsNull();
        long totalUsers = userRepository.count();
        // 对话/消息实体尚未建设，暂时返回默认值
        long totalQuestions = 0;
        double accuracyRate = 95.0;

        return DashboardStatsResponse.builder()
                .totalDocs(totalDocs)
                .totalUsers(totalUsers)
                .totalQuestions(totalQuestions)
                .accuracyRate(accuracyRate)
                .build();
    }

    /**
     * 获取最近创建的文档列表 — 排除已软删除的文档。
     *
     * @param limit 返回数量上限
     * @return 最近文档列表
     */
    public List<Document> getRecentDocuments(int limit) {
        return documentRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream()
                .filter(doc -> doc.getDeletedAt() == null)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询待审核文档列表。
     *
     * @param page 页码（1-based）
     * @param size 每页大小
     * @return 待审核文档分页结果
     */
    public Page<Document> getPendingReviews(int page, int size) {
        return documentRepository.findByStatus(DocumentStatus.PENDING, PageRequest.of(page - 1, size));
    }
}
