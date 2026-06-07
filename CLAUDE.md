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
- commit 格式：`type(scope): 中文描述`（feat/fix/refactor/docs/style/chore）

## 安全底线

- 配置密码使用 Jasypt `ENC()` 加密，主密钥仅存 `.env`
- 密码传输使用 RSA 加密，禁止明文
- 响应中 email/phone 必须脱敏
- 禁止提交 `.env`，禁止硬编码密码

## 变更检查

- `docker-compose.yml` 变更 → `docker compose config --services`
- `pom.xml` 变更 → `mvn compile`（JDK 17）
- `package.json` 变更 → `npm run build`
- 移动文件/目录 → `grep -rn "旧路径"` 确认无残留引用
