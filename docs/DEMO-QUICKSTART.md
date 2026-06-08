# 🚀 快速开始指南

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

**文档版本：** v1.1
**最后更新：** 2026-06-08
