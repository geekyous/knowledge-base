"""
健康检查 API 模块

========================================
📚 模块概述
====================================
本模块提供服务健康检查端点，用于监控和运维。

什么是健康检查（Health Check）？
    在微服务架构中，需要一种机制来判断服务是否正常运行。
    健康检查端点通常返回简单的 JSON 响应，Docker / Kubernetes /
    负载均衡器会定期调用此端点来检测服务状态。

    健康检查的工作方式：
    ┌────────────┐     GET /api/health     ┌──────────────┐
    │ Docker/K8s │  ──────────────────→    │  AI 服务     │
    │  探针      │  ←──────────────────    │  /health     │
    └────────────┘   {"status":"healthy"}  └──────────────┘
                        ↓
                如果返回 200 → 服务健康
                如果超时/5xx → 服务异常，触发重启

    K8s 中的探针类型：
        - livenessProbe: 判断是否需要重启容器
        - readinessProbe: 判断是否可以将流量路由到此容器
        - startupProbe: 判断容器是否已启动完成

为什么单独的 health.py？
    - 职责分离：健康检查是运维关注点，与业务逻辑分离
    - 不依赖认证：健康检查不应要求认证
    - 轻量级：只返回固定信息，不查询数据库

💡 学习要点:
    1. 健康检查在微服务架构中的重要性
    2. Pydantic response_model 在 API 文档自动生成中的作用
    3. API 版本化和路径规划（/api/health vs /health）

架构角色：
    本模块是最轻量的 API 模块，不依赖任何核心业务逻辑，
    只返回固定的服务状态信息。
"""

from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter()


class HealthResponse(BaseModel):
    """
    健康检查响应模型

    使用 Pydantic BaseModel 定义响应结构，确保：
        1. 返回数据格式一致（类型安全）
        2. FastAPI 自动生成 OpenAPI 文档中的响应示例
        3. IDE 提供类型提示和自动补全

    Attributes:
        status: 服务状态（"healthy" 表示正常运行）
        service: 服务名称
        version: 服务版本号
    """
    status: str
    service: str
    version: str


@router.get("/health", response_model=HealthResponse)
async def health_check():
    """
    健康检查端点

    返回服务的基本健康状态信息。
    Docker Compose 和 K8s 通过此端点判断服务是否正常运行。

    response_model=HealthResponse 的作用：
        - 自动将返回的 dict 转换为 HealthResponse 格式
        - 自动过滤多余的字段
        - 在 Swagger UI (/docs) 中显示正确的响应结构

    Returns:
        HealthResponse: 包含状态、服务名和版本的响应

    Example:
        GET /api/health
        →
        {
            "status": "healthy",
            "service": "Knowledge Base AI Service",
            "version": "1.0.0"
        }
    """
    return {
        "status": "healthy",
        "service": "Knowledge Base AI Service",
        "version": "1.0.0",
    }
