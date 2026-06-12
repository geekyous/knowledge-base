package com.geekyous.kb.service;

import com.geekyous.kb.dto.request.CreateCategoryRequest;
import com.geekyous.kb.dto.request.UpdateCategoryRequest;
import com.geekyous.kb.dto.response.CategoryTreeResponse;
import com.geekyous.kb.entity.Category;
import com.geekyous.kb.exception.BusinessException;
import com.geekyous.kb.repository.CategoryRepository;
import com.geekyous.kb.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 分类管理服务 — 处理后台分类树形结构管理的业务逻辑，包括
 * 分类树查询、创建、更新、删除等操作。
 *
 * @author Geekyous Guo
 */
@Slf4j
@Service
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final DocumentRepository documentRepository;

    public AdminCategoryService(CategoryRepository categoryRepository, DocumentRepository documentRepository) {
        this.categoryRepository = categoryRepository;
        this.documentRepository = documentRepository;
    }

    /**
     * 获取完整的分类树 — 查询所有分类，从根节点递归构建树形结构，
     * 并统计每个分类下的文档数量。
     *
     * @return 分类树列表
     */
    public List<CategoryTreeResponse> getCategoryTree() {
        List<Category> allCategories = categoryRepository.findAll();
        List<Category> roots = allCategories.stream()
                .filter(c -> c.getParent() == null)
                .collect(Collectors.toList());

        return roots.stream()
                .map(root -> toResponse(root, allCategories))
                .collect(Collectors.toList());
    }

    /**
     * 创建新分类 — 从名称自动生成 slug，可选设置父分类。
     *
     * @param req 创建分类请求
     * @return 创建后的分类树视图
     */
    @Transactional
    public CategoryTreeResponse createCategory(CreateCategoryRequest req) {
        Category parent = null;
        if (req.getParentId() != null) {
            parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new BusinessException(404, "父分类不存在"));
        }

        Category category = Category.builder()
                .name(req.getName())
                .slug(generateSlug(req.getName()))
                .parent(parent)
                .icon(req.getIcon())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("分类创建: name={}, slug={}", saved.getName(), saved.getSlug());
        return toResponse(saved, List.of());
    }

    /**
     * 更新分类信息 — 只更新请求中非 null 的字段。
     *
     * @param id  分类 ID
     * @param req 更新分类请求
     * @return 更新后的分类树视图
     */
    @Transactional
    public CategoryTreeResponse updateCategory(Long id, UpdateCategoryRequest req) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "分类不存在"));

        if (req.getName() != null) {
            category.setName(req.getName());
            category.setSlug(generateSlug(req.getName()));
        }
        if (req.getParentId() != null) {
            Category parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new BusinessException(404, "父分类不存在"));
            category.setParent(parent);
        }
        if (req.getIcon() != null) {
            category.setIcon(req.getIcon());
        }
        if (req.getSortOrder() != null) {
            category.setSortOrder(req.getSortOrder());
        }

        Category saved = categoryRepository.save(category);
        log.info("分类更新: id={}", id);
        return toResponse(saved, List.of());
    }

    /**
     * 删除分类 — 检查分类下是否有文档或子分类，有则拒绝删除。
     *
     * @param id 分类 ID
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "分类不存在"));

        long docCount = documentRepository.countByCategoryIdAndNotDeleted(id);
        if (docCount > 0) {
            throw new BusinessException(400, "该分类下还有文档，无法删除");
        }

        List<Category> children = categoryRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw new BusinessException(400, "该分类下还有子分类，无法删除");
        }

        categoryRepository.delete(category);
        log.info("分类删除: id={}", id);
    }

    /**
     * 从分类名称生成 slug — 转小写，空格替换为连字符。
     * 实际项目中可引入 transliteration 库处理中文，此处采用简单策略。
     */
    private String generateSlug(String name) {
        return name.toLowerCase().replace(" ", "-");
    }

    /**
     * 递归构建分类树响应 — 包含子分类和文档计数。
     *
     * @param category      当前分类
     * @param allCategories 所有分类列表（用于查找子分类）
     * @return 分类树响应节点
     */
    private CategoryTreeResponse toResponse(Category category, List<Category> allCategories) {
        long docCount = documentRepository.countByCategoryIdAndNotDeleted(category.getId());

        List<CategoryTreeResponse> children = allCategories.stream()
                .filter(c -> category.equals(c.getParent()))
                .map(child -> toResponse(child, allCategories))
                .collect(Collectors.toList());

        return CategoryTreeResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .icon(category.getIcon())
                .sortOrder(category.getSortOrder())
                .docCount(docCount)
                .children(children)
                .build();
    }
}
