package com.geekyous.kb.controller;

import com.geekyous.kb.entity.Category;
import com.geekyous.kb.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类控制器 — 处理分类查询和树形结构请求
 * @author Geekyous Guo
 * @see CategoryRepository
 */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "分类管理", description = "文档分类的查询和树形结构接口")
@Validated
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    @Operation(summary = "获取所有分类")
    public List<Category> list() {
        return categoryRepository.findAll();
    }

    @GetMapping("/tree")
    @Operation(summary = "获取分类树", description = "返回顶层分类，前端可按需加载子节点")
    public List<Category> tree() {
        return categoryRepository.findByParentIdIsNull();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取分类详情")
    public Category get(@PathVariable Long id) {
        return categoryRepository.findById(id).orElse(null);
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "获取子分类列表")
    public List<Category> children(@PathVariable Long id) {
        return categoryRepository.findByParentId(id);
    }
}
