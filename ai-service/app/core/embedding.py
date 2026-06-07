"""
文本向量化（Embedding）模块

========================================
📚 模块概述
========================================
本模块负责将文本转换为固定长度的浮点数向量（Embedding），
是 RAG 系统中连接自然语言与数学运算的桥梁。

什么是文本向量化（Embedding）？
    将自然语言文本映射到高维向量空间中的一个点。
    语义相近的文本在向量空间中的距离也相近。
    例如："年假怎么申请" 和 "如何请年假" 的向量会非常接近，
    而 "年假怎么申请" 和 "系统架构设计" 的向量会相距很远。

    文本 → 嵌入模型 → [0.12, -0.34, 0.56, ..., 0.78]  （384 维浮点数组）

为什么向量维度是 384？
    本项目使用 all-MiniLM-L6-v2 模型，它的输出维度就是 384。
    常见模型的维度对比：
    - all-MiniLM-L6-v2:    384 维（轻量级，适合开发/演示）
    - all-mpnet-base-v2:   768 维（精度更高，体积更大）
    - text-embedding-ada-002: 1536 维（OpenAI 的商用模型）

本模块支持两种模式：
    1. 真实模式：使用 sentence-transformers 的 all-MiniLM-L6-v2 模型
    2. 演示模式：使用确定性哈希算法生成伪向量（无需安装 ML 库）

💡 学习要点:
    1. 文本向量化的原理：如何将语义信息编码为数学向量
    2. 向量归一化（Normalization）的意义：使余弦相似度等价于点积
    3. 懒加载（Lazy Loading）设计模式在模型加载中的应用
    4. 确定性伪向量：在没有 GPU/模型时如何保持系统可演示

架构角色：
    本模块是 RAG 流水线的第一步 —— 向量化。
    所有文本（用户查询和文档内容）都需要经过本模块处理才能进行向量搜索。
"""

import hashlib
from typing import List, Optional

# =====================================================
# 向量维度配置
# =====================================================
# 必须与嵌入模型的输出维度一致！
# all-MiniLM-L6-v2 输出 384 维向量
# 如果使用不同模型，需要相应修改此值和 Qdrant 集合配置
VECTOR_SIZE = 384

# 全局模型实例（懒加载）
# 使用模块级变量实现单例模式，避免重复加载模型（模型文件约 80MB）
_model = None


def _get_model():
    """
    懒加载 sentence-transformers 模型

    为什么使用懒加载？
        1. 模型文件较大（约 80MB），首次加载需要时间
        2. 如果只是使用 Mock 模式，就不需要加载模型
        3. 避免在 import 时就触发模型下载（可能因网络问题导致导入失败）

    懒加载模式：只有在第一次实际需要使用模型时才加载

    Returns:
        SentenceTransformer: 加载好的模型实例，如果未安装库则返回 None
    """
    global _model
    if _model is None:
        try:
            from sentence_transformers import SentenceTransformer
            # all-MiniLM-L6-v2:
            #   - 6 层 Transformer（L6）
            #   - MiniLM 蒸馏模型（体积小、速度快）
            #   - 首次使用会从 HuggingFace 下载模型文件
            _model = SentenceTransformer("all-MiniLM-L6-v2")
        except ImportError:
            # sentence-transformers 未安装时返回 None
            # 调用方会自动降级到伪向量模式
            return None
    return _model


def embed_text(text: str) -> List[float]:
    """
    将单条文本转化为向量

    优先级：
        1. Ollama Embedding API（如果 LLM_PROVIDER=ollama 且 Ollama 可用）
        2. sentence-transformers 本地模型（如果已安装）
        3. 伪向量（降级兜底）

    Args:
        text: 输入文本（如用户问题或文档标题+内容）

    Returns:
        List[float]: 归一化后的浮点向量
    """
    # 优先使用 Ollama Embedding（学习环境推荐）
    provider = __import__("os").getenv("LLM_PROVIDER", "ollama").lower()
    if provider == "ollama":
        ollama_vector = _ollama_embed(text)
        if ollama_vector is not None:
            return ollama_vector

    # 其次使用 sentence-transformers 本地模型
    model = _get_model()
    if model is not None:
        vector = model.encode(text).tolist()
        return vector

    # 最后降级到伪向量
    return _pseudo_embed(text)


def embed_texts(texts: List[str]) -> List[List[float]]:
    """
    批量向量化 —— 将多条文本同时转为向量

    Args:
        texts: 输入文本列表

    Returns:
        List[List[float]]: 向量列表
    """
    model = _get_model()
    if model is not None:
        return model.encode(texts).tolist()
    # Ollama 或伪向量模式：逐条生成
    return [embed_text(t) for t in texts]


def _ollama_embed(text: str) -> Optional[List[float]]:
    """
    使用 Ollama Embedding API 进行向量化

    Ollama 的 Embedding API：
        POST http://ollama:11434/api/embed
        {"model": "nomic-embed-text", "input": "文本"}

    推荐的 Embedding 模型：
        - nomic-embed-text: 768 维，英文+中文，轻量高效
        - mxbai-embed-large: 1024 维，更精确
        - all-minilm: 384 维，与本地 sentence-transformers 兼容

    注意：不同模型的向量维度不同！
        使用新模型时需要同步更新 VECTOR_SIZE 和 Qdrant 集合配置。

    Args:
        text: 输入文本

    Returns:
        Optional[List[float]]: 向量，失败返回 None（降级到其他模式）
    """
    import json
    import urllib.request
    import urllib.error
    import os

    base_url = os.getenv("OLLAMA_BASE_URL", "http://ollama:11434")
    model = os.getenv("OLLAMA_EMBED_MODEL", "nomic-embed-text")

    try:
        payload = {
            "model": model,
            "input": text,
        }

        req = urllib.request.Request(
            f"{base_url}/api/embed",
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )

        with urllib.request.urlopen(req, timeout=30) as resp:
            result = json.loads(resp.read().decode("utf-8"))

        # Ollama embed API 返回 {"embeddings": [[...]]}
        embeddings = result.get("embeddings", [])
        if embeddings and len(embeddings) > 0:
            return embeddings[0]

        return None

    except Exception as e:
        # Ollama 不可用或模型未下载，返回 None 触发降级
        return None


def _pseudo_embed(text: str) -> List[float]:
    """
    伪向量化（演示用）—— 基于文本哈希生成确定性向量

    什么是确定性向量？
        同一段文本每次生成的向量都相同。
        这是通过哈希函数保证的：相同输入 → 相同哈希 → 相同向量。

    为什么需要伪向量？
        - 开发环境可能没有 GPU 或 ML 库
        - 演示时不需要真实的语义能力
        - 保证系统在缺少依赖时仍然可以运行（降级策略）

    伪向量的局限性：
        - 不具备真正的语义理解能力
        - "年假" 和 "请假" 的伪向量相似度可能很低
        - 仅用于测试系统流程是否通畅，不用于评估搜索质量

    算法原理：
        1. 对文本进行 SHA-256 哈希，得到 32 字节的确定性字节序列
        2. 通过迭代哈希扩展到 384 个值（每个维度一个值）
        3. 将每个值归一化到 [0, 1] 范围
        4. 对整个向量做 L2 归一化（使向量模长为 1）

    Args:
        text: 输入文本

    Returns:
        List[float]: 归一化的 384 维伪向量
    """
    # 第一步：将文本编码为字节并进行 SHA-256 哈希
    # SHA-256 输出 32 字节（256 位），作为后续扩展的"种子"
    text_bytes = text.encode("utf-8")
    hash_bytes = hashlib.sha256(text_bytes).digest()

    vector = []
    for i in range(VECTOR_SIZE):
        # 第二步：迭代哈希扩展
        # 32 字节不够 384 维，需要通过组合哈希来扩展
        # byte_idx: 循环使用哈希字节（0-31）
        byte_idx = i % len(hash_bytes)
        # hash_offset: 利用循环索引制造变化，避免所有维度使用相同的字节
        hash_offset = (i // len(hash_bytes)) * byte_idx
        # 对每一步生成新的哈希值，确保每个维度有不同的值
        extended = hashlib.sha256(f"{text}:{i}:{hash_offset}".encode()).digest()
        # 取一个字节（0-255）归一化到 [0.0, 1.0]
        vector.append(float(extended[byte_idx % len(extended)]) / 255.0)

    # 第三步：L2 归一化（L2 Normalization）
    # 使向量的欧几里得长度（L2 范数）等于 1.0
    # 公式：v_normalized = v / ||v||，其中 ||v|| = sqrt(sum(v_i^2))
    #
    # 为什么归一化很重要？
    #   1. 归一化后，余弦相似度 = 点积（计算更快）
    #      cos(a, b) = (a · b) / (||a|| * ||b||) = a · b （当 ||a|| = ||b|| = 1）
    #   2. 消除向量大小的影响，只关注方向（语义含义）
    #   3. Qdrant 等向量数据库期望输入向量是归一化的
    norm = sum(x * x for x in vector) ** 0.5
    if norm > 0:
        vector = [x / norm for x in vector]

    return vector
