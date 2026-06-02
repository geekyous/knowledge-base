package com.company.kb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 DTO（Login Response DTO）— 封装登录成功后返回给客户端的数据
 *
 * <h2>设计模式：Builder 模式</h2>
 * <p>本类使用了 Lombok 的 {@code @Builder} 注解实现建造者模式（Builder Pattern），
 * 这是一种创建型设计模式，特别适合字段较多的对象构造。</p>
 *
 * <h3>Builder 模式优势</h3>
 * <ul>
 *   <li><b>可读性强</b>: {@code LoginResponse.builder().token("xxx").user(info).build()}
 *       比构造器 {@code new LoginResponse("xxx", info)} 更易读</li>
 *   <li><b>参数灵活</b>: 不需要记忆参数顺序，可以只设置需要的字段</li>
 *   <li><b>不可变性</b>: 配合 final 字段可以创建不可变对象（线程安全）</li>
 * </ul>
 *
 * <h2>嵌套 DTO 设计</h2>
 * <p>{@code UserInfo} 是 {@code LoginResponse} 的静态内部类。这种设计体现了<b>组合模式</b>：
 * <ul>
 *   <li>将复杂的响应数据拆分为多个小的 DTO，每个 DTO 职责单一</li>
 *   <li>使用静态内部类（而非独立类），因为这些 DTO 只在特定场景下使用，不会复用</li>
 *   <li>JSON 序列化后，UserInfo 会作为 user 字段的嵌套对象</li>
 * </ul>
 * </p>
 *
 * <h3>响应 JSON 结构示例</h3>
 * <pre>
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": {
 *     "token": "eyJhbGciOiJIUzI1NiJ9...",
 *     "user": {
 *       "id": 1,
 *       "username": "admin",
 *       "email": "admin@company.com",
 *       "role": "ADMIN",
 *       "avatar": "https://..."
 *     }
 *   }
 * }
 * </pre>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>为什么 token 和 user 信息分开返回？</b>: Token 用于后续请求的身份验证
 *       （放在 Authorization 头部），user 信息用于前端展示（如导航栏用户名）。
 *       分开后前端可以独立缓存和使用。</li>
 *   <li><b>为什么不直接返回 User 实体？</b>: User 实体包含密码、审计字段等敏感信息。
 *       DTO 只暴露前端需要的字段，是<b>最小知识原则</b>的体现。</li>
 *   <li><b>静态内部类</b>: {@code static} 修饰的内部类不持有外部类的引用，
 *       可以独立创建实例。如果是非静态内部类，则必须先有外部类实例才能创建。</li>
 *   <li><b>Lombok 注解组合</b>: {@code @Data + @Builder + @NoArgsConstructor + @AllArgsConstructor}
 *       是 DTO 的标准 Lombok 配置，既支持 Builder 构造，也支持 Jackson 反序列化。</li>
 * </ul>
 *
 * @author Knowledge Base Team
 * @since 1.0.0
 * @see LoginRequest
 * @see com.company.kb.service.AuthService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * JWT 访问令牌 — 登录成功后由服务端签发的身份凭证。
     *
     * <p>客户端应在后续请求的 Authorization 头部携带此 Token：
     * {@code Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...}</p>
     *
     * <p>Token 包含了用户的身份信息（用户名、角色、ID）和过期时间，
     * 服务端通过签名验证 Token 的真实性。无需在服务端存储 Session。</p>
     */
    private String token;

    /**
     * 用户基本信息 — 登录成功后返回的用户摘要数据。
     *
     * <p>使用嵌套 DTO 而非完整的 User 实体，只包含前端需要的字段，
     * 排除了密码、审计时间等敏感/无用字段。</p>
     */
    private UserInfo user;

    /**
     * 用户信息嵌套 DTO — 只包含前端展示所需的用户字段。
     *
     * <p>作为 {@code LoginResponse} 的静态内部类，体现了高内聚的设计原则：
     * UserInfo 只在登录响应场景下使用，不需要独立存在。</p>
     *
     * <p><b>安全设计</b>: 注意此 DTO 中没有 password 字段。这是 DTO 的核心价值——
     * 作为实体（Entity）和外部接口之间的过滤器，防止敏感数据泄露。</p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {

        /** 用户唯一标识 */
        private Long id;

        /** 用户名 */
        private String username;

        /** 邮箱 */
        private String email;

        /** 用户角色（字符串形式，如 "ADMIN"） */
        private String role;

        /** 头像 URL */
        private String avatar;
    }
}
