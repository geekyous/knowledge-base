"""
Elasticsearch 索引初始化脚本

========================================
📚 模块概述
====================================
本脚本负责初始化 Elasticsearch 搜索引擎，创建配置了中文分词的索引，
并导入示例文档数据，为系统的全文搜索功能提供基础。

Elasticsearch vs Qdrant —— 本系统为什么同时使用两者？
    - Elasticsearch: 关键词全文搜索（BM25 算法），适合精确匹配和结构化查询
    - Qdrant: 向量语义搜索（余弦相似度），适合模糊匹配和语义理解
    - 两者互补：Elasticsearch 擅长"包含关键词的文档"，Qdrant 擅长"意思相近的文档"

    搜索对比示例：
        查询: "如何休假"
        Elasticsearch (BM25): 找到包含"休假"关键词的文档
        Qdrant (向量搜索): 找到语义相近的文档（如"年假申请流程"）

什么是中文分词？
    中文没有天然的词边界（不像英文用空格分隔），需要分词器将句子切分为词语。
    例如："年假申请流程" → ["年假", "申请", "流程"]

    本脚本使用 IK 分词器，它提供了两种分词模式：
        - ik_max_word: 最细粒度切分（索引时使用，提高召回率）
          "年假申请流程" → ["年假", "申请", "流程", "年", "假", "申", "请", "流", "程"]
        - ik_smart: 智能切分（搜索时使用，提高精确度）
          "年假申请流程" → ["年假", "申请", "流程"]

    索引用细粒度、搜索用智能切分的原因：
        索引时切得越细，搜索时越不容易漏掉匹配（高召回率）
        搜索时切得越准确，返回的结果越相关（高精确率）

Elasticsearch 核心概念：
    - Index（索引）: 类似数据库的表，存储一类文档
    - Document（文档）: 一条记录，JSON 格式
    - Mapping（映射）: 定义文档的字段类型和分词方式
    - Analyzer（分析器）: 文本处理管道（字符过滤 → 分词 → Token 过滤）
    - Shard（分片）: 索引的水平分片，支持分布式存储和查询

💡 学习要点:
    1. Elasticsearch Mapping 中的字段类型：text vs keyword 的区别
    2. IK 中文分词器的 ik_max_word 和 ik_smart 模式
    3. multi_match 查询和字段权重（title^3 表示标题权重 x3）
    4. number_of_shards 和 number_of_replicas 对性能和可靠性的影响
    5. 降级策略：IK 分词器不可用时回退到标准分词器

架构角色：
    本脚本与 init_qdrant.py 互补，负责全文搜索索引的初始化。
    两者通常一起运行，确保搜索服务（关键词 + 语义）都可用。

使用方式：
    python scripts/init_elasticsearch.py --host localhost --port 9200
"""

import argparse
import json
import sys

# 检测 elasticsearch 库是否已安装
try:
    from elasticsearch import Elasticsearch
except ImportError:
    print("请先安装 elasticsearch: pip install elasticsearch")
    sys.exit(1)


# =====================================================
# 示例文档数据
# =====================================================
# 这些文档数据与 init_qdrant.py 中的数据相同，
# 但 content 字段使用了完整的句子（而非关键词堆叠）。
# 这是因为 Elasticsearch 的全文搜索更适合完整句子：
# BM25 算法会计算词频（TF）和逆文档频率（IDF），
# 完整句子能提供更准确的词频统计。
SAMPLE_DOCUMENTS = [
    {
        "id": 1,
        "title": "员工手册（2026版）",
        "content": "公司员工行为规范和基本制度汇编。考勤管理：周一至周五9:00-18:00。请假制度：年假、病假、事假、婚假。加班制度：工作日1.5倍、周末2倍、法定假日3倍。薪酬福利：基本工资+绩效奖金+各项补贴。五险一金。行为规范：职业道德、信息技术使用。",
        "summary": "公司员工行为规范和基本制度汇编",
        "category": "人事制度",
        "author": "admin",
        "status": "PUBLISHED",
    },
    {
        "id": 2,
        "title": "年假申请流程",
        "content": "年假天数计算：工作1-5年享有5天，5-10年7天，10-15年10天，15年以上15天。申请流程：提前15个工作日提交申请，通过OA系统填写年假申请表。审批流程：直属领导审批、部门经理审批、HR备案确认。年假不可跨年累积，离职时未休年假按日工资300%折算。",
        "summary": "详细说明年假天数计算方法和申请审批流程",
        "category": "人事制度",
        "author": "editor",
        "status": "PUBLISHED",
    },
    {
        "id": 3,
        "title": "企业知识库系统架构设计",
        "content": "企业知识库问答系统采用前后端分离加AI微服务的混合架构。前端Vue3+TypeScript+ElementPlus，后端Java17+SpringBoot3.2，AI服务Python3.11+FastAPI+LangChain。数据库使用MySQL+Redis+Elasticsearch+Qdrant。核心模块包括用户认证、文档管理、搜索、AI问答（RAG）。",
        "summary": "基于微服务架构的企业知识库系统整体技术架构",
        "category": "技术文档",
        "author": "editor",
        "status": "PUBLISHED",
    },
    {
        "id": 4,
        "title": "RESTful API 接口规范",
        "content": "RESTful API接口设计规范。URL规范：基础路径api/v1，使用名词复数。HTTP方法：GET查询、POST创建、PUT更新、DELETE删除。统一响应格式包含code、message、data。认证接口、文档接口、搜索接口、AI问答接口。错误码：400参数错误、401未认证、403无权限、404不存在、500服务器错误。",
        "summary": "系统所有REST API的设计规范和通用约定",
        "category": "技术文档",
        "author": "editor",
        "status": "PUBLISHED",
    },
    {
        "id": 5,
        "title": "销售话术与客户沟通指南",
        "content": "标准销售话术和客户常见问题应对。开场白：电话开场和会面开场。需求挖掘：了解团队规模、痛点、AI需求、安全要求。产品演示：搜索功能、AI问答、文档管理、权限控制。异议处理：价格异议用ROI对比、已有系统展示AI差异化、数据安全说明私有化部署。成交话术：建议POC测试。",
        "summary": "标准销售话术和客户常见问题应对",
        "category": "销售支持",
        "author": "editor",
        "status": "PUBLISHED",
    },
    {
        "id": 6,
        "title": "合同管理暂行办法",
        "content": "公司合同签订审批归档的管理规定。审批权限：10万元以下部门经理、10到50万元分管副总、50万元以上总经理。审批流程：业务部门起草、法务审核、领导审批、用印归档。合同原件签订后5个工作日内交行政部归档。",
        "summary": "公司合同签订、审批、归档的管理规定",
        "category": "合规法务",
        "author": "user1",
        "status": "DRAFT",
    },
]

# 索引名称（类似 MySQL 的表名）
INDEX_NAME = "documents"

# =====================================================
# 索引配置（Settings + Mappings）
# =====================================================
# 这是 Elasticsearch 最核心的配置，定义了索引如何存储和检索数据。
#
# Settings 部分：
#   - number_of_shards: 主分片数量（影响写入性能和存储分布）
#     开发环境设为 1（单节点），生产环境通常设为 3-5
#   - number_of_replicas: 副本数量（影响读取性能和数据可靠性）
#     开发环境设为 0（无需副本），生产环境通常设为 1-2
#   - analysis: 自定义分析器配置
#
# Mappings 部分（字段类型说明）：
#   - text: 全文搜索类型，会被分词后建立倒排索引
#   - keyword: 精确匹配类型，不分词，用于聚合、排序、精确过滤
#   - long: 64位整数类型
#
# 字段类型选择原则：
#   - 需要全文搜索 → text（如 title、content）
#   - 需要精确匹配/聚合 → keyword（如 category、status）
#   - 数值计算/范围查询 → long/integer/float（如 id）
#
# analyzer vs search_analyzer：
#   - analyzer: 索引时使用的分词器（写入文档时）
#   - search_analyzer: 搜索时使用的分词器（查询时）
#   索引用 ik_max_word（细粒度），搜索用 ik_smart（粗粒度），
#   这是一种常见的中文搜索最佳实践。
INDEX_SETTINGS = {
    "settings": {
        "number_of_shards": 1,      # 单节点开发环境，1 个分片足够
        "number_of_replicas": 0,    # 开发环境不需要副本（节省磁盘空间）
        "analysis": {
            "analyzer": {
                # IK 最细粒度分词（索引时使用）
                "ik_max_word": {"type": "custom", "tokenizer": "ik_max_word"},
                # IK 智能分词（搜索时使用）
                "ik_smart": {"type": "custom", "tokenizer": "ik_smart"},
                # 标准分词器后备方案（IK 未安装时使用）
                # 对中文只能按单字切分，效果较差
                "standard_chinese": {
                    "type": "custom",
                    "tokenizer": "standard",
                    "filter": ["lowercase"],
                },
            }
        },
    },
    "mappings": {
        "properties": {
            # id 字段：使用 long 类型（64 位整数），用于范围查询和精确匹配
            "id": {"type": "long"},
            # title 字段：多字段映射（multi-field mapping）
            #   - 默认：IK 分词的全文搜索
            #   - keyword 子字段：不分词，用于精确匹配和排序
            #   - standard 子字段：标准分词器的全文搜索（后备方案）
            "title": {
                "type": "text",
                "analyzer": "ik_max_word",         # 索引时用细粒度分词
                "search_analyzer": "ik_smart",     # 搜索时用智能分词
                "fields": {
                    "keyword": {"type": "keyword"},                    # 精确匹配
                    "standard": {"type": "text", "analyzer": "standard_chinese"},  # 后备搜索
                },
            },
            # content 字段：正文内容，使用 IK 分词
            "content": {
                "type": "text",
                "analyzer": "ik_max_word",
                "search_analyzer": "ik_smart",
                "fields": {
                    "standard": {"type": "text", "analyzer": "standard_chinese"},
                },
            },
            # summary 字段：摘要，使用 IK 分词
            "summary": {
                "type": "text",
                "analyzer": "ik_max_word",
                "fields": {
                    "standard": {"type": "text", "analyzer": "standard_chinese"},
                },
            },
            # category 字段：分类，keyword 类型（不分词，用于精确过滤）
            # 例如：搜索时可以添加 filter: {"term": {"category": "人事制度"}}
            "category": {"type": "keyword"},
            # author 字段：作者，keyword 类型（不分词）
            "author": {"type": "keyword"},
            # status 字段：状态，keyword 类型（PUBLISHED / DRAFT 等枚举值）
            "status": {"type": "keyword"},
        }
    },
}


def create_index(es: Elasticsearch):
    """
    创建 Elasticsearch 索引

    执行流程：
        1. 删除同名索引（确保干净初始化）
        2. 尝试使用 IK 分词器配置创建索引
        3. 如果 IK 分词器不可用，降级到标准分词器

    降级策略（Fallback Strategy）：
        IK 分词器需要额外安装（Elasticsearch 插件），
        如果未安装则自动降级到标准分词器。
        标准分词器对中文只能按单字切分（"年假" → ["年", "假"]），
        搜索效果不如 IK，但至少保证系统能运行。

    Args:
        es: 已连接的 Elasticsearch 客户端实例

    Raises:
        Exception: 如果索引创建失败且不是 IK 分词器问题
    """
    # 删除已有索引（开发环境使用）
    if es.indices.exists(index=INDEX_NAME):
        print(f"  删除已有索引: {INDEX_NAME}")
        es.indices.delete(index=INDEX_NAME)

    # 尝试使用 IK 分词器创建索引
    try:
        es.indices.create(index=INDEX_NAME, body=INDEX_SETTINGS)
        print("  使用 IK 中文分词器 ✓")
    except Exception as e:
        # 检测是否是 IK 分词器未安装导致的错误
        if "ik_max_word" in str(e) or "tokenizer" in str(e).lower():
            print("  ⚠ IK分词器未安装，使用标准分词器（中文分词效果较差）")
            # 降级配置：所有 text 字段使用标准分词器
            fallback_settings = {
                "settings": {
                    "number_of_shards": 1,
                    "number_of_replicas": 0,
                },
                "mappings": {
                    "properties": {
                        "id": {"type": "long"},
                        "title": {"type": "text", "analyzer": "standard"},
                        "content": {"type": "text", "analyzer": "standard"},
                        "summary": {"type": "text", "analyzer": "standard"},
                        "category": {"type": "keyword"},
                        "author": {"type": "keyword"},
                        "status": {"type": "keyword"},
                    }
                },
            }
            es.indices.create(index=INDEX_NAME, body=fallback_settings)
        else:
            # 其他类型的错误，不处理，直接抛出
            raise

    print(f"  索引 {INDEX_NAME} 创建完成 ✓")


def index_documents(es: Elasticsearch):
    """
    将示例文档批量索引到 Elasticsearch

    索引过程说明：
        1. 对每篇文档，Elasticsearch 会自动使用配置的 analyzer 进行分词
        2. 分词结果建立倒排索引（inverted index）：
           "年假" → [文档1, 文档2]
           "申请" → [文档2]
           "架构" → [文档3]
        3. 搜索时，查询词也会被分词，然后在倒排索引中查找匹配的文档

    为什么需要 refresh？
        Elasticsearch 的索引操作是异步的（写入后不会立即可搜索）。
        refresh() 强制将内存中的索引数据刷写到可搜索状态。
        生产环境通常依赖自动 refresh（默认每 1 秒）。

    Args:
        es: 已连接的 Elasticsearch 客户端实例
    """
    print(f"\n📤 索引 {len(SAMPLE_DOCUMENTS)} 篇文档...")

    # 逐条索引文档
    # es.index() 参数：
    #   - index: 索引名称
    #   - id: 文档 ID（指定 ID 后可支持 upsert 操作）
    #   - body: 文档内容（JSON 格式）
    for doc in SAMPLE_DOCUMENTS:
        es.index(index=INDEX_NAME, id=doc["id"], body=doc)

    # 强制刷新索引，使文档立即可搜索
    # 开发/初始化脚本中需要手动刷新，确保后续的 verify 能搜到数据
    es.indices.refresh(index=INDEX_NAME)
    print(f"  索引完成 ✓")


def verify(es: Elasticsearch):
    """
    验证索引数据

    执行以下检查：
        1. 文档总数
        2. 多个测试查询的搜索结果

    测试搜索使用的 multi_match 查询：
        multi_match 允许同时在多个字段中搜索，并可以为不同字段设置权重。
        - title^3: 标题权重 x3（标题匹配更重要）
        - summary^2: 摘要权重 x2
        - content: 内容权重 x1（默认）
        权重影响 BM25 评分：标题匹配的文档会排在更前面。

    BM25 算法简介：
        Elasticsearch 默认的相关性评分算法。
        考虑三个因素：
        1. TF（词频）：关键词在文档中出现的频率越高，分数越高
        2. IDF（逆文档频率）：越罕见的关键词，分数越高
        3. 字段长度：短字段中的匹配比长字段中的匹配更有价值

    Args:
        es: 已连接的 Elasticsearch 客户端实例
    """
    # 获取索引中的文档总数
    count = es.count(index=INDEX_NAME)
    print(f"\n📊 索引状态:")
    print(f"  文档数量: {count['count']}")

    # 执行测试搜索
    # 使用不同类型的查询词来验证分词和搜索效果
    test_queries = ["年假", "API", "销售话术", "架构"]

    for query in test_queries:
        result = es.search(
            index=INDEX_NAME,
            body={
                "query": {
                    # multi_match: 在多个字段中搜索
                    # fields 中的 ^N 表示权重倍数
                    "multi_match": {
                        "query": query,
                        "fields": ["title^3", "summary^2", "content"],
                        # 默认使用 best_fields 策略：取匹配最好的字段的分数
                    }
                },
                "size": 2,  # 只返回前 2 个结果
            },
        )
        hits = result["hits"]["hits"]
        print(f"\n🔍 搜索 '{query}':")
        for hit in hits:
            # hit["_score"]: BM25 相关性评分（越高越相关）
            # hit["_source"]: 文档原始内容
            score = hit["_score"]
            title = hit["_source"]["title"]
            print(f"  - {title} (score: {score:.2f})")


def main():
    """
    脚本主入口

    命令行参数说明：
        --host: Elasticsearch 服务器地址（默认 localhost）
        --port: Elasticsearch HTTP 端口（默认 9200）

    执行流程：连接 → 创建索引 → 导入文档 → 验证
    """
    parser = argparse.ArgumentParser(description="初始化 Elasticsearch 索引")
    parser.add_argument("--host", default="localhost", help="ES 主机")
    parser.add_argument("--port", type=int, default=9200, help="ES 端口")
    args = parser.parse_args()

    print("=" * 50)
    print("🚀 Elasticsearch 索引初始化")
    print("=" * 50)

    # 连接 Elasticsearch
    es_url = f"http://{args.host}:{args.port}"
    try:
        es = Elasticsearch(es_url, request_timeout=10)
        info = es.info()
        print(f"✓ 连接成功: {info['name']} (ES {info['version']['number']})")
    except Exception as e:
        print(f"✗ 连接失败: {e}")
        print(f"  请确保 Elasticsearch 已启动: docker compose up -d elasticsearch")
        sys.exit(1)

    # 执行初始化流程
    create_index(es)       # 步骤1：创建索引（含中文分词配置）
    index_documents(es)    # 步骤2：导入示例文档
    verify(es)             # 步骤3：验证数据和搜索功能

    print("\n✅ Elasticsearch 初始化完成!")


if __name__ == "__main__":
    main()
