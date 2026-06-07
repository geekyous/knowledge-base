# F03 - 文档管理 - 功能设计文档

## 📋 功能概述

文档管理是企业知识库问答系统的核心功能，负责文档的创建、编辑、发布、分类、版本控制等全生命周期管理。

## 🎯 用户故事

### 1. 文档创建
**作为** 知识库贡献者  
**我想要** 创建新文档  
**以便** 分享知识和经验

**验收标准：**
- [ ] 支持富文本编辑
- [ ] 支持Markdown编辑
- [ ] 支持上传文件创建文档
- [ ] 支持设置文档分类和标签
- [ ] 支持保存草稿

### 2. 文档编辑
**作为** 知识库贡献者  
**我想要** 编辑现有文档  
**以便** 更新文档内容

**验收标准：**
- [ ] 支持在线编辑
- [ ] 支持格式化工具
- [ ] 支持插入图片和链接
- [ ] 支持AI辅助写作
- [ ] 支持版本对比

### 3. 文档审核
**作为** 内容审核员  
**我想要** 审核待发布文档  
**以便** 确保内容质量

**验收标准：**
- [ ] 查看待审核文档列表
- [ ] 查看文档详细内容
- [ ] 批准或拒绝发布
- [ ] 提供审核意见
- [ ] 支持批量审核

### 4. 文档分类管理
**作为** 管理员  
**我想要** 管理文档分类  
**以便** 组织文档结构

**验收标准：**
- [ ] 创建、编辑、删除分类
- [ ] 支持多级分类
- [ ] 分类排序
- [ ] 分类图标设置

## 🔐 功能需求

### 1. 文档列表

#### 功能描述
显示所有文档的列表，支持分页、排序、筛选和搜索。

#### 列表字段
| 字段 | 说明 |
|------|------|
| 文档标题 | 文档名称，可点击查看详情 |
| 分类 | 文档所属分类 |
| 作者 | 文档创建者 |
| 状态 | 草稿/待审核/已发布/已归档 |
| 更新时间 | 最后更新时间 |
| 操作 | 编辑、删除、发布等操作 |

#### 操作权限
- **查看权限**：所有用户
- **编辑权限**：作者、编辑、管理员
- **删除权限**：作者、管理员
- **发布权限**：编辑、管理员

### 2. 文档编辑器

#### 功能描述
提供强大的文档编辑器，支持富文本编辑、Markdown编辑、实时预览等功能。

#### 编辑器功能

##### 基础功能
- **文本格式**：粗体、斜体、下划线、删除线
- **标题**：H1-H6标题
- **列表**：有序列表、无序列表
- **链接**：插入超链接
- **图片**：上传或引用图片

##### 高级功能
- **代码块**：插入代码块，支持语法高亮
- **表格**：插入和编辑表格
- **引用**：引用块和引用行
- **分割线**：水平分割线
- **公式**：支持LaTeX数学公式

##### AI辅助功能
- **智能续写**：AI辅助内容续写
- **智能改写**：AI辅助优化表达
- **内容扩展**：AI辅助内容扩展
- **错误检查**：AI辅助检查错误

### 3. 文档上传

#### 功能描述
支持上传各种格式的文件，自动提取内容并创建文档。

#### 支持格式
- **文档类**：PDF、Word、PPT、Excel、TXT
- **图片类**：JPG、PNG、GIF
- **压缩包**：RAR、ZIP（提取内容）

#### 上传流程
```
选择文件 → 格式验证 → 文件上传 → 内容提取 → 自动分类 → 保存为草稿 → 编辑完善 → 发布
```

#### 批量上传
- 支持选择多个文件
- 显示上传进度
- 支持拖拽上传
- 错误处理和重试

### 4. 文档版本控制

#### 功能描述
支持文档版本管理，可以查看历史版本和版本对比。

#### 版本信息
- **版本号**：自动递增
- **修改说明**：版本变更说明
- **修改人**：修改者信息
- **修改时间**：修改时间戳

#### 版本操作
- **查看历史版本**：查看所有历史版本
- **版本对比**：对比两个版本的差异
- **版本恢复**：恢复到历史版本

### 5. 文档分类

#### 分类结构
```
一级分类
├── 人事制度
│   ├── 招聘流程
│   ├── 薪酬福利
│   └── 考勤制度
├── 技术文档
│   ├── 架构设计
│   ├── 接口文档
│   └── 部署指南
└── 销售支持
    ├── 销售话术
    └── 客户案例
```

#### 分类功能
- **创建分类**：创建新的分类
- **编辑分类**：修改分类名称和描述
- **删除分类**：删除空分类
- **排序**：设置分类显示顺序

### 6. 文档标签

#### 标签管理
- **创建标签**：创建新标签
- **标签颜色**：设置标签显示颜色
- **标签统计**：显示标签使用次数

#### 标签使用
- **添加标签**：为文档添加多个标签
- **删除标签**：移除文档标签
- **标签筛选**：按标签筛选文档
- **热门标签**：显示使用最多的标签

## 📊 数据模型

### 文档表设计

```sql
CREATE TABLE documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(500),
    content LONGTEXT,
    category_id INT,
    author_id BIGINT NOT NULL,
    status ENUM('DRAFT', 'PENDING', 'PUBLISHED', 'ARCHIVED') DEFAULT 'DRAFT',
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    is_featured BOOLEAN DEFAULT FALSE,
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,

    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_category_id (category_id),
    INDEX idx_author_id (author_id),
    INDEX idx_status (status),
    INDEX idx_published_at (published_at)
);
```

### 文档版本表

```sql
CREATE TABLE document_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    title VARCHAR(255),
    content LONGTEXT,
    change_log TEXT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_document_id (document_id),
    UNIQUE KEY uk_document_version (document_id, version_number)
);
```

## 🔗 接口定义

### 1. 文档列表

**接口地址：** `GET /api/v1/documents`

**请求参数：**
```json
{
  "page": 1,
  "pageSize": 20,
  "categoryId": 1,
  "status": "PUBLISHED",
  "keyword": "搜索关键词",
  "sortBy": "updatedAt",
  "sortOrder": "desc"
}
```

### 2. 创建文档

**接口地址：** `POST /api/v1/documents`

**请求参数：**
```json
{
  "title": "产品发布流程",
  "content": "# 产品发布流程\n\n## 1. 准备阶段",
  "categoryId": 1,
  "tags": ["流程", "发布"],
  "status": "DRAFT"
}
```

### 3. 更新文档

**接口地址：** `PUT /api/v1/documents/{id}`

**请求参数：**
```json
{
  "title": "更新后的标题",
  "content": "更新后的内容",
  "categoryId": 2,
  "tags": ["流程", "管理"]
}
```

### 4. 文档上传

**接口地址：** `POST /api/v1/documents/upload`

**请求参数：**
```json
{
  "file": [binary],
  "categoryId": 1,
  "autoExtract": true
}
```

### 5. 提交审核

**接口地址：** `POST /api/v1/documents/{id}/submit`

**响应示例：**
```json
{
  "code": 200,
  "message": "文档已提交审核"
}
```

## 🧪 测试用例

### 1. 文档CRUD测试
- **TC-DOC-001**: 创建新文档成功
- **TC-DOC-002**: 编辑文档内容成功
- **TC-DOC-003**: 删除文档成功
- **TC-DOC-004**: 文档版本对比正确

### 2. 文档上传测试
- **TC-UP-001**: 上传PDF文件成功
- **TC-UP-002**: 批量上传文件成功
- **TC-UP-003**: 上传超限文件提示错误
- **TC-UP-004**: 不支持格式提示错误

### 3. 权限测试
- **TC-AUTH-001**: 普通用户无法编辑他人文档
- **TC-AUTH-002**: 普通用户无法删除文档
- **TC-AUTH-003**: 编辑人员可以编辑文档

## 📝 验收标准总结

### 功能完整性
- [ ] 支持文档CRUD操作
- [ ] 支持富文本和Markdown编辑
- [ ] 支持文件上传创建文档
- [ ] 支持文档版本控制
- [ ] 支持文档分类和标签

### 用户体验
- [ ] 编辑器功能强大易用
- [ ] 文档上传流程顺畅
- [ ] 版本管理清晰明了
- [ ] 分类管理方便实用

### 性能要求
- [ ] 文档加载速度 < 500ms
- [ ] 编辑器响应流畅
- [ ] 文件上传稳定可靠

---

**文档版本：** v1.0  
**创建日期：** 2026-05-31  
**最后更新：** 2026-05-31  
**状态：** 待审核
