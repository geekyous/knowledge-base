/**
 * ============================================================================
 * HTTP 请求工具 - utils/request.ts
 * ============================================================================
 *
 * 【文件说明】
 * 这个文件封装了 Axios HTTP 客户端，提供统一的请求/响应处理。
 * 核心功能包括：
 * 1. 创建 Axios 实例并配置基础参数（baseURL、超时时间）
 * 2. 请求拦截器：自动注入 JWT Token 到请求头
 * 3. 响应拦截器：统一处理错误码（401 跳转登录、500 错误提示等）
 * 4. 导出类型安全的 http 工具对象（get/post/put/delete/patch）
 *
 * 【核心概念】
 * - Axios 实例: 通过 axios.create() 创建独立配置的 HTTP 客户端
 * - 请求拦截器: 在请求发出前修改配置（如添加 Token）
 * - 响应拦截器: 在收到响应后统一处理（如错误提示、Token 过期处理）
 * - JWT (JSON Web Token): 无状态的身份认证方案，放在 Authorization 请求头中
 *
 * 💡 学习要点:
 * 1. axios.create() 创建独立实例，不会影响全局的 axios 默认配置
 * 2. 请求拦截器的 config 参数包含完整的请求配置（URL、headers、params 等）
 * 3. 响应拦截器可以区分"业务错误"（code !== 200）和"网络/HTTP 错误"
 * 4. 401 状态码通常表示 Token 过期，需要清除登录状态并跳转到登录页
 * 5. 泛型 http.get<T>() 让 TypeScript 知道返回数据的类型
 * 6. Vite 的 import.meta.env.VITE_API_BASE_URL 读取环境变量
 * ============================================================================
 */

// 导入 Axios 库及其类型定义
// axios: HTTP 客户端库
// AxiosError: Axios 错误类型
// AxiosRequestConfig: 请求配置类型
// AxiosResponse: 响应类型
import axios, { type AxiosError, type AxiosRequestConfig, type AxiosResponse } from 'axios'

// 导入项目的通用 API 响应类型，用于拦截器中的类型标注
import type { ApiResponse } from '@/types'

// 导入 Element Plus 的消息提示组件，用于显示错误通知
import { ElMessage } from 'element-plus'

// 导入用户状态 store，用于获取 Token 和执行登出操作
import { useUserStore } from '@/stores/user'

// ============================================================================
// 创建 Axios 实例
// ============================================================================
// axios.create() 创建一个独立的 Axios 实例，可以自定义默认配置
// 这不会影响全局的 axios 对象，多个实例可以有不同的 baseURL 等配置
const request = axios.create({
  // baseURL: 所有请求的 URL 前缀
  // import.meta.env.VITE_API_BASE_URL 从 .env 文件读取（Vite 特有语法）
  // 如果环境变量未设置，则使用 '/api' 作为默认值（会被 Vite proxy 代理到后端）
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',

  // timeout: 请求超时时间（毫秒），超过这个时间请求会被中断
  // 15000ms = 15秒，适用于大多数 API 请求
  timeout: 15000,

  // headers: 默认请求头
  // Content-Type: application/json 表示请求体是 JSON 格式
  headers: {
    'Content-Type': 'application/json'
  }
})

// ============================================================================
// 请求拦截器（Request Interceptor）
// ============================================================================
// 在每个请求发出之前执行，用于修改请求配置
// 典型用途：添加认证 Token、设置请求 ID、显示加载状态等
request.interceptors.request.use(
  // 成功回调：在请求发出前修改 config
  (config) => {
    // 获取用户状态 store 中的 Token
    // 注意：在拦截器中使用 Pinia store 是安全的，因为此时 store 已经初始化
    const userStore = useUserStore()

    // 如果用户已登录（Token 存在），将 Token 添加到请求头
    // JWT 的标准格式是：Authorization: Bearer <token>
    // Bearer 是认证类型，表示"持有者"，即持有这个 Token 的人就是合法用户
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }

    // 必须返回 config，否则请求不会发出
    return config
  },
  // 错误回调：处理请求配置阶段的错误（很少触发）
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// ============================================================================
// 响应拦截器（Response Interceptor）
// ============================================================================
// 在收到后端响应后执行，用于统一处理响应数据
request.interceptors.response.use(
  // --------------------------------------------------------------------------
  // 成功回调（HTTP 状态码 2xx）
  // --------------------------------------------------------------------------
  (response: AxiosResponse<ApiResponse>) => {
    // 获取响应体数据（Axios 自动将 JSON 解析为对象）
    const res = response.data

    // 检查业务状态码（与 HTTP 状态码不同，这是后端自定义的业务编码）
    // code === 200 表示业务逻辑处理成功
    if (res.code === 200) {
      // 直接返回 res（而不是 response），这样 API 调用处可以直接拿到 data
      // 这是一个常见的设计决策：让调用者不需要 .data.data 就能拿到业务数据
      return res
    }

    // 业务错误（HTTP 成功但业务逻辑失败，如参数校验不通过）
    // 使用 Element Plus 的消息组件显示错误提示
    ElMessage.error(res.message || '请求失败')

    // 将业务错误转为 Promise 拒绝，让调用者的 catch 可以捕获
    return Promise.reject(new Error(res.message || '请求失败'))
  },

  // --------------------------------------------------------------------------
  // 错误回调（HTTP 状态码非 2xx，如 401、404、500 等）
  // --------------------------------------------------------------------------
  (error: AxiosError<ApiResponse>) => {
    // 从错误对象中解构出 response（HTTP 响应对象）
    const { response } = error

    if (response) {
      // 服务器返回了响应，但状态码表示错误
      const { status, data } = response

      // 根据不同的 HTTP 状态码进行不同的处理
      switch (status) {
        case 401:
          // 401 Unauthorized: Token 无效或已过期
          // 需要清除本地登录状态并跳转到登录页
          ElMessage.error('登录已过期，请重新登录')
          const userStore = useUserStore()
          userStore.logout()
          // 使用 window.location.href 而非 router.push 是因为
          // 此处可能不在 Vue 组件的上下文中，直接修改 URL 更可靠
          window.location.href = '/login'
          break
        case 403:
          // 403 Forbidden: 没有权限访问该资源
          ElMessage.error('没有权限访问')
          break
        case 404:
          // 404 Not Found: 请求的资源不存在
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          // 500 Internal Server Error: 服务器内部错误
          ElMessage.error('服务器错误')
          break
        default:
          // 其他未专门处理的错误码
          ElMessage.error(data?.message || '请求失败')
      }
    } else {
      // 没有 response 说明是网络层面的错误（如断网、DNS 解析失败、CORS 错误等）
      ElMessage.error('网络连接失败，请检查网络')
    }

    // 将错误继续抛出，让调用者可以进一步处理
    return Promise.reject(error)
  }
)

// ============================================================================
// 导出封装的 HTTP 工具对象
// ============================================================================
// 将常用的 HTTP 方法封装为类型安全的函数
// 每个方法都使用泛型 <T> 指定响应数据的具体类型
// 这样在调用 API 时可以获得完整的类型提示

export const http = {
  /**
   * GET 请求
   *
   * @template T - 响应数据中 data 字段的类型
   * @param url - 请求路径
   * @param config - 可选的 Axios 配置（如 params、headers）
   *
   * 使用示例：
   * const res = await http.get<User>('/users/me')
   * // res.data 的类型自动推导为 User
   */
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return request.get(url, config)
  },

  /**
   * POST 请求
   *
   * @template T - 响应数据中 data 字段的类型
   * @param url - 请求路径
   * @param data - 请求体数据（会自动序列化为 JSON）
   * @param config - 可选的额外 Axios 配置
   *
   * 使用示例：
   * const res = await http.post<LoginResponse>('/auth/login', { username, password })
   */
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return request.post(url, data, config)
  },

  /**
   * PUT 请求（全量更新）
   *
   * RESTful API 中 PUT 用于全量替换资源（与 PATCH 的部分更新不同）
   */
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return request.put(url, data, config)
  },

  /**
   * DELETE 请求
   */
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return request.delete(url, config)
  },

  /**
   * PATCH 请求（部分更新）
   *
   * RESTful API 中 PATCH 用于修改资源的部分字段（与 PUT 的全量替换不同）
   */
  patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return request.patch(url, data, config)
  }
}

// 默认导出原始 Axios 实例，供需要直接使用 Axios API 的场景
export default request
