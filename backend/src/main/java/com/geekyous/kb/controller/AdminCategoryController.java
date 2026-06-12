package com.geekyous.kb.controller;

import com.geekyous.kb.dto.request.CreateCategoryRequest;
import com.geekyous.kb.dto.request.UpdateCategoryRequest;
import com.geekyous.kb.dto.response.CategoryTreeResponse;
import com.geekyous.kb.service.AdminCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 分类管理控制器 — 管理后台分类树查询、新增、编辑、删除等操作
 *
 * @author Geekyous Guo
 * @see AdminCategoryService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/categories")
@Tag(name = "管理后台-分类管理", description = "管理后台分类树查询、新增、编辑、删除等接口")
@Validated
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    public AdminCategoryController(AdminCategoryService adminCategoryService) {
        this.adminCategoryService = adminCategoryService;
    }

    @GetMapping("/tree")
    @Operation(summary = "获取分类树")
    public List<CategoryTreeResponse> getCategoryTree() {
        log.info("管理后台-查询分类树");
        return adminCategoryService.getCategoryTree();
    }

    @PostMapping
    @Operation(summary = "创建分类")
    public CategoryTreeResponse createCategory(@RequestBody @Valid CreateCategoryRequest request) {
        log.info("管理后台-创建分类: name={}", request.getName());
        return adminCategoryService.createCategory(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新分类")
    public CategoryTreeResponse updateCategory(@PathVariable Long id, @RequestBody @Valid UpdateCategoryRequest request) {
        log.info("管理后台-更新分类: id={}", id);
        return adminCategoryService.updateCategory(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类")
    public Map<String, String> deleteCategory(@PathVariable Long id) {
        log.info("管理后台-删除分类: id={}", id);
        adminCategoryService.deleteCategory(id);
        return Map.of("message", "分类已删除");
    }
}
