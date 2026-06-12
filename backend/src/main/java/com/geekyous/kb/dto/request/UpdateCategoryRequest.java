package com.geekyous.kb.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新分类请求 DTO
 *
 * @author Geekyous Guo
 */
@Data
@Schema(description = "更新分类请求")
public class UpdateCategoryRequest {

    @Size(max = 50, message = "分类名称最长 50 个字符")
    @Schema(description = "分类名称", example = "Spring Boot")
    private String name;

    @Schema(description = "父分类 ID，为空表示顶级分类")
    private Long parentId;

    @Size(max = 50, message = "图标名称最长 50 个字符")
    @Schema(description = "图标", example = "Briefcase")
    private String icon;

    @Schema(description = "排序序号")
    private Integer sortOrder;
}
