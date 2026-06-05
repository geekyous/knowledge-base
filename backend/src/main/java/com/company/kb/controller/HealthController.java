package com.company.kb.controller;

import com.company.kb.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查控制器（Health Check Controller）— 提供应用运行状态探测端点
 *
 * <h2>什么是健康检查（Health Check）？</h2>
 * <p>健康检查是一种运维模式，用于监控应用是否正常运行。
 * 外部监控系统（如 Kubernetes、Docker、Nagios、Prometheus）定期调用健康检查端点，
 * 如果返回异常或超时，则触发告警或自动重启。</p>
 *
 * <h3>应用场景</h3>
 * <ul>
 *   <li><b>Kubernetes Liveness Probe</b>: 检测应用是否存活，失败则重启容器</li>
 *   <li><b>Kubernetes Readiness Probe</b>: 检测应用是否就绪（可接收流量）</li>
 *   <li><b>负载均衡器</b>: 判断后端实例是否健康，决定是否转发流量</li>
 *   <li><b>告警系统</b>: 持续监控应用可用性</li>
 * </ul>
 *
 * <h2>两个健康端点的区别</h2>
 * <table>
 *   <tr><th>端点</th><th>用途</th><th>返回格式</th></tr>
 *   <tr>
 *     <td>{@code /actuator/health}</td>
 *     <td>兼容 Spring Boot Actuator 标准路径</td>
 *     <td>{@code {"status": "UP"}}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code /api/v1/health}</td>
 *     <td>本项目自定义路径，返回更多信息</td>
 *     <td>{@code ApiResponse<Map>} 标准格式</td>
 *   </tr>
 * </table>
 *
 * <h2>Spring Boot Actuator</h2>
 * <p>Spring Boot Actuator 是 Spring Boot 的运维监控模块，提供了大量开箱即用的端点：
 * <ul>
 *   <li>{@code /actuator/health} — 健康检查</li>
 *   <li>{@code /actuator/info} — 应用信息</li>
 *   <li>{@code /actuator/metrics} — 性能指标</li>
 *   <li>{@code /actuator/env} — 环境变量</li>
 * </ul>
 * 本项目自定义了 /actuator/health 端点，简化了 Actuator 的配置。</p>
 *
 * 💡 学习要点:
 * <ul>
 *   <li><b>健康检查的完整性</b>: 生产环境的健康检查通常还会检测数据库连接、
 *       缓存连接、磁盘空间等。如果数据库连接断开，应返回 {"status": "DOWN"}。</li>
 *   <li><b>{@code Map.of()}</b>: JDK 9+ 提供的不可变 Map 工厂方法。
 *       比手动创建 HashMap 再 put 更简洁，适合少量键值对的场景。</li>
 *   <li><b>为什么不需要 {@code @RequestMapping}？</b>: 本控制器的两个端点路径完全不同，
 *       没有公共前缀，所以不需要类级别的 {@code @RequestMapping}。</li>
 *   <li><b>简单即最优</b>: 健康检查应该尽可能简单、快速。如果它本身耗时过长或依赖太多组件，
 *       就失去了"快速判断应用状态"的意义。</li>
 * </ul>
 *
 * @author Geekyous Guo
 * @since 1.0.0
 */
@RestController
@Tag(name = "健康检查", description = "应用运行状态探测接口")
public class HealthController {

    /**
     * Actuator 标准健康检查端点 — 模拟 Spring Boot Actuator 的 /actuator/health。
     *
     * <p>此端点在 {@code SecurityConfig} 中被配置为公开访问（permitAll），
     * 无需认证即可调用。</p>
     *
     * <p>返回简化的 JSON：{@code {"status": "UP"}}。
     * "UP" 是 Spring Boot Actuator 的标准状态值，其他可能的值有 DOWN、OUT_OF_SERVICE、UNKNOWN。</p>
     *
     * @return 包含状态的 Map（会被 Jackson 序列化为 JSON）
     */
    @GetMapping("/actuator/health")
    @Operation(summary = "Actuator 健康检查", description = "兼容 Spring Boot Actuator 标准路径，返回 {\"status\": \"UP\"}")
    public Map<String, Object> health() {
        // Map.of("key", "value") 创建不可变的单元素 Map
        // 在生产环境中，这里应该检查数据库连接、Redis 等依赖服务的状态
        return Map.of("status", "UP");
    }

    /**
     * 自定义 API 健康检查端点 — 返回更多信息。
     *
     * <p>与 Actuator 端点不同，此端点返回标准 {@code ApiResponse} 格式，
     * 包含服务名称等额外信息。可以用于前端判断 API 是否可用。</p>
     *
     * <p>响应示例：
     * <pre>
     * {
     *   "code": 200,
     *   "message": "success",
     *   "data": {
     *     "status": "UP",
     *     "service": "knowledge-base"
     *   }
     * }
     * </pre></p>
     *
     * @return 标准格式的健康状态响应
     */
    @GetMapping("/api/v1/health")
    @Operation(summary = "API 健康检查", description = "返回标准 ApiResponse 格式的健康状态，包含服务名称")
    public ApiResponse<Map<String, String>> apiHealth() {
        return ApiResponse.success(Map.of("status", "UP", "service", "knowledge-base"));
    }
}
