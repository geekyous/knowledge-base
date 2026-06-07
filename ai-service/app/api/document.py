"""
文档处理 API 模块

========================================
📚 模块概述
====================================
本模块提供文档向量化和语义搜索的 HTTP API 接口。

主要功能：
    1. 单文档索引（/index）：将一篇文档向量化并存入 Qdrant
    2. 向量语义搜索（/search）：根据查询文本搜索最相关的文档
    3. 批量索引（/batch-index）：一次性索引多篇文档

文档索引流程：
    ┌──────────────────┐
    │ 前端上传文档内容  │
    └────────┬─────────┘
             │ POST /api/v1/documents/index
             ▼
    ┌──────────────────┐
    │ 提取元数据        │  category, author 等
    └────────┬─────────┘
             ▼
    ┌──────────────────┐
    │ 调用 index_document()│
    │ (vector_store.py)   │
    └────────┬─────────┘
             │ 内部: embed_text() → Qdrant upsert
             ▼
    ┌──────────────────┐
    │ 返回索引结果      │  {"code": 200, "message": "文档索引成功"}
    └──────────────────┘

语义搜索流程：
    ┌──────────────────┐
    │ 用户输入搜索词    │
    └────────┬─────────┘
             │ POST /api/v1/documents/search
             ▼
    ┌──────────────────┐
    │ 调用 search()     │
    │ (vector_store.py) │
    └────────┬─────────┘
             │ 内部: embed_text(query) → cosine similarity
             ▼
    ┌──────────────────┐
    │ 返回搜索结果      │  按相似度排序的文档列表
    └──────────────────┘

💡 学习要点:
    1. 向量索引 API 的设计：何时索引、何时搜索
    2. 批量操作的 API 设计模式
    3. 元数据（metadata）在向量搜索中的作用（过滤、排序）
    4. score_threshold 参数如何影响搜索结果的精度和召回率

架构角色：
    本模块是文档管理的"AI 增强"层。Java 后端负责文档的 CRUD 操作，
    本模块负责将文档内容向量化索引并提供语义搜索能力。
    两者通过文档 ID（document_id）关联。
"""

import logging
from typing import Optional, List

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

# 导入向量存储操作
from app.core.vector_store import index_document, search

logger = logging.getLogger(__name__)
router = APIRouter()


# =====================================================
# 请求/响应模型
# =====================================================


class IndexRequest(BaseModel):
    """
    文档索引请求模型

    当前端（或 Java 后端）创建/更新文档后，会调用此接口
    将文档内容向量化并存入 Qdrant。

    为什么需要 title + content 分开传递？
        - title 用于向量化的加权（标题通常比正文更重要）
        - content 是文档的正文内容
        - vector_store.py 中会将 title + content[:500] 拼接后向量化

    Attributes:
        document_id: 文档在 MySQL 中的主键 ID（用于关联）
        title: 文档标题
        content: 文档正文内容
        category: 文档分类（如"人事制度"、"技术文档"）
        author: 文档作者
    """
    document_id: int = Field(ge=1, description="文档 ID")
    title: str = Field(min_length=1, max_length=255, description="文档标题")
    content: str = Field(min_length=1, description="文档正文")
    category: Optional[str] = Field(None, max_length=100, description="文档分类")
    author: Optional[str] = Field(None, max_length=100, description="文档作者")


class SearchRequest(BaseModel):
    """
    搜索请求模型

    支持三个参数来控制搜索行为：
        - query: 要搜索的文本（会被向量化后与数据库中的向量比较）
        - limit: 返回结果的数量（top-k）
        - score_threshold: 最低相似度阈值

    Attributes:
        query: 搜索查询文本
        limit: 最多返回的结果数量（默认 5）
        score_threshold: 最低相似度分数（0.0-1.0，默认 0.3）
    """
    query: str = Field(min_length=1, max_length=500, description="搜索关键词")
    limit: int = Field(default=5, ge=1, le=100, description="返回结果数量")
    score_threshold: float = Field(default=0.3, ge=0.0, le=1.0, description="相似度阈值")


class SearchResult(BaseModel):
    """
    搜索结果模型

    每个搜索结果代表一篇与查询相关的文档。

    Attributes:
        document_id: 文档 ID
        title: 文档标题
        score: 余弦相似度分数（0.0-1.0，越高越相关）
        payload: 完整的文档元数据
    """
    document_id: int
    title: str
    score: float
    payload: dict = {}


# =====================================================
# API 接口
# =====================================================


@router.post("/index")
async def index_doc(request: IndexRequest):
    """
    将文档向量化并索引到 Qdrant

    当新文档创建或已有文档更新时，前端/后端会调用此接口
    将文档的文本内容转换为向量并存入 Qdrant 向量数据库。

    处理步骤：
        1. 提取可选的元数据（category、author）
        2. 调用 index_document() 进行向量化和存储
        3. 返回操作结果

    Args:
        request: IndexRequest 包含文档 ID、标题、内容和可选元数据

    Returns:
        dict: {"code": 200, "message": "文档索引成功"}

    Raises:
        HTTPException: 500 - 向量化或存储失败

    Example:
        POST /api/v1/documents/index
        {
            "document_id": 1,
            "title": "年假申请流程",
            "content": "年假天数计算...",
            "category": "人事制度",
            "author": "admin"
        }
    """
    logger.info(f"索引文档: {request.document_id} - {request.title}")

    # 构建元数据字典（只包含非空的可选字段）
    # 这些元数据会存储在 Qdrant 的 payload 中，可用于后续的过滤查询
    metadata = {}
    if request.category:
        metadata["category"] = request.category
    if request.author:
        metadata["author"] = request.author

    # 调用核心向量存储模块进行索引
    # 内部流程：title + content[:500] → embed_text() → Qdrant upsert
    success = index_document(
        doc_id=request.document_id,
        title=request.title,
        content=request.content,
        metadata=metadata,
    )

    if success:
        return {"code": 200, "message": "文档索引成功"}
    else:
        raise HTTPException(status_code=500, detail="文档索引失败")


@router.post("/search")
async def search_docs(request: SearchRequest):
    """
    向量语义搜索

    将用户输入的查询文本向量化，然后在 Qdrant 中搜索语义最相似的文档。
    这与传统的关键词搜索不同——它理解文本的"含义"而非精确匹配字面文字。

    例如：
        查询 "如何申请休假" → 能匹配到 "年假申请流程"（语义相近）
        而关键词搜索 "休假" → 可能匹配不到 "年假"（字面不同）

    Args:
        request: SearchRequest 包含查询文本、结果数量和相似度阈值

    Returns:
        dict: {"code": 200, "data": [搜索结果列表]}

    Example:
        POST /api/v1/documents/search
        {
            "query": "年假怎么申请",
            "limit": 5,
            "score_threshold": 0.3
        }
        →
        {
            "code": 200,
            "data": [
                {"document_id": 2, "title": "年假申请流程", "score": 0.89, "payload": {...}},
                ...
            ]
        }
    """
    logger.info(f"向量搜索: {request.query}")

    # 调用向量搜索核心函数
    results = search(
        query=request.query,
        limit=request.limit,
        score_threshold=request.score_threshold,
    )

    return {"code": 200, "data": results}


@router.post("/batch-index")
async def batch_index(documents: List[IndexRequest]):
    """
    批量索引文档

    一次性将多篇文档向量化并存入 Qdrant。
    适用于系统初始化或批量导入文档的场景。

    为什么需要批量接口？
        1. 系统初始化时需要一次性索引所有已有文档
        2. 减少网络请求次数（一次请求索引多篇 vs 逐篇请求）
        3. 便于前端/后端实现批量操作功能

    注意：当前实现是逐条处理，没有使用事务。
    如果部分文档索引失败，已成功的不会回滚。
    生产环境应考虑使用 Qdrant 的批量 upsert 或添加事务机制。

    Args:
        documents: IndexRequest 列表，每项包含一篇文档的信息

    Returns:
        dict: 包含成功数量的操作结果

    Example:
        POST /api/v1/documents/batch-index
        [
            {"document_id": 1, "title": "文档1", "content": "..."},
            {"document_id": 2, "title": "文档2", "content": "..."}
        ]
        →
        {
            "code": 200,
            "message": "批量索引完成: 2/2 成功"
        }
    """
    success_count = 0
    for doc in documents:
        # 为每篇文档构建元数据
        metadata = {}
        if doc.category:
            metadata["category"] = doc.category
        if doc.author:
            metadata["author"] = doc.author

        # 逐条索引并统计成功数
        if index_document(doc.document_id, doc.title, doc.content, metadata):
            success_count += 1

    return {
        "code": 200,
        "message": f"批量索引完成: {success_count}/{len(documents)} 成功",
    }
