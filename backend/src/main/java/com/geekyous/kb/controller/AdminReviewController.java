package com.geekyous.kb.controller;

import com.geekyous.kb.dto.request.ReviewActionRequest;
import com.geekyous.kb.entity.Document;
import com.geekyous.kb.service.AdminReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 文档审核控制器 — 管理后台文档审核、批量审批等操作
 *
 * @author Geekyous Guo
 * @see AdminReviewService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/reviews")
@Tag(name = "管理后台-文档审核", description = "管理后台文档审核、批量审批等接口")
@Validated
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    public AdminReviewController(AdminReviewService adminReviewService) {
        this.adminReviewService = adminReviewService;
    }

    @GetMapping("/pending")
    @Operation(summary = "获取待审核文档列表")
    public Page<Document> listPending(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("管理后台-查询待审核文档: page={}, size={}", page, size);
        return adminReviewService.listPending(page, size);
    }

    @GetMapping
    @Operation(summary = "获取已审核文档列表", description = "按审核状态过滤")
    public Page<Document> listReviewed(
            @RequestParam String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("管理后台-查询已审核文档: status={}, page={}, size={}", status, page, size);
        return adminReviewService.listReviewed(status, page, size);
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "通过文档审核")
    public Map<String, String> approve(@PathVariable Long id) {
        log.info("管理后台-通过审核: id={}", id);
        adminReviewService.approve(id);
        return Map.of("message", "文档已通过审核");
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "拒绝文档审核")
    public Map<String, String> reject(@PathVariable Long id, @RequestParam(required = false) String reason) {
        log.info("管理后台-拒绝审核: id={}, reason={}", id, reason);
        adminReviewService.reject(id, reason);
        return Map.of("message", "文档已拒绝");
    }

    @PutMapping("/batch-approve")
    @Operation(summary = "批量通过审核")
    public Map<String, String> batchApprove(@RequestBody @Valid ReviewActionRequest request) {
        log.info("管理后台-批量审核: ids={}", request.getIds());
        adminReviewService.batchApprove(request.getIds());
        return Map.of("message", "批量审核完成");
    }
}
