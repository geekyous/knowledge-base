#!/bin/bash
# =============================================================================
# Ollama 模型拉取脚本
# =============================================================================
#
# 【用途】
# 首次使用时，需要下载 LLM 和 Embedding 模型到 Ollama 容器中。
# 模型文件较大（qwen2:7b 约 4.7GB），下载时间取决于网络速度。
#
# 【使用方式】
#   ./docs/scripts/pull-ollama-models.sh          # 拉取默认模型
#   ./docs/scripts/pull-ollama-models.sh qwen2    # 只拉取聊天模型
#
# 【可选模型】
#   聊天模型（选一个即可）：
#     qwen2:7b        — 阿里通义千问，中文能力最强（推荐，~4.7GB）
#     llama3:8b       — Meta Llama 3，英文能力强（~4.7GB）
#     mistral:7b      — Mistral AI，推理速度快（~4.1GB）
#     gemma2:9b       — Google Gemma 2（~5.4GB）
#     phi3:mini       — 微软 Phi-3，超小模型（~2.3GB，适合低配机器）
#
#   Embedding 模型：
#     nomic-embed-text — 轻量高效（~274MB，推荐）
#     mxbai-embed-large — 更精确（~670MB）
#
# 💡 学习要点:
#   1. 模型名称格式：name:tag（如 qwen2:7b）
#   2. 7b/8b 表示 70亿/80亿参数，参数越多能力越强但越慢
#   3. 模型下载后缓存在 Ollama 的 volume 中，重启不丢失
# =============================================================================

set -e

# Ollama 容器名称（与 docker-compose 中的服务名一致）
OLLAMA_CONTAINER="${OLLAMA_CONTAINER:-$(docker compose ps -q ollama 2>/dev/null | head -1)}"

if [ -z "$OLLAMA_CONTAINER" ]; then
    echo "❌ 未找到 Ollama 容器，请先启动服务：docker compose up -d"
    exit 1
fi

echo "🎯 Ollama 容器: $OLLAMA_CONTAINER"
echo ""

# 聊天模型（默认 qwen2）
CHAT_MODEL="${1:-qwen2}"
# Embedding 模型
EMBED_MODEL="${EMBED_MODEL:-nomic-embed-text}"

echo "📦 拉取聊天模型: $CHAT_MODEL"
echo "   这可能需要几分钟，取决于网络速度..."
docker exec -it "$OLLAMA_CONTAINER" ollama pull "$CHAT_MODEL"

echo ""
echo "📦 拉取 Embedding 模型: $EMBED_MODEL"
docker exec -it "$OLLAMA_CONTAINER" ollama pull "$EMBED_MODEL"

echo ""
echo "✅ 模型拉取完成！已安装的模型："
docker exec "$OLLAMA_CONTAINER" ollama list

echo ""
echo "🧪 测试聊天模型..."
docker exec "$OLLAMA_CONTAINER" ollama run "$CHAT_MODEL" "你好，请用一句话介绍自己"

echo ""
echo "🎉 全部就绪！现在可以在前端 Chat 页面提问了"
