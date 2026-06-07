"""
LLM（大语言模型）接口模块

========================================
📚 模块概述
====================================
本模块封装了大语言模型（LLM）的调用逻辑，支持三种工作模式：
    1. OpenAI API（GPT-3.5/4 系列）
    2. Anthropic API（Claude 系列）
    3. Mock 模式（使用预定义的问答对，无需 API Key）

什么是 LLM（Large Language Model）？
    LLM 是通过海量文本数据训练的深度学习模型，能够理解和生成自然语言。
    在 RAG 架构中，LLM 负责最终的"生成"步骤：
    接收检索到的文档上下文 + 用户问题 → 生成结构化的自然语言回答。

    本系统的 LLM 调用方式：
    ┌──────────────┐     ┌─────────────┐     ┌──────────────┐
    │ System Prompt │ +   │  用户问题    │ +   │  文档上下文   │
    │ (角色定义)    │     │  (question) │     │  (context)   │
    └──────┬───────┘     └──────┬──────┘     └──────┬───────┘
           │                     │                    │
           └─────────────────────┼────────────────────┘
                                 ▼
                          ┌──────────────┐
                          │     LLM      │
                          │ (GPT/Claude) │
                          └──────┬───────┘
                                 ▼
                          ┌──────────────┐
                          │  结构化回答   │
                          │  + 来源引用   │
                          └──────────────┘

Mock 模式的设计思想：
    在开发和演示时，可能没有可用的 API Key 或网络连接。
    Mock 模式使用关键词匹配，从预定义的 MOCK_QA 字典中返回模拟回答，
    确保系统在不连接真实 LLM 的情况下仍然可以完整演示 RAG 流程。

💡 学习要点:
    1. Prompt Engineering（提示词工程）：System Prompt 如何引导 LLM 的行为
    2. Temperature 参数如何控制 LLM 输出的随机性
    3. 多 Provider 支持（OpenAI vs Anthropic）的适配器模式
    4. Mock 模式在开发和测试中的重要性

架构角色：
    本模块是 RAG 流水线的"生成"环节。
    retriever.py 构建好上下文后调用本模块生成最终回答。
"""

import os
import logging
from typing import List, Dict, Any, Optional

logger = logging.getLogger(__name__)


def is_mock_mode() -> bool:
    """
    检查是否使用模拟模式

    Mock 模式在以下场景中非常有用：
        1. 本地开发没有 API Key
        2. 单元测试中不希望调用真实 API
        3. 演示系统流程而不产生 API 费用
        4. CI/CD 环境中避免外部依赖

    Returns:
        bool: True 表示使用 Mock 模式

    Example:
        >>> is_mock_mode()
        True  # 当环境变量 MOCK_MODE=true 时
    """
    return os.getenv("MOCK_MODE", "true").lower() == "true"


def generate_answer(
    question: str,
    context: str = "",
    sources: List[Dict[str, Any]] = None,
) -> Dict[str, Any]:
    """
    生成回答 —— LLM 调用的统一入口

    本函数是 LLM 调用的"门面"（Facade 模式），调用方不需要知道底层
    使用的是 OpenAI、Anthropic 还是 Mock 模式。

    决策流程：
        1. 如果 MOCK_MODE=true → 使用 Mock 模式
        2. 否则根据 LLM_PROVIDER 环境变量选择 Provider
        3. 如果对应 Provider 的 API Key 未配置 → 降级到 Mock 模式

    Args:
        question: 用户的问题（如"年假怎么申请"）
        context: 检索到的文档上下文（由 retriever.py 构建）
            格式通常是 "文档1: 标题\n文档2: 标题\n..."
        sources: 来源文档列表，包含 documentId、title、relevance

    Returns:
        Dict[str, Any]: 统一格式的回答，包含：
            - answer (str): LLM 生成的自然语言回答
            - sources (list): 引用的来源文档列表
            - followUps (list): 推荐的追问问题列表

    Example:
        >>> result = generate_answer("年假怎么申请", "文档1: 年假申请流程...", [])
        >>> result["answer"]
        '根据《员工手册》规定，年假需要提前15个工作日通过OA系统申请...'
    """
    if is_mock_mode():
        return _mock_generate(question, sources)

    # 尝试使用真实 LLM
    # LLM_PROVIDER 环境变量决定使用哪个 LLM 服务商
    provider = os.getenv("LLM_PROVIDER", "ollama").lower()

    # 根据配置的 Provider 和是否有 API Key 来决定调用路径
    if provider == "ollama":
        return _ollama_generate(question, context, sources)
    elif provider == "openai" and os.getenv("OPENAI_API_KEY"):
        return _openai_generate(question, context, sources)
    elif provider == "anthropic" and os.getenv("ANTHROPIC_API_KEY"):
        return _anthropic_generate(question, context, sources)
    else:
        # 没有可用的 API Key，降级到 Mock 模式
        logger.warning("未配置有效的 LLM API Key，使用模拟模式")
        return _mock_generate(question, sources)


# =====================================================
# Mock 模式 —— 预定义问答对
# =====================================================
# MOCK_QA 是一个关键词 → 回答的映射字典。
# 设计思想：
#   - 使用关键词匹配而非语义理解，简化实现
#   - 每个回答都是精心编写的 Markdown 格式文本
#   - 包含 sources 和 followUps 字段，模拟真实 LLM 的输出结构
#   - 覆盖系统中的核心知识领域：人事制度、技术文档、销售、法务
#
# 局限性：
#   - 只能回答预设关键词相关的问题
#   - 不具备理解上下文或多轮对话的能力
#   - 回答内容是静态的，不会随知识库更新而变化
MOCK_QA = {
    "年假": {
        "answer": "根据《员工手册》和《年假申请流程》规定：\n\n1. **年假天数**：工作1-5年享有5天，5-10年7天，10-15年10天，15年以上15天\n2. **申请流程**：至少提前15个工作日通过OA系统提交申请\n3. **审批流程**：直属领导 → 部门经理 → HR备案\n4. **注意事项**：年假不可跨年累积，每年12月31日前需休完\n\n需要了解更多详情吗？",
        "sources": [{"documentId": 2, "title": "年假申请流程", "relevance": 0.95}],
        "followUps": ["年假可以跨年使用吗？", "离职时未休年假怎么办？"],
    },
    "请假": {
        "answer": "公司请假制度包含以下类型：\n\n- **年假**：根据工龄5-15天，提前15个工作日申请\n- **病假**：凭医院证明，每年不超过10天\n- **事假**：需提前申请，每年不超过5天\n- **婚假**：法定婚假3天，晚婚假7天\n\n所有请假均需通过OA系统提交申请。",
        "sources": [
            {"documentId": 1, "title": "员工手册（2026版）", "relevance": 0.92},
            {"documentId": 2, "title": "年假申请流程", "relevance": 0.85},
        ],
        "followUps": ["病假需要什么证明？", "事假扣工资吗？"],
    },
    "加班": {
        "answer": "公司加班制度如下：\n\n- **工作日加班**：按1.5倍工资计算\n- **周末加班**：按2倍工资计算或安排调休\n- **法定假日加班**：按3倍工资计算\n\n加班需提前获得部门经理批准。",
        "sources": [{"documentId": 1, "title": "员工手册（2026版）", "relevance": 0.90}],
        "followUps": ["加班申请流程是什么？", "调休怎么申请？"],
    },
    "架构": {
        "answer": "企业知识库系统采用前后端分离 + AI微服务的混合架构：\n\n- **前端**: Vue 3 + TypeScript + Element Plus\n- **后端**: Java 17 + Spring Boot 3.2\n- **AI服务**: Python 3.11 + FastAPI + LangChain\n- **数据存储**: MySQL + Redis + Elasticsearch + Qdrant\n\nAI问答基于 RAG（检索增强生成）架构。",
        "sources": [
            {"documentId": 3, "title": "企业知识库系统架构设计", "relevance": 0.98}
        ],
        "followUps": ["RAG是什么？", "Qdrant和Elasticsearch有什么区别？"],
    },
    "api": {
        "answer": "系统 API 设计遵循 RESTful 规范：\n\n- 基础路径: `/api/v1/`\n- 认证: JWT Token (Bearer)\n- 响应格式: `{code, message, data}`\n- 主要接口: 认证、文档、搜索、AI问答\n\n常用错误码: 400参数错误、401未认证、403无权限、404不存在",
        "sources": [
            {"documentId": 4, "title": "RESTful API 接口规范", "relevance": 0.95}
        ],
        "followUps": ["如何获取JWT Token？", "支持分页查询吗？"],
    },
    "销售": {
        "answer": "关于销售话术的关键要点：\n\n1. **开场白**：简洁介绍身份和来意\n2. **需求挖掘**：了解团队规模、痛点、AI需求\n3. **异议处理**：价格异议用ROI对比，已有系统展示差异化\n4. **成交策略**：建议先做POC测试\n\n核心原则：把焦点从'价格'转移到'价值'。",
        "sources": [
            {"documentId": 5, "title": "销售话术与客户沟通指南", "relevance": 0.93}
        ],
        "followUps": ["客户说已有系统怎么应对？", "如何做POC测试？"],
    },
    "合同": {
        "answer": "合同管理审批权限：\n\n- **10万元以下**：部门经理审批\n- **10-50万元**：分管副总审批\n- **50万元以上**：总经理审批\n\n审批流程：业务部门起草 → 法务审核 → 领导审批 → 用印归档\n\n合同原件须在签订后5个工作日内交行政部归档。",
        "sources": [
            {"documentId": 6, "title": "合同管理暂行办法", "relevance": 0.94}
        ],
        "followUps": ["合同审批需要多长时间？", "电子合同是否有效？"],
    },
}

# 默认回复：当用户的问题不匹配任何预设关键词时返回
# 这是一个"兜底"策略，确保用户始终能得到有意义的响应
DEFAULT_RESPONSE = {
    "answer": "感谢您的提问。我已收到您的问题，正在从知识库中查找相关信息。\n\n目前我能够回答关于以下主题的问题：\n- 人事制度（考勤、请假、薪酬等）\n- 技术文档（架构设计、API规范等）\n- 销售支持（话术、客户沟通等）\n- 合规法务（合同管理等）\n\n请尝试使用更具体的关键词提问。",
    "sources": [],
    "followUps": ["年假怎么申请？", "系统用了什么技术架构？", "销售话术有哪些？"],
}


def _mock_generate(question: str, sources: List[Dict] = None) -> Dict[str, Any]:
    """
    Mock 模式生成回答

    使用简单的关键词匹配从预定义的问答字典中查找回答。
    这不是语义理解，只是字符串包含判断（keyword in question_lower）。

    为什么 Mock 模式也需要 sources 参数？
        因为 RAG 流程中可能已经检索到了真实的文档来源，
        此时应该使用真实来源覆盖 Mock 数据中的预设来源，
        保持数据来源的准确性。

    Args:
        question: 用户问题
        sources: 检索到的文档来源（可能覆盖 Mock 中的预设值）

    Returns:
        Dict[str, Any]: 包含 answer、sources、followUps 的回答字典
    """
    question_lower = question.lower()

    # 遍历所有预设关键词，找到第一个匹配的
    # 注意：这是简单包含匹配，不是语义匹配
    # "我想了解年假" 包含 "年假" → 匹配成功
    for keyword, response in MOCK_QA.items():
        if keyword in question_lower:
            result = dict(response)  # 浅拷贝，避免修改原始预设数据
            # 如果 RAG 检索到了真实来源，覆盖预设的 sources
            if sources:
                result["sources"] = sources
            return result

    # 没有匹配的关键词，返回默认响应
    return DEFAULT_RESPONSE


# =====================================================
# Ollama 本地 LLM —— 学习 LLM 的最佳起点
# =====================================================
# 什么是 Ollama？
#   Ollama 是一个本地 LLM 运行框架，让你在自己的电脑上运行开源大模型。
#   它提供 OpenAI 兼容的 HTTP API，无需 API Key，无需网络连接。
#
# 为什么从 Ollama 开始学习？
#   1. 零成本：完全本地运行，不按 Token 计费
#   2. 隐私安全：数据不离开本机
#   3. OpenAI 兼容 API：学会的调用方式可直接迁移到云端 API
#   4. 模型丰富：qwen2、llama3、mistral、gemma 等主流模型都支持
#
# Ollama API 调用流程：
#   HTTP POST http://ollama:11434/api/chat
#   ┌─────────────────────────────────────────────────┐
#   │  请求体:                                        │
#   │  {                                              │
#   │    "model": "qwen2",           ← 使用哪个模型   │
#   │    "messages": [                                │
#   │      {"role": "system", "content": "..."},      │
#   │      {"role": "user", "content": "..."}         │
#   │    ],                                           │
#   │    "stream": false              ← 等待完整响应   │
#   │  }                                              │
#   └─────────────────────────────────────────────────┘
#                      ↓
#   ┌─────────────────────────────────────────────────┐
#   │  响应体:                                        │
#   │  {                                              │
#   │    "message": {                                 │
#   │      "role": "assistant",                       │
#   │      "content": "根据文档内容..."                │
#   │    }                                            │
#   │  }                                              │
#   └─────────────────────────────────────────────────┘


def _ollama_generate(question: str, context: str, sources: List[Dict]) -> Dict[str, Any]:
    """
    使用 Ollama 本地 LLM 生成回答

    Ollama 提供 OpenAI 兼容的 HTTP API，无需 API Key。
    这是最适合学习 LLM 的方式：所有推理都在本地完成。

    支持的模型（通过 docker exec -it <容器> ollama pull <模型名> 下载）：
        - qwen2:7b      — 阿里通义千问，中文能力强（推荐）
        - llama3:8b     — Meta Llama 3，英文能力强
        - mistral:7b    — Mistral AI，推理速度快
        - gemma2:9b     — Google Gemma 2，均衡表现

    Args:
        question: 用户问题
        context: 检索到的文档上下文
        sources: 来源文档列表

    Returns:
        Dict[str, Any]: LLM 生成的回答
    """
    import json
    import urllib.request
    import urllib.error

    base_url = os.getenv("OLLAMA_BASE_URL", "http://ollama:11434")
    model = os.getenv("OLLAMA_CHAT_MODEL", "qwen2")

    # 构造用户消息（与 OpenAI 调用相同的 RAG Prompt 策略）
    user_message = f"问题：{question}\n\n相关文档：\n{context}\n\n请基于以上文档内容回答。" if context else question

    try:
        # 构造 Ollama Chat API 请求（OpenAI 兼容格式）
        payload = {
            "model": model,
            "messages": [
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": user_message},
            ],
            "stream": False,  # 等待完整响应（非流式）
            "options": {
                "temperature": 0.3,  # 低温度：更确定性的回答
                "num_predict": 1000,  # 最大生成 Token 数
            },
        }

        req = urllib.request.Request(
            f"{base_url}/api/chat",
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )

        # 发送请求并读取响应
        with urllib.request.urlopen(req, timeout=120) as resp:
            result = json.loads(resp.read().decode("utf-8"))

        answer = result.get("message", {}).get("content", "")

        if not answer:
            logger.warning("Ollama 返回空回答，降级到 Mock 模式")
            return _mock_generate(question, sources)

        return {
            "answer": answer,
            "sources": sources or [],
            "followUps": [],
        }

    except urllib.error.URLError as e:
        # Ollama 服务不可用（可能模型未下载或服务未启动）
        logger.error(f"Ollama 调用失败（服务不可用）: {e}")
        logger.info("提示：请确认 Ollama 服务已启动且模型已下载")
        return _mock_generate(question, sources)
    except Exception as e:
        logger.error(f"Ollama 调用失败: {e}")
        return _mock_generate(question, sources)

# System Prompt（系统提示词）—— Prompt Engineering 的核心
#
# 什么是 System Prompt？
#   System Prompt 是发送给 LLM 的"角色设定"和"行为指令"。
#   它不直接出现在用户的问题中，但会影响 LLM 的回答风格和内容。
#   类似于给员工一份"工作手册"，告诉他们如何回答客户问题。
#
# Prompt Engineering 最佳实践（本 System Prompt 中的应用）：
#   1. 明确角色定义："你是一个专业的企业知识库助手"
#   2. 列出具体任务：基于文档回答、没有信息时告知、保持简洁
#   3. 设定约束条件：不要编造信息、不确定时建议查看文档
#   4. 指定输出风格：友好和专业的语气
#
# RAG 场景中的 Prompt 设计要点：
#   - 强调"基于提供的文档内容回答"（让 LLM 依赖上下文而非自身知识）
#   - "不要编造信息"（减少幻觉，hallucination）
#   - "引用具体的文档来源"（可追溯性）
SYSTEM_PROMPT = """你是一个专业的企业知识库助手，负责回答员工的问题。

你的任务：
1. 基于提供的文档内容回答问题
2. 如果文档中没有相关信息，明确告知用户
3. 保持答案简洁、准确、专业
4. 引用具体的文档来源

注意事项：
- 不要编造信息
- 如果不确定，建议用户查看相关文档
- 保持友好和专业的语气
"""


def _openai_generate(question: str, context: str, sources: List[Dict]) -> Dict[str, Any]:
    """
    使用 OpenAI API 生成回答

    调用 OpenAI 的 Chat Completions 接口，使用 GPT-3.5-turbo 模型。

    Messages 结构说明：
        - system: 系统提示词，定义 AI 的角色和行为规则
        - user: 用户消息，包含问题 + 检索到的文档上下文

    Temperature 参数：
        0.3 是较低的值，让回答更加确定性和一致性。
        对于 RAG 场景，低温度更好：我们希望基于事实回答，不需要创意。

    为什么 max_tokens=1000？
        1000 tokens 约等于 500-1000 个中文字符，对于企业知识库问答足够。
        设置上限可以防止 LLM 输出过长无关内容，也有助于控制 API 成本。

    Args:
        question: 用户问题
        context: 检索到的文档上下文
        sources: 来源文档列表

    Returns:
        Dict[str, Any]: LLM 生成的回答
    """
    try:
        from openai import OpenAI

        # OpenAI SDK 会自动读取 OPENAI_API_KEY 环境变量
        client = OpenAI()
        # 构造用户消息：将问题和检索到的上下文组合
        # 如果有上下文，告诉 LLM 基于文档内容回答（RAG 的核心思想）
        # 如果没有上下文（检索未命中），直接传递问题
        user_message = f"问题：{question}\n\n相关文档：\n{context}\n\n请基于以上文档内容回答。" if context else question

        response = client.chat.completions.create(
            model="gpt-3.5-turbo",
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},  # 角色设定
                {"role": "user", "content": user_message},      # 用户问题 + 上下文
            ],
            temperature=0.3,   # 低温度：更确定性、更基于事实的回答
            max_tokens=1000,   # 限制回答长度
        )

        return {
            "answer": response.choices[0].message.content,
            "sources": sources or [],
            "followUps": [],  # 真实 LLM 模式暂不生成追问建议（可扩展）
        }
    except Exception as e:
        # API 调用失败时降级到 Mock 模式（优雅降级策略）
        logger.error(f"OpenAI 调用失败: {e}")
        return _mock_generate(question, sources)


def _anthropic_generate(question: str, context: str, sources: List[Dict]) -> Dict[str, Any]:
    """
    使用 Anthropic API 生成回答

    调用 Anthropic 的 Messages API，使用 Claude Haiku 模型。
    Anthropic 的 API 结构与 OpenAI 略有不同：
        - system 提示词是顶级参数，不在 messages 数组中
        - 模型名称格式不同（claude-haiku-4-5-20251001）

    注意两种 API 的差异：
        OpenAI:    messages=[{"role": "system", ...}, {"role": "user", ...}]
        Anthropic: system=SYSTEM_PROMPT, messages=[{"role": "user", ...}]

    这是适配器模式（Adapter Pattern）的典型应用：
        不同的 LLM Provider 有不同的 API 接口，但我们对外暴露统一的调用方式。

    Args:
        question: 用户问题
        context: 检索到的文档上下文
        sources: 来源文档列表

    Returns:
        Dict[str, Any]: LLM 生成的回答
    """
    try:
        import anthropic

        # Anthropic SDK 会自动读取 ANTHROPIC_API_KEY 环境变量
        client = anthropic.Anthropic()
        user_message = f"问题：{question}\n\n相关文档：\n{context}\n\n请基于以上文档内容回答。" if context else question

        response = client.messages.create(
            model="claude-haiku-4-5-20251001",  # Claude Haiku 4.5: 快速且经济
            max_tokens=1000,
            system=SYSTEM_PROMPT,  # Anthropic 的 system 是顶级参数
            messages=[{"role": "user", "content": user_message}],
        )

        return {
            "answer": response.content[0].text,  # Anthropic 的响应结构
            "sources": sources or [],
            "followUps": [],
        }
    except Exception as e:
        # 同样采用优雅降级策略
        logger.error(f"Anthropic 调用失败: {e}")
        return _mock_generate(question, sources)
