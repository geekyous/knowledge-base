package com.company.kb.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统一 API 响应包装类（Generic API Response Wrapper）
 *
 * <h2>设计模式：统一响应包装器（Uniform Response Wrapper）</h2>
 * <p>所有 API 接口都返回相同结构的 JSON，使前端可以用统一的方式处理响应。
 * 这是一种在 REST API 开发中非常普遍的最佳实践。</p>
 *
 * <h3>为什么需要统一响应格式？</h3>
 * <ul>
 *   <li><b>前端处理简化</b>: 前端只需判断 {@code code === 200} 就知道请求是否成功，
 *       无需根据 HTTP 状态码分别处理</li>
 *   <li><b>错误信息标准化</b>: 所有错误信息都以相同结构返回，便于统一展示</li>
 *   <li><b>便于拦截器统一处理</b>: Axios 等前端库可以在拦截器中统一处理响应格式</li>
 * </ul>
 *
 * <h3>响应 JSON 结构示例</h3>
 * <pre>
 * 成功: { "code": 200, "message": "success", "data": { ... } }
 * 失败: { "code": 401, "message": "未授权" }  // data 为 null，因 @JsonInclude 被省略
 * </pre>
 *
 * <h2>泛型设计</h2>
 * <p>{@code ApiResponse<T>} 使用 Java 泛型，{@code T} 代表 data 字段的类型。
 * 这使得不同接口可以返回不同类型的数据，同时保持统一的响应结构。
 * 例如：
 * <ul>
 *   <li>{@code ApiResponse<LoginResponse>} — 登录接口</li>
 *   <li>{@code ApiResponse<Page<Document>>} — 文档列表接口</li>
 *   <li>{@code ApiResponse<Void>} — 无数据的操作（如删除）</li>
 * </ul>
 * </p>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>{@code @JsonInclude(JsonInclude.Include.NON_NULL)}</b>: Jackson 序列化注解，
 *       当字段值为 {@code null} 时，该字段不会出现在 JSON 输出中。
 *       这避免了响应中出现 {@code "data": null} 这样的冗余信息。</li>
 *   <li><b>静态工厂方法</b>: {@code success()} 和 {@code error()} 是工厂方法模式（Factory Method），
 *       隐藏了构造细节，提供更语义化的 API 调用方式。</li>
 *   <li><b>泛型方法推断</b>: {@code public static <T> ApiResponse<T> success(T data)} 中的
 *       {@code <T>} 是方法级别的泛型声明，编译器会根据传入参数自动推断类型。</li>
 *   <li><b>HTTP 状态码 vs 业务状态码</b>: 本项目的 {@code code} 是业务状态码，
 *       始终在 HTTP 200 中返回。另一种做法是让 HTTP 状态码与业务状态码一致（更 RESTful）。</li>
 * </ul>
 *
 * @param <T> 响应数据的类型
 * @author Geekyous Guo
 * @since 1.0.0
 * @see com.fasterxml.jackson.annotation.JsonInclude
 */
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * 业务状态码 — 200 表示成功，其他值表示各种错误。
     *
     * <p>注意：这是<b>业务状态码</b>，与 HTTP 状态码是两个概念。
     * HTTP 响应始终返回 200，具体业务结果通过此字段判断。</p>
     */
    @Schema(description = "业务状态码，200 表示成功", example = "200")
    private int code;

    /**
     * 响应消息 — 人类可读的描述信息。
     */
    @Schema(description = "响应消息", example = "success")
    private String message;

    /**
     * 响应数据 — 泛型类型，携带实际的业务数据。
     */
    @Schema(description = "响应数据")
    private T data;

    /**
     * 成功响应的工厂方法 — 携带数据，使用默认消息 "success"。
     *
     * <p>使用示例：{@code ApiResponse.success(user)}</p>
     *
     * @param data 要返回的业务数据
     * @param <T>  数据的泛型类型
     * @return 包装后的成功响应对象
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    /**
     * 成功响应的工厂方法 — 携带自定义消息和数据。
     *
     * <p>使用示例：{@code ApiResponse.success("创建成功", document)}</p>
     *
     * @param message 自定义成功消息
     * @param data    要返回的业务数据
     * @param <T>     数据的泛型类型
     * @return 包装后的成功响应对象
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    /**
     * 错误响应的工厂方法 — 不携带数据。
     *
     * <p>使用示例：{@code ApiResponse.error(401, "未授权")}</p>
     *
     * @param code    错误状态码（如 400、401、403、500 等）
     * @param message 错误描述信息
     * @param <T>     泛型类型（错误响应中通常为 Void）
     * @return 包装后的错误响应对象
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
