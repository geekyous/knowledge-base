package com.company.kb.controller;

import com.company.kb.dto.ApiResponse;
import com.company.kb.entity.Category;
import com.company.kb.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类控制器 — 处理分类查询和树形结构请求。
 * 直接注入 Repository，适用于纯读取场景。
 *
 * @author Geekyous Guo
 * @since 1.0.0
 * @see CategoryRepository
 * @see Category
 */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "分类管理", description = "文档分类的查询和树形结构接口")
@Validated
public class CategoryController {

    private final CategoryRepository categoryRepository;

    /**
     * @param categoryRepository 分类 Repository 实例
     */
    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * @return 所有分类的列表
     */
    @GetMapping
    @Operation(summary = "获取所有分类", description = "返回平铺列表形式的全部分类")
    public ApiResponse<List<Category>> list() {
        return ApiResponse.success(categoryRepository.findAll());
    }

    /**
     * @return 顶层分类列表（parent 为 null）
     */
    @GetMapping("/tree")
    @Operation(summary = "获取分类树", description = "返回顶层分类（根节点），前端可按需加载子节点")
    public ApiResponse<List<Category>> tree() {
        List<Category> roots = categoryRepository.findByParentIdIsNull();
        return ApiResponse.success(roots);
    }

    /**
     * @param id 分类 ID
     * @return 分类详情，不存在时 data 为 null
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取分类详情", description = "根据 ID 获取单个分类信息")
    public ApiResponse<Category> get(@PathVariable Long id) {
        return ApiResponse.success(categoryRepository.findById(id).orElse(null));
    }

    /**
     * @param id 父分类 ID
     * @return 该分类下的直接子分类列表
     */
    @GetMapping("/{id}/children")
    @Operation(summary = "获取子分类列表", description = "根据父分类 ID 获取其直接子分类")
    public ApiResponse<List<Category>> children(@PathVariable Long id) {
        return ApiResponse.success(categoryRepository.findByParentId(id));
    }
}
