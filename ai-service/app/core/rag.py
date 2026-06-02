"""
RAG（检索增强生成）统一入口模块

========================================
📚 模块概述
====================================
本模块是 RAG 功能的门面（Facade），对外暴露简洁的函数接口，
隐藏内部模块的实现细节。

什么是 Facade 模式？
    Facade（外观）模式是一种结构型设计模式：
    为子系统中的一组接口提供一个一致的界面。

    在本项目中：
    ┌─────────────────────────────────────┐
    │           app.core.rag              │  ← Facade（门面）
    │  retrieve_and_generate()            │
    │  init_collection()                  │
    │  index_document()                   │
    ├─────────────────────────────────────┤
    │  retriever.py → vector_store.py     │  ← 子系统
    │  llm.py → embedding.py             │
    └─────────────────────────────────────┘

    API 层（chat.py）只需要 `from app.core.rag import retrieve_and_generate`，
    不需要知道底层调用了哪些模块。

RAG（Retrieval-Augmented Generation）是什么？
    一种将信息检索与大语言模型生成相结合的 AI 架构模式：

    传统 LLM:
        用户问题 → LLM → 回答（可能包含"幻觉"）

    RAG:
        用户问题 → 向量检索 → 相关文档 → LLM + 文档上下文 → 回答（基于事实）

    RAG 的优势：
        1. 减少幻觉：LLM 基于真实文档回答，而不是"凭记忆"
        2. 实时性：知识库更新后，不需要重新训练模型
        3. 可追溯：可以引用具体的文档来源
        4. 成本低：不需要微调或训练 LLM

    RAG 的局限：
        1. 依赖检索质量：如果检索不到相关文档，LLM 也无法给出好回答
        2. 上下文长度限制：LLM 能处理的上下文有限（通常 4K-128K tokens）
        3. 向量化信息损失：嵌入模型不能完美保留所有语义信息

💡 学习要点:
    1. Facade 设计模式在模块化系统中的应用
    2. __all__ 如何控制模块的公开 API
    3. RAG 架构端到端的工作流程
    4. 为什么检索 + 生成的组合比纯 LLM 更适合知识库场景

架构角色：
    本模块是 core 子系统的"公共 API"，API 层通过本模块间接调用 core 内部功能。
"""

# 从 retriever 导入完整的 RAG 流程函数
from .retriever import retrieve_and_generate
# 从 vector_store 导入初始化和索引函数
from .vector_store import init_collection, index_document

# __all__ 定义了当使用 `from app.core.rag import *` 时会导出的名称
# 这是一种"白名单"机制，控制模块的公开 API
# 只有这三个函数是外部需要使用的，其他内部函数保持私有
__all__ = ["retrieve_and_generate", "init_collection", "index_document"]
