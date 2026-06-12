package com.geekyous.kb.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新标签请求 DTO
 *
 * @author Geekyous Guo
 */
@Data
@Schema(description = "更新标签请求")
public class UpdateTagRequest {

    @Size(max = 50, message = "标签名称最长 50 个字符")
    @Schema(description = "标签名称", example = "Spring")
    private String name;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "颜色格式错误")
    @Schema(description = "标签颜色", example = "#ef4444")
    private String color;
}
