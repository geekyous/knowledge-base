package com.company.kb.utils;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 统一响应结果工具类（Response Utility）— 另一种风格的统一响应包装器
 *
 * <h2>与 {@link com.company.kb.dto.ApiResponse} 的关系</h2>
 * <p>本类和 {@code ApiResponse} 功能类似，都是统一响应包装器。
 * 它们是同一设计模式的两种实现。区别在于：
 * <table>
 *   <tr><th>特性</th><th>ApiResponse</th><th>Response（本类）</th></tr>
 *   <tr><td>时间戳</td><td>无</td><td>有（timestamp 字段）</td></tr>
 *   <tr><td>静态工厂方法</td><td>仅 success/error</td><td>更丰富（含 unauthorized、forbidden 等）</td></tr>
 *   <tr><td>HTTP 状态码</td><td>手动传入</td><td>使用 {@code HttpStatus} 枚举</td></tr>
 *   <tr><td>验证失败</td><td>无专用方法</td><td>有 {@code validationFailed} 方法</td></tr>
 * </table>
 * </p>
 *
 * <h2>设计模式：统一响应包装器（Uniform Response Wrapper）</h2>
 * <p>这是 REST API 开发中最常见的设计模式之一。所有接口返回相同结构的 JSON，
 * 前端可以用统一的方式处理成功和失败响应。</p>
 *
 * <h2>HTTP 状态码语义化</h2>
 * <p>本类通过 {@link HttpStatus} 枚举提供了语义化的状态码：
 * <ul>
 *   <li>{@code HttpStatus.OK} (200) — 成功</li>
 *   <li>{@code HttpStatus.BAD_REQUEST} (400) — 参数错误</li>
 *   <li>{@code HttpStatus.UNAUTHORIZED} (401) — 未认证</li>
 *   <li>{@code HttpStatus.FORBIDDEN} (403) — 无权限</li>
 *   <li>{@code HttpStatus.NOT_FOUND} (404) — 资源不存在</li>
 *   <li>{@code HttpStatus.INTERNAL_SERVER_ERROR} (500) — 服务器错误</li>
 * </ul>
 * </p>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>静态工厂方法模式</b>: 所有的 {@code success()}、{@code error()} 等方法都是静态工厂。
 *       比直接使用构造器的优势：1) 方法名有语义；2) 可以返回子类型；3) 可以缓存实例。</li>
 *   <li><b>时间戳的生成</b>: {@code LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()}
 *       将本地时间转换为 Unix 时间戳（毫秒）。这样前端可以根据时区自行格式化显示。</li>
 *   <li><b>{@code @Data}</b>: Lombok 注解，自动生成 getter/setter/equals/hashCode/toString。
 *       注意：如果使用 Jackson 序列化，setter 方法让此对象也可以用于反序列化（如测试中）。</li>
 *   <li><b>未使用 {@code @JsonInclude}</b>: 与 ApiResponse 不同，本类没有 {@code @JsonInclude(NON_NULL)}，
 *       意味着 null 字段也会出现在 JSON 中（如 {@code "data": null}）。</li>
 *   <li><b>泛型方法</b>: {@code public static <T> Response<T>} 中的 {@code <T>} 允许每个方法
 *       返回不同泛型类型的 Response，而不需要强制类型转换。</li>
 * </ul>
 *
 * @param <T> 响应数据的泛型类型
 * @author Knowledge Base Team
 * @since 1.0.0
 * @see com.company.kb.dto.ApiResponse
 * @see HttpStatus
 */
@Data
public class Response<T> {

    /**
     * HTTP 风格的状态码 — 表示请求处理结果。
     *
     * <p>使用 {@link HttpStatus} 枚举的 {@code value()} 获取整数状态码。
     * 例如 {@code HttpStatus.OK.value()} 返回 200。</p>
     */
    private Integer code;

    /**
     * 响应消息 — 人类可读的处理结果描述。
     *
     * <p>成功时为 "操作成功"，失败时为具体的错误描述。</p>
     */
    private String message;

    /**
     * 响应数据 — 泛型类型的业务数据。
     *
     * <p>成功时携带实际数据，失败时通常为 null。</p>
     */
    private T data;

    /**
     * 响应时间戳 — 毫秒级 Unix 时间戳。
     *
     * <p>在构造器中自动设置为当前时间，用于：
     * <ul>
     *   <li>调试时追踪请求</li>
     *   <li>前端判断响应的时效性</li>
     *   <li>日志关联和分析</li>
     * </ul>
     * </p>
     */
    private Long timestamp;

    /**
     * 默认构造器 — 自动设置时间戳。
     *
     * <p>时间戳转换链：
     * <pre>
     * LocalDateTime.now()                          // 获取当前本地时间
     *   .atZone(ZoneId.systemDefault())            // 附加时区信息 → ZonedDateTime
     *   .toInstant()                                // 转为 UTC 时间线上的瞬时点 → Instant
     *   .toEpochMilli()                             // 转为 Unix 时间戳（毫秒）→ long
     * </pre></p>
     */
    public Response() {
        this.timestamp = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 全参构造器 — 设置所有字段。
     *
     * <p>先调用默认构造器（设置 timestamp），再设置 code、message、data。</p>
     *
     * @param code    状态码
     * @param message 响应消息
     * @param data    响应数据
     */
    public Response(Integer code, String message, T data) {
        this();  // 调用默认构造器，设置 timestamp
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（无数据）— 用于不需要返回数据的操作，如删除、登出。
     *
     * <p>使用示例：{@code Response.success()}</p>
     *
     * @param <T> 泛型类型（通常为 Void）
     * @return 状态码 200，消息 "操作成功"，数据为 null 的响应
     */
    public static <T> Response<T> success() {
        return new Response<>(HttpStatus.OK.value(), "操作成功", null);
    }

    /**
     * 成功响应（有数据）— 最常用的成功响应形式。
     *
     * <p>使用示例：{@code Response.success(user)}</p>
     *
     * @param data 要返回的业务数据
     * @param <T>  数据的泛型类型
     * @return 状态码 200，消息 "操作成功"，携带数据的响应
     */
    public static <T> Response<T> success(T data) {
        return new Response<>(HttpStatus.OK.value(), "操作成功", data);
    }

    /**
     * 成功响应（自定义消息）— 用于需要特别说明的成功操作。
     *
     * <p>使用示例：{@code Response.success("创建成功", newDocument)}</p>
     *
     * @param message 自定义成功消息
     * @param data    要返回的业务数据
     * @param <T>     数据的泛型类型
     * @return 状态码 200，自定义消息，携带数据的响应
     */
    public static <T> Response<T> success(String message, T data) {
        return new Response<>(HttpStatus.OK.value(), message, data);
    }

    /**
     * 错误响应（仅消息）— 使用默认的 500 状态码。
     *
     * <p>使用示例：{@code Response.error("操作失败")}</p>
     *
     * @param message 错误描述
     * @param <T>     泛型类型
     * @return 状态码 500，错误消息，数据为 null 的响应
     */
    public static <T> Response<T> error(String message) {
        return new Response<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, null);
    }

    /**
     * 错误响应（带状态码）— 支持不同类型的错误。
     *
     * <p>使用示例：
     * <ul>
     *   <li>{@code Response.error(400, "参数错误")}</li>
     *   <li>{@code Response.error(404, "资源不存在")}</li>
     * </ul></p>
     *
     * @param code    错误状态码
     * @param message 错误描述
     * @param <T>     泛型类型
     * @return 指定状态码和消息，数据为 null 的响应
     */
    public static <T> Response<T> error(Integer code, String message) {
        return new Response<>(code, message, null);
    }

    /**
     * 错误响应（带状态码和数据）— 适用于需要在错误响应中携带数据的场景。
     *
     * <p>例如参数验证失败时，返回所有验证错误信息列表。</p>
     *
     * @param code    错误状态码
     * @param message 错误描述
     * @param data    错误详情数据
     * @param <T>     数据的泛型类型
     * @return 指定状态码、消息和数据的响应
     */
    public static <T> Response<T> error(Integer code, String message, T data) {
        return new Response<>(code, message, data);
    }

    /**
     * 参数校验失败响应 — 专门用于 Bean Validation 校验错误。
     *
     * <p>配合 Spring 的 {@code @Valid} 注解使用。当请求参数校验失败时，
     * 将所有错误信息收集到 List 中返回给前端展示。</p>
     *
     * <p>使用示例：{@code Response.validationFailed(List.of("用户名不能为空", "邮箱格式不正确"))}</p>
     *
     * @param errors 所有校验错误信息的列表
     * @param <T>    泛型类型（此处 data 字段为 List<String> 类型）
     * @return 状态码 400，消息 "参数校验失败"，携带错误列表的响应
     */
    public static <T> Response<T> validationFailed(List<String> errors) {
        return new Response<>(HttpStatus.BAD_REQUEST.value(), "参数校验失败", errors);
    }

    /**
     * 未授权响应 — 客户端未提供有效的认证信息。
     *
     * <p>HTTP 401 Unauthorized。适用于：
     * <ul>
     *   <li>未携带 Token</li>
     *   <li>Token 过期</li>
     *   <li>Token 无效</li>
     * </ul></p>
     *
     * @param message 错误描述（如 "请先登录"）
     * @param <T>     泛型类型
     * @return 状态码 401，错误消息，数据为 null 的响应
     */
    public static <T> Response<T> unauthorized(String message) {
        return new Response<>(HttpStatus.UNAUTHORIZED.value(), message, null);
    }

    /**
     * 禁止访问响应 — 已认证但权限不足。
     *
     * <p>HTTP 403 Forbidden。与 401 的区别：
     * <ul>
     *   <li>401 — "你是谁？"（身份未确认）</li>
     *   <li>403 — "我知道你是谁，但你没有权限"（身份已确认，权限不足）</li>
     * </ul></p>
     *
     * @param message 错误描述（如 "权限不足"）
     * @param <T>     泛型类型
     * @return 状态码 403，错误消息，数据为 null 的响应
     */
    public static <T> Response<T> forbidden(String message) {
        return new Response<>(HttpStatus.FORBIDDEN.value(), message, null);
    }

    /**
     * 资源不存在响应 — 请求的资源未找到。
     *
     * <p>HTTP 404 Not Found。适用于：
     * <ul>
     *   <li>查询的 ID 不存在</li>
     *   <li>URL 路径错误</li>
     *   <li>资源已被删除</li>
     * </ul></p>
     *
     * @param message 错误描述（如 "用户不存在"）
     * @param <T>     泛型类型
     * @return 状态码 404，错误消息，数据为 null 的响应
     */
    public static <T> Response<T> notFound(String message) {
        return new Response<>(HttpStatus.NOT_FOUND.value(), message, null);
    }
}
