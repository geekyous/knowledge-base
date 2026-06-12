package com.geekyous.kb.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 审核操作请求 DTO
 *
 * @author Geekyous Guo
 */
@Data
@Schema(description = "审核操作请求")
public class ReviewActionRequest {

    @NotEmpty(message = "文档ID列表不能为空")
    @Schema(description = "文档ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;

    @Size(max = 500, message = "拒绝原因最长 500 个字符")
    @Schema(description = "拒绝原因")
    private String reason;
}
