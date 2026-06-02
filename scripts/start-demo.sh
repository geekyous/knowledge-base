#!/bin/bash
# ============================================
# 企业知识库问答系统 - 一键启动脚本
# ============================================
#
# 💡 学习要点:
#   1. Shell 脚本基础: 函数定义、变量、条件判断、循环
#   2. Docker Compose 服务编排与启动顺序
#   3. 健康检查轮询模式（等待服务就绪再继续）
#   4. 端口冲突检测与用户交互
#   5. 环境变量文件(.env)的自动生成
#
# 📖 使用方式:
#   chmod +x scripts/start-demo.sh   # 首次使用需赋予执行权限
#   ./scripts/start-demo.sh          # 一键启动所有服务
#
# 🔗 相关文件:
#   - docker-compose.yml    Docker 服务编排配置
#   - .env.example          环境变量模板
#   - scripts/health-check.sh   服务健康检查脚本
#
# ============================================

# set -e: 任何命令执行失败时立即退出脚本，防止错误继续传播
set -e

# =====================================================
# 颜色定义
# 使用 ANSI 转义码在终端中输出彩色文字
# 格式: \033[样式;前景色m  其中 0=默认 1=加粗
# =====================================================
RED='\033[0;31m'       # 红色 - 用于错误信息
GREEN='\033[0;32m'     # 绿色 - 用于成功信息
YELLOW='\033[1;33m'    # 黄色 - 用于警告信息
BLUE='\033[0;34m'      # 蓝色 - 用于提示信息
NC='\033[0m'           # No Color - 重置为默认颜色

# =====================================================
# 打印启动横幅
# =====================================================
echo "============================================"
echo "  🚀 企业知识库问答系统 - 启动脚本"
echo "============================================"
echo ""

# =====================================================
# 函数: check_docker
# 用途: 检查 Docker 是否已安装
# =====================================================
check_docker() {
    echo -n "🔍 检查 Docker... "
    # command -v: 检查命令是否存在
    # &> /dev/null: 将标准输出和错误都丢弃（静默模式）
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}未安装${NC}"
        echo "  请先安装 Docker: https://docs.docker.com/get-docker/"
        exit 1  # 退出码 1 表示异常退出
    fi
    echo -e "${GREEN}✓${NC}"
}

# =====================================================
# 函数: check_compose
# 用途: 检查 Docker Compose 是否可用
#
# 💡 学习要点:
#   Docker Compose 有两种安装方式:
#   1. 新版: 作为 Docker CLI 插件 (docker compose)
#   2. 旧版: 独立二进制文件 (docker-compose)
#   这里做了兼容处理，优先使用新版命令
# =====================================================
check_compose() {
    echo -n "🔍 检查 Docker Compose... "
    if docker compose version &> /dev/null; then
        # 新版 Docker Compose V2（作为 docker 子命令）
        COMPOSE_CMD="docker compose"
    elif command -v docker-compose &> /dev/null; then
        # 旧版独立安装的 docker-compose
        COMPOSE_CMD="docker-compose"
    else
        echo -e "${RED}未安装${NC}"
        echo "  请先安装 Docker Compose"
        exit 1
    fi
    echo -e "${GREEN}✓${NC}"
}

# =====================================================
# 函数: check_ports
# 用途: 检查所需端口是否被占用
#
# 💡 学习要点:
#   端口占用是 Docker 启动失败的常见原因
#   - lsof -i :端口: 列出占用该端口的进程（macOS/Linux）
#   - netstat -an: 显示所有网络连接状态
#
#   各端口对应的服务:
#   80   - Nginx 反向代理
#   8080 - Spring Boot 后端
#   8000 - FastAPI AI 服务
#   3306 - MySQL 数据库
#   6379 - Redis 缓存
#   9200 - Elasticsearch 搜索引擎
#   6333 - Qdrant 向量数据库
# =====================================================
check_ports() {
    echo "🔍 检查端口占用..."
    # 定义需要检查的端口列表
    local ports=(80 8080 8000 3306 6379 9200 6333)
    # 用于收集被占用的端口
    local occupied=()

    # 遍历每个端口，检查是否被占用
    for port in "${ports[@]}"; do
        # lsof: macOS/Linux 检查端口的工具
        # netstat: 备用检查方式
        if lsof -i ":$port" &> /dev/null || netstat -an 2>/dev/null | grep -q ":$port "; then
            occupied+=($port)
        fi
    done

    # 如果有端口被占用，提示用户确认
    if [ ${#occupied[@]} -gt 0 ]; then
        echo -e "  ${YELLOW}端口 ${occupied[*]} 已被占用${NC}"
        echo "  提示: 可以使用 'lsof -i :端口号' 查看占用进程"
        # read -p: 交互式读取用户输入
        read -p "  是否继续? (y/N): " choice
        # =~ 正则匹配: 检查用户输入是否为 y 或 Y
        if [[ ! "$choice" =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        echo -e "  ${GREEN}所有端口可用 ✓${NC}"
    fi
}

# =====================================================
# 函数: create_env
# 用途: 创建 .env 环境变量文件（如果不存在）
#
# 💡 学习要点:
#   .env 文件用于存储敏感配置（数据库密码、API Key 等）
#   - 不应该提交到 Git（已在 .gitignore 中排除）
#   - docker compose 会自动读取 .env 文件
#   - .env.example 是模板文件，包含所有可配置项但不含真实密码
# =====================================================
create_env() {
    if [ ! -f .env ]; then
        echo "📝 创建 .env 文件..."
        # 尝试从模板复制，如果模板不存在则创建默认配置
        cp .env.example .env 2>/dev/null || cat > .env << 'EOF'
# 数据库配置
DB_ROOT_PASSWORD=root_password
DB_DATABASE=knowledge_base
DB_USERNAME=kb_user
DB_PASSWORD=kb_password

# JWT 配置
# ⚠️ 生产环境务必更换为随机生成的长密钥
JWT_SECRET=your-secret-key-must-be-at-least-32-characters-long

# AI 服务配置
# MOCK_MODE=true 表示使用模拟模式（无需 API Key）
MOCK_MODE=true
# 取消注释并填入真实 Key 以启用真实 LLM
# OPENAI_API_KEY=sk-xxx
# ANTHROPIC_API_KEY=sk-ant-xxx
EOF
        echo -e "  ${GREEN}✓${NC}"
    fi
}

# =====================================================
# 函数: start_services
# 用途: 启动 Docker Compose 服务并等待就绪
#
# 💡 学习要点:
#   1. docker compose up -d: 后台启动所有服务（-d = detached）
#   2. 服务启动需要时间，必须轮询等待就绪
#   3. 不同服务的就绪检测方式不同:
#      - MySQL: mysqladmin ping 命令
#      - Elasticsearch: HTTP 健康检查 API
#      - Qdrant: HTTP healthz 端点
#      - Redis: redis-cli ping 命令（返回 PONG 表示就绪）
#   4. 使用 docker exec 在容器内执行命令
#   5. docker ps -qf "name=mysql": 按名称过滤获取容器 ID
# =====================================================
start_services() {
    echo ""
    echo "📦 启动 Docker 服务..."
    # -d: 后台运行（detached mode）
    $COMPOSE_CMD up -d

    echo ""
    echo "⏳ 等待服务就绪..."

    # ---- 等待 MySQL 就绪 ----
    # MySQL 启动较慢（通常需要 15-30 秒）
    # mysqladmin ping: MySQL 提供的健康检查命令
    echo -n "  MySQL... "
    for i in {1..30}; do
        # docker exec: 在运行中的容器内执行命令
        # --silent: 静默模式，不输出警告信息
        if docker exec $(docker ps -qf "name=mysql") mysqladmin ping -h localhost --silent 2>/dev/null; then
            echo -e "${GREEN}✓${NC}"
            break
        fi
        sleep 2  # 每次检查间隔 2 秒，最长等待 60 秒
    done

    # ---- 等待 Elasticsearch 就绪 ----
    # ES 启动最慢（通常需要 30-60 秒），且消耗较多内存
    # _cluster/health API: 返回集群状态（green/yellow/red）
    echo -n "  Elasticsearch... "
    for i in {1..30}; do
        # curl -s: 静默模式，不显示进度
        # grep -q: 静默匹配，只返回成功/失败
        if curl -s http://localhost:9200/_cluster/health | grep -q '"status":"green\|yellow"' 2>/dev/null; then
            echo -e "${GREEN}✓${NC}"
            break
        fi
        sleep 3  # ES 启动较慢，每次间隔 3 秒，最长等待 90 秒
    done

    # ---- 等待 Qdrant 就绪 ----
    # Qdrant 启动很快（通常 1-3 秒）
    # /healthz 端点: Qdrant 的健康检查接口
    echo -n "  Qdrant... "
    for i in {1..20}; do
        if curl -s http://localhost:6333/healthz | grep -q "ok" 2>/dev/null; then
            echo -e "${GREEN}✓${NC}"
            break
        fi
        sleep 1
    done

    # ---- 等待 Redis 就绪 ----
    # Redis 启动很快（通常 1-2 秒）
    # redis-cli ping: 返回 "PONG" 表示服务就绪
    echo -n "  Redis... "
    for i in {1..15}; do
        if docker exec $(docker ps -qf "name=redis") redis-cli ping 2>/dev/null | grep -q "PONG"; then
            echo -e "${GREEN}✓${NC}"
            break
        fi
        sleep 1
    done
}

# =====================================================
# 函数: init_data
# 用途: 初始化缓存数据（Redis 热门文档、搜索热词等）
#
# 💡 学习要点:
#   这里只初始化 Redis 缓存，因为:
#   - MySQL 数据: 由 docker-entrypoint-initdb.d 下的 SQL 自动导入
#   - Elasticsearch/Qdrant: 需要依赖 Python 环境，由单独脚本处理
#   - Redis: 缓存数据可选，不影响核心功能
# =====================================================
init_data() {
    echo ""
    echo "📊 初始化数据..."

    # 初始化 Redis 缓存
    echo -n "  Redis 缓存... "
    if command -v python3 &> /dev/null; then
        # 进入 ai-service 目录执行初始化脚本
        # || { cd .. && ... }: 失败时先回到上级目录再输出提示
        cd ai-service && python3 scripts/init_redis.py --host localhost 2>/dev/null && cd .. && echo -e "${GREEN}✓${NC}" || {
            cd .. && echo -e "${YELLOW}跳过${NC}"
        }
    else
        echo -e "${YELLOW}需要 Python3${NC}"
    fi
}

# =====================================================
# 函数: show_result
# 用途: 显示启动结果和访问信息
# =====================================================
show_result() {
    echo ""
    echo "============================================"
    echo -e "  ${GREEN}✅ 系统启动完成！${NC}"
    echo "============================================"
    echo ""
    echo "  📍 访问地址:"
    echo "     前端:     http://localhost"
    echo "     后端API:  http://localhost:8080"
    echo "     AI服务:   http://localhost:8000/docs"
    echo "     ES管理:   http://localhost:9200"
    echo ""
    echo "  👤 默认账号:"
    echo "     管理员:   admin / admin123"
    echo "     编辑:     editor / admin123"
    echo "     用户:     user1 / admin123"
    echo ""
    echo "  🛠️ 管理命令:"
    echo "     查看状态:  $COMPOSE_CMD ps"
    echo "     查看日志:  $COMPOSE_CMD logs -f [服务名]"
    echo "     停止服务:  $COMPOSE_CMD down"
    echo "     重新构建:  $COMPOSE_CMD up -d --build"
    echo ""
    echo "============================================"
}

# =====================================================
# 主流程
# 按顺序执行所有步骤:
# 1. 环境检查 → 2. 端口检查 → 3. 配置文件 →
# 4. 启动服务 → 5. 数据初始化 → 6. 显示结果
# =====================================================
main() {
    check_docker      # 检查 Docker 是否安装
    check_compose     # 检查 Docker Compose 是否可用
    check_ports       # 检查端口是否被占用
    create_env        # 创建 .env 环境变量文件
    start_services    # 启动所有 Docker 服务
    init_data         # 初始化缓存数据
    show_result       # 显示启动结果
}

# "$@": 将脚本的所有命令行参数传递给 main 函数
# 这样可以在 main 中通过 $1, $2 等访问参数
main "$@"
