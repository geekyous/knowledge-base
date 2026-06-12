package com.geekyous.kb.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分类树响应 DTO — 封装分类树形结构
 *
 * @author Geekyous Guo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分类树响应")
public class CategoryTreeResponse {

    @Schema(description = "分类唯一标识", example = "1")
    private Long id;

    @Schema(description = "分类名称", example = "Spring Boot")
    private String name;

    @Schema(description = "分类别名", example = "spring-boot")
    private String slug;

    @Schema(description = "图标", example = "Briefcase")
    private String icon;

    @Schema(description = "排序序号", example = "0")
    private Integer sortOrder;

    @Schema(description = "文档数量", example = "12")
    private Long docCount;

    @Schema(description = "子分类列表")
    private List<CategoryTreeResponse> children;
}
