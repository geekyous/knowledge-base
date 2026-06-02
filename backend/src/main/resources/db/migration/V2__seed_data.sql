-- 企业知识库问答系统 - 精简版种子数据
-- 版本: V2 (精简版，后续通过 V3 扩充)
-- 说明: 包含最小可用数据，用于学习和演示

SET NAMES utf8mb4;

-- =====================================================
-- 1. 用户数据 (3个: 管理员 + 编辑 + 普通用户)
-- 密码均为 BCrypt 加密的 admin123
-- =====================================================
INSERT INTO users (id, username, password, email, phone, role, status) VALUES
(1, 'admin',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@company.com',    '13800000001', 'ADMIN',  'ACTIVE'),
(2, 'editor', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'editor@company.com',   '13800000002', 'EDITOR', 'ACTIVE'),
(3, 'user1',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'user1@company.com',    '13800000003', 'USER',   'ACTIVE');

-- =====================================================
-- 2. 分类数据 (4个一级分类 + 二级分类)
-- =====================================================
-- 一级分类
INSERT INTO categories (id, name, slug, description, icon, sort_order, parent_id) VALUES
(1,  '人事制度', 'hr',          '人力资源管理相关制度文档',   'Users',       1, NULL),
(2,  '技术文档', 'tech',        '技术开发相关文档',           'Code',        2, NULL),
(3,  '销售支持', 'sales',       '销售培训和支持材料',         'TrendCharts', 3, NULL),
(4,  '合规法务', 'legal',       '法律法规和合规管理',         'Document',    4, NULL);

-- 二级分类
INSERT INTO categories (id, name, slug, description, icon, sort_order, parent_id) VALUES
(11, '招聘流程',   'hr-recruit',    '招聘管理流程',     'UserAdd',   1, 1),
(12, '薪酬福利',   'hr-salary',     '薪酬和福利制度',   'Money',     2, 1),
(13, '考勤制度',   'hr-attendance', '考勤和请假制度',   'Clock',     3, 1),
(21, '架构设计',   'tech-arch',     '系统架构设计文档', 'Grid',      1, 2),
(22, '接口文档',   'tech-api',      'API接口规范',     'Connection', 2, 2),
(23, '部署指南',   'tech-deploy',   '部署运维指南',     'Upload',    3, 2);

-- =====================================================
-- 3. 标签数据 (8个常用标签)
-- =====================================================
INSERT INTO tags (id, name, color, usage_count) VALUES
(1,  '流程',   '#409EFF', 3),
(2,  '规范',   '#67C23A', 2),
(3,  '指南',   '#E6A23C', 2),
(4,  '制度',   '#F56C6C', 2),
(5,  'API',    '#909399', 1),
(6,  '架构',   '#00B5AD', 1),
(7,  '销售',   '#F56C6C', 1),
(8,  '法律',   '#303133', 1);

-- =====================================================
-- 4. 文档数据 (6篇精简文档，覆盖主要分类)
-- =====================================================

-- 文档1: 员工手册 (人事制度 > 招聘流程)
INSERT INTO documents (id, title, summary, content, category_id, author_id, status, view_count, like_count, is_featured, published_at) VALUES
(1, '员工手册（2026版）',
 '公司员工行为规范和基本制度汇编',
 '# 员工手册（2026版）

## 第一章 总则

本手册适用于公司全体正式员工，旨在明确员工的基本权利和义务。

## 第二章 考勤管理

### 2.1 工作时间
- 标准工时：周一至周五 9:00-18:00
- 午休时间：12:00-13:00
- 弹性工时：8:30-10:00之间打卡即可

### 2.2 请假制度
- 年假：工作满1年享有5天年假，每增加1年增加1天，上限15天
- 病假：凭医院证明，每年不超过10天
- 事假：需提前申请，每年不超过5天
- 婚假：法定婚假3天，晚婚假7天

### 2.3 加班制度
- 工作日加班：按1.5倍工资计算
- 周末加班：按2倍工资计算或安排调休
- 法定假日加班：按3倍工资计算

## 第三章 薪酬福利

### 3.1 薪资结构
- 基本工资 + 绩效奖金 + 各项补贴
- 每月15日发放上月工资

### 3.2 五险一金
- 养老保险、医疗保险、失业保险、工伤保险、生育保险
- 住房公积金：个人和公司各缴纳12%

### 3.3 补充福利
- 年度体检
- 节日礼品
- 团队建设活动
- 餐补和交通补贴

## 第四章 行为规范

### 4.1 职业道德
- 遵守公司规章制度
- 保守公司商业秘密
- 维护公司形象和利益

### 4.2 信息技术使用
- 不得安装未经授权的软件
- 不得将公司数据外泄
- 定期更换系统密码',
 11, 1, 'PUBLISHED', 256, 18, TRUE, '2026-01-15 10:00:00');

-- 文档2: 年假申请流程 (人事制度 > 考勤制度)
INSERT INTO documents (id, title, summary, content, category_id, author_id, status, view_count, like_count, is_featured, published_at) VALUES
(2, '年假申请流程',
 '详细说明年假天数计算方法和申请审批流程',
 '# 年假申请流程

## 一、年假天数计算

| 工龄      | 年假天数 |
|-----------|----------|
| 1-5年     | 5天      |
| 5-10年    | 7天      |
| 10-15年   | 10天     |
| 15年以上   | 15天     |

> 注：年假天数按照累计工龄计算，非本司工龄。

## 二、申请流程

### 步骤1：提前申请
- 至少提前 **15个工作日** 提交年假申请
- 登录OA系统 → 请假管理 → 新建年假申请

### 步骤2：填写信息
- 选择请假类型：年假
- 填写起止日期
- 填写请假事由
- 选择审批人

### 步骤3：审批流程
1. 直属领导审批（1-3个工作日）
2. 部门经理审批（1-2个工作日）
3. HR备案确认（1个工作日）

### 步骤4：结果通知
- 审批通过后，OA系统自动通知
- 请假期间系统自动设置外出状态

## 三、注意事项

1. 年假不可跨年累积，每年12月31日前需休完
2. 如遇法定假日，年假天数顺延
3. 年假可以分多次使用，每次最少半天
4. 紧急情况下可先口头申请，事后3日内补办手续

## 四、常见问题

**Q: 离职时未休完年假怎么办？**
A: 离职时未休年假按日工资的300%折算发放。

**Q: 年假可以和其他假期一起用吗？**
A: 可以。年假可以和周末、法定假日连续使用。',
 13, 2, 'PUBLISHED', 189, 12, TRUE, '2026-02-01 09:00:00');

-- 文档3: 系统架构设计 (技术文档 > 架构设计)
INSERT INTO documents (id, title, summary, content, category_id, author_id, status, view_count, like_count, is_featured, published_at) VALUES
(3, '企业知识库系统架构设计',
 '基于微服务架构的企业知识库系统整体技术架构',
 '# 企业知识库系统架构设计

## 1. 系统概述

企业知识库问答系统采用前后端分离 + AI微服务的混合架构，核心技术栈包括：

- **前端**: Vue 3 + TypeScript + Element Plus
- **后端**: Java 17 + Spring Boot 3.2
- **AI服务**: Python 3.11 + FastAPI + LangChain
- **数据库**: MySQL 8.0 + Redis 7 + Elasticsearch 8 + Qdrant

## 2. 架构图

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐
│  Nginx   │────▶│ Spring Boot  │────▶│   MySQL      │
│ 反向代理  │     │   后端服务    │────▶│   Redis      │
└──────────┘     └──────┬───────┘     └──────────────┘
                        │
                        ▼
                 ┌──────────────┐     ┌──────────────┐
                 │  FastAPI     │────▶│   Qdrant     │
                 │  AI服务      │     │   向量数据库   │
                 └──────────────┘     └──────────────┘
```

## 3. 核心模块

### 3.1 用户认证模块
- JWT Token认证机制
- RBAC角色权限控制（USER/EDITOR/ADMIN）
- Redis会话管理

### 3.2 文档管理模块
- 文档CRUD操作
- 文档版本控制
- 文件上传与解析
- 文档审核流程

### 3.3 搜索模块
- Elasticsearch全文检索
- 中文分词（IK Analyzer）
- 搜索建议和热词

### 3.4 AI问答模块（RAG）
- 向量化文档存储（Qdrant）
- 语义检索和召回
- 大语言模型生成答案
- 答案来源引用

## 4. RAG流程

```
用户提问 → 意图识别 → 向量检索 → 文档召回 → Prompt构建 → LLM生成 → 答案返回
```

## 5. 部署方案

使用 Docker Compose 一键部署所有服务，包含健康检查和自动重启。',
 21, 2, 'PUBLISHED', 145, 9, TRUE, '2026-03-01 14:00:00');

-- 文档4: API接口规范 (技术文档 > 接口文档)
INSERT INTO documents (id, title, summary, content, category_id, author_id, status, view_count, like_count, is_featured, published_at) VALUES
(4, 'RESTful API 接口规范',
 '系统所有REST API的设计规范和通用约定',
 '# RESTful API 接口规范

## 1. 通用约定

### 1.1 URL规范
- 基础路径: `/api/v1/`
- 使用名词复数: `/api/v1/documents`
- 嵌套资源: `/api/v1/documents/{id}/versions`

### 1.2 HTTP方法
| 方法     | 用途     | 示例                    |
|----------|----------|-------------------------|
| GET      | 查询     | GET /documents          |
| POST     | 创建     | POST /documents         |
| PUT      | 全量更新 | PUT /documents/{id}     |
| DELETE   | 删除     | DELETE /documents/{id}   |

### 1.3 统一响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

## 2. 认证接口

### POST /api/v1/auth/login
请求:
```json
{ "username": "admin", "password": "admin123" }
```
响应:
```json
{ "code": 200, "data": { "token": "jwt-token", "user": {...} } }
```

## 3. 文档接口

### GET /api/v1/documents
参数: `page`, `pageSize`, `categoryId`, `keyword`, `status`

### POST /api/v1/documents
请求: `{ "title", "content", "categoryId", "tags": [] }`

## 4. 搜索接口

### GET /api/v1/search
参数: `keyword`, `categoryId`, `page`, `pageSize`

## 5. AI问答接口

### POST /api/v1/chat/ask
请求: `{ "question": "...", "conversationId": "..." }`
响应:
```json
{
  "code": 200,
  "data": {
    "answer": "...",
    "sources": [{ "documentId": 1, "title": "...", "snippet": "...", "relevance": 0.95 }],
    "followUpQuestions": ["...", "..."]
  }
}
```

## 6. 错误码
| 错误码 | 说明         |
|--------|--------------|
| 400    | 请求参数错误  |
| 401    | 未认证       |
| 403    | 无权限       |
| 404    | 资源不存在    |
| 500    | 服务器错误    |',
 22, 2, 'PUBLISHED', 98, 7, FALSE, '2026-03-15 10:00:00');

-- 文档5: 销售话术指南 (销售支持)
INSERT INTO documents (id, title, summary, content, category_id, author_id, status, view_count, like_count, is_featured, published_at) VALUES
(5, '销售话术与客户沟通指南',
 '标准销售话术和客户常见问题应对',
 '# 销售话术与客户沟通指南

## 1. 开场白

### 1.1 电话开场
> 您好，我是XX公司的产品顾问张三，了解到贵公司正在使用XX系统，想了解下目前使用情况如何？

### 1.2 会面开场
> 感谢您抽出时间，今天主要想了解贵公司的业务需求，看看我们的方案能帮到什么。

## 2. 需求挖掘

### 关键提问
- 目前团队多少人使用知识库？
- 现有的文档管理方式有哪些痛点？
- 是否需要AI智能问答功能？
- 对数据安全有什么要求？

## 3. 产品演示要点

1. **搜索功能**: 展示快速精准搜索
2. **AI问答**: 演示自然语言提问
3. **文档管理**: 展示分类和版本管理
4. **权限控制**: 展示角色和权限管理

## 4. 常见异议处理

| 客户异议         | 应对话术                                    |
|------------------|---------------------------------------------|
| 价格太贵         | 对比节省的人力成本和效率提升                 |
| 已有类似系统     | 展示AI问答的差异化优势                      |
| 担心数据安全     | 说明私有化部署和数据加密方案                 |
| 团队学习成本高   | 强调简洁的操作界面和培训支持                 |

## 5. 成交话术

> 基于您的需求，我建议先做一个为期两周的POC测试，让团队实际体验后再决定。您看如何？',
 3, 2, 'PUBLISHED', 78, 5, FALSE, '2026-04-01 11:00:00');

-- 文档6: 合同管理办法 (合规法务，草稿状态)
INSERT INTO documents (id, title, summary, content, category_id, author_id, status, view_count, like_count, is_featured, published_at) VALUES
(6, '合同管理暂行办法',
 '公司合同签订、审批、归档的管理规定',
 '# 合同管理暂行办法

## 第一章 总则

第一条 为规范公司合同管理，防范法律风险，特制定本办法。

第二条 本办法适用于公司及各部门签订的各类合同。

## 第二章 合同审批

### 第三条 审批权限
- 10万元以下：部门经理审批
- 10-50万元：分管副总审批
- 50万元以上：总经理审批

### 第四条 审批流程
1. 业务部门起草合同
2. 法务部门审核
3. 按权限报领导审批
4. 用印并归档

## 第三章 合同归档

所有合同原件须在签订后5个工作日内交行政部归档，电子版上传至合同管理系统。

## 第四章 附则

本办法自发布之日起施行，由法务部负责解释。',
 4, 3, 'DRAFT', 0, 0, FALSE, NULL);

-- =====================================================
-- 5. 文档-标签关联
-- =====================================================
INSERT INTO document_tags (document_id, tag_id) VALUES
(1, 4), (1, 1), (1, 3),   -- 员工手册: 制度, 流程, 指南
(2, 4), (2, 1),             -- 年假申请: 制度, 流程
(3, 6), (3, 2),             -- 架构设计: 架构, 规范
(4, 5), (4, 2),             -- API规范: API, 规范
(5, 7), (5, 1),             -- 销售话术: 销售, 流程
(6, 8), (6, 2);             -- 合同管理: 法律, 规范

-- =====================================================
-- 6. 示例对话和消息
-- =====================================================

-- 对话1: 关于年假
INSERT INTO conversations (id, user_id, title, message_count) VALUES
('conv-001', 3, '年假相关咨询', 4);

INSERT INTO messages (id, conversation_id, role, content, sources) VALUES
(1, 'conv-001', 'USER', '如何申请年假？', NULL),
(2, 'conv-001', 'ASSISTANT',
 '根据《员工手册》和《年假申请流程》规定：\n\n1. **年假天数**：工作1-5年享有5天，5-10年7天，10-15年10天，15年以上15天\n2. **申请流程**：至少提前15个工作日通过OA系统提交申请\n3. **审批流程**：直属领导 → 部门经理 → HR备案\n\n需要我详细说明某个步骤吗？',
 '[{"documentId": 2, "title": "年假申请流程", "snippet": "至少提前15个工作日提交年假申请", "relevance": 0.95}]'),
(3, 'conv-001', 'USER', '年假可以跨年吗？', NULL),
(4, 'conv-001', 'ASSISTANT',
 '不可以。根据规定，年假不可跨年累积，每年12月31日前需要休完。\n\n如果离职时有未休年假，会按日工资的300%折算发放。',
 '[{"documentId": 2, "title": "年假申请流程", "snippet": "年假不可跨年累积，每年12月31日前需休完", "relevance": 0.92}]');

-- 对话2: 技术相关
INSERT INTO conversations (id, user_id, title, message_count) VALUES
('conv-002', 3, '系统架构咨询', 2);

INSERT INTO messages (id, conversation_id, role, content, sources) VALUES
(5, 'conv-002', 'USER', '知识库系统用了哪些技术栈？', NULL),
(6, 'conv-002', 'ASSISTANT',
 '企业知识库系统采用混合架构：\n\n- **前端**: Vue 3 + TypeScript + Element Plus\n- **后端**: Java 17 + Spring Boot 3.2\n- **AI服务**: Python 3.11 + FastAPI + LangChain\n- **数据存储**: MySQL + Redis + Elasticsearch + Qdrant\n\nAI问答基于RAG（检索增强生成）架构，先检索相关文档再生成答案。',
 '[{"documentId": 3, "title": "企业知识库系统架构设计", "snippet": "采用前后端分离 + AI微服务的混合架构", "relevance": 0.98}]');

-- 对话3: 销售相关
INSERT INTO conversations (id, user_id, title, message_count) VALUES
('conv-003', 3, '销售支持咨询', 2);

INSERT INTO messages (id, conversation_id, role, content, sources) VALUES
(7, 'conv-003', 'USER', '客户说价格太贵怎么应对？', NULL),
(8, 'conv-003', 'ASSISTANT',
 '关于价格异议的应对策略：\n\n1. **价值对比法**: 对比系统节省的人力成本和效率提升，通常3-6个月即可回本\n2. **ROI计算**: 帮客户算一笔账——每月节省多少工时、减少多少错误\n3. **分期方案**: 提供按年付费或分期付款选项\n4. **免费试用**: 建议先做POC测试，让团队体验后再决定\n\n关键是要把焦点从"价格"转移到"价值"。',
 '[{"documentId": 5, "title": "销售话术与客户沟通指南", "snippet": "对比节省的人力成本和效率提升", "relevance": 0.88}]');

-- =====================================================
-- 7. 操作日志（示例）
-- =====================================================
INSERT INTO operation_logs (user_id, module, action, method, url, ip, execution_time) VALUES
(1, 'auth',    'login',    'POST', '/api/v1/auth/login',          '127.0.0.1', 120),
(1, 'document','create',   'POST', '/api/v1/documents',            '127.0.0.1', 85),
(2, 'document','update',   'PUT',  '/api/v1/documents/1',          '127.0.0.1', 95),
(3, 'search',  'search',   'GET',  '/api/v1/search?keyword=年假',  '127.0.0.1', 45),
(3, 'chat',    'ask',      'POST', '/api/v1/chat/ask',             '127.0.0.1', 2300);

-- =====================================================
-- 8. 点赞数据
-- =====================================================
INSERT INTO likes (user_id, target_type, target_id) VALUES
(3, 'DOCUMENT', 1),
(3, 'DOCUMENT', 2),
(2, 'DOCUMENT', 3);

-- =====================================================
-- 完成
-- =====================================================
SELECT 'Seed data inserted successfully!' AS message;
SELECT CONCAT('Users: ', COUNT(*)) AS stats FROM users
UNION ALL SELECT CONCAT('Categories: ', COUNT(*)) FROM categories
UNION ALL SELECT CONCAT('Documents: ', COUNT(*)) FROM documents
UNION ALL SELECT CONCAT('Tags: ', COUNT(*)) FROM tags
UNION ALL SELECT CONCAT('Conversations: ', COUNT(*)) FROM conversations
UNION ALL SELECT CONCAT('Messages: ', COUNT(*)) FROM messages;
