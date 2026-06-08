package com.geekyous.kb.dto;

import com.geekyous.kb.annotation.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 DTO — 封装登录成功后返回给客户端的数据
 *
 * @author Geekyous Guo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    @Schema(description = "JWT 访问令牌，后续请求需放在 Authorization: Bearer <token> 头部")
    private String token;

    @Schema(description = "用户基本信息")
    private UserInfo user;

    /**
     * 用户信息嵌套 DTO — 只包含前端展示所需的用户字段。
     * 敏感字段通过 @Sensitive 注解自动脱敏，Service 层传入原始值即可。
     *
     * @author Geekyous Guo
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {

        @Schema(description = "用户唯一标识", example = "1")
        private Long id;

        @Schema(description = "用户名", example = "admin")
        private String username;

        @Schema(description = "邮箱（自动脱敏）", example = "a***@company.com")
        @Sensitive(Sensitive.SensitiveType.EMAIL)
        private String email;

        @Schema(description = "手机号（自动脱敏）", example = "138****0001")
        @Sensitive(Sensitive.SensitiveType.PHONE)
        private String phone;

        @Schema(description = "用户角色", example = "ADMIN", allowableValues = {"USER", "EDITOR", "ADMIN"})
        private String role;

        @Schema(description = "头像 URL")
        private String avatar;
    }
}
