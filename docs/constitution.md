# 📜 企业知识库问答系统 — 开发宪法

> 本文档是项目开发的最高准则，所有团队成员（包括 AI 助手）必须遵守。
> 违反宪法的代码变更不得合并到主分支。

---

## 一、技术基线

不可违反的技术约束。环境不满足时，**先解决环境问题再继续开发**。

| 技术 | 版本要求 | 验证命令 |
|------|----------|----------|
| JDK | **17+**（默认 JDK 8 会导致编译失败） | `java -version` |
| Node.js | 18+ | `node -v` |
| Python | 3.11+ | `python3 --version` |
| Docker Compose | v2+ | `docker compose version` |
| Maven | 3.9+ | `mvn -v` |

### 编译命令

```bash
# 后端编译（必须指定 JDK 17）
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn compile -f backend/pom.xml

# 前端构建
cd frontend && npm run build

# AI 服务
cd ai-service && pip install -r requirements.txt
```

---

## 二、代码规范

### 2.1 注释原则

| ✅ 保留 | ❌ 删除 |
|---------|---------|
| 业务逻辑"为什么"（安全决策、查询优先级） | 框架概念科普（Spring 注解含义、设计模式介绍） |
| 复杂算法说明（加密流程、脱敏规则） | Java 基础概念（Optional、泛型、构造器注入） |
| 类的一句话描述 + @author | 大段 HTML 表格、学习要点列表 |

> 框架概念统一放在 `docs/10-java-concepts.md`。

### 2.2 分层架构

```
Controller → 只做参数校验和响应封装，不含业务逻辑
Service    → 业务逻辑编排，不含 HTTP 语义（不直接操作 Request/Response）
Repository → 数据访问，只定义查询方法
Entity     → 数据库映射，不包含业务逻辑
DTO        → 前后端交互，不直接暴露 Entity
```

### 2.3 依赖注入

```java
// ✅ 正确：构造器注入
public AuthService(UserRepository repo, PasswordEncoder encoder) { ... }

// ❌ 禁止：字段注入
@Autowired
private UserRepository repo;
```

### 2.4 参数校验

**所有接口入参必须校验，无一例外。**

#### 后端（Java）

- `@RequestBody` 参数必须搭配 `@Valid` 注解
- Controller 类必须标注 `@Validated`（激活 @RequestParam / @PathVariable 级别校验）
- DTO 字段必须使用 JSR 303 注解：`@NotBlank`、`@NotNull`、`@Size`、`@Min`、`@Max`、`@Pattern`
- 分页参数 `page` ≥ 1，`size` 在 1-100 之间
- **禁止**直接用 Entity 作为 `@RequestBody`，必须创建专用 Request DTO

```java
// ✅ 正确
public ApiResponse<Document> create(@RequestBody @Valid CreateDocumentRequest request)

// ❌ 禁止
public ApiResponse<Document> create(@RequestBody Document document)
```

#### AI 服务（Python）

- Pydantic 模型字段必须使用 `Field()` 约束值域
- 字符串字段必须设定 `min_length` 和 `max_length`
- 数值字段必须设定 `ge`（最小值）和 `le`（最大值）

```python
# ✅ 正确
question: str = Field(min_length=1, max_length=2000)
limit: int = Field(default=5, ge=1, le=100)

# ❌ 禁止
question: str
limit: int = 5
```

#### 前端（Vue）

- 所有表单必须配置 `:rules` 校验规则
- 文本输入框必须设置 `maxlength` 属性
- 提交前必须调用 `formRef.validate()`

### 2.5 异常处理

- 所有异常通过 `GlobalExceptionHandler` 统一拦截，返回 `ApiResponse` 格式
- **禁止**在 Controller 中 try-catch 后返回自定义错误格式
- 业务异常使用 `RuntimeException`（后续演进为自定义 `BusinessException`）
- 校验异常返回 HTTP 400 + 具体字段错误信息
- 业务异常返回 HTTP 500 + 用户友好的中文提示

### 2.6 命名规范

| 语言 | 类/组件 | 方法/函数 | 变量 | 文件 |
|------|---------|-----------|------|------|
| Java | PascalCase | camelCase | camelCase | PascalCase.java |
| Vue/TS | PascalCase | camelCase | camelCase | PascalCase.vue / camelCase.ts |
| Python | PascalCase | snake_case | snake_case | snake_case.py |

### 2.7 API 设计规范

- URL 使用小写 + 短横线：`/api/v1/user-profiles`（不是 `/api/v1/UserProfiles`）
- 资源用复数名词：`/documents`、`/categories`（不是 `/document`）
- 操作用 HTTP 方法表达：GET 查询、POST 创建、PUT 更新、DELETE 删除
- 分页参数统一：`page`（从 1 开始）、`size`（默认 20，最大 100）
- 列表接口返回 `Page<T>` 格式（含 total、items）
- 所有接口返回统一的 `ApiResponse<T>` 包装，响应码 200 表示成功

---

## 三、安全规范

安全底线，**不可妥协**。

### 3.1 配置密码

- `application.yml` 中敏感默认值使用 `ENC(密文)` 格式
- Jasypt 主密钥 `JASYPT_ENCRYPTOR_PASSWORD` 仅存于 `.env` 文件
- `.env` 已加入 `.gitignore`，**禁止提交到 Git**
- 加密工具：`backend/.../utils/JasyptEncryptUtil.java`

### 3.2 传输加密

- 登录密码使用 RSA 公钥加密传输，**禁止明文传输**
- 前端通过 `GET /api/v1/auth/public-key` 获取公钥
- 后端 `RsaUtil.tryDecrypt()` 兼容明文模式（仅限开发调试）

### 3.3 响应脱敏

- API 响应中 email、phone、身份证等敏感字段必须脱敏
- 脱敏工具：`backend/.../utils/SensitiveFieldUtil.java`
- 脱敏规则：
  - 邮箱：`user@example.com` → `u***@example.com`
  - 手机：`13812345678` → `138****5678`
  - 身份证：`110101199001011234` → `1101****1234`

### 3.4 禁止事项

- ❌ 禁止在代码中硬编码密码、API Key
- ❌ 禁止将 `.env` 文件提交到 Git
- ❌ 禁止在日志中输出敏感信息
- ❌ 禁止在 API 响应中返回完整密码（即使是 BCrypt 哈希）

---

## 四、日志规范

### 4.1 日志级别

| 级别 | 使用场景 | 示例 |
|------|----------|------|
| ERROR | 影响功能的异常，需要立即处理 | 数据库连接失败、外部 API 超时 |
| WARN | 潜在问题，不影响主流程 | 配置缺失使用默认值、RSA 解密失败降级为明文 |
| INFO | 关键业务节点 | 用户登录、文档创建、服务启动 |
| DEBUG | 调试信息，生产环境不输出 | SQL 参数、请求体详情 |

### 4.2 日志规范

- ✅ 使用 SLF4J（后端）和 `logging`（Python），**禁止** `System.out.println` / `print()`
- ✅ 日志包含上下文信息：`logger.info("用户登录: username={}", username)`
- ❌ **禁止**在日志中输出密码、Token、API Key 等敏感信息
- ❌ **禁止**在循环中打印 DEBUG 日志（性能杀手）

---

## 五、数据库规范

### 5.1 表命名

- 表名使用小写 + 下划线，前缀 `kb_`：`kb_users`、`kb_documents`
- 字段名使用小写 + 下划线：`created_at`、`category_id`
- 关联字段命名：`<关联表>_id`：`author_id`、`parent_id`

### 5.2 必备字段

每张业务表必须包含：

```sql
id          BIGINT AUTO_INCREMENT PRIMARY KEY  -- 自增主键
created_at  DATETIME DEFAULT CURRENT_TIMESTAMP -- 创建时间
updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP -- 更新时间
```

### 5.3 软删除

- 业务数据使用软删除（`deleted_at` 字段），不物理删除
- 查询时必须过滤 `WHERE deleted_at IS NULL`
- 物理删除仅在数据清理/归档任务中使用

### 5.4 数据库迁移

- 使用 Flyway 管理数据库版本迁移（应用启动时自动执行）
- 迁移脚本位置：`backend/src/main/resources/db/migration/`
- 迁移脚本命名：`V<版本号>__<描述>.sql`，如 `V3__add_user_phone.sql`
- 迁移脚本只增不删（生产数据安全）
- 已有数据库首次接入时，`baseline-on-migrate: true` 自动打基线
- 详细教程见 `docs/11-flyway-guide.md`

---

## 六、测试底线

### 6.1 必须测试的场景

| 层级 | 必须覆盖 |
|------|----------|
| Controller | 参数校验（空值、越界、格式错误）返回 400 |
| Service | 核心业务逻辑（登录成功/失败、文档创建/更新） |
| Service | 边界条件（分页 page=0、size=0） |
| Service | 异常分支（用户不存在、密码错误、账号禁用） |

### 6.2 测试命名

```java
// Java: 方法名_场景_预期结果
@Test
void login_whenUserNotFound_shouldThrowException() { ... }

// Python: test_方法名_场景_预期结果
def test_ask_with_empty_question_should_return_400():
```

### 6.3 禁止事项

- ❌ 禁止提交注释掉 `@Test` 的测试
- ❌ 禁止为了通过 CI 而修改断言
- ❌ 禁止测试中硬编码外部依赖（数据库、API）

---

## 七、Git 规范

### 7.1 分支命名

```
feature/<描述>    — 新功能开发
fix/<描述>        — Bug 修复
refactor/<描述>   — 代码重构
docs/<描述>       — 文档更新
```

### 7.2 Commit 格式

```
type(scope): 中文描述
```

| type | 说明 | 示例 |
|------|------|------|
| feat | 新功能 | `feat(auth): 添加 RSA 密码加密传输` |
| fix | Bug 修复 | `fix(login): 修复 JWT 过期时间解析错误` |
| refactor | 重构 | `refactor(comments): 精简 Java 文件冗长注释` |
| docs | 文档 | `docs(api): 更新登录接口文档` |
| style | 格式 | `style(lint): 统一代码缩进` |
| chore | 杂项 | `chore(deps): 升级 Spring Boot 到 3.2.4` |

### 7.3 提交原则

- 一个 commit 做一件事，不混合不相关的变更
- 不提交无法编译的代码
- 不提交 `.env`、`node_modules/`、`dist/`、`.class` 等生成文件

---

## 八、变更检查清单

### 8.1 构建验证

每次修改以下文件时，**必须执行对应的验证命令**：

| 变更文件 | 必须执行 |
|----------|----------|
| `docker-compose.yml` | `docker compose config --services` |
| `backend/pom.xml` | `JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn compile -f backend/pom.xml` |
| `frontend/package.json` | `cd frontend && npm run build` |
| `backend/src/main/resources/application.yml` | `mvn compile` |
| 移动/重命名文件或目录 | `grep -rn "旧路径" --include="*.java" --include="*.yml" --include="*.md" --include="*.sh"` |
| `.env.example` | 与 `.env` 对比，确认字段一致 |

### 8.2 文档同步

**每次代码变更必须检查关联文档是否需要同步更新，不得遗漏。**

| 变更类型 | 需要同步更新的文档 |
|----------|-------------------|
| 新增 / 修改 / 删除 API 接口 | `docs/features/` 下对应的功能设计文档、接口参数/返回值说明 |
| 新增 / 移除 Maven 依赖 | `backend/pom.xml` 注释、`docs/` 架构说明（如引入新中间件） |
| 修改 `application.yml` 配置项 | `.env.example` 对应字段、`docs/features/` 配置说明 |
| 修改 `docker-compose.yml` 服务 | `docs/` 部署文档、`.env.example` 环境变量 |
| 新增工具类 / 配置类 | `CLAUDE.md` 变更检查表（如有新的验证命令）、`docs/constitution.md` 对应章节 |
| 修改宪法规则 | `CLAUDE.md`（精简版）必须同步更新 |
| 修改项目目录结构 | `docs/constitution.md` 九、项目目录约定 |
| 新增安全机制 | `docs/constitution.md` 三、安全规范 |

**原则：**

- 代码变更是"因"，文档滞后是"债"，技术债必须当场清偿
- 如果不确定是否影响文档，**宁可多检查也不要跳过**
- commit 中应包含文档更新，或说明"本次变更无文档影响"

---

## 九、项目目录约定

```
knowledge-base/
├── frontend/              # Vue 3 前端（禁止后端逻辑）
├── backend/               # Spring Boot 后端（禁止前端代码）
├── ai-service/            # Python AI 服务
├── deploy/nginx/          # Nginx 部署配置
├── docs/                  # 项目文档
│   ├── constitution.md    # ← ·本文件
│   ├── 10-java-concepts.md # Java 概念教学（从代码中迁移）
│   ├── features/          # 功能设计文档
│   └── scripts/           # 开发脚本
├── CLAUDE.md              # AI 助手指令（本文件的精简版）
├── docker-compose.yml     # Docker 编排
├── .env.example           # 环境变量模板
└── .env                   # 本地环境变量（gitignored）
```

---

**文档版本：** v2.2
**最后更新：** 2026-06-08
**适用范围：** 所有开发者和 AI 助手
