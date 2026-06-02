"""
Qdrant 向量存储与检索模块

========================================
📚 模块概述
========================================
本模块封装了 Qdrant 向量数据库的所有操作，包括：
    - 集合（Collection）的创建和初始化
    - 文档向量的索引（写入）
    - 基于余弦相似度的向量搜索

什么是向量数据库？
    传统数据库（如 MySQL）通过精确匹配或关键词搜索来查找数据。
    向量数据库则通过数学运算（余弦相似度、欧氏距离等）在向量空间中
    找到"语义最相近"的数据，这就是"语义搜索"的基础。

    传统搜索:  SELECT * FROM docs WHERE title LIKE '%年假%'  -- 关键词匹配
    向量搜索:  找到与"年假怎么申请"向量最相似的 top-5 文档      -- 语义匹配

Qdrant 核心概念：
    - Collection（集合）: 类似 MySQL 的表，存储一组向量数据
    - Point（点）: 一条向量记录，包含 id、vector、payload
    - Vector（向量）: 文本经过嵌入模型转换后的浮点数组
    - Payload（载荷）: 向量的元数据（如文档标题、分类等）
    - Distance（距离度量）: 衡量向量间相似度的方法（本系统使用余弦相似度）

    Qdrant 中的数据结构：
    ┌──────────────────────────────────────────────────────┐
    │ Collection: "documents"                              │
    │ ┌────────┬──────────────────────┬─────────────────┐  │
    │ │   ID   │      Vector (384维)   │     Payload     │  │
    │ ├────────┼──────────────────────┼─────────────────┤  │
    │ │   1    │ [0.12, -0.34, ...]   │ {title: "..."}  │  │
    │ │   2    │ [0.56, 0.78, ...]    │ {title: "..."}  │  │
    │ │  ...   │  ...                 │  ...            │  │
    │ └────────┴──────────────────────┴─────────────────┘  │
    └──────────────────────────────────────────────────────┘

余弦相似度（Cosine Similarity）：
    衡量两个向量方向的相似程度，值域 [-1, 1]：
    - 1.0: 完全相同方向（语义完全一致）
    - 0.0: 正交（语义无关）
    - -1.0: 完全相反方向（语义相反）

    公式：cos(A, B) = (A · B) / (||A|| × ||B||)
    当向量归一化后（||A|| = ||B|| = 1），余弦相似度 = 点积（A · B）

💡 学习要点:
    1. 向量数据库与传统数据库的本质区别
    2. 集合、点、载荷等向量数据库核心概念
    3. 余弦相似度的原理和为什么它是语义搜索的首选度量
    4. score_threshold 阈值如何平衡"查全率"和"查准率"

架构角色：
    本模块是 RAG 流水线的"存储与检索"环节。
    retriever.py 调用本模块的 search() 获取相关文档，
    document.py API 调用 index_document() 写入文档向量。
"""

import os
import logging
from typing import List, Dict, Any, Optional

from .embedding import embed_text

logger = logging.getLogger(__name__)

# =====================================================
# 集合配置
# =====================================================
# 集合名称：类似 MySQL 的表名，所有文档向量存储在同一个集合中
COLLECTION_NAME = "documents"
# 向量维度：必须与 embedding.py 中的 VECTOR_SIZE 和嵌入模型输出维度一致
VECTOR_SIZE = 384

# =====================================================
# 全局客户端（单例模式）
# =====================================================
# 使用模块级变量缓存 Qdrant 客户端连接，避免每次操作都创建新连接
_client = None


def get_client():
    """
    获取 Qdrant 客户端（懒加载单例）

    连接参数从环境变量读取，默认连接本地 Qdrant 服务：
        - QDRANT_HOST: Qdrant 服务器地址（默认 localhost）
        - QDRANT_PORT: Qdrant gRPC 端口（默认 6333）

    为什么使用懒加载？
        1. 避免在 import 模块时就尝试连接数据库
        2. 测试时可能不需要真实的 Qdrant 连接
        3. 如果 Qdrant 服务未启动，不应该阻止应用启动

    Returns:
        QdrantClient: 已连接的客户端实例
        None: 连接失败或 qdrant-client 未安装
    """
    global _client
    if _client is None:
        try:
            from qdrant_client import QdrantClient
            host = os.getenv("QDRANT_HOST", "localhost")
            port = int(os.getenv("QDRANT_PORT", "6333"))
            _client = QdrantClient(host=host, port=port)
            logger.info(f"Qdrant 连接成功: {host}:{port}")
        except ImportError:
            # qdrant-client 库未安装，记录警告但不中断运行
            logger.warning("qdrant-client 未安装，向量检索功能不可用")
            return None
        except Exception as e:
            # Qdrant 服务未启动或网络不通
            logger.warning(f"Qdrant 连接失败: {e}")
            return None
    return _client


def init_collection():
    """
    初始化向量集合

    确保 Qdrant 中存在名为 "documents" 的集合，配置为：
        - 向量维度: 384
        - 距离度量: COSINE（余弦相似度）

    如果集合已存在则跳过创建，不会覆盖已有数据。

    Returns:
        bool: 初始化成功返回 True，失败返回 False
    """
    client = get_client()
    if client is None:
        return False

    try:
        from qdrant_client.models import Distance, VectorParams

        # 检查集合是否已存在
        # 避免重复创建导致数据丢失
        collections = client.get_collections().collections
        if any(c.name == COLLECTION_NAME for c in collections):
            logger.info(f"集合 {COLLECTION_NAME} 已存在")
            return True

        # 创建新集合
        # VectorParams 参数说明：
        #   size: 向量维度（必须与嵌入模型输出维度一致）
        #   distance: 距离度量方式
        #     - Distance.COSINE: 余弦相似度（适合文本语义搜索）
        #     - Distance.EUCLID: 欧氏距离
        #     - Distance.DOT: 点积（向量需预先归一化）
        client.create_collection(
            collection_name=COLLECTION_NAME,
            vectors_config=VectorParams(size=VECTOR_SIZE, distance=Distance.COSINE),
        )
        logger.info(f"集合 {COLLECTION_NAME} 创建成功")
        return True
    except Exception as e:
        logger.error(f"初始化集合失败: {e}")
        return False


def index_document(doc_id: int, title: str, content: str, metadata: Dict[str, Any] = None):
    """
    将单个文档向量化并存入 Qdrant

    处理流程：
        1. 拼接标题和内容的前 500 字符作为向量化输入
        2. 调用 embed_text() 将文本转为 384 维向量
        3. 构造 PointStruct（包含 id、向量、载荷）
        4. 通过 upsert 操作写入 Qdrant

    为什么只取内容的前 500 字符？
        - 嵌入模型有输入长度限制（通常 256-512 tokens）
        - 标题 + 前 500 字符通常已包含文档的核心信息
        - 减少向量化的计算开销

    什么是 upsert？
        - UPDATE + INSERT 的组合操作
        - 如果该 ID 已存在则更新，不存在则插入
        - 保证幂等性（重复调用不会创建重复数据）

    Args:
        doc_id: 文档唯一标识（通常对应 MySQL 中的文档 ID）
        title: 文档标题
        content: 文档内容
        metadata: 可选的元数据（如分类、作者等），会存入 payload

    Returns:
        bool: 索引成功返回 True，失败返回 False

    Example:
        >>> index_document(1, "年假申请流程", "年假天数计算...", {"category": "人事制度"})
        True
    """
    client = get_client()
    if client is None:
        return False

    try:
        from qdrant_client.models import PointStruct

        # 拼接标题和部分内容作为向量化输入
        # 只取前 500 字符以平衡信息量和性能
        text = f"{title} {content[:500]}"
        vector = embed_text(text)

        # 构造载荷（Payload）：存储文档的元数据
        # 载荷会随搜索结果一起返回，用于展示和前端渲染
        payload = {"document_id": doc_id, "title": title}
        if metadata:
            payload.update(metadata)

        # upsert：插入或更新
        # PointStruct 是 Qdrant 的数据结构：
        #   - id: 唯一标识（整数或 UUID）
        #   - vector: 浮点向量（384 维）
        #   - payload: JSON 格式的元数据
        client.upsert(
            collection_name=COLLECTION_NAME,
            points=[PointStruct(id=doc_id, vector=vector, payload=payload)],
        )
        logger.info(f"文档 {doc_id} 索引成功")
        return True
    except Exception as e:
        logger.error(f"索引文档 {doc_id} 失败: {e}")
        return False


def search(query: str, limit: int = 5, score_threshold: float = 0.5) -> List[Dict[str, Any]]:
    """
    向量语义搜索 —— 根据查询文本找到最相关的文档

    搜索流程：
        1. 将用户查询文本通过 embed_text() 转为 384 维向量
        2. 在 Qdrant 中搜索与查询向量最接近的文档向量
        3. 按余弦相似度降序排列，返回 top-k 结果
        4. 过滤掉低于 score_threshold 的低质量结果

    score_threshold 的选择策略：
        - 0.3: 宽松模式，适合探索性搜索（宁可多返回一些）
        - 0.5: 平衡模式，适合通用场景（推荐默认值）
        - 0.7: 严格模式，适合高精度要求的场景
        注意：伪向量的相似度分布与真实向量不同，阈值可能需要调整

    Args:
        query: 用户查询文本（如"年假怎么申请"）
        limit: 返回的最大结果数量（即 top-k 中的 k）
        score_threshold: 最低相似度阈值（0.0-1.0），低于此值的结果会被过滤

    Returns:
        List[Dict[str, Any]]: 搜索结果列表，每个结果包含：
            - document_id: 文档 ID
            - title: 文档标题
            - score: 相似度分数（0.0-1.0，越高越相似）
            - payload: 完整的文档元数据

    Example:
        >>> results = search("年假怎么申请", limit=3, score_threshold=0.3)
        >>> for r in results:
        ...     print(f"{r['title']}: {r['score']:.4f}")
        年假申请流程: 0.8912
        员工手册（2026版）: 0.7654
    """
    client = get_client()
    if client is None:
        return []

    try:
        # 第一步：将查询文本转为向量
        # 这个向量会与 Qdrant 中存储的所有文档向量计算相似度
        query_vector = embed_text(query)

        # 第二步：在 Qdrant 中执行向量搜索
        # client.search() 内部会：
        #   1. 使用 HNSW 算法（层次导航小世界图）快速定位候选向量
        #   2. 计算查询向量与候选向量的余弦相似度
        #   3. 按相似度降序排列
        #   4. 过滤低于 score_threshold 的结果
        #   5. 返回 top limit 个结果
        results = client.search(
            collection_name=COLLECTION_NAME,
            query_vector=query_vector,
            limit=limit,
            score_threshold=score_threshold,
        )

        # 第三步：将 Qdrant 结果转换为业务格式
        # r.score: 余弦相似度分数（归一化向量下等于点积）
        # r.payload: 写入时存储的文档元数据
        return [
            {
                "document_id": r.payload.get("document_id"),
                "title": r.payload.get("title", ""),
                "score": r.score,
                "payload": r.payload,
            }
            for r in results
        ]
    except Exception as e:
        logger.error(f"向量检索失败: {e}")
        return []
