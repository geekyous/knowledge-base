package com.geekyous.kb.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查控制器 — 提供应用运行状态探测端点
 * @author Geekyous Guo
 */
@RestController
@Tag(name = "健康检查", description = "应用运行状态探测接口")
public class HealthController {

    @GetMapping("/actuator/health")
    @Operation(summary = "Actuator 健康检查")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/api/v1/health")
    @Operation(summary = "API 健康检查")
    public Map<String, String> apiHealth() {
        return Map.of("status", "UP", "service", "knowledge-base");
    }
}
