# 企业知识库问答系统 - 部署指南

## 📋 部署概览

### 部署架构

```
┌─────────────────────────────────────────────────┐
│              Nginx (反向代理)                     │
│         SSL终止 • 负载均衡 • 静态资源             │
└─────────────────────────────────────────────────┘
                    ↓
        ┌───────────┴───────────┐
        ↓                       ↓
┌──────────────┐        ┌──────────────┐
│   前端静态    │        │   后端API    │
│   (Vue 3)    │        │ (Spring Boot)│
└──────────────┘        └──────────────┘
                               ↓
┌──────────────┬──────────────┬──────────────┬──────────────┐
│    MySQL     │    Redis     │Elasticsearch│   AI 服务     │
│   (RDS/Master)│  (缓存)      │   (搜索)     │  (Python)     │
└──────────────┴──────────────┴──────────────┴──────────────┘
                                       ↓
                               ┌──────────────┐
                               │   Qdrant     │
                               │(向量数据库)   │
                               └──────────────┘
```

## 🐳 Docker 部署

### 1. 环境准备

#### 安装 Docker 和 Docker Compose

```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 2. 配置文件准备

#### 环境变量配置 (.env)

```bash
# 应用配置
APP_NAME=KnowledgeBase
APP_ENV=production
APP_DEBUG=false

# 数据库配置
DB_HOST=mysql
DB_PORT=3306
DB_DATABASE=knowledge_base
DB_USERNAME=kb_user
DB_PASSWORD=secure_password_here        # 支持明文或 ENC(密文) 格式

# Jasypt 主密钥（用于解密 ENC() 配置值，⚠️ 勿提交到代码仓库）
JASYPT_ENCRYPTOR_PASSWORD=your-master-key

# Redis 配置
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=                          # 支持明文或 ENC(密文) 格式

# Elasticsearch 配置
ES_HOST=elasticsearch
ES_PORT=9200
ES_USERNAME=elastic
ES_PASSWORD=elastic_password

# JWT 配置
JWT_SECRET=your-production-secret-key-here
JWT_EXPIRATION=86400

# AI 服务配置
AI_SERVICE_URL=http://ai-service:8000
OPENAI_API_KEY=your-openai-api-key
ANTHROPIC_API_KEY=your-anthropic-api-key

# Qdrant 配置
QDRANT_HOST=qdrant
QDRANT_PORT=6333

# 文件上传配置
UPLOAD_MAX_SIZE=50MB
UPLOAD_PATH=/app/uploads
```

### 3. Docker Compose 配置

#### 生产环境配置 (docker-compose.yml)

```yaml
version: '3.8'

services:
  # Nginx 反向代理
  nginx:
    image: nginx:1.25-alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./deploy/nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./deploy/nginx/ssl:/etc/nginx/ssl
      - ./frontend/dist:/usr/share/nginx/html
    depends_on:
      - backend
    networks:
      - app-network
    restart: unless-stopped

  # 后端服务
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_DATABASE=${DB_DATABASE}
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - ES_HOST=elasticsearch
      - ES_PORT=9200
      - JASYPT_ENCRYPTOR_PASSWORD=${JASYPT_ENCRYPTOR_PASSWORD}
    depends_on:
      - mysql
      - redis
      - elasticsearch
    networks:
      - app-network
    restart: unless-stopped

  # AI 服务
  ai-service:
    build:
      context: ./ai-service
      dockerfile: Dockerfile
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
      - QDRANT_HOST=qdrant
      - QDRANT_PORT=6333
    depends_on:
      - qdrant
    networks:
      - app-network
    restart: unless-stopped

  # MySQL 数据库
  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=${DB_ROOT_PASSWORD}
      - MYSQL_DATABASE=${DB_DATABASE}
      - MYSQL_USER=${DB_USERNAME}
      - MYSQL_PASSWORD=${DB_PASSWORD}
    volumes:
      - mysql-data:/var/lib/mysql
      # 数据库迁移由 Flyway 管理，无需挂载 initdb 脚本
    networks:
      - app-network
    restart: unless-stopped

  # Redis 缓存
  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data
    networks:
      - app-network
    restart: unless-stopped

  # Elasticsearch
  elasticsearch:
    image: elasticsearch:8.19.16
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
      - ELASTIC_PASSWORD=${ES_PASSWORD}
    volumes:
      - elasticsearch-data:/usr/share/elasticsearch/data
    networks:
      - app-network
    restart: unless-stopped

  # Qdrant 向量数据库
  qdrant:
    image: qdrant/qdrant:latest
    ports:
      - "6333:6333"
    volumes:
      - qdrant-data:/qdrant/storage
    networks:
      - app-network
    restart: unless-stopped

networks:
  app-network:
    driver: bridge

volumes:
  mysql-data:
  redis-data:
  elasticsearch-data:
  qdrant-data:
```

### 4. 构建和启动

```bash
# 构建镜像
docker-compose build

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 查看服务状态
docker-compose ps
```

## 🖥️ 生产服务器配置

### 服务资源消耗分析

系统共 8 个 Docker 服务，各服务内存占用估算：

| 服务 | 内存占用 | 说明 |
|------|---------|------|
| Ollama（qwen2 + embed 模型） | 4-8 GB | 最大的资源消耗者，模型加载到内存 |
| Elasticsearch | 1-2 GB | JVM 堆，可通过 `ES_JAVA_OPTS` 调节 |
| MySQL | 1-2 GB | `innodb_buffer_pool_size` 决定 |
| Spring Boot 后端 | 1-2 GB | JVM 堆，`-Xmx` 参数控制 |
| AI 服务（FastAPI） | 0.5-1 GB | Python 进程 + LangChain |
| Qdrant | 0.5-1 GB | 向量数据量决定 |
| Redis | 0.5 GB | 缓存数据量决定 |
| Nginx | ~50 MB | 极轻量 |
| **合计** | **~12-16 GB** | 不含操作系统开销 |

### 磁盘空间明细

| 数据类型 | 预估大小 | 存储位置 |
|----------|---------|---------|
| Ollama 模型（qwen2 ~4GB + nomic-embed-text ~300MB） | ~5 GB | `ollama-data` volume |
| MySQL 数据库 | 1-10 GB | `mysql-data` volume |
| Elasticsearch 索引 | 1-5 GB | `elasticsearch-data` volume |
| Qdrant 向量数据 | 1-5 GB | `qdrant-data` volume |
| Docker 镜像 | ~5 GB | `/var/lib/docker` |
| 日志 + 临时文件 | 2-5 GB | 宿主机 |
| **合计** | **~20-35 GB** | 建议预留 50% 余量 |

### 方案一：单机部署（本地 LLM）

> 适用场景：小团队 / 内部系统，≤50 并发用户，数据不出内网。

| 配置项 | 最低要求 | 推荐配置 |
|--------|---------|---------|
| **CPU** | 4 核 | **8 核**（Ollama 推理是 CPU 密集型） |
| **内存** | 16 GB | **32 GB** |
| **磁盘** | 100 GB SSD | **200 GB SSD** |
| **GPU** | 无（CPU 推理，响应慢） | NVIDIA GPU（4GB+ 显存，如 T4 / 4060） |

⚠️ 16 GB 是"勉强能跑"，32 GB 才有余量应对并发和 GC。无 GPU 时 qwen2 单次推理可能需要 10-30 秒。

### 方案二：单机部署（云端 LLM API）⭐ 推荐

> 适用场景：大多数生产环境。去掉 Ollama，LLM 推理交给云端 API，大幅降低硬件需求。

| 配置项 | 要求 | 说明 |
|--------|------|------|
| **CPU** | 4 核 | 足够运行其余 7 个服务 |
| **内存** | **8 GB** | 去掉 Ollama 后内存需求减半 |
| **磁盘** | 50 GB SSD | 无模型文件，磁盘需求也减半 |
| **GPU** | 不需要 | LLM 推理在云端完成 |

配置方式：`.env` 中设置 `LLM_PROVIDER=openai`（或 `anthropic`），并在 `docker-compose.yml` 中注释掉 `ollama` 服务。

### 方案三：分离部署（中大规模生产）

> 适用场景：>50 并发用户，需要高可用。

将中间件拆分到独立服务器或云托管服务：

| 组件 | 部署方式 | 推荐配置 |
|------|---------|---------|
| **应用服务器**（Nginx + Backend + AI Service） | ECS / 自建 | 4C8G |
| **MySQL** | 云 RDS 或独立服务器 | 2C4G，SSD |
| **Redis** | 云 Redis 或独立服务器 | 1G 主从版 |
| **Elasticsearch** | 独立服务器 | 4C8G，SSD |
| **Qdrant** | 独立服务器 | 2C4G |
| **LLM** | 云端 API（OpenAI / Anthropic） | 按调用量付费 |

### 云服务器成本参考

| 方案 | 配置 | 月成本参考（国内云厂商） |
|------|------|------------------------|
| 学习/开发 | 4C8G（去掉 Ollama） | ~200-400 元/月 |
| 小团队生产（云端 API） | 4C8G | ~300-500 元/月 + API 调用费 |
| 企业生产（本地 LLM） | 8C32G + GPU | ~1000-2000 元/月 |
| 分离部署（高可用） | 多台服务器 | ~2000-5000 元/月 |

> 💡 以上为 2026 年国内主流云厂商（阿里云/腾讯云/华为云）的参考价格，实际以官网报价为准。

### 生产环境性能调优

#### JVM 参数（Backend）

```bash
# docker-compose.yml 中 backend 环境变量添加
JAVA_OPTS: "-Xms1g -Xmx2g -XX:+UseG1GC"
```

#### Elasticsearch 堆内存

```yaml
# 从 512m 提升到 1-2g
environment:
  - "ES_JAVA_OPTS=-Xms1g -Xmx1g"    # 生产建议 1-2g，不超过物理内存 50%
```

#### MySQL 关键参数

```yaml
# docker-compose.yml 中 mysql command 追加
command: >
  --character-set-server=utf8mb4
  --collation-server=utf8mb4_unicode_ci
  --innodb-buffer-pool-size=1G       # 建议物理内存的 50-70%
  --max-connections=200
  --innodb-log-file-size=256M
```

#### Ollama GPU 加速

```yaml
# docker-compose.yml 中 ollama 服务添加 GPU 支持
ollama:
  image: ollama/ollama:latest
  deploy:
    resources:
      reservations:
        devices:
          - driver: nvidia
            count: 1
            capabilities: [gpu]
```

## ☁️ 云服务部署

### 阿里云部署

#### 1. ECS 配置

根据上方「生产服务器配置」选择合适方案：

| 方案 | ECS 规格 | 系统 | 存储 |
|------|---------|------|------|
| 云端 API（推荐） | 2核4G ~ 4核8G | Ubuntu 22.04 | 50-100 GB SSD |
| 本地 LLM | 8核32G + GPU | Ubuntu 22.04 | 200 GB SSD |

#### 2. RDS MySQL
- **规格**: 2核4G
- **版本**: MySQL 8.0
- **存储**: 50GB

#### 3. Redis 缓存
- **规格**: 1G主从版
- **版本**: Redis 7.0

#### 4. 部署脚本

```bash
#!/bin/bash
# deploy.sh

set -e

echo "开始部署..."

# 1. 拉取代码
git pull origin main

# 2. 构建前端
cd frontend
npm install
npm run build

# 3. 构建后端
cd ../backend
mvn clean package -DskipTests

# 4. 启动服务
docker-compose up -d --build

# 5. 健康检查
sleep 30
curl -f http://localhost:8080/actuator/health || exit 1

echo "部署完成！"
```

### 腾讯云部署

#### 使用轻量应用服务器
- **配置**: 4核8G
- **镜像**: Docker 20.10
- **存储**: 80GB

```bash
# 1. 安装 Docker
curl -fsSL https://get.docker.com | sh

# 2. 部署应用
git clone https://github.com/your-repo/knowledge-base.git
cd knowledge-base
docker-compose up -d
```

## 🔧 Kubernetes 部署

### 1. 命名空间创建

```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: knowledge-base
```

### 2. ConfigMap 配置

```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: knowledge-base
data:
  APP_ENV: "production"
  DB_HOST: "mysql-service"
  REDIS_HOST: "redis-service"
  ES_HOST: "elasticsearch-service"
```

### 3. Secret 配置

```yaml
# secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  namespace: knowledge-base
type: Opaque
data:
  DB_PASSWORD: c2VjdXJlX3Bhc3N3b3Jk
  JWT_SECRET: eW91ci1zZWNyZXQta2V5
  OPENAI_API_KEY: eW91ci1vcGVuYWktYXBpLWtleQ==
```

### 4. 部署配置

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  namespace: knowledge-base
spec:
  replicas: 3
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
    spec:
      containers:
      - name: backend
        image: your-registry/knowledge-base-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: DB_PASSWORD
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: JWT_SECRET
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
```

### 5. 服务配置

```yaml
# service.yaml
apiVersion: v1
kind: Service
metadata:
  name: backend-service
  namespace: knowledge-base
spec:
  selector:
    app: backend
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP
```

### 6. Ingress 配置

```yaml
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: knowledge-base-ingress
  namespace: knowledge-base
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
  - hosts:
    - kb.example.com
    secretName: kb-tls
  rules:
  - host: kb.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: backend-service
            port:
              number: 80
```

### 7. 部署命令

```bash
# 应用配置
kubectl apply -f namespace.yaml
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
kubectl apply -f ingress.yaml

# 查看状态
kubectl get pods -n knowledge-base
kubectl get services -n knowledge-base
```

## 🔒 安全配置

### 1. SSL/TLS 配置

```nginx
# nginx/nginx.conf
server {
    listen 443 ssl http2;
    server_name kb.example.com;

    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    location / {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen 80;
    server_name kb.example.com;
    return 301 https://$server_name$request_uri;
}
```

### 2. 防火墙配置

```bash
# Ubuntu UFW
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable
```

### 3. 数据库安全

```sql
-- 创建只读用户
CREATE USER 'kb_readonly'@'%' IDENTIFIED BY 'readonly_password';
GRANT SELECT ON knowledge_base.* TO 'kb_readonly'@'%';

-- 创建应用用户
CREATE USER 'kb_app'@'%' IDENTIFIED BY 'app_password';
GRANT SELECT, INSERT, UPDATE, DELETE ON knowledge_base.* TO 'kb_app'@'%';
```

### 4. 配置密码加密（Jasypt）

项目使用 [Jasypt](https://github.com/ulisesbocchio/jasypt-spring-boot) 对 `application.yml` 中的敏感配置进行加密，避免密码明文存储在代码仓库中。

#### 加密原理

```
明文密码 → JasyptEncryptUtil 加密 → ENC(密文) → 写入 application.yml
                                                    ↓
                                       运行时由 JASYPT_ENCRYPTOR_PASSWORD 主密钥解密
```

#### 生成加密密文

```bash
# 1. 在 backend 目录下，使用 JDK 17 编译
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn compile

# 2. 构建类路径
JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn -q dependency:build-classpath -Dmdep.outputFile=/tmp/kb-cp.txt

# 3. 加密密码明文
JAVA_HOME=$(/usr/libexec/java_home -v 17) && \
$JAVA_HOME/bin/java -cp "$(cat /tmp/kb-cp.txt):target/classes" \
  -Djasypt.encryptor.password=你的主密钥 \
  com.geekyous.kb.utils.JasyptEncryptUtil encrypt 密码明文

# 输出示例：ENC(BM2F1Myec/dtnAO9xl1nAJ7...)
```

#### 在 .env 中使用加密值

```bash
# .env 文件中密码支持明文和 ENC() 两种格式
JASYPT_ENCRYPTOR_PASSWORD=你的主密钥           # 主密钥，仅存于 .env（gitignored）
DB_PASSWORD=ENC(BM2F1Myec/dtnAO9xl1nAJ7...)    # 加密后的数据库密码
JWT_SECRET=ENC(Gubp/mWyNOEHoXcyvWGqn...)       # 加密后的 JWT 密钥
```

#### 关键注意事项

- **主密钥 `JASYPT_ENCRYPTOR_PASSWORD` 绝不能提交到代码仓库**，只存放在 `.env` 文件中
- 算法为 `PBEWITHHMACSHA512ANDAES_256`，需要 JDK 17+ 运行
- 未包裹在 `ENC()` 中的配置值会被 Jasypt 原样传递，支持明文与密文混用

## 📊 监控和日志

### 1. 应用监控

#### Prometheus 配置

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'knowledge-base'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']
```

### 2. 日志收集

#### ELK Stack 配置

```yaml
# filebeat.yml
filebeat.inputs:
- type: container
  paths:
    - '/var/lib/docker/containers/*/*.log'
  processors:
  - add_docker_metadata:
      host: "unix:///var/run/docker.sock"

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  indices:
    - index: "knowledge-base-logs-%{+yyyy.MM.dd}"
```

## 🔄 CI/CD 流程

### GitHub Actions 配置

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3

    - name: Build Frontend
      run: |
        cd frontend
        npm install
        npm run build

    - name: Build Backend
      run: |
        cd backend
        mvn clean package -DskipTests

    - name: Build Docker Images
      run: |
        docker build -t your-registry/kb-backend:${{ github.sha }} ./backend
        docker push your-registry/kb-backend:${{ github.sha }}

    - name: Deploy to Server
      uses: appleboy/ssh-action@master
      with:
        host: ${{ secrets.SERVER_HOST }}
        username: ${{ secrets.SERVER_USER }}
        key: ${{ secrets.SERVER_KEY }}
        script: |
          docker pull your-registry/kb-backend:${{ github.sha }}
          docker-compose up -d
```

## 🧪 部署验证

### 健康检查

```bash
# 检查服务状态
curl http://localhost:8080/actuator/health

# 检查数据库连接
curl http://localhost:8080/actuator/db

# 检查AI服务
curl http://localhost:8000/health
```

### 功能测试

```bash
# 用户登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'

# 搜索测试
curl http://localhost:8080/api/v1/search?q=测试

# AI问答测试
curl -X POST http://localhost:8000/api/v1/chat/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"如何申请年假？"}'
```

## 🚨 故障排查

### 常见问题

#### 1. 服务无法启动
```bash
# 查看日志
docker-compose logs backend

# 检查配置
docker-compose config
```

#### 2. 数据库连接失败
```bash
# 测试数据库连接
mysql -h localhost -u kb_user -p knowledge_base

# 检查网络
docker network ls
docker network inspect app-network
```

#### 3. 性能问题
```bash
# 查看资源使用
docker stats

# 查看应用指标
curl http://localhost:8080/actuator/metrics
```

---

**文档版本：** v2.0
**最后更新：** 2026-06-07
