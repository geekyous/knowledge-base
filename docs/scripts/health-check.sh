#!/bin/bash
# ============================================
# 企业知识库问答系统 - 健康检查脚本
# ============================================
#
# 💡 学习要点:
#   1. 健康检查（Health Check）是微服务架构的重要实践
#   2. HTTP 状态码: 200=成功, 5xx=服务异常
#   3. Docker 容器状态: running / exited / paused
#   4. 分层检查: 容器状态 → 网络连接 → API 可用性
#
# 📖 使用方式:
#   ./docs/scripts/health-check.sh
#
# 🔗 相关文件:
#   - docker-compose.yml       服务定义
#   - docs/scripts/start-demo.sh    启动脚本
#
# ============================================

# =====================================================
# 颜色定义（ANSI 转义码）
# =====================================================
RED='\033[0;31m'       # 红色 - 异常
GREEN='\033[0;32m'     # 绿色 - 正常
YELLOW='\033[1;33m'    # 黄色 - 警告
NC='\033[0m'           # 重置颜色

# =====================================================
# 打印检查横幅
# =====================================================
echo "============================================"
echo "  🏥 企业知识库问答系统 - 健康检查"
echo "============================================"
echo ""

# =====================================================
# 函数: check
# 用途: 通过 HTTP 请求检查服务是否可达
#
# 💡 学习要点:
#   curl 常用参数:
#   -s: 静默模式，不显示进度条
#   -o /dev/null: 丢弃响应体（只关心状态码）
#   -w "%{http_code}": 输出 HTTP 状态码
#
# Args:
#   $1 - name: 服务名称（用于显示）
#   $2 - url: 检查的 URL 地址
#   $3 - expected: 期望的 HTTP 状态码
#
# Returns:
#   0 - 服务正常
#   1 - 服务异常
# =====================================================
check() {
    local name=$1
    local url=$2
    local expected=$3

    echo -n "  $name... "
    # 发送 HTTP 请求并获取状态码
    response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)

    # 判断响应状态码是否为期望值或 200（通用成功码）
    if [ "$response" = "$expected" ] || [ "$response" = "200" ]; then
        echo -e "${GREEN}✓ 正常${NC}"
        return 0  # 返回 0 表示成功（Unix 惯例）
    else
        echo -e "${RED}✗ 异常 (HTTP $response)${NC}"
        return 1  # 返回非 0 表示失败
    fi
}

# =====================================================
# 函数: check_container
# 用途: 检查 Docker 容器是否处于运行状态
#
# 💡 学习要点:
#   docker ps: 列出运行中的容器
#   --format '{{.Names}}': 只输出容器名称
#   docker inspect: 获取容器详细信息
#   --format '{{.State.Status}}': 只输出状态字段
#
# Args:
#   $1 - name: 容器名称（用于模糊匹配）
#
# Returns:
#   0 - 容器运行中
#   1 - 容器未运行
# =====================================================
check_container() {
    local name=$1
    echo -n "  $name... "
    # 第一步: 检查是否有匹配名称的容器在运行
    if docker ps --format '{{.Names}}' | grep -q "$name"; then
        # 第二步: 获取容器的详细状态
        # -qf "name=$name": 按名称过滤，返回容器 ID
        status=$(docker inspect --format='{{.State.Status}}' "$(docker ps -qf "name=$name")" 2>/dev/null)
        if [ "$status" = "running" ]; then
            echo -e "${GREEN}✓ 运行中${NC}"
            return 0
        fi
    fi
    echo -e "${RED}✗ 未运行${NC}"
    return 1
}

# =====================================================
# 第一层: 容器状态检查
# 检查所有 Docker 容器是否处于 running 状态
# =====================================================
echo "📦 容器状态:"
echo ""
check_container "mysql"          # MySQL 8.0 数据库
check_container "redis"          # Redis 7 缓存
check_container "elasticsearch"  # Elasticsearch 8 搜索引擎
check_container "qdrant"         # Qdrant 向量数据库
check_container "nginx"          # Nginx 反向代理

# =====================================================
# 第二层: 服务连接检查
# 通过 HTTP/TCP 检查服务端口是否可达
#
# 💡 学习要点:
#   MySQL(3306) 和 Redis(6379) 不是 HTTP 服务，
#   curl 返回的 HTTP 状态码为 000（无响应），
#   这里用 "000" 作为期望值来验证端口可达
# =====================================================
echo ""
echo "🔗 服务连接:"
echo ""
check "MySQL"         "http://localhost:3306" "000"                      # TCP 端口检查
check "Redis"         "http://localhost:6379" "000"                      # TCP 端口检查
check "Elasticsearch" "http://localhost:9200/_cluster/health" "200"     # ES 集群健康 API
check "Qdrant"        "http://localhost:6333/collections" "200"         # Qdrant 集合列表 API

# =====================================================
# 第三层: 应用 API 检查
# 检查后端和 AI 服务的 API 是否正常响应
# =====================================================
echo ""
echo "📊 API 状态:"
echo ""
check "后端健康检查"   "http://localhost:8080/api/v1/health" "200"       # Spring Boot 健康检查
check "AI服务健康检查" "http://localhost:8000/health" "200"              # FastAPI 健康检查
check "前端首页"       "http://localhost:80" "200"                       # Nginx 静态资源

# =====================================================
# 输出检查完成横幅
# =====================================================
echo ""
echo "============================================"
echo "  健康检查完成"
echo "============================================"
