package com.geekyous.kb.dto.response;

import com.geekyous.kb.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户管理响应 DTO — 封装管理后台返回的用户信息
 *
 * @author Geekyous Guo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户管理响应")
public class UserAdminResponse {

    @Schema(description = "用户唯一标识", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "邮箱（脱敏）", example = "z***@company.com")
    private String email;

    @Schema(description = "手机号（脱敏）", example = "138****0001")
    private String phone;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "用户角色", example = "USER", allowableValues = {"USER", "EDITOR", "ADMIN"})
    private User.Role role;

    @Schema(description = "用户状态", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "LOCKED"})
    private User.UserStatus status;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
