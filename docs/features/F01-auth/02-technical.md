# F01 - 用户认证与权限管理 - 技术设计文档

## 🏗️ 架构设计

### 系统架构图

```
┌─────────────────────────────────────────────────┐
│              前端认证层 (Vue 3)                  │
│  ┌──────────────┬──────────────┬──────────────┐ │
│  │  LoginView   │  AuthStore   │  Router      │ │
│  │              │  (Pinia)      │  Guards      │ │
│  └──────────────┴──────────────┴──────────────┘ │
│           ↓ Token Storage (localStorage)         │
└─────────────────────────────────────────────────┘
                    ↓ JWT Token
┌─────────────────────────────────────────────────┐
│           后端认证层 (Spring Boot)               │
│  ┌──────────────┬──────────────┬──────────────┐ │
│  │SecurityFilter│ JWT Provider │  AuthService │ │
│  │              │              │              │ │
│  └──────────────┴──────────────┴──────────────┘ │
│           ↓ UserDetailsService                 │
└─────────────────────────────────────────────────┘
                    ↓ JPA Repository
┌─────────────────────────────────────────────────┐
│              数据存储层 (MySQL)                   │
│  ┌──────────────┬──────────────┬──────────────┐ │
│  │  users       │   roles      │ permissions  │ │
│  │  表          │   表         │   关联表      │ │
│  └──────────────┴──────────────┴──────────────┘ │
└─────────────────────────────────────────────────┘
```

### 组件职责

#### 前端组件
- **LoginView**: 登录页面组件，负责用户交互和表单验证
- **AuthStore**: Pinia状态管理，管理用户状态和Token
- **Router Guards**: 路由守卫，验证用户权限
- **Request Interceptor**: HTTP拦截器，自动附加Token

#### 后端组件
- **SecurityFilter**: JWT过滤器，验证请求Token
- **JWT Provider**: Token生成和验证工具
- **Auth Service**: 认证业务逻辑
- **User Details Service**: Spring Security用户详情服务
- **Auth Controller**: 认证相关API接口

## 🗄️ 数据库设计

### ER图

```
┌─────────────────┐         ┌─────────────────┐
│      roles      │         │     users       │
│  ┌───────────┐  │         │  ┌───────────┐  │
│  │ id        │  │         │  │ id        │  │
│  │ name      │  │         │  │ username  │  │
│  │ perms     │  │         │  │ password  │  │
│  └───────────┘  │         │  │ email     │  │
└─────────────────┘         │  │ role_id   │◄─┼────────┐
                            │  └───────────┘  │         │
                            └─────────────────┘         │
                                                          │
                                               ┌──────────┴──────────┐
                                               │  user_sessions   │
                                               │  ┌───────────┐    │
                                               │  │ id        │    │
                                               │  │ user_id   │◄───┼───────┐
                                               │  │ token     │    │       │
                                               │  │ expire_at │    │       │
                                               │  └───────────┘    │       │
                                               └───────────────────┘       │
                                                                          │
                                                          ┌──────────────┴──────────┐
                                                          │   login_history        │
                                                          │  ┌───────────┐       │
                                                          │  │ id        │       │
                                                          │  │ user_id   │◄──────┘
                                                          │  │ login_at  │
                                                          │  │ ip_addr   │
                                                          │  └───────────┘
                                                          └───────────────────────┘
```

### 表结构详解

#### users 用户表

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    avatar VARCHAR(255) COMMENT '头像URL',
    role_id INT NOT NULL COMMENT '角色ID',
    status ENUM('ACTIVE', 'INACTIVE', 'LOCKED') DEFAULT 'ACTIVE' COMMENT '状态',
    login_attempts INT DEFAULT 0 COMMENT '登录失败次数',
    locked_until TIMESTAMP NULL COMMENT '锁定到期时间',
    last_login_at TIMESTAMP NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at TIMESTAMP NULL COMMENT '删除时间（软删除）',

    FOREIGN KEY (role_id) REFERENCES roles(id),
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

#### roles 角色表

```sql
CREATE TABLE roles (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
    description VARCHAR(255) COMMENT '角色描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';
```

#### permissions 权限表

```sql
CREATE TABLE permissions (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '权限名称',
    description VARCHAR(255) COMMENT '权限描述',
    resource VARCHAR(100) COMMENT '资源路径',
    action VARCHAR(50) COMMENT '操作类型（READ,WRITE,DELETE）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';
```

#### role_permissions 角色权限关联表

```sql
CREATE TABLE role_permissions (
    role_id INT NOT NULL COMMENT '角色ID',
    permission_id INT NOT NULL COMMENT '权限ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';
```

#### user_sessions 会话表

```sql
CREATE TABLE user_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    token VARCHAR(500) NOT NULL COMMENT 'Token哈希',
    device_info VARCHAR(255) COMMENT '设备信息',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_token (token),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会话表';
```

#### login_history 登录历史表

```sql
CREATE TABLE login_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    status ENUM('SUCCESS', 'FAILURE') COMMENT '登录状态',
    failure_reason VARCHAR(255) COMMENT '失败原因',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录历史表';
```

## 🔄 数据流设计

### 1. 登录流程

```
用户登录流程：

┌─────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  前端   │────│ 用户输入  │────│ 前端验证  │────│ API请求  │────│ 后端验证  │
│  表单   │     │          │     │          │     │          │     │          │
└─────────┘     └──────────┘     └──────────┘     └──────────┘     └──────────┘
                                                           │
                                                           ↓
┌────────────────────────────────────────────────────────────────┐
│                      后端处理流程                              │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐  │
│  │查询用户   │───→│验证密码   │───→│检查状态   │───→│记录登录   │  │
│  │          │    │          │    │          │    │历史       │  │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘  │
│                                                           │
│                                                           ↓
│  ┌──────────┐    ┌──────────┐    ┌──────────┐              │
│  │生成Token │───→│存储会话   │───→│返回响应   │              │
│  └──────────┘    └──────────┘    └──────────┘              │
└────────────────────────────────────────────────────────────────┘
                                                           │
                                                           ↓
┌────────────────────────────────────────────────────────────────┐
│                      前端响应处理                              │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐              │
│  │存储Token  │───→│更新状态   │───→│跳转首页   │              │
│  └──────────┘    └──────────┘    └──────────┘              │
└────────────────────────────────────────────────────────────────┘
```

### 2. Token验证流程

```
Token验证流程：

┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐
│  API请求   │────│ JWT Filter │────│ Token验证  │────│ 授权检查   │
│  (带Token) │     │            │     │            │     │            │
└────────────┘     └────────────┘     └────────────┘     └────────────┘
                           │                  │                  │
                           ↓                  ↓                  ↓
                      ┌────────────────────────────────────┐
                      │         Token验证流程              │
                      │  1. 提取Token                       │
                      │  2. 验证签名                       │
                      │  3. 检查过期                       │
                      │  4. 检查会话是否存在              │
                      │  5. 解析用户信息                   │
                      └────────────────────────────────────┘
                                              │
                                              ↓
                                        ┌─────────────────┐
                                        │ 验证成功         │
                                        │ 设置SecurityContext│
                                        │ 继续处理请求     │
                                        └─────────────────┘
                                              │
                                              ↓
                                        ┌─────────────────┐
                                        │ 验证失败         │
                                        │ 返回401 Unauthorized│
                                        └─────────────────┘
```

### 3. 权限检查流程

```
权限检查流程：

┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐
│  API请求   │────│ JWT Filter │────│ Token验证  │────│ 授权拦截器 │
│            │     │            │     │ (通过)     │     │            │
└────────────┘     └────────────┘     └────────────┘     └────────────┘
                                                           │
                                                           ↓
                                        ┌──────────────────────────┐
                                        │      权限验证流程          │
                                        │  1. 获取用户角色           │
                                        │  2. 获取请求资源           │
                                        │  3. 检查角色权限           │
                                        │  4. 特殊权限检查           │
                                        └──────────────────────────┘
                                                           │
                                              ┌──────────────┴──────────────┐
                                              ↓                           ↓
                                        ┌─────────────┐           ┌─────────────┐
                                        │  有权限     │           │  无权限     │
                                        │  继续处理   │           │  返回403    │
                                        └─────────────┘           └─────────────┘
```

## 🔌 接口设计

### 1. RESTful API规范

#### 认证接口

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| POST | /v1/auth/login | 用户登录 | 否 |
| POST | /v1/auth/register | 用户注册 | 否 |
| POST | /v1/auth/logout | 用户登出 | 是 |
| POST | /v1/auth/refresh | 刷新Token | 是 |
| GET | /v1/auth/me | 获取当前用户 | 是 |

#### 用户接口

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| GET | /v1/users/me | 获取个人信息 | 是 | USER |
| PUT | /v1/users/me | 更新个人信息 | 是 | USER |
| PUT | /v1/users/me/password | 修改密码 | 是 | USER |
| PUT | /v1/users/me/avatar | 更新头像 | 是 | USER |
| GET | /v1/users/:id | 获取用户信息（管理员） | 是 | ADMIN |
| PUT | /v1/users/:id/role | 修改用户角色 | 是 | ADMIN |
| PUT | /v1/users/:id/status | 修改用户状态 | 是 | ADMIN |

### 2. 请求/响应格式

#### 登录请求

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

#### 登录响应

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwidXNlcm5hbWUiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTcxNzEzNTY3NSwiZXhwIjoxNzE3MjIwNzV9.F5m7VGKUM-Px76Q2JXOYL3N5rvgZA4Vp5C8XqMkPKxI",
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com",
      "role": "ADMIN",
      "avatar": "https://cdn.example.com/avatars/admin.jpg",
      "createdAt": "2024-01-01T00:00:00Z"
    }
  },
  "timestamp": 1717135675678
}
```

#### 错误响应格式

```json
{
  "code": 401,
  "message": "用户名或密码错误",
  "data": null,
  "timestamp": 1717135675678
}
```

### 3. 状态码定义

| 状态码 | 说明 | 使用场景 |
|--------|------|----------|
| 200 | 成功 | 登录成功、获取信息成功 |
| 201 | 创建成功 | 注册成功 |
| 400 | 请求错误 | 参数验证失败 |
| 401 | 未授权 | Token无效或过期、密码错误 |
| 403 | 禁止访问 | 权限不足 |
| 409 | 冲突 | 用户名或邮箱已存在 |
| 423 | 锁定 | 账号被锁定 |

## 🔐 安全设计

### 1. 密码安全

#### BCrypt加密
```java
// 加密强度：10轮
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
String hashedPassword = encoder.encode(rawPassword);

// 验证密码
boolean matches = encoder.matches(rawPassword, hashedPassword);
```

#### 密码策略
```java
public class PasswordPolicy {
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$");
    
    public static boolean isValid(String password) {
        if (password == null || password.length() < 8 || password.length() > 32) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}
```

### 2. JWT Token安全

#### Token结构
```java
// JWT Payload
public class JwtPayload {
    private Long sub;              // 用户ID
    private String username;       // 用户名
    private String role;           // 角色
    private Long iat;              // 签发时间
    private Long exp;              // 过期时间
}
```

#### Token生成
```java
// Token配置
private long tokenExpiration = 86400; // 24小时
private String secretKey = "your-secret-key-here";

// 生成Token
String token = Jwts.builder()
    .setSubject(user.getId().toString())
    .claim("username", user.getUsername())
    .claim("role", user.getRole().name())
    .setIssuedAt(new Date())
    .setExpiration(new Date(System.currentTimeMillis() + tokenExpiration * 1000))
    .signWith(SignatureAlgorithm.HS256, secretKey)
    .compact();
```

#### Token验证
```java
// 验证Token
Claims claims = Jwts.parser()
    .setSigningKey(secretKey)
    .parseClaimsJws(token)
    .getBody();

// 检查过期
Date expiration = claims.getExpiration();
if (expiration.before(new Date())) {
    throw new ExpiredJwtException("Token已过期");
}
```

### 3. 登录防暴力破解

#### 为什么需要

攻击者可使用自动化工具对登录接口发起字典攻击或撞库攻击，尝试大量用户名/密码组合。
若无限制，弱密码账号将被轻易攻破。

#### 技术方案：Redis 分布式计数 + 自动锁定

采用 Redis 而非数据库字段（`login_attempts` + `locked_until`）存储失败计数，
原因：
- **无侵入**：不修改 users 表结构，不增加 JPA 实体字段
- **自动过期**：Redis TTL 天然实现"时间窗口滑动"和"锁定自动解除"
- **分布式**：多实例部署时共享计数，避免单机绕过

#### 保护流程

```
登录请求 → isLocked(username)?
              ├─ 是 → 返回 423 + 剩余锁定时间
              └─ 否 → 验证凭证
                        ├─ 失败 → recordFailure() → 计数 ≥5? → 触发锁定 15 分钟
                        └─ 成功 → recordSuccess() → 清除失败计数
```

#### 实现代码

```java
@Service
public class LoginProtectionService {

    private static final String FAIL_KEY_PREFIX = "login_fail:";
    private static final String LOCK_KEY_PREFIX = "login_lock:";
    private static final int MAX_FAIL_COUNT = 5;         // 连续失败 5 次触发锁定
    private static final int FAIL_WINDOW_SECONDS = 300;  // 失败计数窗口 5 分钟
    private static final int LOCK_DURATION_SECONDS = 900; // 锁定 15 分钟

    private final StringRedisTemplate redisTemplate;

    public int recordFailure(String username) {
        String failKey = FAIL_KEY_PREFIX + username;
        Long count = redisTemplate.opsForValue().increment(failKey);

        if (count != null && count == 1) {
            redisTemplate.expire(failKey, FAIL_WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        if (count != null && count >= MAX_FAIL_COUNT) {
            // 触发锁定
            String lockKey = LOCK_KEY_PREFIX + username;
            redisTemplate.opsForValue().set(lockKey, "1", LOCK_DURATION_SECONDS, TimeUnit.SECONDS);
            redisTemplate.delete(failKey); // 清除失败计数，锁定解除后重新计数
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

#### Redis Key 设计

| Key 模式 | 用途 | TTL |
|----------|------|-----|
| `login_fail:{username}` | 失败计数器 | 300s（5 分钟窗口） |
| `login_lock:{username}` | 锁定标记 | 900s（15 分钟锁定） |

#### 在 AuthService 中的集成

```java
// 0. 检查锁定
if (loginProtectionService.isLocked(username)) {
    long remaining = loginProtectionService.getRemainingLockTime(username);
    throw new BusinessException(423, "账号已锁定，请" + (remaining / 60 + 1) + "分钟后再试");
}
// ... 验证凭证 ...
// 失败时：
loginProtectionService.recordFailure(username);
throw new BusinessException(401, "用户名或密码错误");
// 成功时：
loginProtectionService.recordSuccess(username);
```

### 4. 权限控制

#### 角色权限检查
```java
@Service
public class PermissionService {
    
    public boolean hasPermission(Long userId, String resource, String action) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UnauthorizedException("用户不存在"));
        
        // 管理员拥有所有权限
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        
        // 检查角色权限
        return rolePermissionRepository.existsByRoleIdAndPermissionResourceAndPermissionAction(
            user.getRole().getId(), resource, action);
    }
}
```

#### 自定义权限注解
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String resource();
    String action();
    default String operator() = "AND";
}

// 使用示例
@RequirePermission(resource = "document", action = "write")
public DocumentDTO updateDocument(Long id, DocumentUpdateRequest request) {
    // ...
}
```

### 5. Token 黑名单（登出即失效）

#### 为什么需要

JWT 是无状态的，一旦签发在过期前始终有效。如果用户主动登出、修改密码或 Token 泄露，
无法让已签发的 Token 失效。黑名单机制解决了"登出后 Token 仍可使用"的问题。

#### 技术方案：Redis + jti + TTL 自动清理

- 使用 JWT 的 `jti`（JWT ID，UUID）作为黑名单 key，而非完整 token 字符串
  - 避免在 Redis 中存储大体积 token
  - `jti` 是全局唯一标识，天然适合做 key
- TTL 设为 Token 剩余有效期，过期后自动从 Redis 清除
- 每次 JWT 验证时额外检查 jti 是否在黑名单中

#### 实现代码

```java
@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 登出时调用：将 Token 的 jti 加入黑名单 */
    public void blacklist(String jti, long remainingMillis) {
        if (remainingMillis <= 0) {
            return; // Token 已过期，无需加入黑名单
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

#### 登出流程（AuthController）

```java
@PostMapping("/logout")
public ApiResponse<Void> logout(HttpServletRequest request) {
    String token = extractBearerToken(request);
    if (token != null) {
        try {
            String jti = jwtConfig.getTokenId(token);
            long remaining = jwtConfig.getTokenRemainingMillis(token);
            tokenBlacklistService.blacklist(jti, remaining);
        } catch (Exception e) {
            // Token 已无效（过期/格式错误），无需加入黑名单
        }
    }
    return ApiResponse.success("登出成功", null);
}
```

#### JWT 过滤器中的黑名单校验

```java
// JwtAuthenticationFilter.doFilterInternal() 中：
Claims claims = jwtConfig.parseToken(token);
String jti = claims.get("jti", String.class);
if (tokenBlacklistService.isBlacklisted(jti)) {
    response.setStatus(401);
    return; // Token 已被吊销
}
```

#### Redis Key 设计

| Key 模式 | 用途 | TTL |
|----------|------|-----|
| `token:blacklist:{jti}` | 已吊销的 Token 标记 | Token 剩余有效期（毫秒） |

### 6. 接口限流

#### 为什么需要

无限制的接口调用可被滥用：
- **暴力破解**：攻击者高速重试密码
- **资源耗尽**：恶意请求消耗服务器资源（DoS）
- **数据爬取**：批量抓取公开内容

限流是成本最低、效果最直接的防护手段，在入口处拦截超限请求。

#### 技术方案：@RateLimit 注解 + Redis 滑动窗口

采用**注解声明式限流**，Controller 只需标注 `@RateLimit` 即可生效，
无需在业务代码中耦合限流逻辑。

核心机制：
- **Redis INCR + EXPIRE**：原子递增计数 + 首次请求设置过期时间，实现固定窗口限流
- **IP + URI 维度**：按客户端 IP 和请求路径组合计数，不同接口互不影响
- **方法级优先**：方法上的 `@RateLimit` 优先于类上的，同一接口可覆盖全局配置

#### 注解定义

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String key() default "global";     // 限流标识，用于 Redis key 前缀
    int permits() default 60;          // 时间窗口内最大请求数
    int seconds() default 60;          // 时间窗口（秒）
}
```

#### 拦截器实现

```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String KEY_PREFIX = "rate_limit:";
    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 1. 查找注解（方法级优先，其次类级）
        RateLimit rateLimit = findRateLimit(handler);
        if (rateLimit == null) return true;

        // 2. 构建 Redis Key: rate_limit:{key}:{ip}:{uri}
        String redisKey = KEY_PREFIX + rateLimit.key() + ":" + getClientIp(request)
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
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
            return false;
        }
        return true;
    }
}
```

#### 使用示例

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @PostMapping("/login")
    @RateLimit(key = "login", permits = 5, seconds = 60)  // 每分钟 5 次
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) { ... }
}

@RestController
@RequestMapping("/api/v1/documents")
@RateLimit(key = "document", permits = 60, seconds = 60)  // 每分钟 60 次（类级别）
public class DocumentController { ... }
```

#### Redis Key 设计

| Key 模式 | 用途 | TTL |
|----------|------|-----|
| `rate_limit:{key}:{ip}:{uri}` | 接口访问计数 | 由 `@RateLimit.seconds` 决定 |

### 7. 安全响应头

#### 为什么需要

浏览器默认行为存在多种安全隐患：
- **点击劫持**：恶意网站通过 iframe 嵌套你的页面，诱导用户点击
- **MIME 嗅探**：浏览器猜测响应类型，可能将上传的恶意文件当脚本执行
- **协议降级**：HTTP → HTTPS 无强制跳转，中间人可截获流量

安全响应头通过 HTTP Header 指令约束浏览器行为，成本极低但效果显著。

#### 技术方案：SecurityHeadersFilter

通过 `OncePerRequestFilter` 在所有响应中添加安全头，
使用 `@Order(Ordered.HIGHEST_PRECEDENCE)` 确保最先执行。

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");       // 禁止 MIME 嗅探
        response.setHeader("X-Frame-Options", "DENY");                  // 禁止 iframe 嵌套
        response.setHeader("X-XSS-Protection", "1; mode=block");        // 启用 XSS 过滤器
        response.setHeader("Content-Security-Policy", "default-src 'self'; ..."); // 内容安全策略
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains"); // 强制 HTTPS
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin"); // 引用来源控制
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate"); // 禁用缓存
        response.setHeader("Pragma", "no-cache");

        filterChain.doFilter(request, response);
    }
}
```

#### 响应头说明

| Header | 值 | 防御目标 |
|--------|---|---------|
| `X-Content-Type-Options` | `nosniff` | MIME 嗅探攻击 |
| `X-Frame-Options` | `DENY` | 点击劫持（Clickjacking） |
| `X-XSS-Protection` | `1; mode=block` | 反射型 XSS |
| `Content-Security-Policy` | `default-src 'self'; ...` | XSS / 数据注入 |
| `Strict-Transport-Security` | `max-age=31536000` | 协议降级 / SSL 剥离 |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | 引用来源信息泄露 |
| `Cache-Control` | `no-store` | 敏感数据被浏览器缓存 |

> **注意**：CSP 中 `unsafe-inline` 仅用于开发环境，生产环境应使用 nonce 或 hash 策略。

### 8. CORS 跨域安全

#### 为什么需要

浏览器同源策略默认阻止跨域请求。前后端分离架构中前端（如 `localhost:5173`）和后端
（如 `localhost:8080`）端口不同，必须配置 CORS。但 `allowedOrigins("*")` 允许任意
来源的请求，等于关闭了同源策略的保护。

#### 技术方案：配置文件驱动 + 环境变量覆盖

CORS 域名通过 `application.yml` 配置，生产环境通过环境变量注入：

```yaml
# application.yml
app:
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000}
```

```java
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
        config.setAllowCredentials(true);   // 允许携带 Cookie / Authorization
        config.setMaxAge(3600L);            // 预检请求缓存 1 小时

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

#### 部署配置

| 环境 | 配置方式 |
|------|---------|
| 开发 | 默认值 `http://localhost:5173,http://localhost:3000` |
| 生产 | 环境变量 `CORS_ALLOWED_ORIGINS=https://kb.example.com` |

### 9. 异常处理体系

#### 为什么需要

Spring 默认的错误响应（Whitelabel Page 或 `{"status":401,"error":"Unauthorized"}`）
格式不统一，且可能泄露内部堆栈信息。前端需要统一格式的 JSON 响应来正确处理错误。
同时，不同类型的业务错误应返回正确的 HTTP 状态码（401 认证失败 vs 423 账号锁定），
而非全部返回 500。

#### 技术方案：BusinessException + GlobalExceptionHandler

**BusinessException** — 可预期的业务错误，携带 HTTP 状态码：

```java
public class BusinessException extends RuntimeException {
    private final int status;

    public BusinessException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() { return status; }
}
```

**GlobalExceptionHandler** — 统一拦截，返回标准 `ApiResponse` 格式：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @RequestBody + @Valid 校验失败 → 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) { ... }

    // 业务异常 → 动态状态码（401/403/423）
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getStatus())
                .body(ApiResponse.error(e.getStatus(), e.getMessage()));
    }

    // 权限不足 → 403
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException e) { ... }

    // 兜底 → 500，记录日志但不暴露内部细节
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleRuntime(RuntimeException e) {
        logger.error("未处理的运行时异常: {}", e.getMessage(), e);
        return ApiResponse.error(500, "服务器内部错误");
    }
}
```

#### 状态码映射

| 异常类型 | HTTP 状态码 | 场景 |
|----------|------------|------|
| `MethodArgumentNotValidException` | 400 | 参数校验失败 |
| `ConstraintViolationException` | 400 | 路径参数校验失败 |
| `BusinessException(401, ...)` | 401 | 用户名或密码错误 |
| `BusinessException(403, ...)` | 403 | 账号已被禁用 |
| `AccessDeniedException` | 403 | 权限不足 |
| `BusinessException(423, ...)` | 423 | 账号已锁定 |
| RateLimitInterceptor | 429 | 请求过于频繁 |
| `RuntimeException`（兜底） | 500 | 未预期的内部错误 |

### 10. 请求安全链路总览

```
请求到达
  │
  ├── SecurityHeadersFilter (@Order(HIGHEST_PRECEDENCE))
  │     └── 添加 7 项安全响应头
  │
  ├── RateLimitInterceptor (Spring MVC 拦截器)
  │     └── 检查 @RateLimit 注解 → Redis 计数 → 超限返回 429
  │
  ├── JwtAuthenticationFilter (Spring Security 过滤器)
  │     ├── 提取 Bearer Token
  │     ├── 解析签名 + 检查过期
  │     ├── 检查 jti 是否在黑名单中（TokenBlacklistService）
  │     └── 存入 SecurityContext
  │
  ├── Spring Security 授权检查
  │     └── URL + HTTP 方法匹配规则（GET 公开 / POST/PUT/DELETE 需认证）
  │
  └── Controller → Service
        ├── BusinessException → GlobalExceptionHandler → 正确的状态码
        └── 正常返回 → ApiResponse<T>
```

## 💾 缓存设计

### 1. Redis缓存策略

#### 用户信息缓存
```
Key: user:info:{userId}
Type: String (JSON)
Value: {
  "id": 1,
  "username": "admin",
  "role": "ADMIN"
}
TTL: 3600秒 (1小时)
```

#### Token缓存
```
Key: token:{userId}:{tokenHash}
Type: String
Value: "valid"
TTL: 86400秒 (24小时)
```

#### 权限缓存
```
Key: permissions:{userId}
Type: Set
Value: ["document:read", "document:write", "user:read"]
TTL: 1800秒 (30分钟)
```

### 2. 缓存更新策略

#### 缓存失效
- 用户信息更新时清除缓存
- 角色变更时清除权限缓存
- Token刷新时更新Token缓存
- 登出时添加Token到黑名单

## 🎨 前端技术实现

### 1. 状态管理

#### Pinia Store定义
```typescript
// stores/auth.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, LoginRequest, LoginResponse } from '@/types'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const token = ref<string | null>(localStorage.getItem('token'))
  const currentUser = ref<User | null>(null)

  // 计算属性
  const isLoggedIn = computed(() => !!token && !!currentUser.value)
  const userRole = computed(() => currentUser.value?.role)
  const hasPermission = computed(() => (resource: string, action: string) => {
    if (!currentUser.value) return false
    if (currentUser.value.role === 'ADMIN') return true
    // 检查具体权限
    return checkPermission(resource, action)
  })

  // 方法
  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUser = (user: User) => {
    currentUser.value = user
  }

  const login = async (credentials: LoginRequest) => {
    const res = await authApi.login(credentials)
    const { token: newToken, user } = res.data

    setToken(newToken)
    setUser(user)

    return res
  }

  const logout = () => {
    token.value = null
    currentUser.value = null
    localStorage.removeItem('token')
  }

  const refreshToken = async () => {
    const res = await authApi.refresh()
    const { token: newToken } = res.data
    setToken(newToken)
    return res
  }

  return {
    token,
    currentUser,
    isLoggedIn,
    userRole,
    hasPermission,
    setToken,
    setUser,
    login,
    logout,
    refreshToken
  }
})
```

### 2. 路由守卫

```typescript
// router/index.ts
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 不需要认证的页面
  if (!to.meta.requiresAuth) {
    next()
    return
  }

  // 需要认证但未登录
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next({
      name: 'Login',
      query: { redirect: to.fullPath }
    })
    return
  }

  // 检查角色权限
  if (to.meta.requiresRole && authStore.userRole !== to.meta.requiresRole) {
    next({ name: 'Home' })
    return
  }

  next()
})
```

### 3. HTTP拦截器

```typescript
// utils/request.ts
// 请求拦截器
axios.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
axios.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const originalRequest = error.config

    // Token过期，尝试刷新
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true

      try {
        const authStore = useAuthStore()
        await authStore.refreshToken()

        // 重新发送原始请求
        return axios(originalRequest)
      } catch (refreshError) {
        // 刷新失败，跳转登录
        authStore.logout()
        window.location.href = '/login'
        return Promise.reject(refreshError)
      }
    }

    return Promise.reject(error)
  }
)
```

## 🧪 测试策略

### 1. 单元测试

#### 后端测试
```java
@SpringBootTest
class AuthServiceTest {
    @Autowired
    private AuthService authService;

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest("admin", "password123");
        LoginResponse response = authService.login(request);
        
        assertNotNull(response.getToken());
        assertEquals("admin", response.getUser().getUsername());
    }

    @Test
    void shouldThrowExceptionWhenInvalidCredentials() {
        LoginRequest request = new LoginRequest("admin", "wrongpassword");
        
        assertThrows(UnauthorizedException.class, () -> {
            authService.login(request);
        });
    }
}
```

### 2. 集成测试

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldLoginAndReturnToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"admin\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.token").exists();
    }
}
```

### 3. 安全测试

```java
@SpringBootTest
class SecurityTest {

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectRequestWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
            .header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized());
    }
}
```

## 📈 性能优化

### 1. 数据库优化

#### 索引策略
```sql
-- 用户表索引
CREATE INDEX idx_username ON users(username);
CREATE INDEX idx_email ON users(email);
CREATE INDEX idx_status ON users(status);
CREATE INDEX idx_role_id ON users(role_id);

-- 登录历史索引
CREATE INDEX idx_user_created ON login_history(user_id, created_at);
```

#### 查询优化
```java
// 使用JPA EntityGraph避免N+1查询
@EntityGraph(attributePaths = {"role", "permissions"})
Optional<User> findById(Long id);
```

### 2. 缓存优化

#### 用户信息缓存
```java
@Cacheable(value = "users", key = "#userId")
public User getUserById(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
}

@CacheEvict(value = "users", key = "#userId")
public void updateUser(Long userId, UserUpdateRequest request) {
    // 更新逻辑
}
```

### 3. 并发控制

#### Token刷新并发控制
```java
public class TokenRefreshService {
    
    private final Map<Long, String> refreshTokens = new ConcurrentHashMap<>();
    
    @Transactional
    public String refreshToken(String oldToken) {
        // 使用分布式锁防止并发刷新
        String lockKey = "refresh:lock:" + oldToken.hashCode();
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            if (lock.tryLock(5, TimeUnit.SECONDS)) {
                // 刷新Token逻辑
                return generateNewToken(oldToken);
            }
        } finally {
            lock.unlock();
        }
    }
}
```

---

**文档版本：** v1.1
**创建日期：** 2026-05-31
**最后更新：** 2026-06-10
**状态：** 已更新（同步安全防护实现）
