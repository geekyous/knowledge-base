"""
API 接口模块

========================================
📚 模块概述
========================================
本模块定义了 AI 服务的所有 RESTful API 端点，使用 FastAPI 的 Router 机制
将不同功能的接口分组管理。

子模块说明：
    - health.py:   健康检查接口，用于服务探活和 Docker/K8s 健康检查
    - chat.py:     智能问答接口，处理用户对话和 RAG 问答流程
    - document.py: 文档处理接口，提供文档向量索引和语义搜索功能

架构角色：
    API 层是整个系统的入口，负责：
    1. 接收 HTTP 请求并通过 Pydantic 模型进行数据校验
    2. 调用 core 层的业务逻辑处理请求
    3. 将结果封装为统一的 JSON 响应格式

💡 学习要点:
    1. FastAPI Router 如何实现接口的模块化组织
    2. Pydantic BaseModel 在请求/响应数据校验中的应用
    3. RESTful API 设计规范：资源命名、HTTP 方法、状态码
    4. API 层与业务逻辑层的分离原则（Controller-Service 模式）
"""
