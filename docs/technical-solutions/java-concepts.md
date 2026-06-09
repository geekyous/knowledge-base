# Java 后端核心概念速查

> 本文档收纳了项目中涉及的核心 Java / Spring Boot 概念，供学习参考。

---

## 一、Spring 注解速查

### 核心注解

| 注解 | 作用 | 等价写法 |
|------|------|----------|
| `@RestController` | REST 控制器，方法返回值直接作为 HTTP 响应体 | `@Controller` + `@ResponseBody` |
| `@Service` | 服务层组件，被 Spring 扫描注册为 Bean | — |
| `@Repository` | 数据访问层组件，自动转换数据库异常 | — |
| `@Configuration` | 配置类，可声明 `@Bean` 方法 | — |
| `@Bean` | 方法返回的对象注册为 Spring Bean | — |
| `@Autowired` | 自动注入依赖（推荐使用构造器注入替代） | — |

### Web 注解

| 注解 | 说明 |
|------|------|
| `@RequestMapping` | 类/方法级 URL 映射 |
| `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` | HTTP 方法映射快捷方式 |
| `@RequestBody` | 将请求体 JSON 反序列化为 Java 对象 |
| `@RequestParam` | 绑定 URL 查询参数 |
| `@PathVariable` | 绑定 URL 路径变量 |

### 依赖注入

```java
// ✅ 推荐：构造器注入（Spring 官方最佳实践）
@Service
public class AuthService {
    private final UserRepository userRepository;
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}

// ⚠️ 不推荐：字段注入
@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;
}
```

---

## 二、设计模式

### Builder 模式（建造者模式）

用于构建字段较多的对象，配合 Lombok `@Builder` 使用：

```java
LoginResponse response = LoginResponse.builder()
    .token("eyJhbGci...")
    .user(userInfo)
    .build();
```

### DTO 模式（数据传输对象）

- **Entity** → 对应数据库表，包含所有字段（含敏感字段如 password）
- **Request DTO** → 前端提交的数据结构（如 LoginRequest）
- **Response DTO** → 返回给前端的数据结构，过滤敏感字段

**核心原则**：永远不直接返回 Entity，通过 DTO 控制暴露哪些字段。

### 统一响应封装

```java
// 所有 API 返回统一格式
{
    "code": 200,
    "message": "success",
    "data": { ... }
}
```

---

## 三、JPA / Hibernate

### 实体映射

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(unique = true)
    private String email;
}
```

### 派生查询（Derived Query）

Spring Data JPA 根据方法名自动生成 SQL：

```java
// 方法名 → SQL
findByUsername(String username)       → SELECT * FROM users WHERE username = ?
findByEmail(String email)             → SELECT * FROM users WHERE email = ?
findByUsernameAndStatus(name, status) → SELECT * FROM users WHERE username = ? AND status = ?
```

### 审计字段

```java
@CreatedDate
@Column(updatable = false)
private LocalDateTime createdAt;

@LastModifiedDate
private LocalDateTime updatedAt;
```

配合 `@EnableJpaAuditing` 在启动类上开启自动填充。

---

## 四、Spring Security

### 过滤器链

```
HTTP 请求 → CORS Filter → JWT Filter → Spring Security Filter Chain → Controller
```

自定义 `JwtAuthenticationFilter` 继承 `OncePerRequestFilter`，在过滤器链中：
1. 从请求头提取 Token
2. 验证 Token 签名和有效期
3. 解析用户信息，设置到 `SecurityContextHolder`

### JWT 无状态认证

- 传统 Session：服务端存储会话，Cookie 传递 Session ID
- JWT 无状态：Token 包含所有认证信息，服务端不存储状态

```
Token 结构：Header.Payload.Signature
- Header:  { "alg": "HS256", "typ": "JWT" }
- Payload: { "sub": "admin", "role": "ADMIN", "exp": 1740000000 }
- Signature: HMACSHA256(base64(Header) + "." + base64(Payload), secret)
```

### CSRF 防护

- 传统 Web 应用（Cookie + Session）：**必须启用 CSRF**
- REST API（无状态 + Token 认证）：**可以禁用**，因为 Token 已提供防护

---

## 五、安全实践

### 密码存储

```
用户注册：明文密码 → BCrypt.hash() → 存储哈希值
用户登录：明文密码 → BCrypt.matches(明文, 哈希) → true/false
```

BCrypt 自动管理盐值，无需单独存储。`matches()` 不会泄露原始密码。

### 密码加密传输（RSA）

```
前端：password → RSA 公钥加密 → Base64 密文 → HTTP 传输
后端：Base64 密文 → RSA 私钥解密 → 明文 → BCrypt 比对
```

即使 HTTPS 被中间人攻击，截获的也是 RSA 密文，无法还原原始密码。

### 配置文件加密（Jasypt）

```
明文密码 → JasyptEncryptUtil encrypt → ENC(密文) → 写入 application.yml
运行时：JASYPT_ENCRYPTOR_PASSWORD 主密钥 → Jasypt 自动解密
```

主密钥只存放在 `.env`（gitignored），不提交到代码仓库。

### 数据库版本迁移（Flyway）

```
应用启动 → Flyway 扫描 db/migration/ → 对比 flyway_schema_history → 执行未运行的脚本
```

| 概念 | 说明 |
|------|------|
| 版本迁移 `V{N}__{描述}.sql` | 按版本号顺序执行，**只执行一次** |
| 可重复迁移 `R__{描述}.sql` | 内容变化时重新执行（如视图） |
| `flyway_schema_history` | Flyway 自动创建的版本追踪表 |
| `baseline-on-migrate` | 已有数据库时自动打基线，跳过已存在的表 |
| checksum 校验 | 防止已执行的脚本被篡改 |

> 详细教程见 [Flyway 数据库迁移指南](flyway-guide.md)。

### 敏感数据脱敏

DTO 敏感字段标注 `@Sensitive` 注解，Jackson 序列化时自动掩码：

```java
@Sensitive(Sensitive.SensitiveType.EMAIL)
private String email;    // 序列化后 → "u***@example.com"
```

| 类型 | 原始值 | 脱敏后 |
|------|--------|--------|
| 邮箱 | `user@example.com` | `u***@example.com` |
| 手机号 | `13812345678` | `138****5678` |
| 身份证 | `110101199001011234` | `1101****1234` |

### JPA 字段加密（AttributeConverter）

`AttributeConverter<X, Y>` 是 JPA 提供的字段转换接口，在 Entity 字段与数据库列之间自动转换：

```java
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    // 写入数据库前：明文 → 密文
    public String convertToDatabaseColumn(String plaintext) { ... }
    // 从数据库读取后：密文 → 明文
    public String convertToEntityAttribute(String ciphertext) { ... }
}
```

在 Entity 字段上标注即可启用透明加密：

```java
@Convert(converter = EncryptedStringConverter.class)
@Column(length = 255)
private String email;    // 数据库中存储 AES 密文，Java 中使用明文
```

**关键点：**
- Converter 由 Hibernate 创建（非 Spring Bean），通过 `SpringContextHolder` 获取 Spring Bean
- 加密后无法直接 SQL 查询，需要额外的 SHA-256 哈希列（如 `email_hash`）辅助查询
- `@PrePersist` / `@PreUpdate` 自动维护哈希列

> 详细方案见 [敏感数据保护方案](sensitive-data-guide.md)。

---

## 六、项目分层架构

```
Controller → 接收 HTTP 请求，参数校验，响应封装
    ↓
Service → 业务逻辑编排，事务控制
    ↓
Repository → 数据访问（Spring Data JPA）
    ↓
Entity → 数据库实体
```

- Controller 只做"转发"，不包含业务逻辑
- Service 是业务核心，可调用多个 Repository
- Repository 由 Spring Data JPA 自动实现
- DTO 在各层之间传递数据，Entity 不直接暴露

---

**文档版本：** v1.2
**最后更新：** 2026-06-08
