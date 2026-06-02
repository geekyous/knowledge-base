# 企业知识库问答系统 - API 设计规范

## 📋 API 设计原则

### RESTful 风格
- 使用 HTTP 动词表示操作类型
- 使用名词表示资源
- 统一的响应格式
- 合理的状态码使用

### 版本控制
```
https://api.example.com/v1/users
```

### 安全性
- JWT Token 认证
- HTTPS 传输
- 请求频率限制
- 参数验证

## 🔐 认证授权

### JWT Token 认证流程

```
1. 用户登录 → 获取 Token
2. 请求携带 Token → Header: Authorization: Bearer {token}
3. 服务端验证 Token → 返回数据
```

### Token 格式

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## 📊 统一响应格式

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1717182345678
}
```

### 错误响应

```json
{
  "code": 400,
  "message": "参数验证失败",
  "errors": [
    {
      "field": "username",
      "message": "用户名不能为空"
    }
  ],
  "timestamp": 1717182345678
}
```

### 分页响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [],
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "totalPages": 5
  },
  "timestamp": 1717182345678
}
```

## 📚 API 接口列表

### 1. 认证接口

#### 1.1 用户登录

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com",
      "role": "ADMIN"
    }
  }
}
```

#### 1.2 用户注册

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "newuser",
  "password": "password123",
  "email": "user@example.com"
}
```

#### 1.3 刷新 Token

```http
POST /api/v1/auth/refresh
Authorization: Bearer {token}
```

#### 1.4 用户登出

```http
POST /api/v1/auth/logout
Authorization: Bearer {token}
```

### 2. 用户管理接口

#### 2.1 获取当前用户信息

```http
GET /api/v1/users/me
Authorization: Bearer {token}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "role": "ADMIN",
    "avatar": "https://example.com/avatar.jpg",
    "createdAt": "2024-01-01T00:00:00Z"
  }
}
```

#### 2.2 获取用户列表

```http
GET /api/v1/users?page=1&pageSize=20&role=USER
Authorization: Bearer {token}
```

#### 2.3 更新用户信息

```http
PUT /api/v1/users/{userId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "email": "newemail@example.com",
  "avatar": "https://example.com/new-avatar.jpg"
}
```

### 3. 文档管理接口

#### 3.1 获取文档列表

```http
GET /api/v1/documents?page=1&pageSize=20&categoryId=1&keyword=搜索词
Authorization: Bearer {token}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "items": [
      {
        "id": 1,
        "title": "产品发布流程",
        "summary": "产品从开发到发布的完整流程...",
        "categoryId": 1,
        "author": {
          "id": 1,
          "username": "admin"
        },
        "tags": ["流程", "发布"],
        "status": "PUBLISHED",
        "viewCount": 1234,
        "createdAt": "2024-01-01T00:00:00Z"
      }
    ],
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "totalPages": 5
  }
}
```

#### 3.2 获取文档详情

```http
GET /api/v1/documents/{documentId}
Authorization: Bearer {token}
```

#### 3.3 创建文档

```http
POST /api/v1/documents
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "新产品发布流程",
  "content": "# 产品发布流程\n\n## 1. 准备阶段...",
  "categoryId": 1,
  "tags": ["流程", "发布"],
  "status": "DRAFT"
}
```

#### 3.4 更新文档

```http
PUT /api/v1/documents/{documentId}
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "更新后的标题",
  "content": "更新后的内容..."
}
```

#### 3.5 删除文档

```http
DELETE /api/v1/documents/{documentId}
Authorization: Bearer {token}
```

#### 3.6 文档上传

```http
POST /api/v1/documents/upload
Authorization: Bearer {token}
Content-Type: multipart/form-data

file: [binary]
categoryId: 1
```

### 4. 搜索接口

#### 4.1 全文搜索

```http
GET /api/v1/search?q=搜索词&page=1&pageSize=20
Authorization: Bearer {token}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "items": [
      {
        "id": 1,
        "title": "产品发布流程",
        "highlight": "产品<b>发布</b>流程...",
        "score": 0.95,
        "category": "产品管理"
      }
    ],
    "total": 50,
    "took": 23
  }
}
```

#### 4.2 热门搜索

```http
GET /api/v1/search/hot
```

#### 4.3 搜索建议

```http
GET /api/v1/search/suggest?q=产品
```

### 5. 分类接口

#### 5.1 获取分类列表

```http
GET /api/v1/categories
Authorization: Bearer {token}
```

**响应：**
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "人事制度",
      "slug": "hr-policy",
      "parentId": null,
      "children": [
        {
          "id": 11,
          "name": "招聘流程",
          "slug": "recruitment"
        }
      ],
      "documentCount": 128
    }
  ]
}
```

#### 5.2 创建分类

```http
POST /api/v1/categories
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "新分类",
  "parentId": 1
}
```

### 6. 智能问答接口

#### 6.1 提问

```http
POST /api/v1/chat/ask
Authorization: Bearer {token}
Content-Type: application/json

{
  "question": "如何申请年假？",
  "conversationId": "optional-conversation-id"
}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "conversationId": "conv-123",
    "answer": "根据《员工手册》规定，年假申请流程如下...",
    "sources": [
      {
        "documentId": 1,
        "title": "员工请假管理办法",
        "snippet": "员工申请年假需要提前15天...",
        "relevance": 0.92
      }
    ],
    "followUpQuestions": [
      "年假天数如何计算？",
      "年假可以跨年使用吗？"
    ]
  }
}
```

#### 6.2 获取对话历史

```http
GET /api/v1/chat/conversations?page=1&pageSize=20
Authorization: Bearer {token}
```

#### 6.3 获取对话详情

```http
GET /api/v1/chat/conversations/{conversationId}
Authorization: Bearer {token}
```

### 7. 统计分析接口

#### 7.1 使用统计

```http
GET /api/v1/statistics/overview
Authorization: Bearer {token}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "totalDocuments": 1234,
    "totalUsers": 5678,
    "totalQuestions": 8901,
    "accuracyRate": 0.89,
    "avgResponseTime": 2.3
  }
}
```

#### 7.2 文档统计

```http
GET /api/v1/statistics/documents?startDate=2024-01-01&endDate=2024-12-31
Authorization: Bearer {token}
```

#### 7.3 用户行为统计

```http
GET /api/v1/statistics/user-behavior
Authorization: Bearer {token}
```

### 8. 管理后台接口

#### 8.1 系统配置

```http
GET /api/v1/admin/settings
Authorization: Bearer {token}
```

```http
PUT /api/v1/admin/settings
Authorization: Bearer {token}
Content-Type: application/json

{
  "enableAI": true,
  "maxFileSize": 50,
  "allowedFileTypes": ["pdf", "doc", "docx"]
}
```

#### 8.2 用户管理

```http
GET /api/v1/admin/users?page=1&pageSize=20&role=USER
Authorization: Bearer {token}
```

```http
PUT /api/v1/admin/users/{userId}/role
Authorization: Bearer {token}
Content-Type: application/json

{
  "role": "ADMIN"
}
```

#### 8.3 审核文档

```http
PUT /api/v1/admin/documents/{documentId}/review
Authorization: Bearer {token}
Content-Type: application/json

{
  "status": "APPROVED",
  "comment": "内容符合规范"
}
```

## 🎨 状态码说明

| 状态码 | 说明 | 使用场景 |
|--------|------|----------|
| 200 | 成功 | 请求成功 |
| 201 | 创建成功 | 资源创建成功 |
| 204 | 无内容 | 删除成功 |
| 400 | 请求错误 | 参数验证失败 |
| 401 | 未认证 | Token 无效或过期 |
| 403 | 禁止访问 | 权限不足 |
| 404 | 资源不存在 | 资源未找到 |
| 409 | 资源冲突 | 资源已存在 |
| 429 | 请求过多 | 频率限制 |
| 500 | 服务器错误 | 系统内部错误 |

## 📝 参数验证规则

### 用户名
- 长度：3-20 字符
- 格式：字母、数字、下划线
- 示例：`admin_123`

### 密码
- 长度：8-32 字符
- 必须包含：字母、数字
- 示例：`Password123`

### 邮箱
- 格式：标准邮箱格式
- 示例：`user@example.com`

### 文档标题
- 长度：1-200 字符
- 必填：是

### 搜索关键词
- 长度：1-100 字符
- 可空：是

## 🔒 权限控制

### 角色定义
| 角色 | 权限 |
|------|------|
| ADMIN | 所有权限 |
| EDITOR | 文档编辑、删除 |
| USER | 文档查看、提问 |

### 权限验证
```java
@RequireRole("ADMIN")
@PreAuthorize("hasRole('ADMIN')")
```

## 📊 频率限制

| 接口类型 | 限制 |
|----------|------|
| 登录接口 | 10次/分钟 |
| 搜索接口 | 60次/分钟 |
| 问答接口 | 30次/分钟 |
| 文档上传 | 5次/分钟 |
| 其他接口 | 120次/分钟 |

---

**文档版本：** v1.0
**最后更新：** 2026-05-31
