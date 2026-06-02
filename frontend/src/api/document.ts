/**
 * ============================================================================
 * 文档 API 模块 - api/document.ts
 * ============================================================================
 *
 * 【文件说明】
 * 这个文件封装了所有与文档（Document）相关的后端 API 调用。
 * 采用"API 模块化"的组织模式：
 * - 每个 API 模块对应后端的一个资源（如 /v1/documents）
 * - 所有方法都使用 TypeScript 泛型确保类型安全
 * - 统一使用 http 工具对象发请求（自动携带 Token、统一错误处理）
 *
 * 【设计模式】
 * - 对象字面量模式：将相关 API 方法组织在一个对象中
 * - 优点：调用时语义清晰（documentApi.getList()、documentApi.create()）
 * - RESTful API 风格：GET 查询、POST 创建、PUT 更新、DELETE 删除
 *
 * 💡 学习要点:
 * 1. API 模块化让代码职责清晰，方便维护和测试
 * 2. 泛型 http.get<PageResponse<Document>>() 让返回数据的类型完全确定
 * 3. FormData 用于文件上传，需要设置 Content-Type: multipart/form-data
 * 4. API 方法的参数类型与 types/index.ts 中的接口对应
 * 5. { params } 配置将参数序列化为 URL 查询字符串（?page=1&pageSize=10）
 * ============================================================================
 */

// 导入封装好的 HTTP 请求工具
import { http } from '@/utils/request'

// 导入文档相关的 TypeScript 类型定义
import type {
  Document,
  DocumentCreateRequest,
  DocumentUpdateRequest,
  PageResponse,
  ApiResponse
} from '@/types'

/**
 * documentApi 对象包含所有文档相关的 API 方法
 *
 * 每个方法都：
 * 1. 有清晰的 JSDoc 注释
 * 2. 参数有明确的 TypeScript 类型
 * 3. 使用泛型指定响应数据类型
 * 4. 返回 Promise，支持 async/await 调用
 */
export const documentApi = {
  /**
   * 获取文档列表（分页查询）
   *
   * @param params - 查询参数（页码、每页条数、分类ID、状态、关键词）
   * @returns Promise<ApiResponse<PageResponse<Document>>> 分页文档列表
   *
   * { params } 配置会将参数对象序列化为 URL 查询字符串：
   * /v1/documents?page=1&pageSize=10&status=PUBLISHED
   */
  getList: (params: {
    page?: number
    pageSize?: number
    categoryId?: number
    status?: string
    keyword?: string
  }) => {
    return http.get<PageResponse<Document>>('/v1/documents', { params })
  },

  /**
   * 获取文档详情
   *
   * @param id - 文档 ID
   * @returns Promise<ApiResponse<Document>> 文档详情（包含正文内容）
   *
   * 使用模板字符串动态拼接 URL：`/v1/documents/${id}`
   */
  getDetail: (id: number) => {
    return http.get<Document>(`/v1/documents/${id}`)
  },

  /**
   * 创建新文档
   *
   * @param data - 创建文档的请求体（标题、内容、分类、标签、状态）
   * @returns Promise<ApiResponse<Document>> 创建成功的文档对象
   *
   * POST 请求的第二个参数会作为请求体（request body）发送
   */
  create: (data: DocumentCreateRequest) => {
    return http.post<Document>('/v1/documents', data)
  },

  /**
   * 更新文档
   *
   * @param id - 要更新的文档 ID
   * @param data - 更新的字段（Partial，只需传需要修改的字段）
   * @returns Promise<ApiResponse<Document>> 更新后的文档对象
   */
  update: (id: number, data: DocumentUpdateRequest) => {
    return http.put<Document>(`/v1/documents/${id}`, data)
  },

  /**
   * 删除文档
   *
   * @param id - 要删除的文档 ID
   * @returns Promise<ApiResponse<void>> 无返回数据
   *
   * void 表示这个 API 不返回有意义的业务数据
   */
  delete: (id: number) => {
    return http.delete<void>(`/v1/documents/${id}`)
  },

  /**
   * 文档点赞
   *
   * @param id - 文档 ID
   *
   * 点赞和取消点赞分别使用 POST 和 DELETE 方法（RESTful 设计）
   * 表示"创建一个点赞"和"删除一个点赞"
   */
  like: (id: number) => {
    return http.post<void>(`/v1/documents/${id}/like`)
  },

  /**
   * 取消点赞
   *
   * @param id - 文档 ID
   */
  unlike: (id: number) => {
    return http.delete<void>(`/v1/documents/${id}/like`)
  },

  /**
   * 上传文档（文件上传）
   *
   * @param file - 要上传的文件对象（来自 <input type="file">）
   * @param categoryId - 可选的分类 ID
   *
   * 文件上传需要使用 FormData 对象：
   * 1. FormData 是浏览器原生 API，用于构建 multipart/form-data 格式的请求体
   * 2. 必须设置 Content-Type: multipart/form-data（不能是 application/json）
   * 3. 浏览器会自动添加 boundary 分隔符，所以不要手动设置 Content-Type
   *
   * 注意：headers 中的 'Content-Type': 'multipart/form-data' 会让 Axios
   * 自动设置正确的 boundary
   */
  upload: (file: File, categoryId?: number) => {
    // 创建 FormData 对象
    const formData = new FormData()
    // 将文件添加到表单数据中，字段名为 'file'
    formData.append('file', file)
    // 如果有分类 ID，也添加到表单中（FormData 的值必须是字符串）
    if (categoryId) {
      formData.append('categoryId', categoryId.toString())
    }
    // 发送 POST 请求，第三个参数是额外配置
    return http.post<Document>('/v1/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}
