"""
配置管理模块

========================================
📚 模块概述
========================================
本模块负责 AI 服务的全局配置管理。使用 Pydantic 的 BaseSettings 类实现
类型安全的配置加载，支持从环境变量和 .env 文件中读取配置。

为什么使用 Pydantic BaseSettings？
    - 自动类型转换：环境变量是字符串，BaseSettings 能自动转为 int、float、bool 等
    - 校验机制：配置值不合法时会立即报错，而不是运行时才发现
    - 环境变量优先级：环境变量 > .env 文件 > 代码中的默认值
    - IDE 友好：有类型提示，开发体验更好

💡 学习要点:
    1. Pydantic BaseSettings 的 env_file 机制如何实现多环境配置
    2. 每个 AI/ML 配置参数的含义和对系统行为的影响
    3. 如何通过环境变量在不修改代码的情况下切换 Mock/Real 模式
    4. 配置分组的设计思路：应用、LLM、向量数据库、缓存、RAG 参数

架构角色：
    本模块是整个 AI 服务的"配置中心"，所有其他模块通过 `from app.config import settings`
    获取配置，实现配置的集中管理和单一数据源。
"""

# Pydantic v2 中，配置类从 pydantic_settings 包导入（不再是 pydantic 直接提供）
from pydantic_settings import BaseSettings
from typing import List


class Settings(BaseSettings):
    """
    应用配置类 —— 集中管理所有配置项

    设计原则：
        - 每个配置项都有合理的默认值，确保开发环境"开箱即用"
        - 敏感信息（API Key、密码）默认为空，通过环境变量注入
        - 配置项按功能分组，便于理解和维护

    配置加载优先级（从高到低）：
        1. 系统环境变量（如 export OPENAI_API_KEY=sk-xxx）
        2. .env 文件中的值
        3. 代码中定义的默认值

    Example:
        >>> from app.config import settings
        >>> settings.LLM_MODEL
        'gpt-3.5-turbo'
        >>> settings.QDRANT_HOST
        'localhost'
    """

    # --------------------------------------------------
    # 应用基础配置
    # --------------------------------------------------
    # APP_NAME: 服务名称，用于日志和监控标识
    APP_NAME: str = "KnowledgeBase AI Service"
    # DEBUG: 调试模式开关。开启后：
    #   - 日志级别更详细
    #   - 错误信息包含堆栈跟踪
    #   - uvicorn 开启 hot-reload
    #   生产环境务必设为 False
    DEBUG: bool = True

    # --------------------------------------------------
    # CORS（跨域资源共享）配置
    # --------------------------------------------------
    # CORS 是浏览器安全机制，限制不同源的 HTTP 请求
    # 前端（localhost:5173）和后端（localhost:8000）端口不同，属于跨域
    # 开发环境需要允许跨域，否则前端无法调用 API
    CORS_ORIGINS: List[str] = ["http://localhost:5173", "http://localhost:8080"]

    # --------------------------------------------------
    # LLM（大语言模型）配置
    # --------------------------------------------------
    # OPENAI_API_KEY: OpenAI API 密钥，为空时使用 Mock 模式
    #   获取方式：https://platform.openai.com/api-keys
    OPENAI_API_KEY: str = ""
    # ANTHROPIC_API_KEY: Anthropic API 密钥（Claude 系列模型）
    ANTHROPIC_API_KEY: str = ""
    # LLM_MODEL: 使用的模型名称
    #   - gpt-3.5-turbo: OpenAI 的快速模型，性价比高
    #   - gpt-4: OpenAI 的更强模型，推理能力更好
    #   - claude-haiku-4-5-20251001: Anthropic 的快速模型
    LLM_MODEL: str = "gpt-3.5-turbo"
    # LLM_TEMPERATURE: 生成温度，控制回答的随机性
    #   - 0.0: 确定性输出，适合事实性问答（推荐用于 RAG）
    #   - 0.7: 有一定创造性，适合对话场景
    #   - 1.0: 高随机性，适合创意写作
    LLM_TEMPERATURE: float = 0.7
    # LLM_MAX_TOKENS: 单次生成的最大 token 数
    #   1 个中文字 ≈ 1-2 个 token，2000 tokens 约等于 1000-2000 字
    LLM_MAX_TOKENS: int = 2000

    # --------------------------------------------------
    # Qdrant 向量数据库配置
    # --------------------------------------------------
    # Qdrant 是专为向量搜索优化的开源数据库
    # 向量搜索是 RAG 的核心：将文本转为向量后，通过数学运算找到语义最相似的文档
    QDRANT_HOST: str = "localhost"
    QDRANT_PORT: int = 6333  # Qdrant 默认 gRPC 端口
    # QDRANT_COLLECTION: 集合名称（类似 MySQL 的表名）
    #   所有文档向量存储在同一个集合中
    QDRANT_COLLECTION: str = "documents_collection"

    # --------------------------------------------------
    # Redis 缓存配置
    # --------------------------------------------------
    # Redis 在本系统中的用途：
    #   1. 缓存热门文档（减少数据库查询）
    #   2. 存储搜索热词（Sorted Set，按搜索次数排序）
    #   3. 用户会话管理（Hash 结构，设 TTL 自动过期）
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379  # Redis 默认端口
    REDIS_DB: int = 0       # 数据库编号（Redis 有 0-15 共 16 个逻辑数据库）
    REDIS_PASSWORD: str = ""  # 密码，为空表示无密码

    # --------------------------------------------------
    # Embedding（文本向量化）配置
    # --------------------------------------------------
    # EMBEDDING_MODEL: 使用的嵌入模型
    #   all-MiniLM-L6-v2 的特点：
    #   - 输出 384 维向量（维度越低，搜索越快，但信息损失越多）
    #   - 专门针对语义相似度任务优化
    #   - 模型体积小（约 80MB），适合开发环境
    EMBEDDING_MODEL: str = "sentence-transformers/all-MiniLM-L6-v2"
    # EMBEDDING_DIMENSION: 向量维度
    #   必须与嵌入模型的输出维度一致！
    #   all-MiniLM-L6-v2 → 384 维
    #   text-embedding-ada-002 → 1536 维
    EMBEDDING_DIMENSION: int = 384

    # --------------------------------------------------
    # RAG（检索增强生成）配置
    # --------------------------------------------------
    # RAG_TOP_K: 检索时返回的最相关文档数量
    #   - 值越大，LLM 获得的上下文越丰富，但 token 消耗也越多
    #   - 通常 3-5 是较好的平衡点
    RAG_TOP_K: int = 5
    # RAG_SCORE_THRESHOLD: 相似度阈值（0.0-1.0）
    #   只返回余弦相似度大于此阈值的文档
    #   - 0.3: 宽松，可能返回不太相关的文档
    #   - 0.7: 严格，只返回高度相关的文档
    #   - 过高可能遗漏有用信息，过低可能引入噪声
    RAG_SCORE_THRESHOLD: float = 0.7

    class Config:
        """
        Pydantic 配置元数据

        env_file: 指定环境变量文件路径
            项目根目录下的 .env 文件，格式为 KEY=VALUE

        case_sensitive: 环境变量名是否区分大小写
            设为 True 意味着环境变量名必须与属性名完全一致
            例如：QDRANT_HOST=qdrant-server（而不是 qdrant_host=xxx）
        """
        env_file = ".env"
        case_sensitive = True


# =====================================================
# 创建全局配置单例
# =====================================================
# 使用模块级变量实现单例模式：
#   - Python 模块只加载一次，因此 settings 只会实例化一次
#   - 所有模块通过 `from app.config import settings` 引用同一个实例
#   - 这是 Python 中实现配置单例的惯用方式
settings = Settings()
