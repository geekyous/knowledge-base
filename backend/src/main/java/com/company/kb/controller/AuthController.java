package com.company.kb.controller;

import com.company.kb.dto.ApiResponse;
import com.company.kb.dto.LoginRequest;
import com.company.kb.dto.LoginResponse;
import com.company.kb.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器（Auth Controller）— 处理用户登录和登出请求
 *
 * <h2>架构角色 — Controller Layer（控制器层）</h2>
 * <p>Controller 是 MVC 架构中的 C，负责：
 * <ul>
 *   <li><b>接收请求</b>: 接收 HTTP 请求并映射到方法参数</li>
 *   <li><b>参数绑定</b>: 将请求体、路径参数、查询参数绑定到 Java 对象</li>
 *   <li><b>调用服务</b>: 将请求委托给 Service 层处理业务逻辑</li>
 *   <li><b>返回响应</b>: 将 Service 返回的结果包装为统一的 API 响应格式</li>
 * </ul>
 * Controller 本身不包含业务逻辑，它是 HTTP 世界和业务世界的桥梁。</p>
 *
 * <h2>REST API 设计</h2>
 * <p>本控制器遵循 RESTful API 设计原则：
 * <ul>
 *   <li>{@code @RestController} = {@code @Controller} + {@code @ResponseBody}</li>
 *   <li>使用 HTTP 方法（POST）表达操作语义</li>
 *   <li>使用标准 HTTP 状态码（本项目中统一返回 200，通过 body 中的 code 区分）</li>
 *   <li>URL 路径采用名词 + 动词的混合风格</li>
 * </ul>
 * </p>
 *
 * <h3>URL 映射</h3>
 * <table>
 *   <tr><th>HTTP 方法</th><th>路径</th><th>功能</th></tr>
 *   <tr><td>POST</td><td>/api/v1/auth/login</td><td>用户登录</td></tr>
 *   <tr><td>POST</td><td>/api/v1/auth/logout</td><td>用户登出</td></tr>
 * </table>
 *
 * <h2>API 版本控制</h2>
 * <p>{@code /api/v1/auth} 中的 v1 是版本号。当 API 发生不兼容变更时，
 * 可以创建 v2 版本，保持旧版本继续运行。这是 API 生命周期管理的最佳实践。</p>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>{@code @RestController}</b>: 组合注解，等价于 {@code @Controller} + {@code @ResponseBody}。
 *       它告诉 Spring：1) 本类是 MVC 控制器；2) 方法的返回值直接作为 HTTP 响应体
 *       （而非视图名称）。Spring 使用 Jackson 将返回对象序列化为 JSON。</li>
 *   <li><b>{@code @RequestMapping}</b>: 类级别的 URL 映射。所有方法的 URL 都以此为前缀。
 *       结合方法级别的 {@code @PostMapping}，完整路径为 /api/v1/auth/login。</li>
 *   <li><b>{@code @PostMapping}</b>: 指定处理 HTTP POST 请求。等价于
 *       {@code @RequestMapping(method = RequestMethod.POST)}。类似还有
 *       {@code @GetMapping}、{@code @PutMapping}、{@code @DeleteMapping}。</li>
 *   <li><b>{@code @RequestBody}</b>: 将 HTTP 请求体（JSON）反序列化为 Java 对象。
 *       Spring 使用 Jackson 完成 JSON → Object 的转换。</li>
 *   <li><b>登录使用 POST 而非 GET</b>: GET 请求的参数会出现在 URL 中（?username=xxx&password=xxx），
 *       可能被浏览器历史记录、日志、代理服务器记录，存在安全风险。</li>
 * </ul>
 *
 * @author Geekyous Guo
 * @since 1.0.0
 * @see AuthService
 * @see LoginRequest
 * @see LoginResponse
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证管理", description = "用户登录、登出等认证相关接口")
public class AuthController {

    /** 认证服务 — 处理登录业务逻辑 */
    private final AuthService authService;

    /**
     * 构造器注入 AuthService。
     *
     * @param authService 认证服务实例
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录接口 — 验证用户凭证并返回 JWT Token。
     *
     * <h3>请求示例</h3>
     * <pre>
     * POST /api/v1/auth/login
     * Content-Type: application/json
     *
     * {
     *   "username": "admin",
     *   "password": "123456"
     * }
     * </pre>
     *
     * <h3>响应示例</h3>
     * <pre>
     * {
     *   "code": 200,
     *   "message": "success",
     *   "data": {
     *     "token": "eyJhbGciOiJIUzI1NiJ9...",
     *     "user": { "id": 1, "username": "admin", ... }
     *   }
     * }
     * </pre>
     *
     * @param request 登录请求 DTO（由 Spring 从请求体 JSON 自动反序列化）
     * @return 统一响应包装的登录结果
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "验证用户名和密码，返回 JWT Token 和用户信息")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        // 委托给 AuthService 处理登录逻辑，将结果包装为统一响应格式
        return ApiResponse.success(authService.login(request));
    }

    /**
     * 用户登出接口 — 客户端丢弃 Token 即可实现登出。
     *
     * <p><b>JWT 的无状态登出</b>: 由于 JWT 是无状态的（服务端不存储 Token），
     * 真正的"登出"由客户端完成——删除本地存储的 Token 即可。
     * 本接口仅作为语义上的补充，告知客户端"登出成功"。</p>
     *
     * <p><b>进阶方案</b>: 如果需要服务端强制登出（如用户修改密码后使旧 Token 失效），
     * 可以使用 Token 黑名单（Redis 存储）或缩短 Token 有效期 + Refresh Token 机制。</p>
     *
     * @return 登出成功提示
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "客户端丢弃 Token 即可实现登出，本接口为语义补充")
    public ApiResponse<Void> logout() {
        return ApiResponse.success("退出登录成功", null);
    }
}
