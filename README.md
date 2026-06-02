# 企业知识库问答系统

一个基于 AI 的企业级知识管理和智能问答系统，帮助企业高效管理内部知识，提供智能问答服务。

## 📋 项目简介

本项目是一个完整的企业知识库解决方案，包含文档管理、全文搜索、智能问答等核心功能。通过 RAG（检索增强生成）技术，为企业提供准确、及时的智能问答服务。

### 核心功能

- 🔍 **智能搜索** - 基于 Elasticsearch 的全文搜索和语义搜索
- 📚 **文档管理** - 支持多种格式文档的上传、编辑、版本控制
- 🤖 **AI 问答** - 基于 LangChain 和 LLM 的智能问答系统
- 👥 **用户管理** - 完善的权限控制和用户管理
- 📊 **数据分析** - 使用统计和行为分析
- 🎨 **响应式设计** - 支持桌面端和移动端

## 🏗️ 技术架构

```
┌─────────────────────────────────────────────────────┐
│              Vue 3 前端 (TypeScript)                 │
└─────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────┐
│         Spring Boot 后端 (Java 17)                  │
│  • 用户管理  • 文档管理  • 权限控制  • 统计分析     │
└─────────────────────────────────────────────────────┘
                          ↓
┌──────────────────┬────────────────┬─────────────────┐
│     MySQL       │    Redis       │  Elasticsearch  │
│   (业务数据)      │   (缓存/会话)   │   (全文搜索)    │
└──────────────────┴────────────────┴─────────────────┘
                          ↓
┌─────────────────────────────────────────────────────┐
│       Python AI 服务 (FastAPI + LangChain)          │
│  • RAG 检索  • LLM 问答  • 文档向量化                │
└─────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────┐
│         Qdrant 向量数据库                            │
└─────────────────────────────────────────────────────┘
```

## 🚀 快速开始

### 环境要求

- Node.js 18+
- Java 17+
- Python 3.11+
- Docker & Docker Compose
- MySQL 8.0+
- Redis 7.x

### 一键启动（开发环境）

```bash
# 克隆项目
git clone <repository-url>
cd knowledge-base

# 启动所有服务
docker-compose -f docker-compose.dev.yml up -d

# 访问应用
# 前端：http://localhost:5173
# 后端：http://localhost:8080
# AI服务：http://localhost:8000
```

### 手动启动

详细步骤请参考 [开发环境搭建指南](docs/04-development-setup.md)

## 📁 项目结构

```
knowledge-base/
├── frontend/              # Vue 3 前端项目
├── backend/               # Spring Boot 后端项目
├── ai-service/            # Python AI 服务
├── docs/                  # 项目文档
├── scripts/               # 开发脚本
└── docker-compose.yml     # Docker 编排配置
```

详细结构说明请参考 [项目结构文档](docs/03-project-structure.md)

## 📚 文档导航

| 文档 | 描述 |
|------|------|
| [项目概述](docs/01-project-overview.md) | 系统介绍和功能说明 |
| [技术栈详解](docs/02-tech-stack.md) | 技术选型和架构设计 |
| [项目结构](docs/03-project-structure.md) | 目录结构和职责划分 |
| [开发环境搭建](docs/04-development-setup.md) | 环境安装和配置 |
| [API 设计规范](docs/05-api-design.md) | 接口标准和示例 |
| [数据库设计](docs/06-database-design.md) | 表结构和关系设计 |
| [编码规范](docs/07-coding-standards.md) | 代码风格和最佳实践 |
| [部署指南](docs/08-deployment-guide.md) | 生产环境部署 |
| [团队协作指南](docs/09-team-guide.md) | 工作流程和规范 |

## 🛠️ 开发指南

### 前端开发

```bash
cd frontend
npm install
npm run dev
```

### 后端开发

```bash
cd backend
mvn spring-boot:run
```

### AI 服务开发

```bash
cd ai-service
pip install -r requirements.txt
python -m uvicorn app.main:app --reload
```

## 🧪 测试

```bash
# 前端测试
cd frontend && npm run test

# 后端测试
cd backend && mvn test

# AI 服务测试
cd ai-service && pytest
```

## 📊 技术栈

### 前端
- Vue 3.3 + TypeScript
- Vite 构建工具
- Element Plus UI 组件库
- Pinia 状态管理
- Vue Router 路由
- Axios HTTP 客户端

### 后端
- Java 17
- Spring Boot 3.2
- Spring Security + JWT
- Spring Data JPA
- MySQL 8.0
- Redis 7.x
- Elasticsearch 8.x

### AI 服务
- Python 3.11
- FastAPI
- LangChain
- OpenAI/Claude API
- Qdrant 向量数据库
- Celery 异步任务

## 🤝 贡献指南

欢迎贡献代码！请阅读 [团队协作指南](docs/09-team-guide.md) 了解详情。

## 📄 许可证

本项目采用 MIT 许可证。

## 👥 团队

- 产品负责人：[Geekyous]
- 技术负责人：[Geekyous]
- 前端开发：[Geekyous]
- 后端开发：[Geekyous]
- AI 工程师：[Geekyous]

## 📞 联系方式

如有问题，请提交 Issue 或联系项目维护者。

---

**最后更新：** 2026-05-31
**版本：** v1.0.0
