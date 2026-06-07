package com.geekyous.kb.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新文档请求 DTO — 所有字段可选，仅更新传入的字段（部分更新）
 * @author Geekyous
 */
@Data
@Schema(description = "更新文档请求")
public class UpdateDocumentRequest {

    @Size(max = 255, message = "标题最长 255 个字符")
    @Schema(description = "文档标题")
    private String title;

    @Size(max = 500, message = "摘要最长 500 个字符")
    @Schema(description = "文档摘要")
    private String summary;

    @Size(max = 100000, message = "内容最长 100000 个字符")
    @Schema(description = "文档正文")
    private String content;

    @Schema(description = "所属分类 ID")
    private Long categoryId;
}
