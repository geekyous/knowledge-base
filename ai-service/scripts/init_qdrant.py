"""
Qdrant 向量数据库初始化脚本

========================================
📚 模块概述
========================================
本脚本负责初始化 Qdrant 向量数据库，是系统部署流程中的关键步骤。
它创建向量集合、生成文档向量并写入数据库，使 RAG 系统能够进行语义搜索。

什么是向量数据库初始化？
    在 RAG 系统上线前，需要将已有的文档数据转换为向量并存入向量数据库。
    这个过程类似于传统数据库的"建表 + 导入数据"，但额外包含了文本向量化步骤。

    初始化流程：
    ┌─────────────────┐
    │ 1. 连接 Qdrant  │  确保服务可用
    ├─────────────────┤
    │ 2. 创建集合      │  配置向量维度(384)和距离度量(余弦相似度)
    ├─────────────────┤
    │ 3. 创建索引      │  为 payload 字段建立索引（加速过滤查询）
    ├─────────────────┤
    │ 4. 文档向量化    │  将文本转换为 384 维浮点向量
    ├─────────────────┤
    │ 5. 写入数据      │  使用 upsert 批量插入
    ├─────────────────┤
    │ 6. 验证          │  执行测试搜索确认数据正确
    └─────────────────┘

为什么需要 Payload 索引？
    Qdrant 不仅存储向量，还存储每个向量的元数据（payload）。
    为 payload 字段创建索引后，可以在向量搜索时添加过滤条件。
    例如：搜索 category="人事制度" 的文档时，先通过索引缩小范围，
    再在匹配的文档中做向量相似度计算，大幅提升搜索效率。

💡 学习要点:
    1. Qdrant PointStruct：向量 + 载荷的数据结构设计
    2. Payload 索引类型：INTEGER vs KEYWORD 的适用场景
    3. 真实嵌入 vs 伪嵌入：开发环境和生产环境的向量化策略
    4. HNSW 算法：Qdrant 底层使用的近似最近邻搜索算法
    5. 向量归一化（L2 Normalization）：使余弦相似度等价于点积

架构角色：
    本脚本是基础设施初始化工具，通常在 docker compose up 后运行一次。
    它确保 Qdrant 中有可供 RAG 流程使用的向量数据。

使用方式：
    # 使用伪向量（演示/开发）
    python scripts/init_qdrant.py --host localhost --port 6333

    # 使用真实向量模型（需要安装 sentence-transformers）
    python scripts/init_qdrant.py --host localhost --port 6333 --real-embedding
"""

import argparse
import json
import sys
from typing import List

# 尝试导入 qdrant_client 库
# 如果未安装，给出明确的安装提示并退出
# 这是脚本文件的常见模式：在顶层检测依赖，而不是在运行时才发现缺失
try:
    from qdrant_client import QdrantClient
    from qdrant_client.models import (
        Distance,         # 距离度量枚举：COSINE（余弦）、EUCLID（欧氏）、DOT（点积）
        PointStruct,      # 向量点的数据结构：id + vector + payload
        VectorParams,     # 向量参数配置：维度 + 距离度量
        PayloadSchemaType,  # Payload 索引类型：INTEGER、KEYWORD、TEXT 等
    )
except ImportError:
    print("请先安装 qdrant-client: pip install qdrant-client")
    sys.exit(1)


# =====================================================
# 示例文档数据（与 V2__seed_data.sql 中的文档对应）
# =====================================================
# 这些文档与 Java 后端数据库中的种子数据保持一致。
# 每篇文档的 ID 必须与 MySQL 中的 document_id 对应，
# 这样前端可以通过 ID 关联获取完整的文档信息。
#
# 文档内容的格式说明：
#   - content 字段使用了"关键词堆叠"的方式（而非完整句子）
#   - 这是因为嵌入模型对关键词密集的文本也能产生较好的语义向量
#   - 在生产环境中，应使用文档的完整正文内容
SAMPLE_DOCUMENTS = [
    {
        "id": 1,
        "title": "员工手册（2026版）",
        "summary": "公司员工行为规范和基本制度汇编",
        # content 使用空格分隔的关键词，便于向量化模型提取语义
        "content": "考勤管理 工作时间 周一至周五 9:00-18:00 请假制度 年假 病假 事假 婚假 加班制度 薪酬福利 基本工资 绩效奖金 五险一金 行为规范 职业道德 信息技术使用",
        "category": "人事制度",
        "author": "admin",
    },
    {
        "id": 2,
        "title": "年假申请流程",
        "summary": "详细说明年假天数计算方法和申请审批流程",
        "content": "年假天数计算 1-5年5天 5-10年7天 10-15年10天 15年以上15天 申请流程 提前15个工作日 OA系统 直属领导审批 部门经理审批 HR备案 年假不可跨年 离职未休年假按日工资300%折算",
        "category": "人事制度",
        "author": "editor",
    },
    {
        "id": 3,
        "title": "企业知识库系统架构设计",
        "summary": "基于微服务架构的企业知识库系统整体技术架构",
        "content": "系统架构 Vue3 Spring Boot FastAPI LangChain MySQL Redis Elasticsearch Qdrant 微服务架构 用户认证 文档管理 搜索模块 AI问答 RAG 向量检索 Docker部署",
        "category": "技术文档",
        "author": "editor",
    },
    {
        "id": 4,
        "title": "RESTful API 接口规范",
        "summary": "系统所有REST API的设计规范和通用约定",
        "content": "RESTful API URL规范 HTTP方法 GET POST PUT DELETE 统一响应格式 认证接口 文档接口 搜索接口 AI问答接口 错误码 400 401 403 404 500",
        "category": "技术文档",
        "author": "editor",
    },
    {
        "id": 5,
        "title": "销售话术与客户沟通指南",
        "summary": "标准销售话术和客户常见问题应对",
        "content": "销售话术 开场白 电话开场 会面开场 需求挖掘 产品演示 异议处理 价格太贵 已有系统 数据安全 学习成本 成交话术 POC测试",
        "category": "销售支持",
        "author": "editor",
    },
    {
        "id": 6,
        "title": "合同管理暂行办法",
        "summary": "公司合同签订、审批、归档的管理规定",
        "content": "合同管理 审批权限 10万元部门经理 50万元分管副总 50万元以上总经理 审批流程 起草 法务审核 领导审批 用印归档 合同归档 行政部",
        "category": "合规法务",
        "author": "user1",
    },
]

# 集合名称：与 app/core/vector_store.py 中的 COLLECTION_NAME 保持一致
# 所有文档向量存储在同一个集合中，通过 payload 的 category 字段区分类型
COLLECTION_NAME = "documents"

# 向量维度：必须与嵌入模型的输出维度一致
# all-MiniLM-L6-v2 输出 384 维向量
# 如果更换嵌入模型，需要同时修改此值和 Qdrant 集合配置
VECTOR_SIZE = 384


def create_simple_embedding(text: str) -> List[float]:
    """
    基于哈希的伪向量化方法（演示/开发环境使用）

    使用 SHA-256 哈希算法生成确定性的伪向量。同一文本每次生成的向量相同，
    但不具备真正的语义理解能力。

    为什么需要伪向量？
        1. 开发环境可能没有 GPU 或未安装 sentence-transformers
        2. CI/CD 环境中不需要真实向量即可测试系统流程
        3. 快速验证：伪向量生成速度远快于真实模型推理

    伪向量的局限性：
        - "年假" 和 "请假" 的伪向量可能毫不相关
        - 无法实现真正的语义搜索
        - 仅用于验证数据管道是否通畅

    Args:
        text: 输入文本（通常是 title + summary + content 的拼接）

    Returns:
        List[float]: 归一化后的 384 维伪向量（模长为 1.0）

    Example:
        >>> vec = create_simple_embedding("年假申请")
        >>> len(vec)
        384
    """
    import hashlib

    # 第一步：SHA-256 哈希生成确定性种子
    text_bytes = text.encode("utf-8")
    hash_obj = hashlib.sha256(text_bytes)
    hash_bytes = hash_obj.digest()  # 32 字节（256 位）

    # 第二步：扩展到 384 维
    # SHA-256 只输出 32 字节，需要循环使用来填充 384 个维度
    # 每个维度的值 = 哈希字节中的某字节值 / 255（归一化到 [0, 1]）
    vector = []
    for i in range(VECTOR_SIZE):
        byte_idx = i % len(hash_bytes)  # 循环使用 32 字节
        vector.append(float(hash_bytes[byte_idx]) / 255.0)

    # 第三步：L2 归一化
    # 使向量的欧几里得长度（L2 范数）等于 1.0
    # 归一化后，余弦相似度 = 点积，简化相似度计算
    # 公式：v_normalized = v / ||v||，其中 ||v|| = sqrt(sum(v_i^2))
    norm = sum(x * x for x in vector) ** 0.5
    if norm > 0:
        vector = [x / norm for x in vector]

    return vector


def try_real_embedding(text: str) -> List[float]:
    """
    尝试使用 sentence-transformers 生成真实向量

    真实向量 vs 伪向量的核心区别：
        - 真实向量：语义相近的文本 → 向量距离近（如"年假"和"请假"）
        - 伪向量：基于哈希，无法保证语义相近的文本距离近

    本函数使用 all-MiniLM-L6-v2 模型：
        - 参数量：约 22M（轻量级）
        - 输出维度：384
        - 训练数据：10 亿+ 句对
        - 推理速度：CPU 上约 14ms/句

    注意：每次调用都会重新加载模型（非效率最优）。
    生产环境应使用 app/core/embedding.py 中的懒加载单例模式。

    Args:
        text: 输入文本

    Returns:
        List[float]: 384 维真实向量，如果模型不可用则返回 None
    """
    try:
        from sentence_transformers import SentenceTransformer

        # 加载模型（首次会从 HuggingFace 下载约 80MB 的模型文件）
        model = SentenceTransformer("all-MiniLM-L6-v2")
        # encode() 返回 numpy 数组，tolist() 转为 Python 列表
        embedding = model.encode(text)
        return embedding.tolist()
    except ImportError:
        # sentence-transformers 未安装，返回 None 让调用方降级到伪向量
        return None


def init_collection(client: QdrantClient):
    """
    创建或重建 Qdrant 向量集合

    集合（Collection）是 Qdrant 的核心数据结构，类似于 MySQL 的表。
    本函数执行以下步骤：
        1. 删除同名集合（如果存在）—— 确保干净初始化
        2. 创建新集合，配置向量维度和距离度量
        3. 为 payload 字段创建索引（加速过滤查询）

    为什么使用 Distance.COSINE（余弦相似度）？
        - 对文本语义搜索最常用的距离度量
        - 只关注向量方向（语义含义），忽略向量大小
        - 值域 [0, 1]（对归一化向量），便于设置阈值
        - 替代方案：EUCLID（欧氏距离，关注绝对位置）、DOT（点积，需预归一化）

    Payload 索引的作用：
        - document_id: INTEGER 索引，支持范围查询（如 WHERE document_id > 100）
        - category: KEYWORD 索引，支持精确匹配过滤（如 WHERE category = '人事制度'）
        没有索引时，过滤查询需要全表扫描，数据量大时性能会严重下降

    Args:
        client: 已连接的 QdrantClient 实例

    Raises:
        Exception: 如果集合创建失败（如 Qdrant 服务异常）
    """
    # 删除已有集合（开发环境使用，生产环境应改为"跳过如果存在"）
    collections = client.get_collections().collections
    if any(c.name == COLLECTION_NAME for c in collections):
        print(f"  删除已有集合: {COLLECTION_NAME}")
        client.delete_collection(COLLECTION_NAME)

    # 创建新集合
    # VectorParams 配置说明：
    #   size: 向量维度（必须与嵌入模型输出维度一致，本系统为 384）
    #   distance: 距离度量方式（COSINE = 余弦相似度）
    print(f"  创建集合: {COLLECTION_NAME} (向量维度: {VECTOR_SIZE})")
    client.create_collection(
        collection_name=COLLECTION_NAME,
        vectors_config=VectorParams(size=VECTOR_SIZE, distance=Distance.COSINE),
    )

    # 创建 payload 索引
    # Payload 索引让 Qdrant 能在向量搜索时高效过滤
    # 例如：搜索"只在人事制度分类中的相似文档"
    client.create_payload_index(
        collection_name=COLLECTION_NAME,
        field_name="document_id",
        field_schema=PayloadSchemaType.INTEGER,  # 整数类型索引，适合 ID 字段
    )
    client.create_payload_index(
        collection_name=COLLECTION_NAME,
        field_name="category",
        field_schema=PayloadSchemaType.KEYWORD,  # 关键词类型索引，适合枚举/分类字段
    )

    print("  集合创建完成 ✓")


def insert_documents(client: QdrantClient, use_real_embedding: bool = False):
    """
    将示例文档向量化并插入 Qdrant

    处理流程：
        1. 选择向量化方法（真实模型 or 伪向量）
        2. 对每篇文档：拼接文本 → 向量化 → 构造 PointStruct
        3. 批量 upsert 到 Qdrant

    PointStruct 数据结构：
        {
            id: int,           # 唯一标识（对应 MySQL 中的文档 ID）
            vector: [float],   # 384 维向量
            payload: {         # 元数据（随搜索结果返回）
                "document_id": int,
                "title": str,
                "summary": str,
                "category": str,
                "author": str
            }
        }

    为什么拼接 title + summary + content？
        标题和摘要包含最核心的语义信息，正文提供细节。
        嵌入模型有输入长度限制（通常 256-512 tokens），
        所以需要平衡信息量和长度。这里是演示数据，所以全部拼接。

    upsert 操作：
        UPDATE + INSERT 的组合。如果 ID 已存在则更新，否则插入。
        这保证了脚本的幂等性——重复运行不会产生重复数据。

    Args:
        client: 已连接的 QdrantClient 实例
        use_real_embedding: 是否尝试使用真实嵌入模型
    """
    print(f"\n📤 向量化 {len(SAMPLE_DOCUMENTS)} 篇文档...")

    # 确定向量化方法
    embed_func = create_simple_embedding  # 默认使用伪向量
    if use_real_embedding:
        # 尝试加载真实模型（如果可用）
        real_result = try_real_embedding("test")
        if real_result:
            print("  使用 sentence-transformers 生成真实向量")
            embed_func = lambda text: try_real_embedding(text)  # noqa: E731
        else:
            print("  ⚠ 未安装 sentence-transformers，使用伪向量（仅演示）")
            print("  提示: pip install sentence-transformers 可启用真实向量化")
    else:
        print("  使用伪向量（演示模式）")

    # 构造向量点列表
    points = []
    for doc in SAMPLE_DOCUMENTS:
        # 拼接标题、摘要和内容作为向量化输入
        text = f"{doc['title']} {doc['summary']} {doc['content']}"
        vector = embed_func(text)

        # 构造 PointStruct：Qdrant 的基本数据单元
        # id: 文档 ID（与 MySQL 中的 ID 对应，实现跨数据库关联）
        # vector: 384 维浮点向量（文档的语义表示）
        # payload: 元数据字典（搜索结果中可直接使用，无需回查 MySQL）
        points.append(
            PointStruct(
                id=doc["id"],
                vector=vector,
                payload={
                    "document_id": doc["id"],
                    "title": doc["title"],
                    "summary": doc["summary"],
                    "category": doc["category"],
                    "author": doc["author"],
                },
            )
        )

    # 批量 upsert：一次性写入所有向量点
    # 比逐条写入效率更高，Qdrant 内部会批量优化索引更新
    client.upsert(collection_name=COLLECTION_NAME, points=points)
    print(f"  插入 {len(points)} 条向量数据 ✓")


def verify(client: QdrantClient):
    """
    验证初始化结果

    执行以下检查：
        1. 集合的基本信息（名称、向量数量、维度、距离度量）
        2. 测试搜索：用"年假怎么申请"作为查询，验证向量搜索是否正常工作

    测试搜索的意义：
        如果初始化成功，搜索"年假怎么申请"应该返回"年假申请流程"文档，
        且相似度分数较高（真实向量应在 0.7+，伪向量可能较低）。

    Args:
        client: 已连接的 QdrantClient 实例
    """
    # 获取集合信息
    info = client.get_collection(COLLECTION_NAME)
    print(f"\n📊 集合状态:")
    print(f"  名称: {COLLECTION_NAME}")
    print(f"  向量数量: {info.points_count}")
    print(f"  向量维度: {info.config.params.vectors.size}")
    print(f"  距离度量: {info.config.params.vectors.distance}")

    # 执行测试搜索
    # 将查询文本向量化后在集合中搜索最相似的 3 个文档
    test_vector = create_simple_embedding("年假怎么申请")
    results = client.search(
        collection_name=COLLECTION_NAME,
        query_vector=test_vector,
        limit=3,
    )
    print(f"\n🔍 测试搜索 '年假怎么申请':")
    for r in results:
        # r.score: 余弦相似度分数（0.0-1.0）
        # r.payload: 文档的元数据
        print(f"  - {r.payload['title']} (score: {r.score:.4f})")


def main():
    """
    脚本主入口

    命令行参数说明：
        --host: Qdrant 服务器地址（默认 localhost）
        --port: Qdrant gRPC 端口（默认 6333）
        --real-embedding: 是否使用真实嵌入模型（默认使用伪向量）

    执行流程：连接 → 创建集合 → 向量化并写入 → 验证
    """
    parser = argparse.ArgumentParser(description="初始化 Qdrant 向量数据库")
    parser.add_argument("--host", default="localhost", help="Qdrant 主机")
    parser.add_argument("--port", type=int, default=6333, help="Qdrant 端口")
    parser.add_argument("--real-embedding", action="store_true", help="使用真实向量模型")
    args = parser.parse_args()

    print("=" * 50)
    print("🚀 Qdrant 向量数据库初始化")
    print("=" * 50)

    # 连接 Qdrant 服务
    try:
        client = QdrantClient(host=args.host, port=args.port)
        print(f"✓ 连接成功: {args.host}:{args.port}")
    except Exception as e:
        print(f"✗ 连接失败: {e}")
        print(f"  请确保 Qdrant 服务已启动: docker compose up -d qdrant")
        sys.exit(1)

    # 执行初始化流程
    init_collection(client)                              # 步骤1：创建集合
    insert_documents(client, use_real_embedding=args.real_embedding)  # 步骤2：写入数据
    verify(client)                                       # 步骤3：验证结果

    print("\n✅ Qdrant 初始化完成!")


if __name__ == "__main__":
    main()
