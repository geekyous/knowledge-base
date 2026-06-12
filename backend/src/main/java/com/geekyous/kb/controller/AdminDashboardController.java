package com.geekyous.kb.controller;

import com.geekyous.kb.dto.response.DashboardStatsResponse;
import com.geekyous.kb.entity.Document;
import com.geekyous.kb.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 仪表盘控制器 — 管理后台首页统计、最近文档、待审核文档等数据
 *
 * @author Geekyous Guo
 * @see DashboardService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@Tag(name = "管理后台-仪表盘", description = "管理后台首页统计数据、最近文档、待审核文档等接口")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    @Operation(summary = "获取仪表盘统计数据")
    public DashboardStatsResponse getStats() {
        log.info("管理后台-查询仪表盘统计");
        return dashboardService.getStats();
    }

    @GetMapping("/recent-docs")
    @Operation(summary = "获取最近文档列表")
    public List<Document> getRecentDocuments(@RequestParam(defaultValue = "5") int limit) {
        log.info("管理后台-查询最近文档: limit={}", limit);
        return dashboardService.getRecentDocuments(limit);
    }

    @GetMapping("/pending-reviews")
    @Operation(summary = "获取待审核文档列表")
    public Page<Document> getPendingReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("管理后台-查询待审核文档: page={}, size={}", page, size);
        return dashboardService.getPendingReviews(page, size);
    }
}
