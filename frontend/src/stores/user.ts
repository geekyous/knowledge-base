/**
 * ============================================================================
 * 用户状态管理 - stores/user.ts
 * ============================================================================
 *
 * 【文件说明】
 * 这个文件定义了用户相关的全局状态管理 store，使用 Pinia 库实现。
 * 负责管理用户的登录状态、Token 令牌、用户信息等数据，并提供
 * 登录、登出、获取用户信息等操作方法。
 *
 * 【Vue 概念】
 * - Pinia Store: Vue 3 推荐的状态管理方案，比 Vuex 更简洁、类型推导更好
 * - Setup Store 语法: 使用 setup() 风格定义 store（对比 Option Store 风格）
 * - ref(): 定义响应式状态（类似 Vue 组件中的 data）
 * - computed(): 定义计算属性（类似 Vue 组件中的 computed）
 * - localStorage: 浏览器本地存储，用于持久化 Token
 *
 * 💡 学习要点:
 * 1. Pinia 的 Setup Store 风格与 Vue 组件的 setup() 写法一致，学习成本低
 * 2. defineStore 的第一个参数 'user' 是 store 的唯一 ID
 * 3. ref() 定义的状态对外暴露时自动解包（不需要 .value）
 * 4. computed() 创建缓存计算属性，依赖不变时不会重新计算
 * 5. Token 存储在 localStorage 中实现页面刷新后保持登录
 * 6. initialize() 方法在应用启动时调用，从 Token 恢复用户信息
 * ============================================================================
 */

// 从 Pinia 导入 defineStore 工厂函数，用于定义一个 store
import { defineStore } from 'pinia'

// 导入 Vue 的响应式 API
// ref: 创建响应式引用（用于基本类型或对象）
// computed: 创建计算属性（根据依赖自动缓存和更新）
import { ref, computed } from 'vue'

// 导入用户相关的 TypeScript 类型定义
// 这些类型来自 @/types 模块，确保数据的类型安全
import type { User, LoginRequest, LoginResponse } from '@/types'

// 导入封装好的 HTTP 请求工具，用于调用后端 API
import { http } from '@/utils/request'

// 导入 JSEncrypt — 前端 RSA 加密库，用于密码加密传输
import JSEncrypt from 'jsencrypt'

/**
 * defineStore() 定义一个 Pinia Store
 *
 * 参数一 'user': Store 的唯一 ID，用于 DevTools 调试和 SSR
 * 参数二: Setup 函数，返回一个包含 state/getters/actions 的对象
 *
 * Setup Store 风格（使用箭头函数）：
 * - ref() = state（状态）
 * - computed() = getters（计算属性）
 * - function() = actions（方法）
 */
export const useUserStore = defineStore('user', () => {
  // ========================================================================
  // State（响应式状态）
  // ========================================================================

  /**
   * Token 令牌
   *
   * 从 localStorage 初始化，实现页面刷新后保持登录状态。
   * ref<string | null> 表示值可以是字符串或 null。
   * localStorage.getItem('token') 在页面加载时读取之前保存的 Token。
   */
  const token = ref<string | null>(localStorage.getItem('token'))

  /**
   * 当前登录用户信息
   *
   * ref<User | null> 表示初始值为 null，登录成功后设置为 User 对象。
   * 页面刷新后此值会丢失（内存中的状态），需要通过 initialize() 重新获取。
   */
  const currentUser = ref<User | null>(null)

  // ========================================================================
  // Getters（计算属性）
  // ========================================================================

  /**
   * 是否已登录
   *
   * computed() 创建只读的计算属性。
   * !!token.value 将 token 转为布尔值（null → false, 'xxx' → true）。
   * 这个 getter 会自动缓存，只有 token.value 变化时才重新计算。
   */
  const isLoggedIn = computed(() => !!token.value)

  /**
   * 用户角色
   *
   * 可选链操作符 ?. 安全地访问嵌套属性，避免 null 引用错误。
   * 如果 currentUser.value 为 null，则返回 undefined。
   */
  const userRole = computed(() => currentUser.value?.role)

  /**
   * 用户名
   *
   * 如果 currentUser 为 null，返回空字符串作为默认值。
   * 使用 || 运算符提供回退值，确保模板中不会显示 undefined。
   */
  const userName = computed(() => currentUser.value?.username || '')

  // ========================================================================
  // Actions（方法/操作）
  // ========================================================================

  /**
   * 设置 Token 并持久化到 localStorage
   *
   * @param newToken - 从登录 API 返回的 JWT Token 字符串
   *
   * 同时更新内存中的状态（token.value）和浏览器本地存储（localStorage），
   * 确保页面刷新后仍能从 localStorage 恢复登录状态。
   */
  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  /**
   * 设置当前用户信息
   *
   * @param user - 用户对象，包含 id、username、email、role 等字段
   */
  const setUser = (user: User) => {
    currentUser.value = user
  }

  /**
   * 登录操作
   *
   * @param credentials - 包含 username 和 password 的登录凭证
   * @returns Promise - 登录 API 的响应
   *
   * 登录流程：
   * 1. 调用后端 /v1/auth/public-key 获取 RSA 公钥
   * 2. 使用公钥加密密码（防止明文传输）
   * 3. 调用后端 /v1/auth/login 接口，发送加密后的密码
   * 4. 从响应中提取 Token 和用户信息
   * 5. 分别存储 Token 和用户信息
   *
   * TypeScript 泛型 http.post<LoginResponse> 确保响应数据的类型安全。
   */
  const login = async (credentials: LoginRequest) => {
    // 步骤 1: 获取 RSA 公钥
    const keyRes = await http.get<{ publicKey: string; algorithm: string }>('/v1/auth/public-key')
    const { publicKey } = keyRes.data

    // 步骤 2: 使用公钥加密密码
    const encryptor = new JSEncrypt()
    encryptor.setPublicKey(publicKey)
    const encryptedPassword = encryptor.encrypt(credentials.password)
    if (!encryptedPassword) {
      throw new Error('密码加密失败，请重试')
    }

    // 步骤 3: 发送加密后的登录请求
    const res = await http.post<LoginResponse>('/v1/auth/login', {
      username: credentials.username,
      password: encryptedPassword
    })
    const { token: newToken, user } = res.data

    // 保存 Token 和用户信息
    setToken(newToken)
    setUser(user)

    return res
  }

  /**
   * 登出操作
   *
   * 清除所有用户相关数据：
   * 1. 清空内存中的 Token 和用户信息
   * 2. 从 localStorage 删除 Token（确保刷新后也不会自动登录）
   */
  const logout = () => {
    token.value = null
    currentUser.value = null
    localStorage.removeItem('token')
  }

  /**
   * 获取当前用户信息
   *
   * 调用 /v1/users/me 接口获取最新的用户信息。
   * 通常在页面刷新后调用，从 Token 恢复用户信息。
   *
   * @returns Promise - 用户信息 API 的响应
   */
  const getUserInfo = async () => {
    const res = await http.get<User>('/v1/users/me')
    setUser(res.data)
    return res
  }

  /**
   * 更新用户信息
   *
   * @param data - 部分用户数据（Partial<User> 表示 User 的所有字段都是可选的）
   * @returns Promise - 更新后的用户信息
   *
   * 使用 Partial<User> 类型，调用者可以只传入需要修改的字段。
   */
  const updateUserInfo = async (data: Partial<User>) => {
    const res = await http.put<User>('/v1/users/me', data)
    setUser(res.data)
    return res
  }

  /**
   * 初始化用户状态
   *
   * 在应用启动时（MainLayout 的 onMounted 中）调用。
   * 如果 localStorage 中有 Token，则用 Token 获取最新用户信息。
   * 如果 Token 已过期（API 返回 401），则清除登录状态。
   *
   * 这个方法解决了"页面刷新后用户信息丢失"的问题。
   */
  const initialize = async () => {
    if (token.value) {
      try {
        // 尝试用 Token 获取用户信息
        await getUserInfo()
      } catch (error) {
        // Token 无效或已过期，执行登出操作清除无效 Token
        logout()
      }
    }
  }

  // ========================================================================
  // 导出 Store 的公共接口
  // ========================================================================
  // Setup Store 必须显式 return 所有需要对外暴露的状态、计算属性和方法
  // 没有被 return 的变量就是"私有的"，外部无法访问（类似类的 private）
  return {
    // State（状态）
    token,
    currentUser,
    // Getters（计算属性）
    isLoggedIn,
    userRole,
    userName,
    // Actions（方法）
    setToken,
    setUser,
    login,
    logout,
    getUserInfo,
    updateUserInfo,
    initialize
  }
})
