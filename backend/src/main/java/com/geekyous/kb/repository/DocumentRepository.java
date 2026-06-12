package com.geekyous.kb.repository;

import com.geekyous.kb.entity.Document;
import com.geekyous.kb.entity.Document.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 文档数据访问层
 *
 * @author Geekyous Guo
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * 按文档状态查询文档列表（分页）
     */
    Page<Document> findByStatus(DocumentStatus status, Pageable pageable);

    /**
     * 按分类 ID 查询文档列表（分页）
     */
    Page<Document> findByCategoryId(Long categoryId, Pageable pageable);

    /**
     * 按作者 ID 查询文档列表（分页）
     */
    Page<Document> findByAuthorId(Long authorId, Pageable pageable);

    /**
     * 全文搜索 — 在已发布的文档中按关键词搜索标题或内容
     */
    @Query("SELECT d FROM Document d WHERE d.status = 'PUBLISHED' AND d.deletedAt IS NULL AND (d.title LIKE %:keyword% OR d.content LIKE %:keyword%)")
    Page<Document> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 获取热门文档 — 按浏览量降序排列（排除已删除文档）
     */
    @Query("SELECT d FROM Document d WHERE d.deletedAt IS NULL ORDER BY d.viewCount DESC")
    Page<Document> findPopularDocuments(Pageable pageable);

    /**
     * 获取推荐/精选文档 — 管理员标记的高质量内容
     */
    @Query("SELECT d FROM Document d WHERE d.status = 'PUBLISHED' AND d.isFeatured = true AND d.deletedAt IS NULL")
    Page<Document> findFeaturedDocuments(Pageable pageable);

    /**
     * 统计指定状态的文档数量
     */
    long countByStatus(DocumentStatus status);

    /** 统计未软删除的文档总数（仪表盘） */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.deletedAt IS NULL")
    long countByDeletedAtIsNull();

    /** 按分类统计文档数（分类树 docCount） */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.categoryId = :categoryId AND d.deletedAt IS NULL")
    long countByCategoryIdAndNotDeleted(@Param("categoryId") Long categoryId);
}
