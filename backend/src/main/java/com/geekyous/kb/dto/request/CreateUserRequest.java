package com.geekyous.kb.dto.request;

import com.geekyous.kb.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建用户请求 DTO
 *
 * @author Geekyous Guo
 */
@Data
@Schema(description = "创建用户请求")
public class CreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度 3-50 个字符")
    @Schema(description = "用户名", example = "zhangsan", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 50, message = "密码长度 6-50 个字符")
    @Schema(description = "密码", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "zhangsan@company.com")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @NotNull(message = "角色不能为空")
    @Schema(description = "用户角色", example = "USER", requiredMode = Schema.RequiredMode.REQUIRED)
    private User.Role role;
}
