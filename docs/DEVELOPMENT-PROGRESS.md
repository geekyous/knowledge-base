# 📋 项目开发进度

> 企业知识库问答系统 — 开发进度跟踪文档
>
> **最后更新：** 2026-06-04

---

## 📊 项目概览

| 指标 | 数据 |
|------|------|
| 技术栈 | Vue 3 + Spring Boot 3.2 + FastAPI + MySQL + Redis + Elasticsearch + Qdrant |
| 源码文件 | 68+ 个 |
| 代码行数 | 17,000+ 行（含教学注释） |
| 教学注释 | 7,000+ 行（含 💡学习要点） |
| 文档数量 | 21 篇 Markdown |
| Git 分支 | `main`（生产）+ `develop`（开发） |

---

## ✅ 已完成功能

### Sprint 0：项目设计与规划
- [x] 产品原型图设计（HTML交互原型）
- [x] 9 篇基础技术文档（项目概述、技术栈、结构、搭建、API、数据库、编码、部署、团队）
- [x] 6 个功能模块设计文档（F01-F06）
- [x] 功能实施指南

### Sprint 1：数据基础设施
- [x] MySQL 数据库 Schema（12 张表，含 kb_ 前缀）
- [x] 精简版种子数据（3 用户 + 16 分类 + 6 文档 + 8 标签 + 3 对话）
- [x] Qdrant 向量集合初始化脚本（支持真实/伪向量双模式）
- [x] Elasticsearch 索引初始化脚本（IK 中文分词 + 降级策略）
- [x] Redis 缓存初始化脚本（Sorted Set + Hash）

### Sprint 2：Docker 容器环境
- [x] Java 后端 Dockerfile（多阶段构建）
- [x] Python AI 服务 Dockerfile
- [x] docker-compose.yml（7 服务编排 + 健康检查 + 依赖管理，ES 镜像修正为 8.19.16）
- [x] .env 环境变量配置

### Sprint 3：Java 后端（最小可用版本）
- [x] Entity 层：User / Document / Category（JPA 注解 + Lombok）
- [x] Repository 层：UserRepository / DocumentRepository / CategoryRepository
- [x] Service 层：AuthService / DocumentService
- [x] Controller 层：Auth / Document / Category / Health
- [x] DTO 层：ApiResponse / LoginRequest / LoginResponse
- [x] Config 层：JwtConfig / SecurityConfig / JwtAuthenticationFilter / OpenApiConfig
	- [x] Swagger UI（springdoc-openapi 2.3.0 + JWT 安全方案 + 4 组 Tag + 全端点 @Operation 注解）
- [x] RESTful API 完整实现（CRUD + 搜索 + 认证）

### Sprint 4：Python AI 服务
- [x] 核心模块：
  - `embedding.py` — 文本向量化（真实/伪向量双模式）
  - `vector_store.py` — Qdrant 向量存储和检索
  - `llm.py` — LLM 接口（支持 OpenAI / Anthropic / 模拟模式）
  - `retriever.py` — RAG 检索器
  - `rag.py` — RAG 主流程
- [x] API 接口：`/chat/ask`（问答）、`/documents/index`（文档索引）、`/documents/search`（向量搜索）
- [x] 模拟模式：预定义 7 组问答对，无需 API Key 即可演示
- [x] FastAPI 应用配置（CORS / 生命周期 / 异常处理）

### Sprint 5：Vue 3 前端
- [x] 路由系统（10+ 路由，懒加载）
- [x] 状态管理（Pinia user store + localStorage 持久化）
- [x] HTTP 客户端（Axios + JWT 拦截器 + 错误处理）
- [x] TypeScript 类型定义
- [x] API 客户端模块（documents / chat / search）
- [x] 页面视图：
  - Home — 首页（分类卡片 + 搜索入口）
  - Login — 登录页（表单验证 + 路由守卫）
  - Search — 搜索页（关键词搜索 + 分类筛选 + 结果高亮）
  - DocumentList — 文档列表（分页 + 筛选 + 推荐文档）
  - DocumentDetail — 文档详情（Markdown 渲染 + 来源引用）
  - DocumentEdit — 文档编辑（表单 + 标签输入 + Markdown 内容）
  - Chat — AI 问答（多轮对话 + 来源展示 + 推荐问题）
  - Profile — 个人中心（统计卡片 + 活动时间轴）
  - Admin/Dashboard — 管理后台（统计 + 表格 + 审核）
  - NotFound — 404 页面
- [x] 布局组件：Header / Footer / MainLayout
- [x] 业务组件：SearchBox（搜索框）/ DocumentCard（文档卡片）
- [x] 全局样式：main.css（滚动条美化 / 过渡动画 / 文字选中样式）

### Sprint 6：工具和文档
- [x] 一键启动脚本（`docs/scripts/start-demo.sh`）
- [x] 健康检查脚本（`docs/scripts/health-check.sh`）
- [x] 数据导入脚本（`docs/scripts/import-demo-data.py`）
- [x] 快速开始指南（`docs/DEMO-QUICKSTART.md`）
- [x] 演示场景指南（`docs/DEMO-SCENARIOS.md`）

### 教学注释
- [x] Java 后端 20 个文件 — Javadoc + 💡学习要点（Spring/JPA/Security 概念）
- [x] Python AI 服务 16 个文件 — 模块文档 + Google-style docstring（RAG/AI/ML 概念）
- [x] Vue 前端 22 个文件 — 组件注释（Vue3/TS/Pinia/Router 概念）
- [x] Shell/Python 脚本 3 个文件 — 命令详解 + 设计模式说明

---

## ⚠️ 待优化项

### 高优先级
| 编号 | 事项 | 说明 | 影响 |
|------|------|------|------|
| OPT-1 | **种子数据扩充** | 当前 6 篇文档，需扩充到 30+ 篇，覆盖所有二级分类 | 演示效果 |
| OPT-2 | **DocumentEdit.vue 已创建** ✅| 文档编辑器页面已创建（表单+标签输入+Markdown内容+草稿保存） | 功能完整性 |
| OPT-3 | **application.yml 调整** | `ddl-auto` 需改为 `none`（表由 SQL 创建） | 部署运行 |
| OPT-4 | **实际构建验证** | 项目尚未 `docker compose up` 实际验证 | 可运行性 |

### 中优先级
| 编号 | 事项 | 说明 |
|------|------|------|
| OPT-5 | 后端 application-dev/prod 多环境配置 | 当前只有单一 application.yml |
| OPT-6 | 前端 mock 数据完善 | API 失败时的 fallback mock 需要更丰富 |
| OPT-7 | AI 对话持久化 | 当前使用内存存储，重启后丢失 |
| OPT-8 | 文档搜索服务（SearchService） | 后端 SearchController 未接入 Elasticsearch |
| OPT-9 | Nginx 配置文件 | `nginx/nginx.conf` 尚未创建 |
| OPT-10 | 文件上传解析功能 | Document 上传接口仅有骨架 |

### 低优先级
| 编号 | 事项 | 说明 |
|------|------|------|
| OPT-11 | 国际化（i18n） | 支持中英文切换 |
| OPT-12 | 暗色模式 | 前端主题切换 |
| OPT-13 | 移动端响应式优化 | 更好的手机端体验 |
| OPT-14 | 单元测试和集成测试 | 后端/前端/AI 服务测试 |
| OPT-15 | CI/CD 流水线 | GitHub Actions 自动构建 |

---

## 📅 下一步计划

### Phase 1：验证可运行性（1-2 天）
1. 创建 Nginx 配置文件
2. 调整 application.yml 多环境配置
3. `docker compose up` 实际构建验证
4. 修复构建过程中发现的问题

### Phase 2：功能完善（3-5 天）
1. 扩充种子数据到 30+ 篇文档
2. 创建 DocumentEdit.vue（富文本编辑器）
3. 后端 SearchService 接入 Elasticsearch
4. AI 对话持久化到 MySQL

### Phase 3：生产就绪（3-5 天）
1. 添加单元测试和集成测试
2. 完善 Docker 生产配置
3. 配置 CI/CD 流水线
4. 性能优化和安全加固

### Phase 4：功能扩展（后续）
1. 通知系统（F07）
2. 移动端适配（F08）
3. 文件上传解析（PDF/Word/TXT）
4. 数据导出功能

---

## 🏗️ 架构总览

```
┌─────────────────────────────────────────────────┐
│              Nginx (反向代理 :80)                │
└────────────┬────────────────────────────────────┘
             │
    ┌────────┴────────┐
    ▼                 ▼
┌──────────┐   ┌──────────────┐
│ 前端     │   │  后端 API    │
│ Vue 3    │   │ Spring Boot  │
│ :80      │   │ :8080        │
└──────────┘   └──┬──────┬───┘
                  │      │
         ┌────────┘      └────────┐
         ▼                        ▼
  ┌─────────────┐        ┌──────────────┐
  │  MySQL 8.0  │        │  AI 服务     │
  │  :3306      │        │  FastAPI     │
  └─────────────┘        │  :8000       │
  ┌─────────────┐        └──┬───────┬──┘
  │  Redis 7    │           │       │
  │  :6379      │           ▼       ▼
  └─────────────┘   ┌──────────┐ ┌──────────┐
                    │ Qdrant   │ │    ES    │
                    │ :6333    │ │  :9200   │
                    └──────────┘ └──────────┘
```

---
## 📝 变更日志

### v0.3.0 — 前端可运行 + Swagger API 文档（2026-06-04）

**新增**
- 前端 Vue 组件补全：DocumentEdit.vue（文档编辑页，支持新建/编辑/草稿）
- 前端通用组件：SearchBox.vue（搜索框）、DocumentCard.vue（文档卡片）
- 全局样式文件：main.css（滚动条美化/过渡动画/选中样式）
- 后端 Swagger UI 完整配置：
  - OpenApiConfig 配置类（API 元信息 + JWT Bearer 安全方案）
  - 4 个 Controller 添加 @Tag + @Operation 注解（共 15 个端点）
  - 3 个 DTO 添加 @Schema 注解（含 example 数据）
  - Swagger UI 可通过 http://localhost:8080/api/swagger-ui.html 访问
  - 支持 Authorize 按钮输入 JWT Token 测试受保护接口

**优化**
- 前端构建依赖补全：添加 sass-embedded
- Element Plus 图标修正：Laptop → Monitor、Shield → Key（Home.vue 分类卡片）
- Docker 镜像版本修正：elasticsearch:8.0.0 → elasticsearch:8.19.16（3 处）
- Docker daemon 镜像加速配置（国内网络环境优化）
- SecurityConfig 放行 Swagger 相关路径（/swagger-ui/**、/v3/api-docs/**）

**修复**
- 前端 Vite 启动错误（共 3 类 15 个错误全部修复）：
  - 13 个 Vue 文件 `<script setup>` 中的 HTML 注释 `<!-- -->` 转为 JS 注释 `//`
  - 2 个 Vue 文件 `<style>` 中的 HTML 注释转为 CSS 注释 `/* */`
  - DocumentDetail.vue JSDoc 注释中 `*/` 导致语法错误
  - 3 个 Vue 文件 `<template>` 中被误转换的注释恢复为 `<!-- -->`
  - 缺失文件创建：SearchBox.vue / DocumentCard.vue / DocumentEdit.vue / main.css
- 后端编译错误：Response.java 泛型推断修复
- 项目所有 @author 统一为 Geekyous Guo（20 个 Java 文件 + 文档）

---

### v0.2.0 — 原型设计风格分离（2026-06-03）

**新增**
- 粘性导航栏（快速跳转 6 大模块，滚动高亮）
- 返回顶部按钮（滚动超过 400px 显示）
- 滚动淡入动画（IntersectionObserver + 交错延迟）
- APP 端手机外壳模拟框（圆角边框 + 刘海 + 底部指示条）
- 项目开发进度文档
- 原型补全所有缺失页面（v1.2 → v1.3），页面总数从 15+ 增至 22+
- **[PC] 认证与权限**：登录页、注册页、密码修改页、401/403/404 错误页
- **[PC] 文档详情页**：面包屑导航 + Markdown 渲染 + 目录 TOC + 收藏/分享
- **[PC] 文档版本对比**：Split Diff 布局 + 差异高亮 + 版本历史
- **[PC] 分类管理**：树形结构 + 编辑区 + 图标选择 + 拖拽排序
- **[PC] 标签管理**：标签云 + 标签列表（颜色/使用统计/操作）
- **[PC] 个人设置**：4 Tab（账户/通知/隐私/界面）
- **[PC] 用户管理**：搜索筛选 + 用户表格（CRUD/角色/状态/分页）
- **[PC] 文档审核**：待审核/已通过/已拒绝 Tab + 审核卡片 + 批量操作
- **[APP] 登录页**：phone-frame 内移动端登录表单
- **[APP] 个人设置页**：iOS 风格设置列表 + Toggle 开关

**优化**
- 数据库表名添加 `kb_` 前缀
- 产品原型 HTML（v1.0 → v1.2）
- PC/APP 端风格分离：PC 端统一为方形企业 Web 风格（4-8px 圆角），APP 端保持移动端大圆角风格
- PC 端组件圆角统一：搜索框、输入框、按钮、标签、徽章等从胶囊形改为方形
- Section 标题添加 `[PC]` / `[APP]` 标识，导航栏同步区分
- PC 端响应式布局（1200px+ 即 3 列，原 1400px+）
- 屏幕卡片悬停效果（上浮 + 阴影增强）
- 头部区域（动画背景光效 + 文字阴影）
- 页脚区域（渐变背景 + 顶部彩条装饰）
- 变更日志改为按版本号分组

**修复**
- CSS 多余字符 `º`（第37行）
- 移动端 `position: fixed` 在 PC 浏览器中的溢出问题

---

### v0.1.0 — 项目初始化与核心开发（2026-05-31 ~ 2026-06-01）

**新增**
- 全部 6 个 Sprint 开发工作（Sprint 0 ~ Sprint 6）
- 7000+ 行教学注释
- Git 规范整理（conventional commits + 分支策略）

**完成**
- 项目初始化、技术栈选型、架构设计
- 9 篇基础文档 + 6 个功能设计文档
- 完整项目脚手架搭建

---

**项目版本：** v0.3.0
**仓库地址：** https://github.com/geekyous/knowledge-base
**维护者：** Geekyous
