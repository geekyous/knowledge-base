/**
 * ============================================================================
 * 聊天/问答 API 模块 - api/chat.ts
 * ============================================================================
 *
 * 【文件说明】
 * 这个文件封装了与 AI 智能问答相关的后端 API 调用。
 * 功能包括：发起提问、获取对话列表、查看对话详情、删除对话、清空历史等。
 *
 * 【AI 问答系统的特殊考虑】
 * - 对话是有状态的（通过 conversationId 关联同一对话的消息）
 * - AI 回答可能较慢，实际项目中可以考虑使用 SSE（Server-Sent Events）
 *   或 WebSocket 实现流式响应（逐字显示 AI 回答，提升用户体验）
 * - AI 回答通常附带来源文档（sources），用于提供可追溯性
 *
 * 💡 学习要点:
 * 1. conversationId 用于关联同一会话的多轮对话
 * 2. PageResponse<any> 使用 any 是因为对话列表的结构可能灵活变化
 * 3. 实际生产中的 AI 对话通常使用流式响应（SSE/WebSocket），而非一次性返回
 * 4. API 方法的命名应清晰表达操作意图（ask、getConversations、clearHistory）
 * ============================================================================
 */

// 导入封装好的 HTTP 请求工具
import { http } from '@/utils/request'

// 导入聊天相关的 TypeScript 类型定义
import type { ChatRequest, ChatResponse, PageResponse, Message, ApiResponse } from '@/types'

/**
 * chatApi 对象包含所有聊天/问答相关的 API 方法
 */
export const chatApi = {
  /**
   * 向 AI 提问
   *
   * @param data - 提问请求（包含问题和可选的对话 ID）
   * @returns ChatResponse（包含 AI 回答、来源文档、推荐后续问题）
   *
   * 这是聊天功能的核心 API：
   * - 首次提问不传 conversationId，服务端会创建新对话
   * - 后续提问传入 conversationId，消息会追加到同一对话中
   *
   * 注意：当前实现是一次性返回完整回答。
   * 在生产环境中，AI 回答通常使用 Server-Sent Events (SSE) 或
   * WebSocket 实现流式输出，让用户看到逐字生成的效果。
   */
  ask: (data: ChatRequest) => {
    return http.post<ChatResponse>('/v1/chat/ask', data)
  },

  /**
   * 获取对话列表
   *
   * @param params - 分页参数
   * @returns 分页的对话列表
   *
   * 返回用户的所有历史对话，按更新时间倒序排列
   */
  getConversations: (params: { page?: number; pageSize?: number }) => {
    // PageResponse<any> 中的 any 表示对话列表项的类型未严格定义
    // 实际项目中应该定义一个 Conversation 接口来替代 any
    return http.get<PageResponse<any>>('/v1/chat/conversations', { params })
  },

  /**
   * 获取单个对话的详情（包含所有消息）
   *
   * @param conversationId - 对话 ID
   * @returns 包含消息列表的对象
   *
   * 返回指定对话中的所有历史消息，用于恢复对话上下文
   */
  getConversation: (conversationId: string) => {
    return http.get<{ messages: Message[] }>(`/v1/chat/conversations/${conversationId}`)
  },

  /**
   * 删除整个对话
   *
   * @param conversationId - 要删除的对话 ID
   *
   * 使用 HTTP DELETE 方法，符合 RESTful API 设计
   */
  deleteConversation: (conversationId: string) => {
    return http.delete<void>(`/v1/chat/conversations/${conversationId}`)
  },

  /**
   * 清空对话中的消息（保留对话，删除所有消息）
   *
   * @param conversationId - 对话 ID
   *
   * 注意：这与删除对话不同。清空消息后对话仍然存在，但消息为空。
   * URL 路径 /messages 表示操作的目标是对话中的消息资源
   */
  clearHistory: (conversationId: string) => {
    return http.delete<void>(`/v1/chat/conversations/${conversationId}/messages`)
  }
}
