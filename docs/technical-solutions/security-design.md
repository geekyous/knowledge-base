# 接口安全防护技术方案

> 本文档是后端 API 安全防护的完整技术方案，涵盖从请求到达、身份认证到异常处理的全链路安全设计。
> 对应代码位于 `feature/split-prototype` 分支。

## 目录

- [1. 安全威胁分析](#1-安全威胁分析)
- [2. 防护体系总览](#2-防护体系总览)
- [3. 登录防暴力破解](#3-登录防暴力破解)
- [4. 接口限流](#4-接口限流)
- [5. Token 黑名单（登出即失效）](#5-token-黑名单登出即失效)
- [6. 安全响应头](#6-安全响应头)
- [7. CORS 跨域安全](#7-cors-跨域安全)
- [8. 异常处理体系](#8-异常处理体系)
- [9. 请求安全链路](#9-请求安全链路)
- [10. 已有安全能力](#10-已有安全能力)
- [11. 生产环境加固清单](#11-生产环境加固清单)

---

## 1. 安全威胁分析

| 威胁 | 严重程度 | 说明 |
|------|---------|------|
| 登录暴力破解 | 🔴 严重 | 攻击者用字典/撞库高速重试密码，攻破弱密码账号 |
| 接口无限制调用 | 🔴 严重 | 无频率限制，可被刷爆（DoS、数据爬取、暴力重试） |
| Token 无法吊销 | 🟠 高 | JWT 过期前始终有效，泄露后无法即失效 |
| 点击劫持 / MIME 嗅探 | 🟠 高 | 浏览器默认行为可被利用执行恶意操作 |
| CORS 通配 | 🟠 高 | `allowedOrigins("*")` 允许任意来源，等于关闭同源策略 |
| 异常信息泄露 | 🟡 中 | 内部堆栈暴露给客户端，帮助攻击者了解系统结构 |

## 2. 防护体系总览

```
请求到达
  │  ── [Servlet Filter 层 · DispatcherServlet 之前] ──
  ├── ① SecurityHeadersFilter → 添加 7 项安全响应头
  │
  ├── ② JwtAuthenticationFilter（Spring Security 链成员）→ Token 解析 + 黑名单校验，写入 SecurityContext
  │
  ├── ③ Spring Security 授权（AuthorizationFilter）→ URL + HTTP 方法级别鉴权
  │  ── DispatcherServlet ──
  ├── ④ RateLimitInterceptor（HandlerInterceptor）→ Redis 计数限流，超限返回 429
  │
  └── ⑤ Controller → Service → BusinessException
        │
        └── ⑥ GlobalExceptionHandler → 统一错误响应（正确状态码）
```

## 3. 登录防暴力破解

### 3.1 为什么需要

攻击者可使用自动化工具（Hydra、Burp Suite）对登录接口发起字典攻击或撞库攻击，
尝试大量用户名/密码组合。若无限制：
- 弱密码账号（如 `admin/123456`）在几分钟内被攻破
- 攻击者可并行发起数千次尝试，消耗服务器资源
- 用户名枚举：通过不同错误提示判断用户是否存在

### 3.2 技术方案：Redis 分布式计数 + 自动锁定

采用 Redis 而非数据库字段（`login_attempts` + `locked_until`）存储失败计数：

| 维度 | Redis 方案 | JPA Entity 方案 |
|------|-----------|----------------|
| 侵入性 | 不修改 users 表结构 | 需新增两个字段 |
| 自动过期 | TTL 天然实现窗口滑动和锁定解除 | 需定时任务清理过期记录 |
| 分布式 | 多实例共享计数 | 单机或需分布式锁 |
| 性能 | 内存操作，微秒级 | 每次失败需写库 |

### 3.3 保护流程

```
POST /api/v1/auth/login
  │
  ├── 限流检查（@RateLimit: 5次/60秒）
  │
  ├── isLocked(username)?
  │     ├─ 是 → 423 "账号已锁定，请X分钟后再试"
  │     └─ 否 → 继续
  │
  ├── 查找用户 → 不存在 → recordFailure() → 401 "用户名或密码错误"
  │
  ├── RSA 解密 → BCrypt 验证 → 失败 → recordFailure() → 401 "用户名或密码错误"
  │
  ├── 检查状态 → 已禁用 → 403 "账号已被禁用"
  │
  └── 登录成功 → recordSuccess() → 清除失败计数 → 返回 JWT
```

### 3.4 核心实现

```java
// LoginProtectionService.java
@Service
public class LoginProtectionService {

    private static final String FAIL_KEY_PREFIX = "login_fail:";
    private static final String LOCK_KEY_PREFIX = "login_lock:";
    private static final int MAX_FAIL_COUNT = 5;          // 连续失败 5 次触发锁定
    private static final int FAIL_WINDOW_SECONDS = 300;    // 失败计数窗口 5 分钟
    private static final int LOCK_DURATION_SECONDS = 900;  // 锁定 15 分钟

    private final StringRedisTemplate redisTemplate;

    /**
     * 记录一次登录失败。
     * 首次失败时设置窗口 TTL；达到阈值时触发账号锁定并清除计数。
     */
    public int recordFailure(String username) {
        String failKey = FAIL_KEY_PREFIX + username;
        Long count = redisTemplate.opsForValue().increment(failKey);

        if (count != null && count == 1) {
            redisTemplate.expire(failKey, FAIL_WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        if (count != null && count >= MAX_FAIL_COUNT) {
            String lockKey = LOCK_KEY_PREFIX + username;
            redisTemplate.opsForValue().set(lockKey, "1", LOCK_DURATION_SECONDS, TimeUnit.SECONDS);
            redisTemplate.delete(failKey);  // 锁定解除后重新计数
        }

        return count != null ? count.intValue() : 1;
    }

    public boolean isLocked(String username) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_KEY_PREFIX + username));
    }

    public long getRemainingLockTime(String username) {
        Long ttl = redisTemplate.getExpire(LOCK_KEY_PREFIX + username, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    public void recordSuccess(String username) {
        redisTemplate.delete(FAIL_KEY_PREFIX + username);
    }
}
```

### 3.5 Redis Key 设计

| Key 模式 | 用途 | TTL | 示例 |
|----------|------|-----|------|
| `login_fail:{username}` | 失败计数器 | 300s | `login_fail:admin` → `"3"` |
| `login_lock:{username}` | 锁定标记 | 900s | `login_lock:admin` → `"1"` |

### 3.6 防御效果

| 攻击场景 | 防御结果 |
|---------|---------|
| 字典攻击（万级密码） | 5 次失败后锁定 15 分钟，攻击速度降至 20 次/小时 |
| 分布式暴力破解（多 IP 同账号） | 按用户名维度锁定，IP 不影响计数 |
| 正常用户输错 | 窗口 5 分钟内 5 次以内不受影响，正确输入即清除计数 |

## 4. 接口限流

### 4.1 为什么需要

无限制的接口调用可被滥用为：

- **暴力破解**：高速重试密码、验证码
- **资源耗尽**：恶意请求消耗 CPU / DB / 带宽（DoS）
- **数据爬取**：批量抓取公开内容（文档、用户信息）

限流是成本最低、效果最直接的防护手段，在入口处拦截超限请求。

### 4.2 技术方案：@RateLimit 注解 + Redis 滑动窗口

采用**注解声明式限流**，Controller 只需标注 `@RateLimit` 即可生效，
无需在业务代码中耦合限流逻辑。

核心机制：
- **Redis INCR + EXPIRE**：原子递增计数 + 首次请求设置过期时间，实现固定窗口限流
- **IP + URI 维度**：按客户端 IP 和请求路径组合计数，不同接口互不影响
- **方法级优先**：方法上的 `@RateLimit` 优先于类上的，同一接口可覆盖全局配置

### 4.3 注解定义

```java
// RateLimit.java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /** 限流标识，用于 Redis key 前缀（如 "login"、"document"） */
    String key() default "global";
    /** 时间窗口内允许的最大请求数 */
    int permits() default 60;
    /** 时间窗口（秒） */
    int seconds() default 60;
}
```

### 4.4 拦截器实现

```java
// RateLimitInterceptor.java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String KEY_PREFIX = "rate_limit:";
    private final StringRedisTemplate redisTemplate;
    private final ClientIpResolver clientIpResolver;  // 可信代理感知的 IP 解析

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 1. 查找注解（方法级优先，其次类级）
        if (!(handler instanceof HandlerMethod handlerMethod)) return true;

        RateLimit methodLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        RateLimit classLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        RateLimit rateLimit = methodLimit != null ? methodLimit : classLimit;
        if (rateLimit == null) return true;

        // 2. 解析真实客户端 IP，构建 Redis Key: rate_limit:{key}:{ip}:{uri}
        String clientIp = getClientIp(request);
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = "anonymous";  // 识别不到 IP 时归入匿名桶，避免字面量 "null" 互相挤占配额
        }
        String redisKey = KEY_PREFIX + rateLimit.key() + ":" + clientIp
                          + ":" + request.getRequestURI();

        // 3. 原子递增
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, rateLimit.seconds(), TimeUnit.SECONDS);
        }

        // 4. 超限返回 429
        if (count != null && count > rateLimit.permits()) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            return false;
        }
        return true;
    }

    /** 委托 ClientIpResolver 解析，抵御 X-Forwarded-For 伪造 */
    private String getClientIp(HttpServletRequest request) {
        return clientIpResolver.resolve(request);
    }
}
```

#### 客户端 IP 解析：可信代理感知

`ClientIpResolver`（`config/ClientIpResolver.java`）解决了代理场景下 IP 解析的两个隐患：

**隐患一：X-Forwarded-For 可伪造**

Nginx 用 `$proxy_add_x_forwarded_for`，把真实客户端 IP **追加到 XFF 末尾**。客户端自带伪造头时：

```
客户端请求:  X-Forwarded-For: 1.2.3.4 (伪造)
Nginx 转发:  X-Forwarded-For: 1.2.3.4, <真实IP>
```

旧实现 `xff.split(",")[0]` 取最左 = **伪造值**，攻击者每请求换一个 IP 即可绕过按 IP 限流。

**隐患二：代理后 getRemoteAddr() 全是代理 IP**

经 Nginx 转发时 `getRemoteAddr()` = Nginx 容器 IP，所有请求塌缩到同一限流桶，失去按客户端隔离的意义。

**算法（抵御伪造的关键）**：仅在 TCP 对端是可信代理时才读 XFF，并从右往左跳过仍是可信代理的条目，取第一个非可信 IP：

| 场景 | getRemoteAddr | XFF | 解析结果 |
|------|--------------|-----|---------|
| 单级代理，正常 | nginx IP（可信） | `<真实IP>` | `<真实IP>` |
| 单级代理，客户端伪造 | nginx IP（可信） | `1.2.3.4, <真实IP>` | `<真实IP>`（伪造的最左值被跳过） |
| 直连 :8080（绕过 Nginx） | 公网 IP（不可信） | 任意伪造 | 公网 IP（XFF 完全忽略） |
| IDE 直跑（无代理配置） | 127.0.0.1 | 任意 | 127.0.0.1（直连模式，XFF 忽略） |

**安全细节**：
- **DNS 安全**：`getRemoteAddr()` 必为数值字面量，可安全用 `InetAddress` 全解析；XFF 来自不可信客户端（可能塞主机名触发 DNS），只走严格正则 + 字符串匹配，绝不调用 `InetAddress.getByName`。
- **IPv4-mapped IPv6**：Docker 下 `getRemoteAddr()` 可能返回 `::ffff:172.18.0.3`，必须提取内嵌 IPv4 再匹配，否则可信代理误判 → Bug 2 复现。
- **保守降级**：`unknown`/空/解析失败的 token 一律视为非可信，绝不误信。

可信代理通过 `app.security.trusted-proxies` 配置（逗号分隔的 IP/CIDR），留空 = 直连模式。开发环境（IDE 直跑）留空最安全；Docker 下默认 `172.16.0.0/12,127.0.0.1,::1`（见 `docker-compose.yml` 与 `.env.example`）。

### 4.5 使用示例

```java
// 登录接口：严格限流（每分钟 5 次）
@PostMapping("/login")
@RateLimit(key = "login", permits = 5, seconds = 60)
public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) { ... }

// 文档接口：类级别限流（每分钟 60 次）
@RestController
@RequestMapping("/api/v1/documents")
@RateLimit(key = "document", permits = 60, seconds = 60)
public class DocumentController { ... }
```

### 4.6 Redis Key 设计

| Key 模式 | 用途 | TTL |
|----------|------|-----|
| `rate_limit:{key}:{ip}:{uri}` | 接口访问计数 | 由 `@RateLimit.seconds` 决定 |

- `{ip}`：经 `ClientIpResolver` 解析的**可信客户端 IP**（直连模式 = TCP 对端地址；代理模式 = XFF 最右非可信 IP）。识别不到时为 `anonymous`。
- 示例：`rate_limit:login:192.168.1.100:/api/v1/auth/login` → `"3"`，TTL 60s

## 5. Token 黑名单（登出即失效）

### 5.1 为什么需要

JWT 是无状态的，一旦签发在过期前始终有效。这带来几个问题：
- 用户点击「登出」后，旧 Token 仍可在过期前被使用
- Token 泄露（如 XSS 窃取 localStorage），无法远程吊销
- 用户修改密码后，旧 Token 仍可使用

黑名单机制让"主动失效"成为可能。

### 5.2 技术方案：Redis + jti + TTL 自动清理

设计要点：
- 使用 JWT 的 **jti（JWT ID，UUID）** 作为黑名单 key，而非完整 token 字符串
  - 避免在 Redis 中存储大体积 token
  - jti 是全局唯一标识，天然适合做 key
- **TTL = Token 剩余有效期**：过期后自动从 Redis 清除，不占空间
- 每次 JWT 验证时额外检查 jti 是否在黑名单中

**为什么不用完整 Token 做 key：**

| 维度 | jti 方案 | 完整 Token 方案 |
|------|---------|----------------|
| Redis 存储 | 36 字节 UUID | 200+ 字节 JWT |
| 查询效率 | `hasKey` O(1) | `hasKey` O(1) |
| 安全性 | Token 内容不存入 Redis | Token 明文存入 Redis |

### 5.3 核心实现

```java
// TokenBlacklistService.java
@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";
    private final StringRedisTemplate redisTemplate;

    /** 登出时调用：将 Token 的 jti 加入黑名单 */
    public void blacklist(String jti, long remainingMillis) {
        if (remainingMillis <= 0) {
            return;  // Token 已过期，无需加入黑名单
        }
        String key = BLACKLIST_KEY_PREFIX + jti;
        redisTemplate.opsForValue().set(key, "1", remainingMillis, TimeUnit.MILLISECONDS);
    }

    /** JWT 过滤器中调用：检查 Token 是否已被吊销 */
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti));
    }
}
```

### 5.4 登出流程

```java
// AuthController.java
@PostMapping("/logout")
public ApiResponse<Void> logout(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
        String token = bearerToken.substring(7);
        try {
            String jti = jwtConfig.getTokenId(token);
            long remaining = jwtConfig.getTokenRemainingMillis(token);
            tokenBlacklistService.blacklist(jti, remaining);
        } catch (Exception e) {
            // Token 已无效（过期/格式错误），无需加入黑名单，直接视为登出成功
        }
    }
    return ApiResponse.success("登出成功", null);
}
```

### 5.5 JWT 过滤器中的校验

```java
// JwtAuthenticationFilter.java
Claims claims = jwtConfig.parseToken(token);
String jti = claims.get("jti", String.class);

if (tokenBlacklistService.isBlacklisted(jti)) {
    response.setStatus(401);
    return;  // Token 已被吊销
}
```

### 5.6 Redis Key 设计

| Key 模式 | 用途 | TTL |
|----------|------|-----|
| `token:blacklist:{jti}` | 已吊销的 Token 标记 | Token 剩余有效期（毫秒） |

示例：`token:blacklist:a1b2c3d4-e5f6-7890-abcd-ef1234567890` → `"1"`，TTL 86340000ms

## 6. 安全响应头

### 6.1 为什么需要

浏览器默认行为存在多种安全隐患：

| 威胁 | 原理 |
|------|------|
| **点击劫持** | 恶意网站通过透明 iframe 嵌套你的页面，诱导用户在不知情时点击 |
| **MIME 嗅探** | 浏览器猜测响应类型，可能将上传的恶意文件当脚本执行 |
| **XSS 注入** | 注入的脚本可读取页面 Cookie / Token |
| **协议降级** | HTTP → HTTPS 无强制跳转，中间人可截获流量 |
| **缓存泄露** | 浏览器缓存敏感 API 响应，在公共电脑上被后续用户看到 |

安全响应头通过 HTTP Header 指令约束浏览器行为，成本极低但效果显著。

### 6.2 技术方案：SecurityHeadersFilter

通过 `OncePerRequestFilter` 在所有响应中统一添加安全头，
使用 `@Order(Ordered.HIGHEST_PRECEDENCE)` 确保在所有过滤器之前执行。

```java
// SecurityHeadersFilter.java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 禁止浏览器猜测 MIME 类型，防止上传漏洞
        response.setHeader("X-Content-Type-Options", "nosniff");

        // 禁止页面被嵌入 iframe，防止点击劫持
        response.setHeader("X-Frame-Options", "DENY");

        // 启用浏览器内置 XSS 过滤器（旧浏览器）
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // 内容安全策略：限制资源加载来源
        response.setHeader("Content-Security-Policy",
            "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'");

        // HTTPS 强制跳转（生产环境生效，HTTP 下浏览器忽略此头）
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // 控制引用来源泄露
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 禁用浏览器缓存敏感 API 响应
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");

        filterChain.doFilter(request, response);
    }
}
```

### 6.3 响应头详解

| Header | 值 | 防御目标 | 浏览器兼容性 |
|--------|---|---------|------------|
| `X-Content-Type-Options` | `nosniff` | 阻止 MIME 嗅探攻击 | 全部 |
| `X-Frame-Options` | `DENY` | 阻止 iframe 嵌套（点击劫持） | 全部 |
| `X-XSS-Protection` | `1; mode=block` | 反射型 XSS（Chrome 已弃用但仍保留） | 旧浏览器 |
| `Content-Security-Policy` | `default-src 'self'; ...` | XSS / 数据注入攻击 | 现代浏览器 |
| `Strict-Transport-Security` | `max-age=31536000` | SSL 剥离 / 协议降级 | 现代浏览器 |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | 防止引用来源泄露 URL 参数 | 现代浏览器 |
| `Cache-Control` | `no-store` | 防止敏感响应被缓存 | 全部 |

> ⚠️ **生产环境注意**：CSP 中的 `unsafe-inline` 仅用于开发环境，生产应使用 nonce 或 hash 策略。

## 7. CORS 跨域安全

### 7.1 同源策略（Same-Origin Policy）

#### 什么是同源策略

同源策略是浏览器最核心的安全机制，限制**一个源的文档或脚本**与**另一个源的资源**进行交互。
它的作用是防止恶意网站读取或操作用户在另一个网站上的数据。

#### 同源判定规则

两个 URL 当且仅当以下三项完全相同时，才被视为"同源"：

| 组成部分 | 说明 | 示例 |
|---------|------|------|
| 协议（Protocol） | `http` 或 `https` | `http` ≠ `https` |
| 域名（Host） | 完整域名 | `example.com` ≠ `api.example.com` |
| 端口（Port） | TCP 端口号（http 默认 80，https 默认 443） | `:8080` ≠ `:3000` |

判定示例（以 `http://localhost:5173` 为基准）：

| URL | 是否同源 | 原因 |
|-----|---------|------|
| `http://localhost:5173/api/data` | ✅ 同源 | 协议、域名、端口完全相同 |
| `http://localhost:8080/api/data` | ❌ 跨域 | 端口不同（5173 ≠ 8080） |
| `https://localhost:5173` | ❌ 跨域 | 协议不同（http ≠ https） |
| `http://api.localhost:5173` | ❌ 跨域 | 域名不同（localhost ≠ api.localhost） |
| `http://192.168.1.100:5173` | ❌ 跨域 | 域名不同（localhost ≠ 192.168.1.100） |

#### 同源策略的限制范围

同源策略主要限制以下跨源行为：

| 行为 | 是否受限 | 说明 |
|------|---------|------|
| AJAX / Fetch 请求读取响应 | ❌ 受限 | 无法读取跨源接口的响应数据（请求本身会发出，但浏览器拦截响应） |
| DOM 操作（iframe） | ❌ 受限 | 无法读取或操作跨源 iframe 的 DOM |
| Cookie / localStorage / IndexedDB | ❌ 受限 | 无法读取跨源的存储数据 |
| `<script src="...">` 加载执行 | ✅ 允许 | 可加载跨源 JS，但无法读取脚本内容 |
| `<img>` / `<video>` / `<audio>` | ✅ 允许 | 可嵌入跨源媒体资源 |
| `<link rel="stylesheet">` | ✅ 允许 | 可加载跨源样式表 |
| 表单 `POST` 提交 | ✅ 允许 | 可向跨源地址提交表单，但无法读取响应 |

> ⚠️ **关键理解**：跨域请求**会正常发出**，浏览器拦截的是**响应数据的读取**。
> 这意味着服务端已经处理了请求，只是浏览器阻止了前端 JS 拿到结果。

#### 本项目的跨域场景

前后端分离架构中，天然存在跨域问题：

```
前端（Vite Dev Server）        后端（Spring Boot）
http://localhost:5173    →    http://localhost:8080
       │                            │
       └──── 协议相同 ✅ ────────────┘
       └──── 域名相同 ✅ ────────────┘
       └──── 端口不同 ❌ ────────────┘  → 跨域！
```

- 前端 `http://localhost:5173` 向后端 `http://localhost:8080` 发起 API 请求
- 浏览器发现端口不同 → 跨源 → 拦截响应
- 必须通过 CORS 机制显式声明允许的来源

#### CORS 的作用

CORS（Cross-Origin Resource Sharing）是 W3C 标准，为同源策略提供了一种**受控放宽**的机制：

- **服务端声明式授权**：通过响应头 `Access-Control-Allow-Origin` 告知浏览器哪些外部源被允许
- **浏览器强制执行**：浏览器检查响应头，未授权的来源直接拦截，前端无法绕过
- **保护而非替代**：CORS 是在同源策略之上的**豁免通道**，不是关闭同源策略

> ⚠️ `allowedOrigins("*")` + `allowCredentials(true)` 允许任意来源携带凭证，等于完全绕过同源策略的保护，生产环境**严禁使用**。

### 7.2 技术方案：配置文件驱动 + 环境变量覆盖

CORS 域名通过 `application.yml` 配置，生产环境通过环境变量注入：

```yaml
# application.yml
app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000}
```

```java
// SecurityConfig.java
@Configuration
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);  // 允许携带 Authorization 头
        config.setMaxAge(3600L);           // 预检请求缓存 1 小时

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

### 7.3 部署配置

| 环境 | 配置方式 | 示例值 |
|------|---------|--------|
| 开发 | 默认值 | `http://localhost:5173,http://localhost:3000` |
| 生产 | 环境变量 `CORS_ALLOWED_ORIGINS` | `https://kb.example.com` |
| Docker | `docker-compose.yml` → `environment` | `CORS_ALLOWED_ORIGINS=https://kb.example.com` |

## 8. 异常处理体系

### 8.1 为什么需要

Spring 默认的错误响应存在几个问题：
- **格式不统一**：Whitelabel Page（HTML）或 `{"status":401,"error":"Unauthorized"}`，前端无法解析
- **信息泄露**：异常堆栈暴露包名、类名、SQL 语句等内部信息
- **状态码不准**：业务错误（如账号锁定）全部返回 500，前端无法区分处理

前端需要统一格式的 JSON 响应 + 正确的 HTTP 状态码来正确路由错误。

### 8.2 技术方案：BusinessException + GlobalExceptionHandler

**BusinessException** — 可预期的业务错误，携带 HTTP 状态码：

```java
// BusinessException.java
public class BusinessException extends RuntimeException {
    private final int status;

    public BusinessException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() { return status; }
}
```

**GlobalExceptionHandler** — 统一拦截 6 种异常类型：

```java
// GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @RequestBody + @Valid 校验失败 → 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ApiResponse.error(400, message);
    }

    // 业务异常 → 动态状态码（401/403/423）
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        HttpStatus httpStatus = HttpStatus.resolve(e.getStatus());
        if (httpStatus == null) httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(httpStatus)
                .body(ApiResponse.error(e.getStatus(), e.getMessage()));
    }

    // 权限不足 → 403
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException e) {
        return ApiResponse.error(403, "权限不足");
    }

    // 兜底 → 500，记录日志但不暴露内部细节
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleRuntime(RuntimeException e) {
        logger.error("未处理的运行时异常: {}", e.getMessage(), e);
        return ApiResponse.error(500, "服务器内部错误");
    }
}
```

### 8.3 状态码映射

| 异常类型 | HTTP 状态码 | 场景 | 用户提示 |
|----------|------------|------|---------|
| `MethodArgumentNotValidException` | 400 | 参数校验失败 | `"username: 不能为空"` |
| `ConstraintViolationException` | 400 | 路径参数校验失败 | 具体校验消息 |
| `BusinessException(401)` | 401 | 用户名或密码错误 | `"用户名或密码错误"` |
| `BusinessException(403)` | 403 | 账号已被禁用 | `"账号已被禁用"` |
| `AccessDeniedException` | 403 | 权限不足 | `"权限不足"` |
| `BusinessException(423)` | 423 | 账号已锁定 | `"账号已锁定，请X分钟后再试"` |
| `RateLimitInterceptor` | 429 | 请求过于频繁 | `"请求过于频繁，请稍后再试"` |
| `RuntimeException`（兜底） | 500 | 未预期的内部错误 | `"服务器内部错误"` |

### 8.4 设计原则

- **不泄露内部信息**：兜底异常只返回"服务器内部错误"，堆栈仅记录到服务端日志
- **统一错误消息**：用户名不存在和密码错误返回相同提示，防止用户名枚举
- **前端可路由**：每种错误有唯一状态码，前端按 `response.status` 跳转对应页面

## 9. 请求安全链路

从请求到响应，安全组件按以下顺序协同工作：

```
浏览器 → HTTP 请求
          │
          ▼
    ── [Servlet Filter 层 · DispatcherServlet 之前] ──
    ① SecurityHeadersFilter (@Order(HIGHEST_PRECEDENCE))
       │ 添加 7 项安全响应头（对所有请求生效）
       │
       ▼
    ② JwtAuthenticationFilter（Spring Security 过滤器链成员）
       │ 提取 Bearer Token
       │ HMAC-SHA256 签名验证
       │ 过期时间检查
       │ jti 黑名单校验（TokenBlacklistService → Redis）
       │ 存入 SecurityContext
       │
       ▼
    ③ Spring Security 授权规则（AuthorizationFilter）
       │ /api/v1/auth/**  → permitAll（登录/登出）
       │ GET  /documents/** → permitAll（公开浏览）
       │ POST/PUT/DELETE /documents/** → authenticated
       │ /api/v1/search/** → permitAll
       │ 其他 → authenticated
       │
       ▼
    ── DispatcherServlet ──
    ④ RateLimitInterceptor（Spring MVC HandlerInterceptor）
       │ 检查 @RateLimit 注解
       │ Redis INCR + EXPIRE 计数
       │ 超限 → 429 {"code":429,"message":"请求过于频繁"}
       │
       ▼
    ⑤ Controller → Service
       │ 业务逻辑
       │ 错误 → throw BusinessException(status, message)
       │
       ▼
    ⑥ GlobalExceptionHandler
       │ BusinessException → 动态状态码
       │ AccessDeniedException → 403
       │ 校验异常 → 400
       │ RuntimeException → 500（不暴露内部信息）
       │
       ▼
    HTTP 响应（标准 ApiResponse JSON + 正确状态码 + 安全头）
```

> **JwtAuthenticationFilter 在过滤器链中的位置**：它是 Spring Security 过滤器链的成员，经 `addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)` 注册（见 `SecurityConfig.java:98`）。SecurityConfig 注释中的「标准认证过滤器」即 `UsernamePasswordAuthenticationFilter`（Spring Security 默认的表单登录过滤器）。本项目采用无状态 JWT，身份来自 `Authorization` 头而非表单提交，故 JWT 过滤器须先于标准认证过滤器解析 Token、填充 `SecurityContext`，后续 `AuthorizationFilter` 才能据此授权。

> **为什么限流发生在认证之后**：Filter 在 `DispatcherServlet` 之前执行（`SecurityHeadersFilter` / `JwtAuthenticationFilter` / 授权均在此层），`HandlerInterceptor`（`RateLimitInterceptor`）在 `DispatcherServlet` 之后、Controller 前后执行——因此限流实际发生在认证之后。若需要让限流先于认证拦截未认证流量，应将其实现为 Filter 并注册到 Security 链最前。

## 10. 已有安全能力

以下安全能力在本次安全加固前已经实现：

| 能力 | 实现方式 | 说明 |
|------|---------|------|
| JWT 认证 | HMAC-SHA256 签名 + 过期时间 | 无状态认证，无需 Session |
| BCrypt 密码哈希 | `BCryptPasswordEncoder` | 自动加盐，10 轮加密 |
| RSA 密码加密传输 | 前端公钥加密 → 后端私钥解密 | 密码不在网络明文传输 |
| 参数校验 | `@Valid` + `@NotBlank` + `@Size` | 请求体字段约束 |
| 统一错误消息 | 登录失败统一提示"用户名或密码错误" | 防止用户名枚举 |
| 隐私数据加密存储 | email/phone `@Convert` AES 加密 | 数据库中不可读 |
| 隐私数据脱敏响应 | `@Sensitive` 注解自动脱敏 | `a***@example.com` |
| CSRF 禁用 | JWT 无状态认证，正确禁用 | 不使用 Cookie 认证 |

## 11. 生产环境加固清单

> 以下项目在开发阶段为便利性做了妥协，生产部署前必须处理：

| 项目 | 当前状态 | 生产要求 | 优先级 |
|------|---------|---------|--------|
| CSP `unsafe-inline` | 允许内联脚本/样式 | 使用 nonce 或 hash 策略 | 🔴 高 |
| HSTS | 已配置但 HTTP 下不生效 | 确保 Nginx/CDN 强制 HTTPS 重定向 | 🟠 高 |
| CORS 域名 | 默认 `localhost` | 环境变量设置生产域名 | 🔴 高 |
| Jasypt 主密钥 | 开发环境硬编码 | 仅存 `.env` / 密钥管理服务 | 🔴 高 |
| RSA 密钥对 | 应用启动时生成 | 持久化密钥对，重启不换 | 🟡 中 |
| 请求体大小限制 | Spring 默认 1MB | 按业务需求显式配置 | 🟡 中 |
| JWT Secret | 配置文件 | 至少 256 位随机字符串，环境变量注入 | 🔴 高 |
| 登录限流 IP 维度 | 已按可信客户端 IP 解析（`ClientIpResolver`） | 可叠加 IP + 用户名双维度防分布式攻击 | 🟡 中 |

---

**文档版本：** v1.0
**创建日期：** 2026-06-10
**最后更新：** 2026-06-10
**状态：** 已完成
**关联文档：** [F01-auth 技术设计](../features/F01-auth/02-technical.md)、[CLAUDE.md 安全底线](../../CLAUDE.md)
