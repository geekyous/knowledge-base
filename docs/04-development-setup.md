# 企业知识库问答系统 - 开发环境搭建

## 📋 环境要求

### 硬件要求
- CPU: 4 核心以上
- 内存: 8GB 以上（推荐 16GB）
- 硬盘: 20GB 可用空间

### 软件要求
| 软件 | 版本 | 说明 |
|------|------|------|
| Node.js | 18.0+ | 前端开发环境 |
| npm | 9.0+ | 前端包管理 |
| Java | 17+ | 后端开发环境 |
| Maven | 3.8+ | Java 构建工具 |
| Python | 3.11+ | AI 服务环境 |
| Docker | 24.0+ | 容器化部署 |
| Docker Compose | 2.20+ | 容器编排 |
| Git | 2.30+ | 版本控制 |
| IDE | - | 推荐 IntelliJ IDEA + VS Code |

## 🚀 快速开始（Docker 方式）

### 前置条件
1. 安装 Docker 和 Docker Compose
2. 克隆项目代码

### 一键启动

```bash
# 克隆项目
git clone <repository-url>
cd knowledge-base

# 复制环境变量文件
cp .env.example .env

# 启动所有服务
docker-compose -f docker-compose.dev.yml up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 访问服务
| 服务 | 地址 | 说明 |
|------|------|------|
| 前端应用 | http://localhost:5173 | Vue 开发服务器 |
| 后端 API | http://localhost:8080 | Spring Boot 应用 |
| AI 服务 | http://localhost:8000 | FastAPI 应用 |
| API 文档 | http://localhost:8000/docs | FastAPI Swagger UI |

## 🔧 手动安装方式

### 1. 安装 Node.js 和 npm

#### macOS
```bash
# 使用 Homebrew
brew install node@18

# 验证安装
node --version
npm --version
```

#### Windows
下载安装包：https://nodejs.org/

#### Linux
```bash
# Ubuntu/Debian
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# 验证安装
node --version
npm --version
```

### 2. 安装 Java 和 Maven

#### macOS
```bash
# 使用 Homebrew
brew install openjdk@17
brew install maven

# 设置环境变量
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# 验证安装
java -version
mvn -version
```

#### Windows
1. 下载 JDK 17: https://www.oracle.com/java/technologies/downloads/
2. 下载 Maven: https://maven.apache.org/download.cgi
3. 配置环境变量

#### Linux
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install -y openjdk-17-jdk maven

# 验证安装
java -version
mvn -version
```

### 3. 安装 Python 和 pip

#### macOS
```bash
# 使用 Homebrew
brew install python@3.11

# 验证安装
python3 --version
pip3 --version
```

#### Windows
下载安装包：https://www.python.org/downloads/

#### Linux
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install -y python3.11 python3-pip

# 验证安装
python3 --version
pip3 --version
```

### 4. 安装 Docker

#### macOS
```bash
# 下载 Docker Desktop
# https://www.docker.com/products/docker-desktop/

# 验证安装
docker --version
docker-compose --version
```

#### Windows
下载 Docker Desktop: https://www.docker.com/products/docker-desktop/

#### Linux
```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker --version
docker-compose --version
```

## 📦 项目依赖安装

### 前端依赖

```bash
cd frontend

# 安装依赖
npm install

# 推荐使用 cnpm 加速（可选）
npm install -g cnpm --registry=https://registry.npmmirror.com
cnpm install
```

### 后端依赖

```bash
cd backend

# Maven 自动下载依赖
mvn clean install

# 或者使用 IDE 打开项目，IDE 会自动下载
```

### AI 服务依赖

```bash
cd ai-service

# 创建虚拟环境（推荐）
python3 -m venv venv

# 激活虚拟环境
# macOS/Linux:
source venv/bin/activate
# Windows:
venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt

# 使用国内镜像加速（可选）
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
```

## 🗄️ 数据库安装

### 使用 Docker（推荐）

```bash
# 启动 MySQL
docker run -d \
  --name kb-mysql \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -e MYSQL_DATABASE=knowledge_base \
  -p 3306:3306 \
  mysql:8.0

# 启动 Redis
docker run -d \
  --name kb-redis \
  -p 6379:6379 \
  redis:7-alpine

# 启动 Elasticsearch
docker run -d \
  --name kb-elasticsearch \
  -p 9200:9200 \
  -p 9300:9300 \
  -e "discovery.type=single-node" \
  elasticsearch:8.19.16

# 启动 Qdrant
docker run -d \
  --name kb-qdrant \
  -p 6333:6333 \
  qdrant/qdrant:latest
```

### 本地安装

#### MySQL
- macOS: `brew install mysql`
- Windows: 下载安装包
- Linux: `sudo apt install mysql-server`

#### Redis
- macOS: `brew install redis`
- Windows: 下载 Windows 版本
- Linux: `sudo apt install redis-server`

## ⚙️ 环境配置

### 1. 创建环境变量文件

```bash
# 在项目根目录
cp .env.example .env
```

### 2. 编辑 .env 文件

```bash
# 应用配置
APP_NAME=KnowledgeBase
APP_ENV=development
APP_DEBUG=true

# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_DATABASE=knowledge_base
DB_USERNAME=root
DB_PASSWORD=root123            # 支持明文或 ENC(密文) 格式

# Jasypt 主密钥（用于解密 application.yml 中的 ENC() 配置值）
JASYPT_ENCRYPTOR_PASSWORD=your-master-key

# Redis 配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=                # 支持明文或 ENC(密文) 格式

# Elasticsearch 配置
ES_HOST=localhost
ES_PORT=9200
ES_USERNAME=
ES_PASSWORD=

# JWT 配置
JWT_SECRET=your-secret-key-here  # 支持明文或 ENC(密文) 格式
JWT_EXPIRATION=86400

# AI 服务配置
AI_SERVICE_URL=http://localhost:8000
OPENAI_API_KEY=your-openai-api-key
ANTHROPIC_API_KEY=your-anthropic-api-key

# Qdrant 配置
QDRANT_HOST=localhost
QDRANT_PORT=6333

# 文件上传配置
UPLOAD_MAX_SIZE=50MB
UPLOAD_ALLOWED_TYPES=pdf,doc,docx,xlsx,ppt,pptx,txt,md
```

## 🚀 启动开发服务

### 启动前端

```bash
cd frontend
npm run dev

# 访问 http://localhost:5173
```

### 启动后端

> **⚠️ 必读**：`application.yml` 中所有配置项均无默认值，**必须通过环境变量注入**。
> Docker Compose 方式会在 `docker-compose.yml` 中提供合理默认值；本地开发需手动 export。
> 若所有环境变量均为明文，则无需设置 `JASYPT_ENCRYPTOR_PASSWORD`。

#### 必须设置的环境变量

| 变量 | 说明 | 示例值 |
|------|------|--------|
| `JAVA_HOME` | JDK 17 路径（系统默认 JDK 8 会导致编译失败） | 见下方各平台说明 |
| `DB_HOST` | MySQL 地址 | `localhost` |
| `DB_PORT` | MySQL 端口 | `3306` |
| `DB_DATABASE` | 数据库名 | `knowledge_base` |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | `root_password` |
| `REDIS_HOST` | Redis 地址 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `ES_HOST` | Elasticsearch 地址 | `localhost` |
| `ES_PORT` | Elasticsearch 端口 | `9200` |
| `JWT_SECRET` | JWT 签名密钥（≥32字符） | `your-secret-key-...` |
| `JWT_EXPIRATION` | JWT 过期时间（秒） | `86400` |
| `AI_SERVICE_URL` | AI 服务地址 | `http://localhost:8000` |
| `UPLOAD_PATH` | 文件上传路径 | `./uploads` |
| `UPLOAD_MAX_SIZE` | 上传大小限制 | `50MB` |
| `UPLOAD_ALLOWED_TYPES` | 允许的文件类型 | `pdf,doc,docx,...` |
| `SERVER_PORT` | 服务端口 | `8080` |
| `LOG_LEVEL` | 应用日志级别 | `INFO` |
| `SECURITY_LOG_LEVEL` | 安全日志级别 | `INFO` |
| `INIT_ADMIN_PASSWORD` | 初始管理员密码 | `admin123` |
| `INIT_EDITOR_PASSWORD` | 初始编辑密码 | `admin123` |
| `INIT_USER_PASSWORD` | 初始普通用户密码 | `admin123` |

#### 可选环境变量

| 变量 | 说明 | 何时需要 |
|------|------|----------|
| `JASYPT_ENCRYPTOR_PASSWORD` | Jasypt 主密钥 | 仅当环境变量值中使用 `ENC()` 密文时 |
| `REDIS_PASSWORD` | Redis 密码 | Redis 启用了认证时设置，否则留空 |
| `ES_USERNAME` / `ES_PASSWORD` | ES 认证 | ES 启用了安全认证时设置，否则留空 |

#### macOS / Linux

**临时设置（当前终端有效）：**

```bash
# ---- 数据库 ----
export DB_HOST=localhost
export DB_PORT=3306
export DB_DATABASE=knowledge_base
export DB_USERNAME=root
export DB_PASSWORD=你的数据库密码

# ---- Redis ----
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=

# ---- Elasticsearch ----
export ES_HOST=localhost
export ES_PORT=9200
export ES_USERNAME=
export ES_PASSWORD=

# ---- JWT ----
export JWT_SECRET=your-secret-key-must-be-at-least-32-characters-long
export JWT_EXPIRATION=86400

# ---- AI 服务 ----
export AI_SERVICE_URL=http://localhost:8000

# ---- 文件上传 ----
export UPLOAD_PATH=./uploads
export UPLOAD_MAX_SIZE=50MB
export UPLOAD_ALLOWED_TYPES=pdf,doc,docx,xlsx,ppt,pptx,txt,md

# ---- 服务器 ----
export SERVER_PORT=8080
export LOG_LEVEL=INFO
export SECURITY_LOG_LEVEL=INFO

# ---- 初始用户密码 ----
export INIT_ADMIN_PASSWORD=admin123
export INIT_EDITOR_PASSWORD=admin123
export INIT_USER_PASSWORD=admin123

# ---- Jasypt（仅当使用 ENC() 密文时需要）----
# export JASYPT_ENCRYPTOR_PASSWORD=你的主密钥

export JAVA_HOME=$(/usr/libexec/java_home -v 17)  # macOS
# export JAVA_HOME=/usr/lib/jvm/java-17           # Linux

cd backend
mvn spring-boot:run
```

**持久化（写入 shell 配置）：**

```bash
# 将上述 export 命令追加到 shell 配置文件中
# Zsh（macOS 默认）
cat >> ~/.zshrc << 'EOF'
export DB_HOST=localhost
export DB_PORT=3306
export DB_DATABASE=knowledge_base
export DB_USERNAME=root
export DB_PASSWORD=你的数据库密码
export REDIS_HOST=localhost
export REDIS_PORT=6379
export ES_HOST=localhost
export ES_PORT=9200
export JWT_SECRET=your-secret-key-must-be-at-least-32-characters-long
export JWT_EXPIRATION=86400
export AI_SERVICE_URL=http://localhost:8000
export UPLOAD_PATH=./uploads
export UPLOAD_MAX_SIZE=50MB
export UPLOAD_ALLOWED_TYPES=pdf,doc,docx,xlsx,ppt,pptx,txt,md
export SERVER_PORT=8080
export LOG_LEVEL=INFO
export SECURITY_LOG_LEVEL=INFO
export INIT_ADMIN_PASSWORD=admin123
export INIT_EDITOR_PASSWORD=admin123
export INIT_USER_PASSWORD=admin123
EOF
source ~/.zshrc
```

#### Windows

**PowerShell（临时设置）：**

```powershell
# ---- 数据库 ----
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_DATABASE = "knowledge_base"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的数据库密码"

# ---- Redis ----
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
$env:REDIS_PASSWORD = ""

# ---- Elasticsearch ----
$env:ES_HOST = "localhost"
$env:ES_PORT = "9200"
$env:ES_USERNAME = ""
$env:ES_PASSWORD = ""

# ---- JWT ----
$env:JWT_SECRET = "your-secret-key-must-be-at-least-32-characters-long"
$env:JWT_EXPIRATION = "86400"

# ---- AI 服务 ----
$env:AI_SERVICE_URL = "http://localhost:8000"

# ---- 文件上传 ----
$env:UPLOAD_PATH = "./uploads"
$env:UPLOAD_MAX_SIZE = "50MB"
$env:UPLOAD_ALLOWED_TYPES = "pdf,doc,docx,xlsx,ppt,pptx,txt,md"

# ---- 服务器 ----
$env:SERVER_PORT = "8080"
$env:LOG_LEVEL = "INFO"
$env:SECURITY_LOG_LEVEL = "INFO"

# ---- 初始用户密码 ----
$env:INIT_ADMIN_PASSWORD = "admin123"
$env:INIT_EDITOR_PASSWORD = "admin123"
$env:INIT_USER_PASSWORD = "admin123"

# ---- Jasypt（仅当使用 ENC() 密文时需要）----
# $env:JASYPT_ENCRYPTOR_PASSWORD = "你的主密钥"

$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"  # 替换为实际 JDK 17 路径

cd backend
mvn spring-boot:run
```

**CMD（临时设置）：**

```cmd
set DB_HOST=localhost
set DB_PORT=3306
set DB_DATABASE=knowledge_base
set DB_USERNAME=root
set DB_PASSWORD=你的数据库密码
set REDIS_HOST=localhost
set REDIS_PORT=6379
set ES_HOST=localhost
set ES_PORT=9200
set JWT_SECRET=your-secret-key-must-be-at-least-32-characters-long
set JWT_EXPIRATION=86400
set AI_SERVICE_URL=http://localhost:8000
set UPLOAD_PATH=./uploads
set UPLOAD_MAX_SIZE=50MB
set UPLOAD_ALLOWED_TYPES=pdf,doc,docx,xlsx,ppt,pptx,txt,md
set SERVER_PORT=8080
set LOG_LEVEL=INFO
set SECURITY_LOG_LEVEL=INFO
set INIT_ADMIN_PASSWORD=admin123
set INIT_EDITOR_PASSWORD=admin123
set INIT_USER_PASSWORD=admin123
set JAVA_HOME=C:\Program Files\Java\jdk-17

cd backend
mvn spring-boot:run
```

#### IDE 配置（IntelliJ IDEA）

使用 IDE 运行 `KnowledgeBaseApplication` 时，需在 Run Configuration 中设置环境变量：

1. **Run → Edit Configurations**
2. 选择 `KnowledgeBaseApplication`
3. **Environment variables** 填入（按需修改值）：
   ```
   DB_HOST=localhost;DB_PORT=3306;DB_DATABASE=knowledge_base;DB_USERNAME=root;DB_PASSWORD=你的数据库密码;REDIS_HOST=localhost;REDIS_PORT=6379;ES_HOST=localhost;ES_PORT=9200;JWT_SECRET=your-secret-key-must-be-at-least-32-characters-long;JWT_EXPIRATION=86400;AI_SERVICE_URL=http://localhost:8000;UPLOAD_PATH=./uploads;UPLOAD_MAX_SIZE=50MB;UPLOAD_ALLOWED_TYPES=pdf,doc,docx,xlsx,ppt,pptx,txt,md;SERVER_PORT=8080;LOG_LEVEL=INFO;SECURITY_LOG_LEVEL=INFO;INIT_ADMIN_PASSWORD=admin123;INIT_EDITOR_PASSWORD=admin123;INIT_USER_PASSWORD=admin123
   ```
4. **Build and run using** 确认使用 JDK 17（Project Structure → SDK 选择 17）

### 启动 AI 服务

```bash
cd ai-service

# 激活虚拟环境
source venv/bin/activate

# 启动服务
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# 访问 http://localhost:8000
# API 文档 http://localhost:8000/docs
```

## 🛠️ 开发工具推荐

### IDE 配置

#### IntelliJ IDEA（后端）
1. 安装 Lombok 插件
2. 安装 MyBatis 插件
3. 配置 Maven 仓库

#### VS Code（前端）
推荐扩展：
```json
{
  "recommendations": [
    "Vue.volar",
    "dbaeumer.vscode-eslint",
    "esbenp.prettier-vscode",
    "bradlc.vscode-tailwindcss",
    "mikestead.dotenv"
  ]
}
```

#### PyCharm（AI 服务）
1. 配置 Python 解释器
2. 安装 Python 插件

### Git 配置

```bash
# 配置用户信息
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# 配置分支策略
git config --global init.defaultbranch main
git config --global pull.rebase false
```

## 🧪 测试环境

### 前端测试

```bash
cd frontend
npm run test
npm run test:e2e
```

### 后端测试

```bash
cd backend
mvn test
```

### AI 服务测试

```bash
cd ai-service
pytest
```

## 🐛 常见问题

### 1. 端口冲突

```bash
# 查看端口占用
lsof -i :5173  # macOS/Linux
netstat -ano | findstr :5173  # Windows

# 修改端口
# 前端: vite.config.ts
# 后端: application.yml
# AI 服务: 启动命令参数
```

### 2. 依赖安装失败

```bash
# 清理缓存
npm cache clean --force
mvn clean
pip cache purge

# 使用镜像加速
npm config set registry https://registry.npmmirror.com
```

### 3. 数据库连接失败

```bash
# 检查数据库状态
docker ps
mysql -u root -p

# 检查防火墙
sudo ufw status
```

### 4. Docker 内存不足

```bash
# 增加内存限制
# Docker Desktop > Settings > Resources > Memory
```

## 📚 学习资源

### 官方文档
- [Vue 3 文档](https://cn.vuejs.org/)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)
- [FastAPI 文档](https://fastapi.tiangolo.com/zh/)
- [Docker 文档](https://docs.docker.com/)

### 推荐教程
- Vue 3 组合式 API
- Spring Boot 实战
- LangChain 开发指南

---

**文档版本：** v1.1
**最后更新：** 2026-06-08
