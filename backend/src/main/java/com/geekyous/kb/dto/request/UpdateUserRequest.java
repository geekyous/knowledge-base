package com.geekyous.kb.dto.request;

import com.geekyous.kb.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Data;

/**
 * 更新用户请求 DTO
 *
 * @author Geekyous Guo
 */
@Data
@Schema(description = "更新用户请求")
public class UpdateUserRequest {

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "zhangsan@company.com")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "用户角色", example = "USER")
    private User.Role role;

    @Schema(description = "用户状态", example = "ACTIVE")
    private User.UserStatus status;
}
