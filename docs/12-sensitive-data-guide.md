# 敏感数据保护方案 — 存储加密 + 响应脱敏

> 本文档描述项目中 email、phone 等隐私字段的双层防护设计方案，
> 供团队成员理解现有方案和后续新增敏感字段时参考。

## 一、设计目标

| 层次 | 目标 | 技术 |
|------|------|------|
| **存储层** | 数据库中敏感字段为密文，即使数据库泄露也无法直接获取隐私数据 | JPA `AttributeConverter` + Jasypt AES-256 |
| **传输层** | API 响应中敏感字段自动掩码，前端无法获取完整值 | `@Sensitive` 注解 + Jackson `ContextualSerializer` |

### 为什么需要双层防护？

- **存储加密**：防止数据库备份泄露、SQL 注入拖库、运维人员直接查看隐私数据
- **响应脱敏**：防止前端日志、浏览器 DevTools、中间人抓包获取完整隐私数据
- 两者互补：即使绕过了一层，另一层仍提供保护

## 二、整体架构

```
写入流程（Service → DB）：
  Service 设置明文 email
    → @PrePersist 计算emailHash = SHA-256(email)
    → EncryptedStringConverter.convertToDatabaseColumn(email) → AES加密
    → 数据库存储密文 + email_hash

读取流程（DB → API 响应）：
  数据库读取密文
    → EncryptedStringConverter.convertToEntityAttribute(ciphertext) → AES解密
    → Entity 持有明文 email
    → DTO 字段有 @Sensitive(EMAIL) 注解
    → Jackson 序列化时 SensitiveSerializer 自动调用 SensitiveFieldUtil.maskEmail()
    → API 响应返回 "a***@company.com"
```

## 三、存储加密方案

### 3.1 核心组件

| 文件 | 职责 |
|------|------|
| `converter/EncryptedStringConverter.java` | JPA AttributeConverter，透明加密/解密 |
| `utils/FieldEncryptor.java` | 封装 Jasypt StringEncryptor + SHA-256 哈希 |
| `config/SpringContextHolder.java` | 让非 Spring 管理的 Converter 获取 Bean |

### 3.2 加密后的查询匹配问题

email 加密后无法使用 `WHERE email = ?` 查询。解决方案：

- 新增 `email_hash` 列（CHAR(64)），存储 email 的 SHA-256 哈希
- 查询时先计算哈希：`fieldEncryptor.hash(email)`，再用哈希值查询
- 唯一约束从 `email` 列迁移到 `email_hash` 列
- Entity 的 `@PrePersist` / `@PreUpdate` 自动维护 `emailHash` 字段

```java
// 查询示例
String hash = fieldEncryptor.hash("admin@company.com");
User user = userRepository.findByEmailHash(hash).orElse(null);
```

### 3.3 已有明文数据的迁移

`SensitiveDataMigrator`（`config/SensitiveDataMigrator.java`）在应用启动时自动执行：

1. 用**原生 SQL** 检查是否存在 `email_hash IS NULL AND email IS NOT NULL` 的行
2. 原生 SQL 读取明文 → `FieldEncryptor` 加密 + 哈希 → 原生 SQL 写回密文
3. 使用原生 SQL 而非 JPA Repository，避免 `AttributeConverter` 尝试解密明文值
4. 仅执行一次（迁移后所有行都有 `email_hash`）

### 3.4 Flyway 迁移

```sql
-- V3__encrypt_user_sensitive_fields.sql
ALTER TABLE kb_users ADD COLUMN email_hash CHAR(64) NULL;
ALTER TABLE kb_users MODIFY COLUMN email VARCHAR(255) NULL;
ALTER TABLE kb_users MODIFY COLUMN phone VARCHAR(255) NULL;
ALTER TABLE kb_users DROP INDEX email;
CREATE UNIQUE INDEX uk_email_hash ON kb_users(email_hash);
```

- `email`/`phone` 列拓宽到 VARCHAR(255) 以容纳 AES 密文（Base64 编码后约为明文 2-3 倍）
- 唯一约束从 `email` 迁移到 `email_hash`

## 四、响应脱敏方案

### 4.1 核心组件

| 文件 | 职责 |
|------|------|
| `annotation/Sensitive.java` | 注解定义，标注在 DTO 字段上 |
| `serializer/SensitiveSerializer.java` | Jackson ContextualSerializer，序列化时自动掩码 |
| `utils/SensitiveFieldUtil.java` | 底层掩码算法（maskEmail / maskPhone / maskIdCard） |

### 4.2 使用方式

在 DTO 字段上标注 `@Sensitive` 注解即可：

```java
public class UserInfo {
    @Sensitive(Sensitive.SensitiveType.EMAIL)
    private String email;

    @Sensitive(Sensitive.SensitiveType.PHONE)
    private String phone;
}
```

Service 层传入原始值，**无需手动调用 `SensitiveFieldUtil`**：

```java
// ✅ 正确：传原始值，@Sensitive 自动脱敏
.email(user.getEmail())
.phone(user.getPhone())

// ❌ 禁止：手动调用脱敏工具（重复脱敏、容易遗漏）
.email(SensitiveFieldUtil.maskEmail(user.getEmail()))
```

### 4.3 工作原理

1. `@Sensitive` 通过 `@JacksonAnnotationsInside` + `@JsonSerialize` 元注解绑定序列化器
2. Jackson 首次序列化该字段时调用 `SensitiveSerializer.createContextual()`
3. 从注解读取 `SensitiveType`，创建已配置的序列化器实例
4. 后续序列化调用 `serialize()`，根据类型委托 `SensitiveFieldUtil` 执行掩码

### 4.4 脱敏规则

| 类型 | 原始值 | 脱敏后 |
|------|--------|--------|
| EMAIL | `user@example.com` | `u***@example.com` |
| PHONE | `13812345678` | `138****5678` |
| ID_CARD | `110101199001011234` | `1101****1234` |

## 五、新增敏感字段操作步骤

当需要新增一个敏感字段（如身份证号）时：

### 5.1 存储加密

1. **Entity**：在字段上加 `@Convert(converter = EncryptedStringConverter.class)`
2. **Flyway**：新建迁移脚本，将列拓宽到 VARCHAR(255)
3. **如需查询**：新增 `xxx_hash` 列 + SHA-256 哈希，`@PrePersist` 中自动计算
4. **数据迁移**：在 `SensitiveDataMigrator` 中补充对新字段的迁移逻辑

### 5.2 响应脱敏

1. **SensitiveFieldUtil**：如无对应掩码方法，先添加
2. **@Sensitive 注解**：在 `SensitiveType` 枚举中新增类型
3. **DTO**：在 DTO 字段上加 `@Sensitive(SensitiveType.XXX)`
4. **文档**：更新本文档的脱敏规则表

## 六、常见问题

### Q: 加密后如何搜索？

使用 SHA-256 哈希列。对需要精确匹配的查询：
1. 调用方先计算哈希：`fieldEncryptor.hash(plaintext)`
2. 用哈希值查询：`repository.findByXxxHash(hash)`
3. 哈希是确定性的（同输入同输出），支持数据库索引和唯一约束

**注意**：不支持模糊搜索（LIKE '%keyword%'），因为密文和哈希都无法模糊匹配。

### Q: 密钥丢失会怎样？

AES 是对称加密，密钥丢失后**无法恢复**已加密的数据。
`JASYPT_ENCRYPTOR_PASSWORD` 必须安全备份。

### Q: 加密对性能的影响？

- **写入**：每次 INSERT/UPDATE 多一次 AES 加密 + 一次 SHA-256 哈希，约 0.1ms
- **读取**：每次 SELECT 多一次 AES 解密，约 0.1ms
- 对 OLTP 场景影响可忽略不计

### Q: 如何在测试环境中禁用加密？

设置 `JASYPT_ENCRYPTOR_PASSWORD` 为统一测试密钥即可。
`EncryptedStringConverter` 对 null/空值直接透传，不加密。

---

**文档版本：** v1.0
**最后更新：** 2026-06-08
