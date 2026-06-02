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

### 3. 登录失败限制

#### 登录失败计数器
```java
@Entity
public class User {
    private Integer loginAttempts = 0;
    private Timestamp lockedUntil;
    
    public boolean isLocked() {
        if (lockedUntil != null && lockedUntil.after(new Date())) {
            return true;
        }
        return false;
    }
    
    public void incrementLoginAttempts() {
        this.loginAttempts++;
        if (this.loginAttempts >= 5) {
            // 锁定30分钟
            this.lockedUntil = new Timestamp(System.currentTimeMillis() + 30 * 60 * 1000);
        }
    }
    
    public void resetLoginAttempts() {
        this.loginAttempts = 0;
        this.lockedUntil = null;
    }
}
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

### 5. 会话管理

#### Token黑名单
```java
@Service
public class TokenBlacklistService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    
    public void blacklistToken(String token, long ttl) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.SECONDS);
    }
    
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.opsForValue().get(key));
    }
}
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

**文档版本：** v1.0  
**创建日期：** 2026-05-31  
**最后更新：** 2026-05-31  
**状态：** 待审核
