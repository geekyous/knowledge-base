"""
Redis 缓存初始化脚本

========================================
📚 模块概述
====================================
本脚本负责初始化 Redis 缓存，演示了 Redis 在知识库系统中的三种典型使用场景：
    1. 热门文档缓存（Sorted Set + TTL）
    2. 搜索热词统计（Sorted Set）
    3. 用户会话管理（Hash + TTL）

Redis 是什么？
    Redis（Remote Dictionary Server）是内存键值数据库，读写速度极快（微秒级）。
    在本系统中，Redis 充当"缓存层"，减轻 MySQL 的查询压力。

    数据在系统中的存储层次：
    ┌─────────────────────────────────────────────┐
    │ L1: Redis 缓存      → 热数据，微秒级响应    │
    ├─────────────────────────────────────────────┤
    │ L2: Elasticsearch    → 全文搜索，毫秒级响应  │
    ├─────────────────────────────────────────────┤
    │ L3: MySQL            → 持久存储，毫秒级响应  │
    └─────────────────────────────────────────────┘
    │ L4: Qdrant           → 向量搜索，毫秒级响应  │
    └─────────────────────────────────────────────┘

Redis 核心数据结构：
    本脚本使用了 Redis 最常用的三种数据结构：

    1. Sorted Set（有序集合）
       - 每个元素关联一个分数（score），自动按分数排序
       - 适合排行榜、热门列表等场景
       - 时间复杂度：O(log N) 插入/删除
       - 命令：ZADD（添加）、ZREVRANGE（按分数倒序获取）

    2. Hash（哈希表）
       - 类似 Python 的 dict，存储字段-值对
       - 适合存储对象（如用户信息、会话数据）
       - 时间复杂度：O(1) 读写
       - 命令：HSET（设置字段）、HGETALL（获取所有字段）

    3. String + TTL（字符串 + 过期时间）
       - 最基本的数据类型，可以存储任何序列化的数据
       - TTL（Time To Live）设置键的自动过期时间
       - 过期后 Redis 自动删除，无需手动清理

TTL（生存时间）策略：
    不同类型的数据设置不同的 TTL：
    - 热门文档/搜索热词：1 小时（3600 秒）—— 定期刷新统计数据
    - 用户会话：24 小时（86400 秒）—— 安全起见，过期需要重新登录

    为什么需要 TTL？
        1. 内存管理：Redis 是内存数据库，不设置过期会导致内存持续增长
        2. 数据新鲜度：过时数据应该被清理，下次访问时重新计算
        3. 安全性：会话等敏感数据不应永久存在

缓存设计模式（Cache-Aside Pattern）：
    这是最常用的缓存策略：
    1. 读数据：先查缓存 → 命中则返回 → 未命中则查数据库 → 写入缓存
    2. 写数据：先写数据库 → 再删除缓存（而非更新缓存）
    3. 缓存过期：TTL 到期后自动失效，触发下次读取时重新加载

💡 学习要点:
    1. Redis Sorted Set：如何实现带排序的缓存（浏览量排行榜）
    2. Redis Hash：如何存储结构化对象（用户会话）
    3. TTL 策略：不同数据类型的过期时间选择
    4. JSON 序列化：Python 对象与 Redis 存储的转换
    5. Cache-Aside 模式：缓存与数据库的一致性策略

架构角色：
    本脚本初始化 Redis 缓存数据。在 RAG 系统中，Redis 主要用于：
    - 缓存热门搜索结果（减少向量搜索的频率）
    - 存储用户对话上下文（支持多轮对话）
    - 搜索热词统计（提供搜索建议）

使用方式：
    python scripts/init_redis.py --host localhost --port 6379 --db 0
"""

import argparse
import json
import sys

# 检测 redis 库是否已安装
try:
    import redis
except ImportError:
    print("请先安装 redis: pip install redis")
    sys.exit(1)


def init_hot_documents(r: redis.Redis):
    """
    初始化热门文档缓存（使用 Sorted Set）

    使用 Redis Sorted Set 存储热门文档列表，按浏览量（viewCount）排序。
    浏览量最高的文档排在最前面，可以直接获取 Top N。

    为什么使用 Sorted Set 而不是 List？
        - Sorted Set 自动按分数排序，无需手动维护顺序
        - 可以高效地获取 Top N（ZRANGE 时间复杂度 O(log N + M)）
        - 支持分数更新（文档浏览量变化时只需 ZADD 更新分数）
        - List 只能按插入顺序，更新排序需要删除+重新插入

    数据存储格式：
        Key: "hot:documents"
        Value: Sorted Set，每个成员是 JSON 序列化的文档信息
        Score: 浏览量（用于排序）

        Redis 内部存储结构：
        ┌──────────────────────────────────────────────┐
        │ Sorted Set: "hot:documents"                  │
        │ ┌──────────────────────┬────────────┐        │
        │ │ Member (JSON)        │ Score       │        │
        │ ├──────────────────────┼────────────┤        │
        │ │ {"id":1,"title":".."}│ 256        │ ← Top1 │
        │ │ {"id":2,"title":".."}│ 189        │ ← Top2 │
        │ │ {"id":3,"title":".."}│ 145        │ ← Top3 │
        │ └──────────────────────┴────────────┘        │
        └──────────────────────────────────────────────┘

    ensure_ascii=False 的作用：
        默认 json.dumps 会将中文转为 Unicode 转义序列（如 "\\u5e74\\u5047"），
        设置 ensure_ascii=False 保留中文字符（如 "年假"），便于调试。

    Args:
        r: 已连接的 Redis 客户端实例
    """
    print("\n📋 初始化热门文档缓存...")

    # 热门文档数据（按浏览量排序）
    # 在生产环境中，这些数据通常从数据库统计查询获得
    hot_docs = [
        {"id": 1, "title": "员工手册（2026版）", "viewCount": 256},
        {"id": 2, "title": "年假申请流程", "viewCount": 189},
        {"id": 3, "title": "企业知识库系统架构设计", "viewCount": 145},
        {"id": 4, "title": "RESTful API 接口规范", "viewCount": 98},
        {"id": 5, "title": "销售话术与客户沟通指南", "viewCount": 78},
    ]

    # 使用 Sorted Set 存储热门文档（按浏览量排序）
    key = "hot:documents"
    for doc in hot_docs:
        # ZADD 命令：添加成员到 Sorted Set
        # 参数：{成员: 分数}，分数相同则更新
        # 成员必须是字符串，所以用 json.dumps 序列化
        r.zadd(key, {json.dumps(doc, ensure_ascii=False): doc["viewCount"]})

    # 设置过期时间 1 小时（3600 秒）
    # 热门文档排名应该定期更新，1 小时后缓存过期
    # 下次访问时触发重新计算（Cache-Aside 模式）
    r.expire(key, 3600)
    print(f"  缓存 {len(hot_docs)} 篇热门文档 ✓ (TTL: 1h)")


def init_hot_searches(r: redis.Redis):
    """
    初始化搜索热词缓存（使用 Sorted Set）

    存储用户最常搜索的关键词及其搜索次数。
    可用于前端展示"热门搜索"标签，引导用户探索。

    与热门文档的区别：
        - 热门文档：成员是 JSON 字符串（复杂对象）
        - 搜索热词：成员是简单字符串（关键词本身）
        - 两者都使用 Sorted Set，但数据的复杂度不同

    搜索热词的应用场景：
        1. 前端搜索框下方的"热门搜索"标签
        2. 搜索建议自动补全的优先级排序
        3. 管理后台的内容热度分析

    Args:
        r: 已连接的 Redis 客户端实例
    """
    print("\n🔥 初始化搜索热词...")

    # 热门搜索词及其搜索次数
    # 注意：ZADD 可以接受字典参数，一次添加多个成员
    hot_searches = {
        "年假": 156,
        "请假": 89,
        "API": 67,
        "架构": 45,
        "销售": 34,
        "加班": 28,
        "薪酬": 23,
        "合同": 18,
    }

    key = "search:hot"
    # 批量添加到 Sorted Set
    # 字典的 key 是成员（搜索词），value 是分数（搜索次数）
    r.zadd(key, hot_searches)
    # 同样设置 1 小时过期
    r.expire(key, 3600)
    print(f"  缓存 {len(hot_searches)} 个热词 ✓ (TTL: 1h)")


def init_user_sessions(r: redis.Redis):
    """
    初始化示例用户会话（使用 Hash）

    使用 Redis Hash 存储用户会话信息。
    Hash 适合存储对象数据，每个字段是对象的一个属性。

    为什么用 Hash 而不是 String？
        - Hash 可以单独读取/更新某个字段（如只更新 loginTime）
        - 内存效率更高（小 Hash 在 Redis 内部用 ziplist 压缩存储）
        - 语义更清晰：HGET user:session:1 role → "ADMIN"

    为什么用 Redis 存会话而不是 MySQL？
        1. 性能：会话检查每次请求都需要，Redis 微秒级 vs MySQL 毫秒级
        2. 过期：Redis 原生支持 TTL，MySQL 需要定时清理过期记录
        3. 分布式：多个 AI 服务实例可以共享 Redis 中的会话

    会话数据结构：
        Key: "user:session:{userId}"
        Hash Fields:
            - userId: 用户 ID
            - username: 用户名
            - role: 角色（ADMIN / EDITOR / USER）
            - loginTime: 登录时间（ISO 8601 格式）

    Key 命名规范：
        "user:session:1" 使用冒号分隔的层级命名：
        - user: 顶级分类（用户相关）
        - session: 子分类（会话）
        - 1: 具体标识（用户 ID）
        这种命名在 Redis 管理工具中会以树状结构展示，便于管理。

    Args:
        r: 已连接的 Redis 客户端实例
    """
    print("\n👤 初始化示例用户会话...")

    # 为 admin 用户创建一个示例会话
    session = {
        "userId": 1,
        "username": "admin",
        "role": "ADMIN",
        "loginTime": "2026-06-01T10:00:00Z",  # ISO 8601 格式时间戳
    }
    key = "user:session:1"
    # HSET 命令：批量设置 Hash 的多个字段
    # mapping 参数接受字典，一次性设置所有字段
    r.hset(key, mapping=session)
    # 会话 TTL 设为 24 小时（86400 秒）
    # 比热门文档的 TTL 更长，因为用户不会希望频繁重新登录
    r.expire(key, 86400)  # 24小时过期
    print("  缓存 admin 用户会话 ✓ (TTL: 24h)")


def verify(r: redis.Redis):
    """
    验证缓存数据

    检查所有初始化的缓存数据是否正确写入。

    ZREVRANGE 命令说明：
        按分数从高到低获取 Sorted Set 的成员（降序排列）
        参数：
            - key: Sorted Set 的键名
            - start: 起始位置（0 表示第一个）
            - stop: 结束位置（-1 表示最后一个）
            - withscores: 是否同时返回分数

    DBSIZE 命令：
        返回当前 Redis 数据库中的键总数。
        注意：这不等于数据条目数（一个键可能包含多个元素）。

    Args:
        r: 已连接的 Redis 客户端实例
    """
    print("\n📊 缓存状态:")

    # 检查热门文档（Sorted Set）
    # ZREVRANGE: 按分数降序获取所有成员
    hot_docs = r.zrevrange("hot:documents", 0, -1, withscores=True)
    print(f"  热门文档: {len(hot_docs)} 条")

    # 检查搜索热词（Sorted Set）
    hot_searches = r.zrevrange("search:hot", 0, -1, withscores=True)
    print(f"  搜索热词: {len(hot_searches)} 条")

    # 显示 Top 3 热词（withscores=True 返回元组列表）
    # 每个元组：(member_bytes, score_float)
    print("\n🔥 热门搜索 Top 3:")
    for term, score in hot_searches[:3]:
        print(f"  - {term}: {int(score)} 次")

    # 检查数据库中的总键数
    db_size = r.dbsize()
    print(f"\n  总缓存键数: {db_size}")


def main():
    """
    脚本主入口

    命令行参数说明：
        --host: Redis 服务器地址（默认 localhost）
        --port: Redis 端口（默认 6379）
        --db: Redis 数据库编号（0-15，默认 0）

    Redis 数据库编号说明：
        Redis 提供 16 个逻辑数据库（DB 0-15），默认使用 DB 0。
        不同应用可以使用不同的 DB 来隔离数据，但共享同一个 Redis 实例。
        生产环境通常使用不同的 Redis 实例而非不同 DB 来隔离。

    执行流程：连接 → 初始化热门文档 → 初始化搜索热词 → 初始化会话 → 验证
    """
    parser = argparse.ArgumentParser(description="初始化 Redis 缓存")
    parser.add_argument("--host", default="localhost", help="Redis 主机")
    parser.add_argument("--port", type=int, default=6379, help="Redis 端口")
    parser.add_argument("--db", type=int, default=0, help="Redis 数据库")
    args = parser.parse_args()

    print("=" * 50)
    print("🚀 Redis 缓存初始化")
    print("=" * 50)

    # 连接 Redis
    # decode_responses=True: 自动将 Redis 返回的 bytes 解码为 str
    # 如果不设置，所有返回值都是 bytes 类型（如 b'healthy' 而非 'healthy'）
    try:
        r = redis.Redis(host=args.host, port=args.port, db=args.db, decode_responses=True)
        r.ping()  # PING 命令测试连接是否正常
        print(f"✓ 连接成功: {args.host}:{args.port}")
    except Exception as e:
        print(f"✗ 连接失败: {e}")
        print(f"  请确保 Redis 已启动: docker compose up -d redis")
        sys.exit(1)

    # 执行初始化流程
    init_hot_documents(r)    # 步骤1：初始化热门文档缓存（Sorted Set）
    init_hot_searches(r)     # 步骤2：初始化搜索热词（Sorted Set）
    init_user_sessions(r)    # 步骤3：初始化用户会话（Hash）
    verify(r)                # 步骤4：验证所有缓存数据

    print("\n✅ Redis 初始化完成!")


if __name__ == "__main__":
    main()
