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

> **⚠️ 必读**：`application.yml` 中数据库密码、JWT 密钥等使用 Jasypt `ENC()` 加密。
> 本地运行 **必须** 设置 `JASYPT_ENCRYPTOR_PASSWORD` 环境变量，否则启动报错。
> 主密钥值参考项目 `.env` 文件中的 `JASYPT_ENCRYPTOR_PASSWORD`。

#### 必须设置的环境变量

| 变量 | 说明 | 示例值 |
|------|------|--------|
| `JASYPT_ENCRYPTOR_PASSWORD` | Jasypt 主密钥，解密 `ENC()` 配置值 | 项目 `.env` 中的值 |
| `JAVA_HOME` | JDK 17 路径（系统默认 JDK 8 会导致编译失败） | 见下方各平台说明 |

#### 可选环境变量（不设置则使用 `application.yml` 中的默认值）

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_HOST` | `localhost` | MySQL 地址 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_DATABASE` | `knowledge_base` | 数据库名 |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | `ENC(加密值)` | 数据库密码（支持明文或 `ENC(密文)`） |
| `REDIS_HOST` | `localhost` | Redis 地址 |
| `ES_HOST` | `localhost` | Elasticsearch 地址 |
| `JWT_SECRET` | `ENC(加密值)` | JWT 签名密钥 |
| `INIT_ADMIN_PASSWORD` | `admin123` | 初始管理员密码（仅首次启动时使用） |

#### macOS / Linux

**临时设置（当前终端有效）：**

```bash
# Bash / Zsh 通用
export JASYPT_ENCRYPTOR_PASSWORD=你的主密钥
export JAVA_HOME=$(/usr/libexec/java_home -v 17)  # macOS
# export JAVA_HOME=/usr/lib/jvm/java-17           # Linux

cd backend
mvn spring-boot:run
```

**单次运行（仅该命令有效）：**

```bash
JASYPT_ENCRYPTOR_PASSWORD=你的主密钥 \
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
mvn -f backend/pom.xml spring-boot:run
```

**持久化（写入 shell 配置）：**

```bash
# Zsh（macOS 默认）
echo 'export JASYPT_ENCRYPTOR_PASSWORD=你的主密钥' >> ~/.zshrc
source ~/.zshrc

# Bash（Linux 常见）
echo 'export JASYPT_ENCRYPTOR_PASSWORD=你的主密钥' >> ~/.bashrc
source ~/.bashrc
```

#### Windows

**PowerShell（临时设置）：**

```powershell
$env:JASYPT_ENCRYPTOR_PASSWORD = "你的主密钥"
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"  # 替换为实际 JDK 17 路径

cd backend
mvn spring-boot:run
```

**CMD（临时设置）：**

```cmd
set JASYPT_ENCRYPTOR_PASSWORD=你的主密钥
set JAVA_HOME=C:\Program Files\Java\jdk-17

cd backend
mvn spring-boot:run
```

**Windows 持久化（系统环境变量）：**

```powershell
# 以当前用户级别永久设置（需重启终端生效）
[System.Environment]::SetEnvironmentVariable("JASYPT_ENCRYPTOR_PASSWORD", "你的主密钥", "User")
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-17", "User")
```

或者通过 **系统设置**：右键"此电脑" → 属性 → 高级系统设置 → 环境变量 → 新建用户变量。

#### IDE 配置（IntelliJ IDEA）

使用 IDE 运行 `KnowledgeBaseApplication` 时，需在 Run Configuration 中设置环境变量：

1. **Run → Edit Configurations**
2. 选择 `KnowledgeBaseApplication`
3. **Environment variables** 填入：
   ```
   JASYPT_ENCRYPTOR_PASSWORD=你的主密钥
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
