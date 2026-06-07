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

### 2.4 命名规范

| 语言 | 类/组件 | 方法/函数 | 变量 | 文件 |
|------|---------|-----------|------|------|
| Java | PascalCase | camelCase | camelCase | PascalCase.java |
| Vue/TS | PascalCase | camelCase | camelCase | PascalCase.vue / camelCase.ts |
| Python | PascalCase | snake_case | snake_case | snake_case.py |

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

## 四、Git 规范

### 4.1 分支命名

```
feature/<描述>    — 新功能开发
fix/<描述>        — Bug 修复
refactor/<描述>   — 代码重构
docs/<描述>       — 文档更新
```

### 4.2 Commit 格式

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

### 4.3 提交原则

- 一个 commit 做一件事，不混合不相关的变更
- 不提交无法编译的代码
- 不提交 `.env`、`node_modules/`、`dist/`、`.class` 等生成文件

---

## 五、变更检查清单

每次修改以下文件时，**必须执行对应的验证命令**：

| 变更文件 | 必须执行 |
|----------|----------|
| `docker-compose.yml` | `docker compose config --services` |
| `backend/pom.xml` | `JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn compile -f backend/pom.xml` |
| `frontend/package.json` | `cd frontend && npm run build` |
| `backend/src/main/resources/application.yml` | `mvn compile` |
| 移动/重命名文件或目录 | `grep -rn "旧路径" --include="*.java" --include="*.yml" --include="*.md" --include="*.sh"` |
| `.env.example` | 与 `.env` 对比，确认字段一致 |

---

## 六、项目目录约定

```
knowledge-base/
├── frontend/              # Vue 3 前端（禁止后端逻辑）
├── backend/               # Spring Boot 后端（禁止前端代码）
├── ai-service/            # Python AI 服务
├── deploy/nginx/          # Nginx 部署配置
├── docs/                  # 项目文档
│   ├── constitution.md    # ← 本文件
│   ├── 10-java-concepts.md # Java 概念教学（从代码中迁移）
│   ├── features/          # 功能设计文档
│   └── scripts/           # 开发脚本
├── CLAUDE.md              # AI 助手指令（本文件的精简版）
├── docker-compose.yml     # Docker 编排
├── .env.example           # 环境变量模板
└── .env                   # 本地环境变量（gitignored）
```

---

**文档版本：** v1.0
**最后更新：** 2026-06-07
**适用范围：** 所有开发者和 AI 助手
