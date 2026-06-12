package com.geekyous.kb.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仪表盘统计响应 DTO — 封装管理后台首页统计数据
 *
 * @author Geekyous Guo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "仪表盘统计响应")
public class DashboardStatsResponse {

    @Schema(description = "文档总数", example = "128")
    private long totalDocs;

    @Schema(description = "用户总数", example = "56")
    private long totalUsers;

    @Schema(description = "提问总数", example = "1024")
    private long totalQuestions;

    @Schema(description = "准确率", example = "0.92")
    private double accuracyRate;
}
