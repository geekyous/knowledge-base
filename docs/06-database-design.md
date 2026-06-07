# 企业知识库问答系统 - 数据库设计

## 📊 数据库概览

### 数据库选型
- **主数据库**: MySQL 8.0+
- **缓存数据库**: Redis 7.0+
- **搜索引擎**: Elasticsearch 8.0+
- **向量数据库**: Qdrant

### 数据分层
```
┌─────────────────────────────────────────────────┐
│              应用层 (Spring Boot)                  │
└─────────────────────────────────────────────────┘
                    ↓
┌──────────────┬──────────────┬──────────────┬─────┐
│   MySQL      │    Redis     │ Elasticsearch│  AI │
│ (业务数据)    │  (缓存数据)   │  (搜索数据)    │ 服务│
└──────────────┴──────────────┴──────────────┴─────┘
```

## 🗄️ MySQL 数据库设计

### 数据库基本信息
```sql
-- 数据库名称
knowledge_base

-- 字符集
CHARACTER SET utf8mb4

-- 排序规则
COLLATE utf8mb4_unicode_ci
```

### 表结构设计

#### 1. 用户表 (users)

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    avatar VARCHAR(255) COMMENT '头像URL',
    role ENUM('USER', 'EDITOR', 'ADMIN') DEFAULT 'USER' COMMENT '角色',
    status ENUM('ACTIVE', 'INACTIVE', 'LOCKED') DEFAULT 'ACTIVE' COMMENT '状态',
    last_login_at TIMESTAMP NULL COMMENT '最后登录时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at TIMESTAMP NULL COMMENT '删除时间（软删除）',

    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

#### 2. 角色表 (roles)

```sql
CREATE TABLE roles (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
    description VARCHAR(255) COMMENT '角色描述',
    permissions JSON COMMENT '权限列表',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';
```

#### 3. 分类表 (categories)

```sql
CREATE TABLE categories (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    slug VARCHAR(50) NOT NULL UNIQUE COMMENT '分类标识',
    description VARCHAR(255) COMMENT '分类描述',
    parent_id INT NULL COMMENT '父分类ID',
    icon VARCHAR(50) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL,
    INDEX idx_parent_id (parent_id),
    INDEX idx_slug (slug),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';
```

#### 4. 文档表 (documents)

```sql
CREATE TABLE documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文档ID',
    title VARCHAR(255) NOT NULL COMMENT '文档标题',
    summary VARCHAR(500) COMMENT '文档摘要',
    content LONGTEXT COMMENT '文档内容',
    category_id INT COMMENT '分类ID',
    author_id BIGINT NOT NULL COMMENT '作者ID',
    status ENUM('DRAFT', 'PENDING', 'PUBLISHED', 'ARCHIVED') DEFAULT 'DRAFT' COMMENT '状态',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    like_count INT DEFAULT 0 COMMENT '点赞次数',
    comment_count INT DEFAULT 0 COMMENT '评论次数',
    is_featured BOOLEAN DEFAULT FALSE COMMENT '是否推荐',
    published_at TIMESTAMP NULL COMMENT '发布时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at TIMESTAMP NULL COMMENT '删除时间（软删除）',

    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_category_id (category_id),
    INDEX idx_author_id (author_id),
    INDEX idx_status (status),
    INDEX idx_published_at (published_at),
    INDEX idx_view_count (view_count),
    FULLTEXT INDEX ft_title_content (title, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';
```

#### 5. 标签表 (tags)

```sql
CREATE TABLE tags (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
    color VARCHAR(7) COMMENT '标签颜色',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_name (name),
    INDEX idx_usage_count (usage_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';
```

#### 6. 文档标签关联表 (document_tags)

```sql
CREATE TABLE document_tags (
    document_id BIGINT NOT NULL COMMENT '文档ID',
    tag_id INT NOT NULL COMMENT '标签ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (document_id, tag_id),
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE,
    INDEX idx_document_id (document_id),
    INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档标签关联表';
```

#### 7. 文档版本表 (document_versions)

```sql
CREATE TABLE document_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '版本ID',
    document_id BIGINT NOT NULL COMMENT '文档ID',
    version_number INT NOT NULL COMMENT '版本号',
    title VARCHAR(255) COMMENT '文档标题',
    content LONGTEXT COMMENT '文档内容',
    change_log TEXT COMMENT '变更日志',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_document_id (document_id),
    INDEX idx_version_number (version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档版本表';
```

#### 8. 对话表 (conversations)

```sql
CREATE TABLE conversations (
    id VARCHAR(50) PRIMARY KEY COMMENT '对话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(255) COMMENT '对话标题',
    message_count INT DEFAULT 0 COMMENT '消息数量',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话表';
```

#### 9. 消息表 (messages)

```sql
CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id VARCHAR(50) NOT NULL COMMENT '对话ID',
    role ENUM('USER', 'ASSISTANT') NOT NULL COMMENT '角色',
    content TEXT NOT NULL COMMENT '消息内容',
    sources JSON COMMENT '来源文档',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';
```

#### 10. 附件表 (attachments)

```sql
CREATE TABLE attachments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '附件ID',
    document_id BIGINT COMMENT '文档ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_size BIGINT NOT NULL COMMENT '文件大小',
    file_type VARCHAR(50) NOT NULL COMMENT '文件类型',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    upload_user_id BIGINT NOT NULL COMMENT '上传人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE SET NULL,
    FOREIGN KEY (upload_user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_document_id (document_id),
    INDEX idx_upload_user_id (upload_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件表';
```

#### 11. 操作日志表 (operation_logs)

```sql
CREATE TABLE operation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT COMMENT '用户ID',
    module VARCHAR(50) NOT NULL COMMENT '模块',
    action VARCHAR(50) NOT NULL COMMENT '操作',
    method VARCHAR(10) COMMENT '请求方法',
    url VARCHAR(500) COMMENT '请求URL',
    ip VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    request_params TEXT COMMENT '请求参数',
    response_status INT COMMENT '响应状态',
    execution_time INT COMMENT '执行时间(ms)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_module (module),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
```

#### 12. 点赞表 (likes)

```sql
CREATE TABLE likes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    target_type ENUM('DOCUMENT', 'COMMENT', 'ANSWER') NOT NULL COMMENT '目标类型',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_target (user_id, target_type, target_id),
    INDEX idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞表';
```

### ER 图

```
┌─────────────┐         ┌─────────────┐
│   users     │────────│  documents  │
│             │ 1    N │             │
└─────────────┘         └─────────────┘
       │                      │
       │                      │
       │ N                 1  │
       │                      │
┌─────────────┐         ┌─────────────┐
│ conversations│        │  categories │
│             │         └─────────────┘
└─────────────┘                 │
       │                     N │
       │ 1                       │
       │                         │
┌─────────────┐         ┌─────────────┐
│  messages   │         │ document_   │
│             │         │   tags      │
└─────────────┘         └─────────────┘
                              │
                              │ N
                              │
                         ┌─────────────┐
                         │    tags     │
                         │             │
                         └─────────────┘
```

## 💾 Redis 数据结构设计

### 键命名规范
```
{module}:{type}:{id}:{field}
```

### 主要数据结构

#### 1. 用户会话
```
Key: user:session:{userId}
Type: String (Hash)
Value: {
  token: "jwt_token",
  loginTime: timestamp,
  expireTime: timestamp,
  userInfo: {...}
}
TTL: 86400 (24小时)
```

#### 2. API 缓存
```
Key: api:cache:{endpoint}:{params_hash}
Type: String
Value: JSON Response
TTL: 300 (5分钟)
```

#### 3. 搜索缓存
```
Key: search:cache:{query_hash}
Type: String
Value: JSON Response
TTL: 600 (10分钟)
```

#### 4. 热门搜索
```
Key: search:hot
Type: Sorted Set
Value: {
  "搜索词": score
}
TTL: 3600 (1小时)
```

#### 5. 在线用户
```
Key: online:users
Type: Set
Value: [userId1, userId2, ...]
TTL: 300 (5分钟)
```

#### 6. 接口限流
```
Key: rate:limit:{userId}:{endpoint}
Type: String
Value: request_count
TTL: 60 (1分钟)
```

#### 7. 分布式锁
```
Key: locks:{resource}:{id}
Type: String
Value: lock_token
TTL: 30 (30秒)
```

## 🔍 Elasticsearch 索引设计

### 文档索引 (documents)

```json
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "analysis": {
      "analyzer": {
        "ik_max_word": {
          "type": "custom",
          "tokenizer": "ik_max_word"
        },
        "ik_smart": {
          "type": "custom",
          "tokenizer": "ik_smart"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "id": {"type": "long"},
      "title": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "content": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "summary": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "category": {"type": "keyword"},
      "author": {"type": "keyword"},
      "tags": {"type": "keyword"},
      "status": {"type": "keyword"},
      "viewCount": {"type": "integer"},
      "createdAt": {"type": "date"},
      "updatedAt": {"type": "date"}
    }
  }
}
```

### 用户索引 (users)

```json
{
  "mappings": {
    "properties": {
      "id": {"type": "long"},
      "username": {"type": "keyword"},
      "email": {"type": "keyword"},
      "role": {"type": "keyword"},
      "status": {"type": "keyword"},
      "createdAt": {"type": "date"}
    }
  }
}
```

## 🤖 向量数据库设计 (Qdrant)

### 文档集合 (documents_collection)

```python
{
  "collection_name": "documents_collection",
  "vectors": {
    "size": 1536,  # OpenAI embedding size
    "distance": "Cosine"
  },
  "payload": {
    "document_id": "integer",
    "title": "text",
    "content": "text",
    "category": "keyword",
    "author": "keyword",
    "created_at": "date"
  }
}
```

## 📊 数据同步策略

### MySQL → Elasticsearch
- **同步方式**: 异步队列
- **触发时机**: 文档增删改
- **同步延迟**: < 1秒

### MySQL → Qdrant
- **同步方式**: 批量处理
- **触发时机**: 文档发布
- **同步延迟**: < 5秒

### Redis → MySQL
- **同步方式**: 定时持久化
- **触发时机**: 热点数据
- **同步频率**: 每分钟

## 🛡️ 数据安全

### 备份策略
- **全量备份**: 每天凌晨
- **增量备份**: 每小时
- **日志备份**: 实时

### 恢复策略
- **RTO**: 1小时
- **RPO**: 5分钟

## 🔄 数据库迁移管理（Flyway）

本项目使用 [Flyway](https://flywaydb.org/) 管理数据库版本迁移，替代之前的 Docker initdb 方案。

### 迁移脚本目录

```
backend/src/main/resources/db/migration/
├── V1__init_schema.sql      ← 建表（12 张表 + 初始角色）
└── V2__seed_data.sql        ← 种子数据（分类、标签、文档等）
```

### 工作原理

1. 应用启动时，Flyway 扫描 `db/migration/` 目录
2. 对比 `flyway_schema_history` 表，找出未执行的迁移脚本
3. 按版本号顺序执行新脚本，并记录到历史表

### 新增迁移脚本

```bash
# 创建新迁移脚本
touch backend/src/main/resources/db/migration/V3__add_user_nickname.sql
```

> 详细使用方法见 [11-flyway-guide.md](11-flyway-guide.md)

---

**文档版本：** v1.1
**最后更新：** 2026-06-08
