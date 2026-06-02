/**
 * ============================================================================
 * 搜索 API 模块 - api/search.ts
 * ============================================================================
 *
 * 【文件说明】
 * 这个文件封装了全文搜索相关的后端 API 调用。
 * 功能包括：全文搜索、热门搜索词、搜索建议（自动补全）、搜索历史管理。
 *
 * 【搜索功能设计】
 * - 全文搜索: 根据关键词搜索所有文档，返回带高亮摘要的结果
 * - 热门搜索: 展示全站热门搜索词，引导用户发现热门内容
 * - 搜索建议: 用户输入时实时提供建议词（类似百度的搜索下拉）
 * - 搜索历史: 记录用户的搜索历史，方便重复搜索
 *
 * 💡 学习要点:
 * 1. GET 请求的 { params } 配置会自动将参数转为 URL 查询字符串
 * 2. 搜索接口通常需要处理高亮文本（HTML 标签），在模板中使用 v-html 渲染
 * 3. 搜索建议 API 需要配合前端 debounce（防抖）使用，避免频繁请求
 * 4. 搜索历史可以存在服务端（跨设备同步）或 localStorage（仅本地）
 * 5. string[] 表示返回字符串数组类型（如 ['年假', '报销']）
 * ============================================================================
 */

// 导入封装好的 HTTP 请求工具
import { http } from '@/utils/request'

// 导入搜索相关的 TypeScript 类型定义
import type { SearchRequest, PageResponse, SearchResult, ApiResponse } from '@/types'

/**
 * searchApi 对象包含所有搜索相关的 API 方法
 */
export const searchApi = {
  /**
   * 全文搜索
   *
   * @param params - 搜索参数（关键词、分页、分类筛选）
   * @returns 分页的搜索结果列表
   *
   * 搜索结果中的 highlight 字段包含 HTML 高亮标签（如 <mark>关键词</mark>），
   * 在 Vue 模板中需要使用 v-html 指令才能正确渲染。
   */
  search: (params: SearchRequest) => {
    // GET 请求将 params 序列化为：/v1/search?q=年假&page=1&pageSize=10
    return http.get<PageResponse<SearchResult>>('/v1/search', { params })
  },

  /**
   * 获取热门搜索词
   *
   * @returns 热门搜索关键词数组（如 ['年假制度', '报销流程', '技术规范']）
   *
   * 用于首页或搜索页展示热门标签，引导用户搜索
   */
  getHotSearch: () => {
    return http.get<string[]>('/v1/search/hot')
  },

  /**
   * 获取搜索建议（自动补全）
   *
   * @param q - 用户当前输入的部分关键词
   * @returns 建议的完整搜索词数组
   *
   * 实际使用时应该配合防抖（debounce）：
   * - 用户每输入一个字符就调用 API 会造成大量无用请求
   * - 通常设置 300ms 的防抖延迟，用户停止输入后才发请求
   *
   * 例如：用户输入 "年" → 返回 ["年假制度", "年假申请流程"]
   */
  getSuggest: (q: string) => {
    return http.get<string[]>('/v1/search/suggest', { params: { q } })
  },

  /**
   * 保存搜索关键词到历史记录
   *
   * @param keyword - 用户搜索的关键词
   *
   * 在用户执行搜索时调用，将关键词保存到服务端
   * 服务端可以统计搜索频次，用于生成热门搜索
   */
  saveHistory: (keyword: string) => {
    return http.post<void>('/v1/search/history', { keyword })
  },

  /**
   * 获取当前用户的搜索历史
   *
   * @returns 用户最近搜索的关键词数组
   *
   * 搜索历史按时间倒序排列（最新的在前）
   */
  getHistory: () => {
    return http.get<string[]>('/v1/search/history')
  },

  /**
   * 清空搜索历史
   *
   * 删除当前用户的所有搜索历史记录
   * 使用 HTTP DELETE 方法表示"删除资源"
   */
  clearHistory: () => {
    return http.delete<void>('/v1/search/history')
  }
}
