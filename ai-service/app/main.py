"""
企业知识库问答系统 - AI 服务主应用

========================================
📚 模块概述
========================================
本模块是 AI 服务的入口文件，负责创建和配置 FastAPI 应用实例。

主要职责：
    1. 应用生命周期管理（启动时初始化向量数据库，关闭时清理资源）
    2. CORS 跨域配置（允许前端调用 API）
    3. 路由注册（将 API 端点挂载到应用上）
    4. 全局异常处理（统一错误响应格式）

FastAPI 应用启动流程：
    1. 加载配置（config.py → settings 单例）
    2. 执行 lifespan startup（初始化 Qdrant 集合、检查 LLM 模式）
    3. 注册中间件和路由
    4. 开始监听 HTTP 请求

💡 学习要点:
    1. FastAPI 的 lifespan 上下文管理器如何管理应用生命周期
    2. CORS 中间件的工作原理及为什么前后端分离需要它
    3. 路由的模块化组织方式（APIRouter）
    4. 全局异常处理器如何保障 API 响应格式的一致性

架构角色：
    本模块是整个 AI 服务的"组装车间"，将各个功能模块（API、核心逻辑、配置）
    组装成一个完整的 FastAPI 应用。
"""

from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import logging
import sys

# 导入 API 路由模块
from app.api import chat, document, health
# 导入全局配置
from app.config import settings
# 导入向量数据库初始化函数
from app.core.vector_store import init_collection

# =====================================================
# 日志配置
# =====================================================
# 配置根日志记录器：
#   - level=INFO: 只记录 INFO 及以上级别的日志（DEBUG 级别会被忽略）
#   - format: 时间 | 级别 | 模块名:函数名:行号 - 消息
#   这里使用 basicConfig 是因为这是主入口，只需配置一次
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)-8s | %(name)s:%(funcName)s:%(lineno)d - %(message)s",
)
# 获取当前模块的 logger，用于记录应用启动/关闭等关键事件
logger = logging.getLogger(__name__)


# =====================================================
# 应用生命周期管理
# =====================================================
@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    FastAPI 应用生命周期管理器

    使用 Python 的 async context manager 模式：
        - yield 之前的代码在应用启动时执行（startup）
        - yield 之后的代码在应用关闭时执行（shutdown）

    这种模式比 @app.on_event("startup") / @app.on_event("shutdown") 更推荐，
    因为它天然支持异步和资源清理。

    Args:
        app: FastAPI 应用实例（自动注入）

    Example:
        在 FastAPI 中通过 lifespan 参数注册：
        >>> app = FastAPI(lifespan=lifespan)
    """
    # ==================== Startup 阶段 ====================
    logger.info("🚀 AI 服务启动中...")

    # 初始化向量数据库集合
    # 如果集合不存在会自动创建；如果已存在则跳过
    # 这确保了服务启动后 Qdrant 处于可用状态
    logger.info("初始化向量数据库...")
    init_collection()

    # 检查 LLM 工作模式
    # Mock 模式：无需 API Key，使用预定义的问答对，适合开发和演示
    # Real 模式：调用 OpenAI 或 Anthropic API，需要有效的 API Key
    from app.core.llm import is_mock_mode
    if is_mock_mode():
        logger.info("⚠ 使用模拟模式（MOCK_MODE=true）")
    else:
        logger.info("✓ 使用真实 LLM 模式")

    logger.info("✅ AI 服务启动完成!")

    # yield 暂停执行，应用开始处理 HTTP 请求
    # 控制权在应用关闭时才会回到这里
    yield

    # ==================== Shutdown 阶段 ====================
    logger.info("🛑 AI 服务关闭中...")
    # 这里可以添加资源清理逻辑，如关闭数据库连接、清理缓存等
    logger.info("✅ AI 服务已关闭")


# =====================================================
# 创建 FastAPI 应用实例
# =====================================================
# FastAPI 是一个高性能的异步 Web 框架，基于 Starlette 和 Pydantic 构建
# 参数说明：
#   title: API 文档标题（显示在 /docs 页面）
#   description: API 文档描述
#   version: API 版本号
#   docs_url: Swagger UI 文档路径（/docs）
#   redoc_url: ReDoc 文档路径（/redoc）
#   lifespan: 生命周期管理器
app = FastAPI(
    title="知识库 AI 服务",
    description="企业知识库问答系统 - AI 智能服务",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)


# =====================================================
# CORS（跨域资源共享）中间件
# =====================================================
# 什么是 CORS？
#   浏览器的同源策略（Same-Origin Policy）默认阻止跨域请求。
#   例如：前端 http://localhost:5173 调用后端 http://localhost:8000 会被拦截。
#   CORS 中间件通过在响应头中添加 Access-Control-Allow-* 字段来允许跨域。
#
# 配置说明：
#   allow_origins: 允许的源（域名+端口），["*"] 表示允许所有来源
#   allow_credentials: 是否允许携带 Cookie 等认证信息
#   allow_methods: 允许的 HTTP 方法，["*"] 表示 GET/POST/PUT/DELETE 全部允许
#   allow_headers: 允许的请求头，["*"] 表示所有自定义头都允许
#
# 安全提示：
#   生产环境应该明确指定允许的域名，不要使用 ["*"]
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS + ["*"],  # 配置的源 + 兜底通配
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# =====================================================
# 注册 API 路由
# =====================================================
# FastAPI 使用 APIRouter 实现路由的模块化管理：
#   - 每个 Router 是一组相关 API 的集合
#   - prefix 定义 URL 前缀，所有该 Router 下的端点都会自动加上这个前缀
#   - tags 用于 API 文档（/docs）中的分组显示
#
# URL 结构示例：
#   /api/health          → 健康检查
#   /api/v1/chat/ask     → 智能问答
#   /api/v1/documents/index → 文档索引
app.include_router(health.router, prefix="/api", tags=["健康检查"])
app.include_router(chat.router, prefix="/api/v1/chat", tags=["智能问答"])
app.include_router(document.router, prefix="/api/v1/documents", tags=["文档处理"])


# =====================================================
# 根路径 —— 服务信息
# =====================================================
@app.get("/")
async def root():
    """
    服务根路径

    返回服务基本信息，用于快速确认服务是否运行。

    Returns:
        dict: 包含服务名称、版本和运行状态
    """
    return {"service": "知识库 AI 服务", "version": "1.0.0", "status": "running"}


# =====================================================
# 健康检查端点（供 Docker Compose / K8s 使用）
# =====================================================
# 这个端点与 health.py 中的不同：
#   - /health: Docker Compose 健康检查使用，路径简短方便配置
#   - /api/health: 完整的健康状态接口，返回更多信息
@app.get("/health")
async def health_check():
    """
    健康检查端点

    Docker Compose 通过此端点判断服务是否健康。
    如果返回 200 状态码，说明服务正常运行。

    Returns:
        dict: 健康状态
    """
    return {"status": "healthy"}


# =====================================================
# 全局异常处理器
# =====================================================
# 捕获所有未被路由内 try-except 处理的异常，返回统一的错误格式
# 这样即使代码抛出意外异常，API 也会返回结构化的 JSON 而不是 500 HTML 页面
#
# 设计决策：
#   - DEBUG 模式下会返回异常详情（detail 字段），方便调试
#   - 生产模式下 detail 为 None，避免泄露内部实现细节
@app.exception_handler(Exception)
async def global_exception_handler(request, exc):
    """
    全局异常处理器

    捕获所有未处理的异常，返回统一格式的 JSON 错误响应。

    Args:
        request: 引发异常的 HTTP 请求
        exc: 捕获到的异常实例

    Returns:
        JSONResponse: 包含错误码、消息和可选详情的 500 响应
    """
    # exc_info=True 让 logger 输出完整的堆栈跟踪
    logger.error(f"未处理的异常: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={
            "code": 500,
            "message": "服务器内部错误",
            # 安全考虑：生产环境不暴露异常详情
            "detail": str(exc) if settings.DEBUG else None,
        },
    )


# =====================================================
# 直接运行入口
# =====================================================
# 当通过 python app/main.py 直接运行时，启动 uvicorn 服务器
# uvicorn 是 ASGI 服务器，负责将 HTTP 请求传递给 FastAPI 应用
#   - host="0.0.0.0": 监听所有网络接口（允许外部访问）
#   - port=8000: 服务端口号
#   - reload=settings.DEBUG: 开发模式下代码修改后自动重启
#
# 注意：生产环境通常使用 `uvicorn app.main:app` 命令启动
if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=settings.DEBUG)
