package com.geekyous.kb.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 标签响应 DTO — 封装标签信息
 *
 * @author Geekyous Guo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "标签响应")
public class TagResponse {

    @Schema(description = "标签唯一标识", example = "1")
    private Integer id;

    @Schema(description = "标签名称", example = "Spring")
    private String name;

    @Schema(description = "标签颜色", example = "#ef4444")
    private String color;

    @Schema(description = "文档数量（使用次数）", example = "8")
    private Integer docCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
