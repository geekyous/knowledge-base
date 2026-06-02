/**
 * ============================================================================
 * TypeScript 类型定义 - types/index.ts
 * ============================================================================
 *
 * 【文件说明】
 * 这个文件定义了整个项目中使用的所有 TypeScript 接口（Interface）和类型。
 * 集中管理类型定义的好处：
 * 1. 保持前后端数据结构一致
 * 2. 在 IDE 中获得智能提示和自动补全
 * 3. 编译时发现类型错误，减少运行时 Bug
 * 4. 作为数据契约，方便团队协作
 *
 * 【TypeScript 概念】
 * - interface: 定义对象的形状（有哪些属性、什么类型）
 * - 泛型 <T>: 参数化类型，让一个接口适用于多种数据类型
 * - 联合类型 |: 允许一个值是多种类型之一（如 'USER' | 'ADMIN'）
 * - 可选属性 ?: 属性可以存在也可以不存在
 * - 类型别名 type: 给类型起一个名字
 *
 * 💡 学习要点:
 * 1. 接口命名约定：请求用 XxxRequest，响应用 XxxResponse，实体用名词
 * 2. 泛型 ApiResponse<T> 让同一个包装结构适用于不同数据类型
 * 3. 联合类型字面量（如 'USER' | 'EDITOR' | 'ADMIN'）限制取值范围
 * 4. 可选属性用 ? 标记，表示该字段可能为 undefined
 * 5. 嵌套接口（如 Document 包含 Category）描述复杂的数据关系
 * ============================================================================
 */

// ============================================================================
// 通用/基础类型
// ============================================================================

/**
 * API 统一响应格式
 *
 * 后端所有接口都返回这个统一格式，通过泛型 <T> 适配不同的数据类型。
 *
 * @template T - data 字段的具体类型，默认为 any
 *
 * 使用示例：
 * - ApiResponse<User> → { code: 200, message: 'ok', data: { id: 1, username: '...' }, timestamp: ... }
 * - ApiResponse<Document[]> → { code: 200, message: 'ok', data: [...], timestamp: ... }
 *
 * 泛型参数 T 让同一个接口定义适用于所有 API 响应，避免重复代码。
 */
export interface ApiResponse<T = any> {
  code: number       // 业务状态码（200 表示成功，其他表示各种错误）
  message: string    // 人类可读的消息文本
  data: T            // 实际的业务数据，类型由泛型参数 T 决定
  timestamp: number  // 服务器时间戳
}

/**
 * 分页响应格式
 *
 * 适用于列表查询接口，包含数据列表和分页信息。
 *
 * @template T - 列表中每项数据的类型
 *
 * 分页是后端列表接口的标准模式：
 * - items: 当前页的数据列表
 * - total: 满足条件的总记录数（用于计算总页数）
 * - page / pageSize: 当前页码和每页条数
 * - totalPages: 总页数（由 total / pageSize 计算得出）
 */
export interface PageResponse<T = any> {
  items: T[]       // 当前页的数据数组
  total: number    // 符合条件的总记录数
  page: number     // 当前页码（从 1 开始）
  pageSize: number // 每页记录数
  totalPages: number // 总页数
}

// ============================================================================
// 用户相关类型
// ============================================================================

/**
 * 用户实体
 *
 * 描述系统中用户对象的完整结构。
 * 这个接口与后端数据库的 User 表结构对应。
 *
 * TypeScript 的联合类型字面量限制取值范围：
 * - role: 只能是 'USER' | 'EDITOR' | 'ADMIN' 中的一个
 * - status: 只能是 'ACTIVE' | 'INACTIVE' | 'LOCKED' 中的一个
 * 这样可以在编译时捕获非法值（如 role: 'SUPERADMIN' 会报错）。
 */
export interface User {
  id: number                              // 用户唯一标识
  username: string                        // 用户名
  email: string                           // 邮箱地址
  role: 'USER' | 'EDITOR' | 'ADMIN'       // 角色：普通用户 | 编辑者 | 管理员
  avatar?: string                         // 头像 URL（可选属性，可能没有头像）
  status: 'ACTIVE' | 'INACTIVE' | 'LOCKED' // 账户状态
  createdAt: string                       // 创建时间（ISO 8601 字符串格式）
}

/**
 * 登录请求参数
 *
 * 登录表单提交时使用的数据结构。
 * 只包含必要的字段：用户名和密码。
 */
export interface LoginRequest {
  username: string  // 用户名
  password: string  // 密码
}

/**
 * 登录响应数据
 *
 * 登录成功后 API 返回的数据，包含 JWT Token 和用户基本信息。
 * Token 用于后续所有 API 请求的身份验证。
 */
export interface LoginResponse {
  token: string  // JWT Token 令牌
  user: User     // 用户信息对象
}

// ============================================================================
// 文档相关类型
// ============================================================================

/**
 * 文档实体
 *
 * 描述知识库中一篇文档的完整结构。
 * 包含标题、内容、分类、标签、统计数据等信息。
 *
 * 嵌套对象使用内联接口定义（如 author 字段），
 * 复用的对象使用独立接口（如 category 字段引用 Category 接口）。
 */
export interface Document {
  id: number                              // 文档唯一标识
  title: string                           // 文档标题
  summary?: string                        // 文档摘要（可选，列表页展示用）
  content?: string                        // 文档正文（可选，列表页通常不返回以节省带宽）
  categoryId?: number                     // 所属分类 ID（可选）
  category?: Category                     // 所属分类对象（关联查询时可能包含）
  author: {                               // 作者信息（内联接口定义）
    id: number                            // 作者 ID
    username: string                      // 作者用户名
  }
  tags: string[]                          // 标签数组（如 ['前端', '规范']）
  status: 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'ARCHIVED' // 文档状态
  viewCount: number                       // 浏览次数
  likeCount: number                       // 点赞次数
  commentCount: number                    // 评论数
  isFeatured: boolean                     // 是否为精选文档
  publishedAt?: string                    // 发布时间（可选，草稿没有发布时间）
  createdAt: string                       // 创建时间
  updatedAt: string                       // 最后更新时间
}

/**
 * 创建文档请求参数
 *
 * 创建新文档时需要提供的字段。
 * 与 Document 实体不同，不包含 id、viewCount 等服务端生成的字段。
 */
export interface DocumentCreateRequest {
  title: string                           // 文档标题（必填）
  content: string                         // 文档内容（必填）
  categoryId?: number                     // 分类 ID（可选）
  tags: string[]                          // 标签数组
  status: 'DRAFT' | 'PUBLISHED'          // 初始状态：草稿或直接发布
}

/**
 * 更新文档请求参数
 *
 * 更新文档时所有字段都是可选的（Partial 语义），
 * 只传需要修改的字段即可（PATCH 语义）。
 */
export interface DocumentUpdateRequest {
  title?: string                          // 新标题（可选）
  content?: string                        // 新内容（可选）
  categoryId?: number                     // 新分类（可选）
  tags?: string[]                         // 新标签（可选）
  status?: 'DRAFT' | 'PUBLISHED'         // 新状态（可选）
}

// ============================================================================
// 分类相关类型
// ============================================================================

/**
 * 分类实体
 *
 * 文档分类，支持树形结构（通过 parentId 和 children 实现父子关系）。
 * 例如："技术文档" → "前端" → "Vue"
 *
 * children?: Category[] 递归引用自身类型，形成树形结构。
 */
export interface Category {
  id: number                              // 分类唯一标识
  name: string                            // 分类名称
  slug: string                            // URL 友好的标识符（如 'tech-docs'）
  description?: string                    // 分类描述（可选）
  parentId?: number | null                // 父分类 ID（null 表示顶级分类）
  icon?: string                           // 分类图标名称（可选）
  sortOrder: number                       // 排序序号（数字越小越靠前）
  status: 'ACTIVE' | 'INACTIVE'           // 分类状态
  children?: Category[]                   // 子分类列表（递归结构）
  documentCount?: number                  // 该分类下的文档数量（可选）
  createdAt: string                       // 创建时间
}

// ============================================================================
// 搜索相关类型
// ============================================================================

/**
 * 搜索结果项
 *
 * 全文搜索引擎返回的单条匹配结果。
 * highlight 字段包含 HTML 高亮标签（如 <mark>），用于在页面中突出显示匹配关键词。
 */
export interface SearchResult {
  id: number                // 匹配的文档 ID
  title: string             // 文档标题（可能包含高亮 HTML）
  highlight: string         // 高亮摘要（包含匹配关键词的上下文片段）
  score: number             // 相关性得分（0-1，越接近 1 越相关）
  category?: string         // 所属分类名称（可选）
  author?: string           // 作者名称（可选）
  createdAt: string         // 创建时间
}

/**
 * 搜索请求参数
 *
 * 只有 q（查询关键词）是必填的，其他都是可选的筛选条件。
 */
export interface SearchRequest {
  q: string                 // 搜索关键词（必填）
  page?: number             // 页码（可选，默认 1）
  pageSize?: number         // 每页条数（可选，默认 10）
  categoryId?: number       // 限定分类 ID（可选）
}

// ============================================================================
// 聊天/问答相关类型
// ============================================================================

/**
 * 聊天消息
 *
 * 一次对话中的单条消息，可以是用户的问题或 AI 的回答。
 * role 字段区分消息发送者：'USER' 是用户，'ASSISTANT' 是 AI 助手。
 */
export interface ChatMessage {
  id: string                              // 消息唯一标识
  role: 'USER' | 'ASSISTANT'              // 消息角色：用户 | AI 助手
  content: string                         // 消息内容
  sources?: SourceDocument[]               // AI 回答引用的来源文档（仅 ASSISTANT 消息有）
  createdAt: string                       // 消息创建时间
}

/**
 * 来源文档
 *
 * AI 回答中引用的参考文档片段，用于提供可追溯的信息来源。
 * 这让用户可以验证 AI 回答的准确性，并深入了解相关内容。
 */
export interface SourceDocument {
  documentId: number       // 来源文档的 ID
  title: string            // 文档标题
  snippet: string          // 引用的文档片段（上下文）
  relevance: number        // 相关性得分（0-1）
}

/**
 * 聊天请求参数
 *
 * 用户向 AI 提问时发送的数据。
 * conversationId 是可选的：首次提问不带，后续对话带上以保持上下文。
 */
export interface ChatRequest {
  question: string                  // 用户的问题
  conversationId?: string           // 对话 ID（可选，首次提问不需要）
}

/**
 * 聊天响应数据
 *
 * AI 回答一个问题时返回的完整数据。
 */
export interface ChatResponse {
  conversationId: string             // 对话 ID（服务端生成或返回已有的）
  answer: string                     // AI 的回答内容
  sources: SourceDocument[]          // 引用的来源文档列表
  followUpQuestions: string[]        // 推荐的后续问题（帮助用户继续对话）
}
