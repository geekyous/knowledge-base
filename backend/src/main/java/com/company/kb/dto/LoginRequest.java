package com.company.kb.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录请求 DTO（Data Transfer Object）— 封装用户登录时提交的凭证
 *
 * <h2>什么是 DTO？</h2>
 * <p>DTO（Data Transfer Object，数据传输对象）是专门用于在不同层之间传输数据的对象。
 * 它与实体（Entity）是不同的概念：
 * <ul>
 *   <li><b>Entity</b>: 映射数据库表，包含所有字段（密码、审计字段等）</li>
 *   <li><b>DTO</b>: 只包含 API 接口需要的字段，起到<b>数据过滤</b>和<b>接口隔离</b>的作用</li>
 * </ul>
 * </p>
 *
 * <h2>为什么要用 LoginRequest 而不是直接用 User？</h2>
 * <ul>
 *   <li><b>安全性</b>: User 实体包含 password、role 等敏感字段，直接接收 User 对象
 *       可能导致客户端恶意注入 role=ADMIN 等字段</li>
 *   <li><b>最小化原则</b>: 登录接口只需要用户名和密码，DTO 明确表达了这个契约</li>
 *   <li><b>解耦</b>: 数据库结构（Entity）的变化不会直接影响 API 接口（DTO）</li>
 *   <li><b>验证分离</b>: 不同接口可以有不同的验证规则，DTO 让验证更精准</li>
 * </ul>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>DTO 模式</b>: 在实际项目中，通常会有 Request DTO（接收参数）和 Response DTO
 *       （返回数据）的区分。本项目使用了 LoginRequest（输入）和 LoginResponse（输出）。</li>
 *   <li><b>数据验证</b>: 在生产环境中，应该添加 JSR 303 验证注解，如：
 *       {@code @NotBlank(message = "用户名不能为空")}、{@code @Size(min = 6, max = 20)} 等。
 *       配合 Controller 层的 {@code @Valid} 注解实现自动验证。</li>
 *   <li><b>{@code @Data}</b>: Lombok 注解，为 DTO 自动生成 getter、setter、toString 等，
 *       同时也生成了 equals 和 hashCode（虽然 DTO 中通常用不到）。</li>
 *   <li><b>JSON 反序列化</b>: Spring Boot 使用 Jackson 将请求体 JSON 自动反序列化为
 *       LoginRequest 对象。要求字段名与 JSON key 一致（或使用 {@code @JsonProperty} 映射）。</li>
 * </ul>
 *
 * @author Geekyous Guo
 * @since 1.0.0
 * @see LoginResponse
 * @see com.company.kb.controller.AuthController
 */
@Data
public class LoginRequest {

    /**
     * 用户名 — 登录凭证之一。
     */
    @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    /**
     * 密码 — 登录凭证之二。
     */
    @Schema(description = "密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
