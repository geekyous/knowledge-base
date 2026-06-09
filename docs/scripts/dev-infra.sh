#!/bin/bash
# =============================================================================
# 本地开发基础设施启动脚本
# =============================================================================
#
# 【用途】
# 仅启动本地开发所需的基础设施服务，前后端在宿主机直接运行（支持热更新）。
#
# 【启动的服务】（5 个基础设施）
#   mysql          — 数据库 (:3306)
#   redis          — 缓存 (:6379)
#   elasticsearch  — 全文搜索 (:9200)
#   qdrant         — 向量数据库 (:6333)
#   ollama         — 本地 LLM 推理 (:11434)
#
# 【不启动的服务】（在宿主机运行）
#   nginx          — 本地开发不需要，前端由 Vite dev server 提供服务
#   backend        — Spring Boot 直接在 IDE / 终端运行，支持热更新
#   ai-service     — FastAPI 直接用 uvicorn 运行，支持热更新
#
# 【使用方式】
#   ./docs/scripts/dev-infra.sh              # 启动基础设施
#   ./docs/scripts/dev-infra.sh stop         # 停止基础设施
#   ./docs/scripts/dev-infra.sh status       # 查看运行状态
#   ./docs/scripts/dev-infra.sh pull-models  # 拉取 Ollama 模型（首次需要）
#
# 【本地开发流程】
#   1. ./docs/scripts/dev-infra.sh                  # 启动基础设施
#   2. ./docs/scripts/dev-infra.sh pull-models      # 拉取模型（仅首次）
#   3. cd backend && mvn spring-boot:run            # 启动后端（或 IDE 运行）
#   4. cd ai-service && uvicorn app.main:app --reload --port 8000  # 启动 AI 服务
#   5. cd frontend && npm run dev                   # 启动前端（http://localhost:5173）
#
# 📖 完整开发环境搭建指南: docs/04-development-setup.md
#
# =============================================================================

set -e

# =====================================================
# 颜色定义
# =====================================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# =====================================================
# 基础设施服务列表（仅这些服务通过 Docker 启动）
# =====================================================
INFRA_SERVICES="mysql redis elasticsearch qdrant ollama"

# =====================================================
# 函数: 检测 Docker Compose 命令
# =====================================================
detect_compose_cmd() {
    if docker compose version &> /dev/null; then
        COMPOSE_CMD="docker compose"
    elif command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        echo -e "${RED}❌ 未找到 Docker Compose，请先安装${NC}"
        exit 1
    fi
}

# =====================================================
# 函数: check_docker
# =====================================================
check_docker() {
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}❌ 未找到 Docker，请先安装: https://docs.docker.com/get-docker/${NC}"
        exit 1
    fi
    if ! docker info &> /dev/null; then
        echo -e "${RED}❌ Docker 未运行，请先启动 Docker Desktop${NC}"
        exit 1
    fi
}

# =====================================================
# 函数: ensure_env
# =====================================================
ensure_env() {
    if [ ! -f .env ]; then
        echo -e "${BLUE}📝 创建 .env 文件...${NC}"
        cp .env.example .env 2>/dev/null || cat > .env << 'EOF'
# Jasypt 主密钥（用于解密 application.yml 中的 ENC() 配置值）
JASYPT_ENCRYPTOR_PASSWORD=kb-demo-2026

# 数据库配置
DB_ROOT_PASSWORD=root_password
DB_DATABASE=knowledge_base
DB_USERNAME=kb_user
DB_PASSWORD=kb_password

# Redis 配置
REDIS_PASSWORD=redis_password

# JWT 配置
JWT_SECRET=dev-secret-key-for-local-testing-only

# AI 服务配置
MOCK_MODE=false
EOF
        echo -e "  ${GREEN}✓${NC}"
    fi
}

# =====================================================
# 函数: start_infra
# 启动基础设施服务并等待就绪
# =====================================================
start_infra() {
    echo "📦 启动基础设施服务..."
    echo ""

    # 只启动指定的服务，不启动 nginx / backend / ai-service
    $COMPOSE_CMD up -d $INFRA_SERVICES

    echo ""
    echo "⏳ 等待服务就绪..."

    # ---- MySQL ----
    echo -n "  MySQL... "
    for i in $(seq 1 30); do
        if docker exec $(docker ps -qf "name=mysql" 2>/dev/null) \
            mysqladmin ping -h localhost --silent 2>/dev/null; then
            echo -e "${GREEN}✓${NC}"
            break
        fi
        [ "$i" -eq 30 ] && echo -e "${YELLOW}超时，请检查日志: $COMPOSE_CMD logs mysql${NC}"
        sleep 2
    done

    # ---- Redis ----
    echo -n "  Redis... "
    for i in $(seq 1 15); do
        if docker exec $(docker ps -qf "name=redis" 2>/dev/null) \
            redis-cli ping 2>/dev/null | grep -q "PONG"; then
            echo -e "${GREEN}✓${NC}"
            break
        fi
        [ "$i" -eq 15 ] && echo -e "${YELLOW}超时${NC}"
        sleep 1
    done

    # ---- Elasticsearch ----
    echo -n "  Elasticsearch... "
    for i in $(seq 1 30); do
        if curl -s http://localhost:9200/_cluster/health 2>/dev/null \
            | grep -q '"status":"green\|yellow"'; then
            echo -e "${GREEN}✓${NC}"
            break
        fi
        [ "$i" -eq 30 ] && echo -e "${YELLOW}超时，请检查日志: $COMPOSE_CMD logs elasticsearch${NC}"
        sleep 3
    done

    # ---- Qdrant ----
    echo -n "  Qdrant... "
    for i in $(seq 1 15); do
        if curl -s http://localhost:6333/healthz 2>/dev/null | grep -q "ok"; then
            echo -e "${GREEN}✓${NC}"
            break
        fi
        [ "$i" -eq 15 ] && echo -e "${YELLOW}超时${NC}"
        sleep 1
    done

    # ---- Ollama ----
    echo -n "  Ollama... "
    for i in $(seq 1 15); do
        if curl -s http://localhost:11434/api/tags 2>/dev/null | grep -q "."; then
            echo -e "${GREEN}✓${NC}"
            break
        fi
        [ "$i" -eq 15 ] && echo -e "${YELLOW}超时${NC}"
        sleep 1
    done
}

# =====================================================
# 函数: pull_models
# 拉取 Ollama 模型
# =====================================================
pull_models() {
    local ollama_container=$(docker ps -qf "name=ollama" 2>/dev/null)
    if [ -z "$ollama_container" ]; then
        echo -e "${RED}❌ Ollama 容器未运行，请先执行: $0${NC}"
        exit 1
    fi

    local chat_model="${1:-qwen2}"
    local embed_model="${EMBED_MODEL:-nomic-embed-text}"

    echo "📦 拉取聊天模型: ${chat_model}（首次下载约 4.7GB，需几分钟）"
    docker exec -it "$ollama_container" ollama pull "$chat_model"

    echo ""
    echo "📦 拉取 Embedding 模型: ${embed_model}（约 274MB）"
    docker exec -it "$ollama_container" ollama pull "$embed_model"

    echo ""
    echo -e "${GREEN}已安装模型:${NC}"
    docker exec "$ollama_container" ollama list
}

# =====================================================
# 函数: show_status
# 显示服务运行状态
# =====================================================
show_status() {
    echo "📊 基础设施服务状态:"
    echo ""
    for svc in $INFRA_SERVICES; do
        echo -n "  $svc... "
        if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "$svc"; then
            local status=$(docker inspect --format='{{.State.Status}}' \
                "$(docker ps -qf "name=$svc" 2>/dev/null)" 2>/dev/null)
            if [ "$status" = "running" ]; then
                echo -e "${GREEN}✓ 运行中${NC}"
            else
                echo -e "${YELLOW}$status${NC}"
            fi
        else
            echo -e "${RED}✗ 未启动${NC}"
        fi
    done
}

# =====================================================
# 函数: stop_infra
# 停止基础设施服务
# =====================================================
stop_infra() {
    echo "🛑 停止基础设施服务..."
    $COMPOSE_CMD stop $INFRA_SERVICES
    echo -e "${GREEN}✓ 已停止${NC}"
    echo ""
    echo "  提示: 数据已保留在 Docker volumes 中"
    echo "  彻底删除数据: $COMPOSE_CMD down -v"
}

# =====================================================
# 函数: show_dev_guide
# 显示本地开发连接信息
# =====================================================
show_dev_guide() {
    echo ""
    echo "============================================"
    echo -e "  ${GREEN}✅ 基础设施已就绪${NC}"
    echo "============================================"
    echo ""
    echo -e "  ${CYAN}📡 服务连接信息${NC}"
    echo "  ────────────────────────────────────────"
    echo "  MySQL:          localhost:3306"
    echo "                  用户: kb_user  密码: kb_password"
    echo "                  数据库: knowledge_base"
    echo ""
    echo "  Redis:          localhost:6379"
    echo ""
    echo "  Elasticsearch:  http://localhost:9200"
    echo ""
    echo "  Qdrant:         localhost:6333 (gRPC)"
    echo "                  http://localhost:6334/dashboard (Web UI)"
    echo ""
    echo "  Ollama:         http://localhost:11434"
    echo ""
    echo -e "  ${CYAN}🚀 启动应用服务（新终端窗口）${NC}"
    echo "  ────────────────────────────────────────"
    echo ""
    echo "  # 后端 (Spring Boot)"
    echo "  cd backend && mvn spring-boot:run \\"
    echo "    -DDB_HOST=localhost \\"
    echo "    -DDB_PORT=3306 \\"
    echo "    -DDB_USERNAME=kb_user \\"
    echo "    -DDB_PASSWORD=kb_password \\"
    echo "    -DJASYPT_ENCRYPTOR_PASSWORD=kb-demo-2026 \\"
    echo "    -DREDIS_HOST=localhost \\"
    echo "    -DES_HOST=http://localhost \\"
    echo "    -DAI_SERVICE_URL=http://localhost:8000"
    echo ""
    echo "  # AI 服务 (FastAPI)"
    echo "  cd ai-service && pip install -r requirements.txt"
    echo "  OLLAMA_BASE_URL=http://localhost:11434 \\"
    echo "  QDRANT_HOST=localhost \\"
    echo "  REDIS_HOST=localhost \\"
    echo "  uvicorn app.main:app --reload --port 8000"
    echo ""
    echo "  # 前端 (Vue + Vite)"
    echo "  cd frontend && npm install && npm run dev"
    echo "  → http://localhost:5173"
    echo ""
    echo -e "  ${CYAN}📋 其他命令${NC}"
    echo "  ────────────────────────────────────────"
    echo "  $0 status       查看服务状态"
    echo "  $0 stop         停止服务（保留数据）"
    echo "  $0 pull-models  拉取 Ollama 模型（首次需要）"
    echo ""
    echo "============================================"
}

# =====================================================
# 主流程
# =====================================================
detect_compose_cmd

case "${1:-}" in
    stop)
        stop_infra
        ;;
    status)
        show_status
        ;;
    pull-models)
        pull_models "$2"
        ;;
    *)
        echo "============================================"
        echo "  🛠️  本地开发基础设施"
        echo "============================================"
        echo ""
        check_docker
        ensure_env
        start_infra
        show_dev_guide
        ;;
esac
