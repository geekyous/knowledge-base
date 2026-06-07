"""
智能问答 API 模块

========================================
📚 模块概述
====================================
本模块提供了智能问答系统的 HTTP API 接口，是前后端交互的核心入口。

主要功能：
    1. AI 智能问答（/ask）：接收用户问题，通过 RAG 流程生成回答
    2. 对话列表（/conversations）：分页获取所有对话记录
    3. 对话详情（/conversations/{id}）：获取单个对话的所有消息
    4. 删除对话（/conversations/{id}）：删除指定对话

FastAPI Router 机制：
    APIRouter 是 FastAPI 提供的路由分组工具，类似于 Express.js 的 Router。
    它允许将相关的一组 API 端点定义在同一个文件中，
    然后在 main.py 中通过 app.include_router() 统一注册。

    好处：
    - 代码组织清晰（不同功能的 API 分文件管理）
    - 可以统一设置前缀（如 /api/v1/chat）和标签
    - 支持独立的依赖注入和中间件

Pydantic Model 在 API 中的作用：
    - 自动校验请求数据（类型、必填项、值范围）
    - 自动生成 API 文档（Swagger UI / ReDoc）
    - 自动序列化响应数据（Python 对象 → JSON）

💡 学习要点:
    1. FastAPI Router 的模块化组织方式
    2. Pydantic BaseModel 在请求/响应数据校验中的应用
    3. 内存存储模式：简单但不适合生产环境（为什么？）
    4. 对话管理的基本设计：对话 ID、消息角色、时间戳

架构角色：
    本模块是 AI 服务的"控制器"层，负责：
    - 接收 HTTP 请求并校验参数
    - 调用 RAG 核心逻辑处理请求
    - 将结果封装为统一格式的响应返回
"""

import uuid
import logging
from typing import Optional, List
from datetime import datetime

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

# 导入 RAG 核心流程函数
from app.core.rag import retrieve_and_generate

logger = logging.getLogger(__name__)

# 创建路由器实例
# 所有使用 @router 装饰器定义的端点都会注册到这个路由器上
# 在 main.py 中通过 app.include_router(chat.router, prefix="/api/v1/chat") 注册
router = APIRouter()


# =====================================================
# 请求/响应模型（Pydantic BaseModel）
# =====================================================
# Pydantic 模型的作用：
#   1. 自动校验：FastAPI 根据模型定义自动校验请求体
#      - 类型校验：str 不能传 int
#      - 必填校验：没有 Optional 的字段必须提供
#      - 默认值：有默认值的字段可以省略
#   2. 自动文档：FastAPI 根据 Pydantic 模型自动生成 Swagger UI 文档
#   3. 类型提示：IDE 可以自动补全和类型检查


class AskRequest(BaseModel):
    """
    提问请求模型

    当前端发送 POST /api/v1/chat/ask 时，请求体会被自动解析为此模型。

    Attributes:
        question: 用户的问题文本（必填）
        conversationId: 对话 ID（可选）
            - 如果提供，消息会追加到已有对话
            - 如果不提供，会创建新的对话
            - 这实现了多轮对话的基本功能
    """
    question: str = Field(min_length=1, max_length=2000, description="用户的问题文本")
    conversationId: Optional[str] = Field(None, max_length=64, description="对话 ID，提供则追加到已有对话")


class SourceDocument(BaseModel):
    """
    来源文档模型

    表示 RAG 回答中引用的文档来源，用于前端展示"参考来源"。

    Attributes:
        documentId: 文档 ID（对应 MySQL 中的文档记录）
        title: 文档标题
        snippet: 文档摘要片段
        relevance: 与用户问题的相关度分数（0.0-1.0）
    """
    documentId: Optional[int] = None
    title: str = ""
    snippet: str = ""
    relevance: float = 0.0


class AskResponse(BaseModel):
    """
    提问响应模型

    RAG 流程处理完成后返回给前端的结构化数据。

    Attributes:
        conversationId: 对话 ID（前端用此 ID 维持对话上下文）
        answer: AI 生成的自然语言回答
        sources: 引用的文档来源列表（用于展示参考来源）
        followUpQuestions: 推荐的后续追问（引导用户深入探索）
    """
    conversationId: str
    answer: str
    sources: List[dict] = []
    followUpQuestions: List[str] = []


class ConversationSummary(BaseModel):
    """
    对话摘要模型

    用于对话列表页展示，只包含摘要信息而非完整消息历史。
    这是一种常见的 API 设计模式：列表接口返回摘要，详情接口返回完整数据。

    Attributes:
        id: 对话唯一标识
        title: 对话标题（通常是第一条消息的前 50 字符）
        messageCount: 消息数量
        lastMessage: 最后一条消息的摘要
        updatedAt: 最后更新时间（ISO 8601 格式）
    """
    id: str
    title: str
    messageCount: int
    lastMessage: str = ""
    updatedAt: str


# =====================================================
# 内存存储（演示用，生产环境应使用数据库）
# =====================================================
# 使用 Python 字典作为内存存储，数据在服务重启后会丢失。
#
# 为什么不用数据库？
#   - 这是 AI 服务的演示/开发版本
#   - 对话历史的主存储在 Java 后端（Spring Boot + MySQL）
#   - AI 服务只临时缓存对话以支持连续问答
#
# 生产环境的改进方向：
#   1. 使用 Redis 替代内存字典（支持分布式和持久化）
#   2. 或者直接由前端每次发送完整对话历史（无状态设计）
#   3. 或者将对话存储委托给 Java 后端的数据库
_conversations = {}


# =====================================================
# API 接口
# =====================================================


@router.post("/ask", response_model=AskResponse)
async def ask_question(request: AskRequest):
    """
    AI 智能问答 —— 系统的核心 API

    处理流程：
        1. 校验用户问题（非空检查）
        2. 生成或复用对话 ID
        3. 调用 RAG 流程（检索 + 生成）
        4. 保存对话记录
        5. 返回结构化响应

    这是前后端交互的核心端点。前端聊天界面发送用户问题到此接口，
    接口返回 AI 生成的回答和参考来源。

    Args:
        request: AskRequest 对象，包含 question 和可选的 conversationId

    Returns:
        AskResponse: 包含对话 ID、AI 回答、来源和追问建议

    Raises:
        HTTPException: 400 - 问题为空

    Example:
        POST /api/v1/chat/ask
        {
            "question": "年假怎么申请？",
            "conversationId": null
        }
        →
        {
            "conversationId": "conv-a1b2c3d4",
            "answer": "根据《员工手册》规定...",
            "sources": [{"documentId": 2, "title": "年假申请流程", ...}],
            "followUpQuestions": ["年假可以跨年使用吗？", ...]
        }
    """
    logger.info(f"收到提问: {request.question}")

    # 参数校验：问题不能为空字符串或纯空格
    if not request.question or not request.question.strip():
        raise HTTPException(status_code=400, detail="问题不能为空")

    # 生成对话 ID
    # 如果前端传了 conversationId，沿用已有的（多轮对话）
    # 否则生成新的 ID（新对话）
    # uuid4().hex[:8]: 取 UUID4 的前 8 位十六进制字符，如 "a1b2c3d4"
    conversation_id = request.conversationId or f"conv-{uuid.uuid4().hex[:8]}"

    # ==================== 核心 RAG 调用 ====================
    # 这是整个系统最关键的一步：
    # 用户问题 → 向量检索 → 上下文构建 → LLM 生成 → 结构化回答
    result = retrieve_and_generate(request.question)

    # ==================== 保存对话记录 ====================
    # 在内存中维护对话历史，用于支持对话列表和详情查询
    if conversation_id not in _conversations:
        _conversations[conversation_id] = {
            "id": conversation_id,
            # 取问题前 50 字符作为对话标题（显示在对话列表中）
            "title": request.question[:50],
            "messages": [],  # 消息历史列表
            "updatedAt": datetime.now().isoformat(),  # ISO 8601 格式时间戳
        }

    conversation = _conversations[conversation_id]
    # 将用户问题和 AI 回答追加到消息历史
    # role 字段区分消息来源：
    #   - USER: 用户发送的消息
    #   - ASSISTANT: AI 生成的回答
    conversation["messages"].extend(
        [
            {"role": "USER", "content": request.question},
            {"role": "ASSISTANT", "content": result["answer"]},
        ]
    )
    conversation["updatedAt"] = datetime.now().isoformat()

    # 构造并返回响应
    return AskResponse(
        conversationId=conversation_id,
        answer=result["answer"],
        sources=result.get("sources", []),
        followUpQuestions=result.get("followUps", []),
    )


@router.get("/conversations")
async def list_conversations(
        page: int = Field(default=1, ge=1, description="页码"),
        pageSize: int = Field(default=20, ge=1, le=100, description="每页数量")):
    """
    获取对话列表（分页）

    返回所有对话的摘要信息，按最后更新时间倒序排列。
    实现了基本的分页逻辑。

    Args:
        page: 页码（从 1 开始）
        pageSize: 每页数量

    Returns:
        dict: 包含 items（对话列表）和 total（总数）的分页数据

    Example:
        GET /api/v1/chat/conversations?page=1&pageSize=10
        →
        {
            "code": 200,
            "data": {
                "items": [{"id": "conv-xxx", "title": "...", ...}],
                "total": 5
            }
        }
    """
    # 从内存字典中获取所有对话
    items = list(_conversations.values())
    # 按更新时间倒序排列（最新的在前面）
    items.sort(key=lambda x: x["updatedAt"], reverse=True)

    # 手动分页计算
    # page=1, pageSize=20 → start=0, end=20（取前 20 条）
    # page=2, pageSize=20 → start=20, end=40（取第 21-40 条）
    start = (page - 1) * pageSize
    end = start + pageSize

    return {
        "code": 200,
        "data": {
            "items": [
                ConversationSummary(
                    id=c["id"],
                    title=c["title"],
                    messageCount=len(c["messages"]),
                    # 最后一条消息的摘要（取前 50 字符）
                    lastMessage=c["messages"][-1]["content"][:50] if c["messages"] else "",
                    updatedAt=c["updatedAt"],
                )
                for c in items[start:end]  # 只取当前页的数据
            ],
            "total": len(items),
        },
    }


@router.get("/conversations/{conversation_id}")
async def get_conversation(conversation_id: str):
    """
    获取对话详情

    根据对话 ID 返回完整的消息历史。

    Args:
        conversation_id: 对话 ID（路径参数）

    Returns:
        dict: 包含对话 ID、标题和完整消息列表

    Raises:
        HTTPException: 404 - 对话不存在

    Example:
        GET /api/v1/chat/conversations/conv-a1b2c3d4
        →
        {
            "code": 200,
            "data": {
                "id": "conv-a1b2c3d4",
                "title": "年假怎么申请？",
                "messages": [
                    {"role": "USER", "content": "年假怎么申请？"},
                    {"role": "ASSISTANT", "content": "根据规定..."}
                ]
            }
        }
    """
    if conversation_id not in _conversations:
        raise HTTPException(status_code=404, detail="对话不存在")

    conv = _conversations[conversation_id]
    return {
        "code": 200,
        "data": {
            "id": conv["id"],
            "title": conv["title"],
            "messages": conv["messages"],
        },
    }


@router.delete("/conversations/{conversation_id}")
async def delete_conversation(conversation_id: str):
    """
    删除对话

    从内存中移除指定对话的所有数据。

    注意：即使对话不存在也返回成功（幂等性设计）。
    DELETE 操作应该是幂等的：多次删除同一个资源的结果应该一致。

    Args:
        conversation_id: 要删除的对话 ID

    Returns:
        dict: 操作结果消息

    Example:
        DELETE /api/v1/chat/conversations/conv-a1b2c3d4
        → {"code": 200, "message": "对话已删除"}
    """
    if conversation_id in _conversations:
        del _conversations[conversation_id]
    return {"code": 200, "message": "对话已删除"}
