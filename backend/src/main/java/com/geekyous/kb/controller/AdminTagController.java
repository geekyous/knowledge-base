package com.geekyous.kb.controller;

import com.geekyous.kb.dto.request.CreateTagRequest;
import com.geekyous.kb.dto.request.UpdateTagRequest;
import com.geekyous.kb.dto.response.TagResponse;
import com.geekyous.kb.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 标签管理控制器 — 管理后台标签增删改查操作
 *
 * @author Geekyous Guo
 * @see TagService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/tags")
@Tag(name = "管理后台-标签管理", description = "管理后台标签增删改查接口")
@Validated
public class AdminTagController {

    private final TagService tagService;

    public AdminTagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    @Operation(summary = "获取标签列表")
    public List<TagResponse> listTags() {
        log.info("管理后台-查询标签列表");
        return tagService.listTags();
    }

    @PostMapping
    @Operation(summary = "创建标签")
    public TagResponse createTag(@RequestBody @Valid CreateTagRequest request) {
        log.info("管理后台-创建标签: name={}", request.getName());
        return tagService.createTag(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新标签")
    public TagResponse updateTag(@PathVariable Integer id, @RequestBody @Valid UpdateTagRequest request) {
        log.info("管理后台-更新标签: id={}", id);
        return tagService.updateTag(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签")
    public Map<String, String> deleteTag(@PathVariable Integer id) {
        log.info("管理后台-删除标签: id={}", id);
        tagService.deleteTag(id);
        return Map.of("message", "标签已删除");
    }
}
