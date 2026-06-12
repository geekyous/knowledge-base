package com.geekyous.kb.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建分类请求 DTO
 *
 * @author Geekyous Guo
 */
@Data
@Schema(description = "创建分类请求")
public class CreateCategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称最长 50 个字符")
    @Schema(description = "分类名称", example = "Spring Boot", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "父分类 ID，为空表示顶级分类")
    private Long parentId;

    @Size(max = 50, message = "图标名称最长 50 个字符")
    @Schema(description = "图标", example = "Briefcase")
    private String icon;

    @Schema(description = "排序序号，默认 0")
    private Integer sortOrder = 0;
}
