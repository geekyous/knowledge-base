# 🚀 快速开始指南

> 📖 **本地开发环境搭建**请参阅 [开发环境搭建指南](04-development-setup.md)，本文档侧重 Docker 一键启动演示。

## 环境要求

| 工具           | 版本要求       | 说明                    |
|----------------|---------------|------------------------|
| Docker         | 20.0+         | 容器运行环境             |
| Docker Compose | 2.0+          | 容器编排工具             |
| Node.js        | 18+           | 前端开发（仅开发时需要）   |
| Java           | 17+           | 后端开发（仅开发时需要）   |
| Python         | 3.11+         | AI服务开发（仅开发时需要） |

## 一键启动（推荐）

```bash
# 1. 克隆项目
cd /path/to/knowledge-base

# 2. 启动所有服务
chmod +x docs/scripts/start-demo.sh
./docs/scripts/start-demo.sh
```

启动脚本会自动：
- ✅ 检查 Docker 环境
- ✅ 检查端口占用
- ✅ 创建 .env 配置文件
- ✅ 启动所有容器
- ✅ 等待服务就绪
- ✅ 初始化缓存数据

## 手动启动

### 环境变量说明

> `application.yml` 中所有配置项均无默认值，**必须通过环境变量注入**。
> Docker Compose 方式会在 `docker-compose.yml` 中提供合理默认值；本地开发需手动 export。

| 变量 | 说明 | 示例值 |
|------|------|--------|
| `DB_HOST` | MySQL 地址 | `localhost` |
| `DB_PORT` | MySQL 端口 | `3306` |
| `DB_DATABASE` | 数据库名 | `knowledge_base` |
| `DB_USERNAME` | 数据库用户名 | `kb_user` |
| `DB_PASSWORD` | 数据库密码 | `kb_password` |
| `REDIS_HOST` | Redis 地址 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码 | `redis_password` |
| `ES_HOST` | Elasticsearch 地址 | `localhost` |
| `ES_PORT` | Elasticsearch 端口 | `9200` |
| `ES_USERNAME` | ES 用户名（空=无认证） | *(留空)* |
| `ES_PASSWORD` | ES 密码（空=无认证） | *(留空)* |
| `JWT_SECRET` | JWT 签名密钥（≥32字符） | `dev-secret-key-for-local-testing-only` |
| `JWT_EXPIRATION` | JWT 过期时间（秒） | `86400` |
| `AI_SERVICE_URL` | AI 服务地址 | `http://localhost:8000` |
| `LLM_PROVIDER` | LLM 提供商 | `ollama` |
| `OLLAMA_BASE_URL` | Ollama 地址（LLM_PROVIDER=ollama 时需要） | `http://localhost:11434` |
| `OLLAMA_CHAT_MODEL` | Ollama 对话模型 | `qwen2` |
| `OLLAMA_EMBED_MODEL` | Ollama 向量模型 | `nomic-embed-text` |
| `MOCK_MODE` | 是否模拟模式 | `false` |
| `QDRANT_HOST` | Qdrant 地址 | `localhost` |
| `QDRANT_PORT` | Qdrant 端口 | `6333` |
| `UPLOAD_PATH` | 文件上传路径 | `./uploads` |
| `UPLOAD_MAX_SIZE` | 上传大小限制 | `50MB` |
| `UPLOAD_ALLOWED_TYPES` | 允许的文件类型 | `pdf,doc,docx,...` |
| `SERVER_PORT` | 服务端口 | `8080` |
| `LOG_LEVEL` | 应用日志级别 | `INFO` |
| `SECURITY_LOG_LEVEL` | 安全日志级别 | `WARN` |
| `INIT_ADMIN_PASSWORD` | 初始管理员密码 | `admin123` |
| `INIT_EDITOR_PASSWORD` | 初始编辑密码 | `admin123` |
| `INIT_USER_PASSWORD` | 初始普通用户密码 | `admin123` |
| `JASYPT_ENCRYPTOR_PASSWORD` | Jasypt 主密钥（仅当使用 `ENC()` 密文时需要） | *(可选)* |

### 方式一：Docker Compose（推荐，使用 .env 文件）

### macOS / Linux

```bash
# 1. 创建环境配置
cp .env.example .env

# 2. 设置必须的环境变量（将 <你的主密钥> 替换为实际值）
sed -i '' \
  -e 's|JASYPT_ENCRYPTOR_PASSWORD=.*|JASYPT_ENCRYPTOR_PASSWORD=<你的主密钥>|' \
  -e 's|DB_ROOT_PASSWORD=.*|DB_ROOT_PASSWORD=root_password|' \
  -e 's|INIT_ADMIN_PASSWORD=.*|INIT_ADMIN_PASSWORD=admin123|' \
  -e 's|INIT_EDITOR_PASSWORD=.*|INIT_EDITOR_PASSWORD=admin123|' \
  -e 's|INIT_USER_PASSWORD=.*|INIT_USER_PASSWORD=admin123|' \
  .env

# 3. 启动基础服务（数据库、缓存等）
docker compose up -d mysql redis elasticsearch qdrant

# 4. 等待服务就绪（约30秒）
sleep 30

# 5. 初始化数据（Elasticsearch 索引、Qdrant 集合等）
cd ai-service
pip install -r requirements.txt
python scripts/init_elasticsearch.py
python scripts/init_qdrant.py
python scripts/init_redis.py
cd ..

# 6. 启动应用服务
#    Flyway 会在后端启动时自动执行数据库迁移（建表 + 种子数据）
#    DataInitializer 会自动创建初始用户（admin/editor/user1）
docker compose up -d
```

### Windows（PowerShell）

```powershell
# 1. 创建环境配置
Copy-Item .env.example .env

# 2. 设置必须的环境变量（将 <你的主密钥> 替换为实际值）
(Get-Content .env) `
  -replace 'JASYPT_ENCRYPTOR_PASSWORD=.*', 'JASYPT_ENCRYPTOR_PASSWORD=<你的主密钥>' `
  -replace 'DB_ROOT_PASSWORD=.*', 'DB_ROOT_PASSWORD=root_password' `
  -replace 'INIT_ADMIN_PASSWORD=.*', 'INIT_ADMIN_PASSWORD=admin123' `
  -replace 'INIT_EDITOR_PASSWORD=.*', 'INIT_EDITOR_PASSWORD=admin123' `
  -replace 'INIT_USER_PASSWORD=.*', 'INIT_USER_PASSWORD=admin123' `
  | Set-Content .env

# 3. 启动基础服务（数据库、缓存等）
docker compose up -d mysql redis elasticsearch qdrant

# 4. 等待服务就绪（约30秒）
Start-Sleep -Seconds 30

# 5. 初始化数据（Elasticsearch 索引、Qdrant 集合等）
cd ai-service
pip install -r requirements.txt
python scripts/init_elasticsearch.py
python scripts/init_qdrant.py
python scripts/init_redis.py
cd ..

# 6. 启动应用服务
docker compose up -d
```

### 方式二：本地开发（不使用 Docker）

> 前提：本地已安装并启动 MySQL、Redis、Elasticsearch、Qdrant。

#### macOS / Linux

```bash
# ---- 数据库 ----
export DB_HOST=localhost
export DB_PORT=3306
export DB_DATABASE=knowledge_base
export DB_USERNAME=kb_user
export DB_PASSWORD=kb_password

# ---- Redis ----
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=redis_password

# ---- Elasticsearch ----
export ES_HOST=localhost
export ES_PORT=9200
export ES_USERNAME=
export ES_PASSWORD=

# ---- JWT ----
export JWT_SECRET=dev-secret-key-for-local-testing-only
export JWT_EXPIRATION=86400

# ---- AI 服务 ----
export AI_SERVICE_URL=http://localhost:8000

# ---- LLM 配置 ----
export LLM_PROVIDER=ollama
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_CHAT_MODEL=qwen2
export OLLAMA_EMBED_MODEL=nomic-embed-text
export MOCK_MODE=false

# ---- Qdrant ----
export QDRANT_HOST=localhost
export QDRANT_PORT=6333

# ---- 文件上传 ----
export UPLOAD_PATH=./uploads
export UPLOAD_MAX_SIZE=50MB
export UPLOAD_ALLOWED_TYPES=pdf,doc,docx,xlsx,ppt,pptx,txt,md

# ---- 服务器 ----
export SERVER_PORT=8080
export LOG_LEVEL=INFO
export SECURITY_LOG_LEVEL=WARN

# ---- 初始用户密码 ----
export INIT_ADMIN_PASSWORD=admin123
export INIT_EDITOR_PASSWORD=admin123
export INIT_USER_PASSWORD=admin123

# ---- Jasypt（仅当环境变量值中使用 ENC() 密文时需要）----
# export JASYPT_ENCRYPTOR_PASSWORD=kb-demo-2026

# 启动后端（Flyway 自动建表 + DataInitializer 创建初始用户）
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -f backend/pom.xml spring-boot:run
```

#### Windows（PowerShell）

```powershell
# ---- 数据库 ----
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_DATABASE = "knowledge_base"
$env:DB_USERNAME = "kb_user"
$env:DB_PASSWORD = "kb_password"

# ---- Redis ----
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
$env:REDIS_PASSWORD = "redis_password"

# ---- Elasticsearch ----
$env:ES_HOST = "localhost"
$env:ES_PORT = "9200"
$env:ES_USERNAME = ""
$env:ES_PASSWORD = ""

# ---- JWT ----
$env:JWT_SECRET = "dev-secret-key-for-local-testing-only"
$env:JWT_EXPIRATION = "86400"

# ---- AI 服务 ----
$env:AI_SERVICE_URL = "http://localhost:8000"

# ---- LLM 配置 ----
$env:LLM_PROVIDER = "ollama"
$env:OLLAMA_BASE_URL = "http://localhost:11434"
$env:OLLAMA_CHAT_MODEL = "qwen2"
$env:OLLAMA_EMBED_MODEL = "nomic-embed-text"
$env:MOCK_MODE = "false"

# ---- Qdrant ----
$env:QDRANT_HOST = "localhost"
$env:QDRANT_PORT = "6333"

# ---- 文件上传 ----
$env:UPLOAD_PATH = "./uploads"
$env:UPLOAD_MAX_SIZE = "50MB"
$env:UPLOAD_ALLOWED_TYPES = "pdf,doc,docx,xlsx,ppt,pptx,txt,md"

# ---- 服务器 ----
$env:SERVER_PORT = "8080"
$env:LOG_LEVEL = "INFO"
$env:SECURITY_LOG_LEVEL = "WARN"

# ---- 初始用户密码 ----
$env:INIT_ADMIN_PASSWORD = "admin123"
$env:INIT_EDITOR_PASSWORD = "admin123"
$env:INIT_USER_PASSWORD = "admin123"

# ---- Jasypt（仅当环境变量值中使用 ENC() 密文时需要）----
# $env:JASYPT_ENCRYPTOR_PASSWORD = "kb-demo-2026"

# 启动后端（需先设置 JAVA_HOME 指向 JDK 17）
cd backend
mvn spring-boot:run
```

## 访问地址

启动成功后，访问以下地址：

| 服务              | 地址                           | 说明          |
|-------------------|-------------------------------|--------------|
| 🌐 前端首页       | http://localhost              | 主界面        |
| 📡 后端 API       | http://localhost:8080         | REST API     |
| 🤖 AI 服务        | http://localhost:8000/docs    | Swagger文档   |
| 🔍 ES 管理        | http://localhost:9200         | Elasticsearch |

## 默认账号

> 仅在首次启动（用户表为空）时自动创建。密码可通过 `.env` 中的 `INIT_ADMIN_PASSWORD` 等变量修改。

| 用户名    | 默认密码  | 角色     | 说明        |
|----------|----------|----------|------------|
| admin    | admin123 | 管理员    | 拥有所有权限  |
| editor   | admin123 | 编辑      | 可管理文档    |
| user1    | admin123 | 普通用户  | 基本使用权限  |

## 功能演示路径

### 1. 浏览文档
```
首页 → 看到推荐文档和分类
点击分类 → 浏览该分类下的文档
点击文档 → 查看文档详情
```

### 2. 搜索功能
```
首页搜索框 → 输入"年假" → 看到搜索结果
→ 点击结果查看文档详情
```

### 3. AI 智能问答
```
导航栏 → AI问答 → 进入对话界面
→ 输入"如何申请年假？" → 获得AI回答
→ 点击推荐问题继续对话
```

### 4. 管理后台
```
使用 admin 账号登录
→ 导航栏 → 管理后台
→ 查看统计数据和用户管理
```

## AI 服务模式

### 模拟模式（默认，无需API Key）
```env
MOCK_MODE=true
```
使用预定义问答对，适合演示和学习。

### 真实模式（需要API Key）
```env
MOCK_MODE=false
LLM_PROVIDER=openai     # 或 anthropic
OPENAI_API_KEY=sk-xxx   # 或 ANTHROPIC_API_KEY
```
使用真实大语言模型，获得更智能的回答。

## 常用命令

```bash
# 查看服务状态
docker compose ps

# 查看服务日志
docker compose logs -f backend       # 后端日志
docker compose logs -f ai-service    # AI服务日志
docker compose logs -f mysql         # 数据库日志

# 停止所有服务
docker compose down

# 停止并删除数据（重置）
docker compose down -v

# 重新构建并启动
docker compose up -d --build

# 健康检查
chmod +x docs/scripts/health-check.sh
./docs/scripts/health-check.sh
```

## 常见问题

### Q: 端口被占用怎么办？
```bash
# 查看占用进程
lsof -i :端口号

# 修改 docker-compose.yml 中的端口映射
```

### Q: MySQL 启动失败？
```bash
# 检查数据目录权限
sudo chown -R 999:999 mysql-data

# 或清除数据重新启动
docker compose down -v
docker compose up -d mysql
```

### Q: Elasticsearch 内存不足？
```bash
# 修改 docker-compose.yml 中的 ES_JAVA_OPTS
- "ES_JAVA_OPTS=-Xms256m -Xmx256m"
```

### Q: 如何重置所有数据？
```bash
docker compose down -v    # 删除所有容器和数据卷
docker compose up -d       # 重新启动，Flyway 会在后端启动时自动重建表结构和种子数据
```

---

**文档版本：** v1.2
**最后更新：** 2026-06-08
