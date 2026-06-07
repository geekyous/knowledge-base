package com.company.kb.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建文档请求 DTO
 * @author Geekyous
 */
@Data
@Schema(description = "创建文档请求")
public class CreateDocumentRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题最长 255 个字符")
    @Schema(description = "文档标题", example = "Spring Boot 入门指南", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Size(max = 500, message = "摘要最长 500 个字符")
    @Schema(description = "文档摘要")
    private String summary;

    @NotBlank(message = "内容不能为空")
    @Size(max = 100000, message = "内容最长 100000 个字符")
    @Schema(description = "文档正文", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "所属分类 ID")
    private Long categoryId;
}
