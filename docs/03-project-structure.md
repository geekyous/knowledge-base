# 企业知识库问答系统 - 项目结构说明

## 📂 项目整体结构

```
knowledge-base/
├── frontend/                    # Vue 3 前端项目
├── backend/                     # Spring Boot 后端项目
├── ai-service/                  # Python AI 服务
├── deploy/                      # 部署配置
│   └── nginx/                   # Nginx 配置与 SSL 证书
├── docs/                        # 项目文档
├── docs/scripts/                # 开发脚本
├── docker-compose.yml           # Docker 编排配置
├── .env.example                 # 环境变量示例
├── .gitignore                   # Git 忽略配置
└── README.md                    # 项目说明文档
```

## 🎨 前端项目结构 (frontend/)

```
frontend/
├── public/                      # 静态资源
│   ├── favicon.ico
│   └── logo.png
├── src/                         # 源代码
│   ├── assets/                  # 资源文件
│   │   ├── images/             # 图片
│   │   └── styles/             # 全局样式
│   ├── components/             # 公共组件
│   │   ├── common/             # 通用组件
│   │   │   ├── Header.vue      # 头部组件
│   │   │   ├── Footer.vue      # 底部组件
│   │   │   └── Sidebar.vue     # 侧边栏组件
│   │   ├── business/           # 业务组件
│   │   │   ├── SearchBox.vue   # 搜索框
│   │   │   ├── DocumentCard.vue # 文档卡片
│   │   │   └── ChatMessage.vue # 聊天消息
│   │   └── layout/             # 布局组件
│   │       ├── MainLayout.vue  # 主布局
│   │       └── BlankLayout.vue # 空白布局
│   ├── views/                  # 页面组件
│   │   ├── auth/               # 认证页面
│   │   │   ├── Login.vue       # 登录页
│   │   │   └── Register.vue    # 注册页
│   │   ├── home/               # 首页
│   │   │   └── Home.vue
│   │   ├── document/           # 文档管理
│   │   │   ├── DocumentList.vue    # 文档列表
│   │   │   ├── DocumentDetail.vue  # 文档详情
│   │   │   └── DocumentEdit.vue    # 文档编辑
│   │   ├── search/             # 搜索
│   │   │   └── Search.vue
│   │   ├── chat/               # 智能问答
│   │   │   └── Chat.vue
│   │   ├── admin/              # 管理后台
│   │   │   ├── Dashboard.vue  # 仪表板
│   │   │   ├── UserManagement.vue # 用户管理
│   │   │   └── SystemSettings.vue  # 系统设置
│   │   └── profile/            # 个人中心
│   │       └── Profile.vue
│   ├── api/                    # API 接口
│   │   ├── request.ts          # HTTP 请求封装
│   │   ├── auth.ts             # 认证接口
│   │   ├── document.ts         # 文档接口
│   │   ├── search.ts           # 搜索接口
│   │   └── chat.ts             # 问答接口
│   ├── stores/                 # Pinia 状态管理
│   │   ├── user.ts             # 用户状态
│   │   ├── document.ts         # 文档状态
│   │   └── chat.ts             # 聊天状态
│   ├── router/                 # 路由配置
│   │   └── index.ts
│   ├── types/                  # TypeScript 类型定义
│   │   ├── user.ts
│   │   ├── document.ts
│   │   └── common.ts
│   ├── utils/                  # 工具函数
│   │   ├── format.ts           # 格式化工具
│   │   ├── validate.ts         # 验证工具
│   │   └── storage.ts          # 存储工具
│   ├── constants/              # 常量定义
│   │   └── index.ts
│   ├── directives/             # 自定义指令
│   │   └── permission.ts       # 权限指令
│   ├── App.vue                 # 根组件
│   └── main.ts                 # 应用入口
├── index.html                  # HTML 模板
├── vite.config.ts              # Vite 配置
├── tsconfig.json               # TypeScript 配置
├── package.json                # 依赖配置
└── README.md                   # 前端说明
```

### 前端目录说明

| 目录 | 职责 |
|------|------|
| `src/components/common/` | 通用组件，可跨项目复用 |
| `src/components/business/` | 业务相关组件 |
| `src/views/` | 页面级组件，对应路由 |
| `src/api/` | API 接口封装，统一管理后端接口 |
| `src/stores/` | Pinia 状态管理 |
| `src/router/` | 路由配置和导航守卫 |
| `src/utils/` | 工具函数库 |
| `src/types/` | TypeScript 类型定义 |

## 🔧 后端项目结构 (backend/)

```
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/geekyous/kb/
│   │   │       ├── KnowledgeBaseApplication.java  # 启动类
│   │   │       ├── config/                         # 配置类
│   │   │       │   ├── SecurityConfig.java        # 安全配置
│   │   │       │   ├── RedisConfig.java            # Redis 配置
│   │   │       │   ├── ElasticsearchConfig.java    # ES 配置
│   │   │       │   ├── CorsConfig.java             # CORS 配置
│   │   │       │   └── AiServiceConfig.java        # AI 服务配置
│   │   │       ├── controller/                     # 控制器层
│   │   │       │   ├── AuthController.java        # 认证控制器
│   │   │       │   ├── DocumentController.java     # 文档控制器
│   │   │       │   ├── SearchController.java       # 搜索控制器
│   │   │       │   ├── ChatController.java        # 问答控制器
│   │   │       │   ├── UserController.java         # 用户控制器
│   │   │       │   └── AdminController.java        # 管理控制器
│   │   │       ├── service/                        # 服务层
│   │   │       │   ├── AuthService.java            # 认证服务
│   │   │       │   ├── DocumentService.java        # 文档服务
│   │   │       │   ├── SearchService.java          # 搜索服务
│   │   │       │   ├── ChatService.java           # 问答服务
│   │   │       │   ├── UserService.java           # 用户服务
│   │   │       │   └── AIService.java              # AI 服务调用
│   │   │       ├── repository/                     # 数据访问层
│   │   │       │   ├── UserRepository.java
│   │   │       │   ├── DocumentRepository.java
│   │   │       │   ├── CategoryRepository.java
│   │   │       │   └── RoleRepository.java
│   │   │       ├── entity/                         # 实体类
│   │   │       │   ├── User.java
│   │   │       │   ├── Document.java
│   │   │       │   ├── Category.java
│   │   │       │   ├── Role.java
│   │   │       │   └── Permission.java
│   │   │       ├── dto/                            # 数据传输对象
│   │   │       │   ├── request/                   # 请求 DTO
│   │   │       │   │   ├── LoginRequest.java
│   │   │       │   │   ├── DocumentCreateRequest.java
│   │   │       │   │   └── SearchRequest.java
│   │   │       │   └── response/                   # 响应 DTO
│   │   │       │       ├── UserResponse.java
│   │   │       │       ├── DocumentResponse.java
│   │   │       │       └── SearchResultResponse.java
│   │   │       ├── security/                       # 安全相关
│   │   │       │   ├── JwtProvider.java           # JWT 工具
│   │   │       │   ├── UserDetailsServiceImpl.java # 用户详情服务
│   │   │       │   └── JwtAuthenticationFilter.java # JWT 过滤器
│   │   │       ├── exception/                      # 异常处理
│   │   │       │   ├── GlobalExceptionHandler.java
│   │   │       │   ├── BusinessException.java
│   │   │       │   └── ErrorCode.java
│   │   │       ├── utils/                          # 工具类
│   │   │       │   ├── Response.java              # 统一响应
│   │   │       │   ├── PasswordUtil.java
│   │   │       │   └── DateUtil.java
│   │   │       ├── annotation/                     # 自定义注解
│   │   │       │   ├── RequireRole.java
│   │   │       │   └── CurrentUser.java
│   │   │       └── interceptor/                    # 拦截器
│   │   │           ├── LoggingInterceptor.java
│   │   │           └── RateLimitInterceptor.java
│   │   └── resources/
│   │       ├── application.yml              # 主配置
│   │       ├── application-dev.yml          # 开发环境
│   │       ├── application-prod.yml         # 生产环境
│   │       ├── application-test.yml         # 测试环境
│   │       ├── logback-spring.xml           # 日志配置
│   │       └── db/
│   │           └── migration/               # 数据库迁移
│   │               └── V1__init_schema.sql
│   └── test/
│       └── java/
│           └── com/geekyous/kb/
│               ├── service/                 # 服务测试
│               ├── controller/             # 控制器测试
│               └── repository/              # 数据访问测试
├── pom.xml                          # Maven 配置
├── Dockerfile                        # Docker 构建文件
└── README.md                         # 后端说明
```

### 后端目录说明

| 目录 | 职责 | 设计原则 |
|------|------|----------|
| `controller/` | 接收 HTTP 请求 | 薄层，只做参数校验和响应封装 |
| `service/` | 业务逻辑处理 | 核心业务逻辑，事务控制 |
| `repository/` | 数据访问 | 继承 JpaRepository，自定义查询 |
| `entity/` | 数据库实体 | 对应数据库表，使用 JPA 注解 |
| `dto/` | 数据传输对象 | 前后端交互数据结构 |
| `security/` | 安全认证 | JWT 认证，权限验证 |
| `config/` | 配置类 | 各组件配置 |
| `utils/` | 工具类 | 通用工具方法 |

## 🤖 AI 服务结构 (ai-service/)

```
ai-service/
├── app/
│   ├── main.py                     # FastAPI 应用入口
│   ├── config.py                   # 配置管理
│   ├── api/                        # API 路由
│   │   ├── __init__.py
│   │   ├── chat.py                 # 问答接口
│   │   ├── document.py             # 文档向量化接口
│   │   └── health.py               # 健康检查
│   ├── services/                   # 业务服务
│   │   ├── __init__.py
│   │   ├── rag_service.py          # RAG 服务
│   │   ├── embedding_service.py    # 向量化服务
│   │   ├── llm_service.py          # LLM 服务
│   │   └── retrieval_service.py    # 检索服务
│   ├── models/                     # 数据模型
│   │   ├── __init__.py
│   │   ├── request.py              # 请求模型
│   │   ├── response.py             # 响应模型
│   │   └── schema.py               # 数据模式
│   ├── core/                       # 核心配置
│   │   ├── __init__.py
│   │   ├── vector_store.py         # 向量数据库
│   │   ├── llm.py                  # LLM 初始化
│   │   └── embeddings.py           # Embedding 模型
│   ├── utils/                      # 工具函数
│   │   ├── __init__.py
│   │   ├── text_processing.py      # 文本处理
│   │   └── logger.py               # 日志工具
│   └── middleware/                 # 中间件
│       ├── __init__.py
│       └── error_handler.py        # 错误处理
├── tests/                          # 测试
│   ├── test_chat.py
│   ├── test_embedding.py
│   └── test_rag.py
├── requirements.txt                # Python 依赖
├── Dockerfile                      # Docker 构建文件
├── .env.example                    # 环境变量示例
└── README.md                       # AI 服务说明
```

### AI 服务目录说明

| 目录 | 职责 |
|------|------|
| `app/api/` | FastAPI 路由定义 |
| `app/services/` | 核心业务逻辑 |
| `app/models/` | Pydantic 数据模型 |
| `app/core/` | LLM、向量数据库等核心组件 |
| `app/utils/` | 文本处理等工具函数 |

## 📁 文档目录结构 (docs/)

```
docs/
├── 01-project-overview.md         # 项目概述
├── 02-tech-stack.md              # 技术栈详解
├── 03-project-structure.md       # 项目结构说明
├── 04-development-setup.md       # 开发环境搭建
├── 05-api-design.md              # API 设计规范
├── 06-database-design.md         # 数据库设计
├── 07-coding-standards.md        # 编码规范
├── 08-deployment-guide.md        # 部署指南
├── 09-team-guide.md              # 团队协作指南
├── features/                     # 功能设计文档
│   ├── F01-auth/                 # 认证模块
│   ├── F02-search/               # 搜索模块
│   ├── F03-document/             # 文档管理模块
│   ├── F04-ai-chat/              # AI 问答模块
│   ├── F05-profile/              # 个人中心模块
│   └── F06-admin/                # 管理后台模块
└── scripts/                      # 开发脚本
    ├── start-demo.sh             # 一键启动
    ├── dev-infra.sh              # 本地开发基础设施
    ├── health-check.sh           # 健康检查
    ├── pull-ollama-models.sh     # 拉取 Ollama 模型
    └── import-demo-data.py       # 导入演示数据
```

## 🛠️ 脚本目录结构 (docs/scripts/)

```
docs/scripts/
├── setup-dev.sh                   # 开发环境搭建
├── build.sh                       # 构建脚本
├── dev-start.sh                   # 开发环境启动
├── dev-stop.sh                    # 开发环境停止
├── test.sh                        # 测试脚本
└── deploy.sh                      # 部署脚本
```

## 🔧 配置文件说明

### 根目录配置文件
| 文件 | 用途 |
|------|------|
| `docker-compose.yml` | Docker 编排配置（生产 + 开发共用，通过 .env 区分） |
| `.env.example` | 环境变量模板 |
| `.gitignore` | Git 忽略规则 |
| `.editorconfig` | 编辑器配置 |

## 📊 数据流向图

```
用户操作
   ↓
Vue 组件 (frontend/src/views/)
   ↓
API 调用 (frontend/src/api/)
   ↓
Spring Boot Controller (backend/src/main/java/.../controller/)
   ↓
Service 业务逻辑 (backend/src/main/java/.../service/)
   ↓
Repository 数据访问 (backend/src/main/java/.../repository/)
   ↓
MySQL 数据库存储
```

## 🎯 模块依赖关系

```
frontend → backend → MySQL
                ↓
               Redis
                ↓
         Elasticsearch
                ↓
         ai-service → Qdrant
```

## 📝 命名规范

### Java 后端
- **包名**：全小写，使用点分隔
- **类名**：大驼峰（PascalCase）
- **方法名**：小驼峰（camelCase）
- **常量名**：全大写，下划线分隔

### Vue 前端
- **组件名**：大驼峰（PascalCase）
- **文件名**：小驼峰（camelCase）或短横线（kebab-case）
- **变量/函数**：小驼峰（camelCase）

### Python
- **文件名**：小写，下划线分隔（snake_case）
- **类名**：大驼峰（PascalCase）
- **函数/变量**：小写，下划线分隔（snake_case）

---

**文档版本：** v1.0
**最后更新：** 2026-05-31
