package com.geekyous.kb.controller;

import com.geekyous.kb.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统设置控制器 — 管理后台系统配置查询与更新
 *
 * @author Geekyous Guo
 * @see SettingsService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/settings")
@Tag(name = "管理后台-系统设置", description = "管理后台系统配置查询与更新接口")
@Validated
public class AdminSettingsController {

    private final SettingsService settingsService;

    public AdminSettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @Operation(summary = "获取所有系统设置")
    public Map<String, Map<String, Object>> getAllSettings() {
        log.info("管理后台-查询系统设置");
        return settingsService.getAllSettings();
    }

    @PutMapping
    @Operation(summary = "更新系统设置")
    public Map<String, String> updateSettings(@RequestBody Map<String, String> settings) {
        log.info("管理后台-更新系统设置");
        settingsService.updateSettings(settings);
        return Map.of("message", "设置已更新");
    }
}
