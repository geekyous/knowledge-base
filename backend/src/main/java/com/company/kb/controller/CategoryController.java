package com.company.kb.controller;

import com.company.kb.dto.ApiResponse;
import com.company.kb.entity.Category;
import com.company.kb.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类控制器（Category Controller）— 处理分类查询和树形结构请求
 *
 * <h2>架构角色</h2>
 * <p>本控制器负责分类数据的读取接口。注意一个特殊之处：
 * 本控制器直接注入了 {@code CategoryRepository} 而非通过 Service 层。
 * 这在<b>纯读取、无业务逻辑</b>的场景中是可以接受的简化做法。
 * 如果分类有复杂的创建/更新逻辑，应该引入 CategoryService。</p>
 *
 * <h2>树形端点设计（Tree Endpoint Design）</h2>
 * <p>分类的树形结构通过以下端点组合实现：
 * <table>
 *   <tr><th>端点</th><th>功能</th><th>返回</th></tr>
 *   <tr>
 *     <td>{@code GET /categories}</td>
 *     <td>获取所有分类（平铺列表）</td>
 *     <td>[{id:1, parent:null}, {id:2, parent:1}, ...]</td>
 *   </tr>
 *   <tr>
 *     <td>{@code GET /categories/tree}</td>
 *     <td>获取根节点（前端递归构建树）</td>
 *     <td>[{id:1, parent:null}, {id:4, parent:null}]</td>
 *   </tr>
 *   <tr>
 *     <td>{@code GET /categories/{id}/children}</td>
 *     <td>获取某节点的子节点</td>
 *     <td>[{id:2, parent:1}, {id:3, parent:1}]</td>
 *   </tr>
 * </table>
 * </p>
 *
 * <h2>嵌套资源模式（Nested Resource Pattern）</h2>
 * <p>{@code /categories/{id}/children} 是一种嵌套资源的 URL 设计模式。
 * 它表达"某个分类下的子分类"这一层级关系。比 {@code /children?parentId=1}
 * 更直观、更 RESTful。</p>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>何时可以跳过 Service 层？</b>: 当 Controller 只做简单的 CRUD 委托，
 *       没有额外业务逻辑时，可以直接使用 Repository。这是一种务实的简化。
 *       但随着业务复杂度增长，建议抽取 Service 层。</li>
 *   <li><b>树形数据的加载策略</b>: 本项目采用"只返回根节点"策略。
 *       前端拿到根节点后，可以：1) 按需加载子节点（每次展开时请求 children API）；
 *       2) 或一次性加载所有分类，在内存中构建树。前者减少初始数据量，
 *       后者减少网络请求次数。</li>
 *   <li><b>{@code @GetMapping("/tree")}</b>: 这是一个语义化的端点名称。
 *       虽然实际只返回根节点，但 "/tree" 清楚地表达了客户端的意图。</li>
 * </ul>
 *
 * @author Geekyous Guo
 * @since 1.0.0
 * @see CategoryRepository
 * @see Category
 */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "分类管理", description = "文档分类的查询和树形结构接口")
public class CategoryController {

    /** 分类 Repository — 直接注入，跳过 Service 层 */
    private final CategoryRepository categoryRepository;

    /**
     * 构造器注入 CategoryRepository。
     *
     * @param categoryRepository 分类 Repository 实例
     */
    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * 获取所有分类（平铺列表）。
     *
     * <p>返回数据库中所有分类记录，不做层级组织。前端可以在此基础上
     * 自行构建树形结构。</p>
     *
     * @return 所有分类的列表
     */
    @GetMapping
    @Operation(summary = "获取所有分类", description = "返回平铺列表形式的全部分类")
    public ApiResponse<List<Category>> list() {
        return ApiResponse.success(categoryRepository.findAll());
    }

    /**
     * 获取分类树 — 只返回顶层分类（根节点）。
     *
     * <p>查询条件：parent_id IS NULL。前端拿到根节点后，
     * 可以通过 {@code /categories/{id}/children} 接口按需加载子节点，
     * 或结合 list() 接口返回的全部数据在内存中构建完整树。</p>
     *
     * <p><b>为什么叫 "tree" 而不是 "roots"？</b>: 从客户端角度看，
     * 它请求的是"分类树"，服务端返回树的入口（根节点）。
     * 端点命名应从客户端使用意图出发，而非实现细节。</p>
     *
     * @return 顶层分类列表（parent 为 null 的记录）
     */
    @GetMapping("/tree")
    @Operation(summary = "获取分类树", description = "返回顶层分类（根节点），前端可按需加载子节点")
    public ApiResponse<List<Category>> tree() {
        // 查询所有 parent_id 为 NULL 的分类，即顶层分类（树的根节点）
        List<Category> roots = categoryRepository.findByParentIdIsNull();
        return ApiResponse.success(roots);
    }

    /**
     * 获取单个分类详情。
     *
     * @param id 分类 ID（从 URL 路径提取）
     * @return 分类详情。如果不存在，data 为 null
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取分类详情", description = "根据 ID 获取单个分类信息")
    public ApiResponse<Category> get(@PathVariable Long id) {
        // orElse(null): 如果分类不存在，返回 null（而非抛异常）
        // 配合 @JsonInclude(NON_NULL)，data 字段在 JSON 中会被省略
        return ApiResponse.success(categoryRepository.findById(id).orElse(null));
    }

    /**
     * 获取指定分类的子分类列表 — 嵌套资源模式。
     *
     * <p>URL 路径 {@code /categories/{id}/children} 表达了层级关系：
     * "某个分类下的子分类"。这是 RESTful API 中嵌套资源的标准设计模式。</p>
     *
     * @param id 父分类 ID
     * @return 该分类下的直接子分类列表
     */
    @GetMapping("/{id}/children")
    @Operation(summary = "获取子分类列表", description = "根据父分类 ID 获取其直接子分类")
    public ApiResponse<List<Category>> children(@PathVariable Long id) {
        return ApiResponse.success(categoryRepository.findByParentId(id));
    }
}
