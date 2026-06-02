"""
RAG 检索器模块

========================================
📚 模块概述
====================================
本模块是 RAG（Retrieval-Augmented Generation，检索增强生成）流程的核心编排器，
将"向量检索"和"LLM 生成"两个步骤串联起来。

为什么叫"检索增强生成"？
    传统的 LLM 直接回答问题，可能产生"幻觉"（编造不存在的信息）。
    RAG 先从知识库中检索相关文档，再让 LLM 基于这些文档生成回答，
    从而"增强"了 LLM 回答的准确性和可追溯性。

RAG 流程详解：

    ┌──────────┐
    │ 用户问题  │  例: "年假怎么申请？"
    └────┬─────┘
         │
         ▼
    ┌──────────────┐
    │ Step 1: 检索  │  向量搜索，找到最相关的 top-k 文档
    │ (Retrieve)   │  embed_text(question) → cosine similarity search
    └────┬─────────┘
         │  结果: [{title: "年假申请流程", score: 0.95}, ...]
         ▼
    ┌──────────────┐
    │ Step 2: 构建  │  将检索结果格式化为文本上下文
    │ (Context)    │  "文档1: 年假申请流程\n文档2: 员工手册..."
    └────┬─────────┘
         │
         ▼
    ┌──────────────┐
    │ Step 3: 生成  │  将问题 + 上下文发送给 LLM
    │ (Generate)   │  LLM 基于上下文生成准确回答
    └────┬─────────┘
         │
         ▼
    ┌──────────────┐
    │   最终回答    │  {answer, sources, followUpQuestions}
    └──────────────┘

为什么 score_threshold 设为 0.3？
    在检索器中我们使用较低的阈值（0.3），宁可多返回一些结果。
    后续由 LLM 来判断哪些信息是真正有用的（LLM 具有信息筛选能力）。
    如果阈值设得太高，可能会遗漏有用的上下文信息。

💡 学习要点:
    1. RAG 的三步流程：Retrieve → Context Build → Generate
    2. 为什么 RAG 能减少 LLM 的"幻觉"
    3. score_threshold 在不同阶段（检索 vs 最终结果）的不同策略
    4. 检索结果到 LLM 上下文的格式化方法

架构角色：
    本模块是 RAG 流水线的"编排器"，协调 vector_store 和 llm 两个模块。
    对外暴露 retrieve_and_generate() 函数，被 API 层直接调用。
"""

import logging
from typing import List, Dict, Any

# 导入向量搜索功能
from .vector_store import search as vector_search
# 导入 LLM 回答生成功能
from .llm import generate_answer

logger = logging.getLogger(__name__)


def retrieve_and_generate(question: str, top_k: int = 5) -> Dict[str, Any]:
    """
    RAG 完整流程：检索 → 上下文构建 → 生成

    这是 RAG 的主入口函数，实现了完整的检索增强生成流水线。

    流程说明：
        1. 向量检索（Retrieve）：
           将用户问题转为向量，在 Qdrant 中搜索语义最相似的文档

        2. 上下文构建（Context Building）：
           将搜索结果格式化为文本，作为 LLM 的输入参考
           格式："文档1: 标题\n文档2: 标题\n..."

        3. 回答生成（Generate）：
           将问题和上下文发送给 LLM，生成基于事实的自然语言回答

    Args:
        question: 用户的问题文本
            例: "年假怎么申请"
            例: "系统的技术架构是什么"

        top_k: 检索返回的文档数量
            默认 5，表示从 Qdrant 中取相似度最高的 5 篇文档
            值越大，LLM 获得的上下文越丰富，但 token 消耗也越多

    Returns:
        Dict[str, Any]: RAG 的完整输出，包含：
            - answer (str): 生成的自然语言回答
            - sources (list): 引用的来源文档（含 ID、标题、相关度分数）
            - followUpQuestions (list): LLM 建议的后续追问问题

    Example:
        >>> result = retrieve_and_generate("年假怎么申请")
        >>> print(result["answer"])
        '根据《员工手册》和《年假申请流程》规定...'
        >>> print(result["sources"])
        [{'documentId': 2, 'title': '年假申请流程', 'relevance': 0.95}]

    Raises:
        本函数不会主动抛出异常，所有错误都在内部处理并返回降级结果
    """
    # ==================== Step 1: 向量检索 ====================
    # 使用向量搜索找到与用户问题语义最相关的文档
    # score_threshold=0.3 是较低的阈值：
    #   - 宁可多返回一些结果（提高查全率 recall）
    #   - 后续由 LLM 来筛选有用信息（LLM 的信息提取能力很强）
    logger.info(f"RAG 检索: {question}")
    search_results = vector_search(question, limit=top_k, score_threshold=0.3)

    # ==================== Step 2: 构建上下文 ====================
    # 将搜索结果格式化为文本，作为 LLM 的参考上下文
    # 这里同时构建两个输出：
    #   - context_parts: 文本格式的上下文（发给 LLM）
    #   - sources: 结构化的来源信息（返回给前端展示）
    context_parts = []
    sources = []
    for i, result in enumerate(search_results, 1):
        # 格式：文档序号 + 标题
        context_parts.append(f"文档{i}: {result.get('title', '')}")
        sources.append({
            "documentId": result.get("document_id"),
            "title": result.get("title", ""),
            # round(x, 4): 保留 4 位小数，避免浮点精度问题
            "relevance": round(result.get("score", 0), 4),
        })

    # 将所有上下文片段拼接为一个字符串
    # "\n".join() 用换行符分隔，让 LLM 能清晰区分不同文档
    context = "\n".join(context_parts)
    logger.info(f"检索到 {len(search_results)} 条相关文档")

    # ==================== Step 3: LLM 生成 ====================
    # 将问题和检索到的上下文一起发送给 LLM
    # LLM 会基于这些上下文生成准确的回答
    # 如果检索结果为空，LLM 会在 System Prompt 的指引下告知用户未找到相关信息
    result = generate_answer(question, context, sources)

    return result
