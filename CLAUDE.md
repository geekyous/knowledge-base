# CLAUDE.md — AI 助手项目指令

> 本文件是 `docs/constitution.md` 的精简版，Claude Code 每次会话自动加载。
> 完整规则请查阅 [docs/constitution.md](docs/constitution.md)。

## 技术基线

- **后端编译必须使用 JDK 17**：`JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn compile -f backend/pom.xml`
- 前端：Node 18+，构建验证 `cd frontend && npm run build`
- Python：3.11+

## 代码规范

- 注释保留"为什么"，框架概念放 `docs/10-java-concepts.md`
- Controller 不含业务逻辑，Service 不含 HTTP 语义
- DTO 不直接暴露 Entity，Response 过滤敏感字段
- 构造器注入，禁止 `@Autowired` 字段注入
- **所有 @RequestBody 必须搭配 @Valid，Controller 类标注 @Validated**
- **禁止用 Entity 做 @RequestBody，必须创建 Request DTO**
- Pydantic 字段必须用 Field() 约束值域
- 前端表单必须配置 :rules + maxlength
- 异常统一通过 GlobalExceptionHandler 处理

## 安全底线

- 配置密码使用 Jasypt `ENC()` 加密，主密钥仅存 `.env`
- 密码传输使用 RSA 加密，禁止明文
- 响应中 email/phone 必须脱敏
- 禁止提交 `.env`，禁止硬编码密码
- 禁止在日志中输出密码、Token、API Key

## 日志规范

- 使用 SLF4J / logging，禁止 System.out.println / print()
- 日志包含上下文：`logger.info("用户登录: username={}", username)`
- 禁止在循环中打 DEBUG 日志

## Git 规范

- 分支命名：`feature/<描述>`、`fix/<描述>`、`refactor/<描述>`
- commit 格式：`type(scope): 中文描述`

## 变更检查

- `docker-compose.yml` 变更 → `docker compose config --services`
- `pom.xml` 变更 → `mvn compile`（JDK 17）
- `package.json` 变更 → `npm run build`
- 移动文件/目录 → `grep -rn "旧路径"` 确认无残留引用
- 数据库变更 → 新建 `V{N}__描述.sql` 迁移脚本（Flyway 管理，详见 `docs/11-flyway-guide.md`）

## 文档同步

- **每次代码变更必须检查关联文档是否需要同步更新**
- API 变更 → 同步 `docs/features/` 功能设计文档
- 配置变更 → 同步 `.env.example`、部署文档
- 新增工具类/配置类 → 检查 `CLAUDE.md` 变更检查表
- 宪法规则变更 → `CLAUDE.md` 精简版必须同步
- 目录结构变更 → 同步宪法「项目目录约定」
- 不确定是否影响文档时，**宁可多检查也不跳过**
