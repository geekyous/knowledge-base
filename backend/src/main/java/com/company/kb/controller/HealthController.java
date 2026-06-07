package com.company.kb.controller;

import com.company.kb.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查控制器 — 提供应用运行状态探测端点
 *
 * @author Geekyous Guo
 * @since 1.0.0
 */
@RestController
@Tag(name = "健康检查", description = "应用运行状态探测接口")
public class HealthController {

    /**
     * Actuator 标准健康检查端点，返回 {"status": "UP"}。
     *
     * @return 包含状态的 Map
     */
    @GetMapping("/actuator/health")
    @Operation(summary = "Actuator 健康检查", description = "兼容 Spring Boot Actuator 标准路径，返回 {\"status\": \"UP\"}")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }

    /**
     * 自定义 API 健康检查端点，返回标准 ApiResponse 格式。
     *
     * @return 标准格式的健康状态响应
     */
    @GetMapping("/api/v1/health")
    @Operation(summary = "API 健康检查", description = "返回标准 ApiResponse 格式的健康状态，包含服务名称")
    public ApiResponse<Map<String, String>> apiHealth() {
        return ApiResponse.success(Map.of("status", "UP", "service", "knowledge-base"));
    }
}
