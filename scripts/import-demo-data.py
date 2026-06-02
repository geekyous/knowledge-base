#!/usr/bin/env python3
"""
企业知识库问答系统 - 数据导入脚本

将示例数据导入到各个数据存储中:
1. Elasticsearch - 全文搜索索引
2. Qdrant - 向量语义搜索索引
3. Redis - 热门数据缓存

💡 学习要点:
    1. argparse: Python 标准库命令行参数解析
    2. urllib: Python 标准库 HTTP 请求（无需第三方依赖）
    3. 数据导入顺序: 先检查服务 → 再导入数据
    4. 容错设计: try/except 确保单个服务失败不影响其他服务

📖 使用方式:
    # 导入所有数据（会先检查服务状态）
    python scripts/import-demo-data.py

    # 跳过服务检查，直接导入
    python scripts/import-demo-data.py --skip-checks

    # 指定服务地址
    python scripts/import-demo-data.py --es-host 192.168.1.100 --qdrant-host 192.168.1.100

🔗 相关文件:
    - ai-service/scripts/init_elasticsearch.py  ES 索引初始化
    - ai-service/scripts/init_qdrant.py          Qdrant 集合初始化
    - ai-service/scripts/init_redis.py            Redis 缓存初始化
"""

import sys
import argparse


def check_service(name: str, check_func) -> bool:
    """
    检查单个服务的健康状态

    通过调用传入的检查函数来判断服务是否可用。
    使用高阶函数模式，使检查逻辑可复用。

    Args:
        name: 服务名称（用于日志输出）
        check_func: 无参数的检查函数，返回 bool

    Returns:
        bool: True 表示服务正常，False 表示异常

    Example:
        >>> check_service("Redis", lambda: ping_redis("localhost", 6379))
        ✅ Redis: 正常
    """
    try:
        result = check_func()
        if result:
            print(f"  ✅ {name}: 正常")
            return True
        else:
            print(f"  ❌ {name}: 异常")
            return False
    except Exception as e:
        print(f"  ❌ {name}: 连接失败 ({e})")
        return False


def check_elasticsearch(host: str = "localhost", port: int = 9200) -> bool:
    """
    检查 Elasticsearch 是否可达

    通过请求 _cluster/health API 来验证服务状态。
    使用 urllib（Python 标准库）避免额外依赖。

    💡 学习要点:
        _cluster/health 是 Elasticsearch 的内置健康检查端点，
        返回集群状态信息（green/yellow/red）。

    Args:
        host: Elasticsearch 主机地址
        port: Elasticsearch 端口号

    Returns:
        bool: True 表示服务正常（HTTP 200）
    """
    import urllib.request

    try:
        url = f"http://{host}:{port}/_cluster/health"
        # timeout=5: 设置超时时间，避免长时间等待无响应的服务
        req = urllib.request.urlopen(url, timeout=5)
        return req.status == 200
    except Exception:
        return False


def check_qdrant(host: str = "localhost", port: int = 6333) -> bool:
    """
    检查 Qdrant 向量数据库是否可达

    通过请求 /healthz 端点来验证服务状态。

    💡 学习要点:
        Qdrant 是一个开源的向量数据库，专门用于存储和检索向量嵌入。
        /healthz 是其内置的健康检查端点。

    Args:
        host: Qdrant 主机地址
        port: Qdrant 端口号（默认 6333）

    Returns:
        bool: True 表示服务正常
    """
    import urllib.request

    try:
        url = f"http://{host}:{port}/healthz"
        req = urllib.request.urlopen(url, timeout=5)
        return req.status == 200
    except Exception:
        return False


def check_redis(host: str = "localhost", port: int = 6379) -> bool:
    """
    检查 Redis 是否可达

    使用 redis-py 库的 ping() 命令验证连接。
    Redis 的 PING/PONG 是最简单的连接测试机制。

    💡 学习要点:
        Redis 使用简洁的文本协议，PING 命令返回 PONG 表示连接正常。
        这是 Redis 的标准健康检查方式。

    Args:
        host: Redis 主机地址
        port: Redis 端口号（默认 6379）

    Returns:
        bool: True 表示服务正常
    """
    try:
        import redis

        r = redis.Redis(host=host, port=port)
        return r.ping()
    except Exception:
        return False


def main():
    """
    主函数: 解析命令行参数并执行数据导入

    执行流程:
    1. 解析命令行参数（服务地址、端口、是否跳过检查）
    2. 检查各服务是否可用
    3. 依次导入 Elasticsearch、Qdrant、Redis 数据
    4. 每个步骤独立 try/except，确保单个失败不影响其他
    """

    # =====================================================
    # 命令行参数解析
    # argparse 是 Python 标准库的命令行参数解析工具
    # =====================================================
    parser = argparse.ArgumentParser(
        description="导入演示数据到 Elasticsearch、Qdrant 和 Redis"
    )
    # --es-host: Elasticsearch 主机地址，默认 localhost
    parser.add_argument("--es-host", default="localhost", help="Elasticsearch 主机地址")
    # --es-port: Elasticsearch 端口，类型为整数，默认 9200
    parser.add_argument("--es-port", type=int, default=9200, help="Elasticsearch 端口")
    parser.add_argument("--qdrant-host", default="localhost", help="Qdrant 主机地址")
    parser.add_argument("--qdrant-port", type=int, default=6333, help="Qdrant 端口")
    parser.add_argument("--redis-host", default="localhost", help="Redis 主机地址")
    parser.add_argument("--redis-port", type=int, default=6379, help="Redis 端口")
    # --skip-checks: 布尔标志，添加后跳过服务检查
    parser.add_argument("--skip-checks", action="store_true", help="跳过服务状态检查")
    args = parser.parse_args()

    print("=" * 50)
    print("  📊 企业知识库 - 数据导入工具")
    print("=" * 50)

    # =====================================================
    # 步骤 1: 检查服务状态
    # 在导入数据之前，先确保所有服务都已启动
    # =====================================================
    if not args.skip_checks:
        print("\n🔍 检查服务状态...")
        results = []

        # lambda: 匿名函数，用于延迟执行（传入参数后再调用）
        results.append(
            check_service("Elasticsearch", lambda: check_elasticsearch(args.es_host, args.es_port))
        )
        results.append(
            check_service("Qdrant", lambda: check_qdrant(args.qdrant_host, args.qdrant_port))
        )
        results.append(
            check_service("Redis", lambda: check_redis(args.redis_host, args.redis_port))
        )

        # all(): 当所有结果都为 True 时返回 True
        if not all(results):
            print("\n⚠️  部分服务不可用，请先启动: docker compose up -d")
            print("   或使用 --skip-checks 跳过检查")
            sys.exit(1)  # 退出码 1 表示因错误退出

    # =====================================================
    # 步骤 2: 导入 Elasticsearch 数据
    # 包括: 创建索引（配置中文分词）+ 索引示例文档
    # =====================================================
    print("\n📤 导入 Elasticsearch 数据...")
    try:
        # 将 ai-service 目录加入 Python 模块搜索路径
        # 这样可以导入 ai-service/scripts/ 下的模块
        sys.path.insert(0, "ai-service")
        from scripts.init_elasticsearch import create_index, index_documents
        from elasticsearch import Elasticsearch

        es = Elasticsearch(f"http://{args.es_host}:{args.es_port}")
        create_index(es)        # 创建索引和映射
        index_documents(es)     # 导入示例文档
    except Exception as e:
        # 即使 ES 导入失败，也继续执行后续步骤
        print(f"  ❌ Elasticsearch 导入失败: {e}")

    # =====================================================
    # 步骤 3: 导入 Qdrant 向量数据
    # 包括: 创建集合 + 向量化文档 + 存储到 Qdrant
    # =====================================================
    print("\n📤 导入 Qdrant 向量数据...")
    try:
        from qdrant_client import QdrantClient
        from scripts.init_qdrant import init_collection, insert_documents

        client = QdrantClient(host=args.qdrant_host, port=args.qdrant_port)
        init_collection(client)      # 创建向量集合
        insert_documents(client)     # 向量化并插入文档
    except Exception as e:
        print(f"  ❌ Qdrant 导入失败: {e}")

    # =====================================================
    # 步骤 4: 导入 Redis 缓存数据
    # 包括: 热门文档缓存 + 搜索热词 + 用户会话
    # =====================================================
    print("\n📤 导入 Redis 缓存...")
    try:
        from scripts.init_redis import init_hot_documents, init_hot_searches, init_user_sessions
        import redis

        # decode_responses=True: 自动将 Redis 返回的 bytes 转为 str
        r = redis.Redis(host=args.redis_host, port=args.redis_port, decode_responses=True)
        init_hot_documents(r)    # 初始化热门文档排行（Sorted Set）
        init_hot_searches(r)     # 初始化搜索热词（Sorted Set）
        init_user_sessions(r)    # 初始化用户会话（Hash）
    except Exception as e:
        print(f"  ❌ Redis 导入失败: {e}")

    # =====================================================
    # 完成
    # =====================================================
    print("\n" + "=" * 50)
    print("  ✅ 数据导入完成!")
    print("=" * 50)


# Python 的标准入口点写法
# 只有直接运行此文件时才会执行 main()
# 被其他模块 import 时不会执行
if __name__ == "__main__":
    main()
