package com.geekyous.kb.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建标签请求 DTO
 *
 * @author Geekyous Guo
 */
@Data
@Schema(description = "创建标签请求")
public class CreateTagRequest {

    @NotBlank(message = "标签名称不能为空")
    @Size(max = 50, message = "标签名称最长 50 个字符")
    @Schema(description = "标签名称", example = "Spring", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "颜色格式错误")
    @Schema(description = "标签颜色", example = "#ef4444")
    private String color;
}
