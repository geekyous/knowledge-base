<!--
================================================================================
智能问答页面 - views/chat/Chat.vue
================================================================================

【文件说明】
这是 AI 智能问答的聊天界面，用户可以与 AI 助手进行多轮对话。
功能包括：
1. 左侧对话列表（新建、选择、删除对话）
2. 右侧聊天主区域（消息展示、输入发送）
3. AI 消息附带来源文档和推荐后续问题
4. 打字动画效果（三点跳动的加载指示器）
5. 自动滚动到最新消息
6. 快速提问建议

【Vue 概念】
- nextTick(): 等待 DOM 更新后执行操作（用于自动滚动）
- el-scrollbar: Element Plus 的自定义滚动条组件
- 复杂的组件交互：对话管理 + 消息发送 + 状态管理
- @click.stop: 阻止事件冒泡（删除按钮点击不触发对话选择）
- @keydown.enter.exact: 精确匹配 Enter 键（排除 Ctrl+Enter 等）

💡 学习要点:
1. nextTick() 用于在 DOM 更新后执行操作（如滚动到底部）
2. el-scrollbar 的 setScrollTop() 方法实现程序化滚动
3. @click.stop 阻止事件冒泡，避免删除按钮的点击触发父元素的点击事件
4. @keydown.enter.exact.prevent 只匹配单独的 Enter 键，并阻止默认行为
5. CSS 动画 @keyframes 实现打字指示器的跳动效果
6. flex-direction: row-reverse 实现用户消息靠右对齐
================================================================================
-->

<template>
  <div class="chat-page">
    <!-- ================================================================== -->
    <!-- 左侧对话列表 -->
    <!-- ================================================================== -->
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <el-button type="primary" class="new-chat-btn" @click="createConversation">
          <el-icon><Plus /></el-icon>
          新建对话
        </el-button>
      </div>

      <!--
        el-scrollbar: 自定义滚动条组件
        当对话列表超出容器高度时显示滚动条
      -->
      <el-scrollbar class="conversation-list">
        <!--
          对话项列表
          :class="{ active: currentConversationId === conv.id }" 动态类名
          当前选中的对话添加 active 样式（高亮）
        -->
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conversation-item"
          :class="{ active: currentConversationId === conv.id }"
          @click="selectConversation(conv.id)"
        >
          <el-icon class="conv-icon"><ChatDotRound /></el-icon>
          <div class="conv-info">
            <div class="conv-title">{{ conv.title }}</div>
            <div class="conv-time">{{ formatDate(conv.updatedAt) }}</div>
          </div>
          <!--
            @click.stop 阻止事件冒泡
            点击删除按钮时，不会同时触发父元素 @click="selectConversation"
          -->
          <el-button
            class="conv-delete"
            link
            size="small"
            @click.stop="deleteConversation(conv.id)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </el-scrollbar>
    </div>

    <!-- ================================================================== -->
    <!-- 主聊天区域 -->
    <!-- ================================================================== -->
    <div class="chat-main">
      <!-- ---------------------------------------------------------------- -->
      <!-- 欢迎页（未选择对话时显示） -->
      <!-- ---------------------------------------------------------------- -->
      <div v-if="!currentConversationId" class="chat-welcome">
        <el-icon :size="64" color="#409eff"><ChatDotRound /></el-icon>
        <h2 class="welcome-title">智能问答助手</h2>
        <p class="welcome-desc">请选择一个对话或新建对话开始提问</p>
        <!-- 快速提问建议卡片 -->
        <div class="welcome-suggestions">
          <div
            v-for="(suggestion, index) in welcomeSuggestions"
            :key="index"
            class="suggestion-card"
            @click="quickAsk(suggestion)"
          >
            <el-icon><QuestionFilled /></el-icon>
            <span>{{ suggestion }}</span>
          </div>
        </div>
      </div>

      <!-- ---------------------------------------------------------------- -->
      <!-- 对话内容（已选择对话时显示） -->
      <!-- ---------------------------------------------------------------- -->
      <template v-else>
        <!-- 消息列表（可滚动） -->
        <!--
          ref="scrollbarRef" 获取滚动条组件实例
          用于在发送消息后调用 scrollToBottom()
        -->
        <el-scrollbar ref="scrollbarRef" class="message-list">
          <div class="message-list-inner">
            <!--
              消息项
              :class 根据消息角色动态添加样式类
              USER → 靠右显示（蓝色气泡）
              ASSISTANT → 靠左显示（灰色气泡）
            -->
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-wrapper"
              :class="msg.role === 'USER' ? 'user' : 'assistant'"
            >
              <!-- 头像 -->
              <div class="message-avatar">
                <!--
                  根据角色显示不同头像和颜色
                  :icon 属性指定头像的默认图标
                -->
                <el-avatar
                  v-if="msg.role === 'USER'"
                  :size="36"
                  :icon="User"
                  style="background-color: #409eff"
                />
                <el-avatar
                  v-else
                  :size="36"
                  :icon="Monitor"
                  style="background-color: #67c23a"
                />
              </div>
              <!-- 消息体 -->
              <div class="message-body">
                <!-- 消息内容文本 -->
                <div class="message-content">{{ msg.content }}</div>

                <!-- AI 消息：来源文档（可折叠） -->
                <!--
                  条件：消息角色是 ASSISTANT 且有来源文档
                  el-collapse 折叠面板：默认折叠，点击展开查看来源
                -->
                <div v-if="msg.role === 'ASSISTANT' && msg.sources && msg.sources.length > 0" class="message-sources">
                  <el-collapse>
                    <el-collapse-item>
                      <template #title>
                        <span class="sources-title">
                          <el-icon><Document /></el-icon>
                          参考文档 ({{ msg.sources.length }})
                        </span>
                      </template>
                      <div
                        v-for="source in msg.sources"
                        :key="source.documentId"
                        class="source-item"
                        @click="goToDocument(source.documentId)"
                      >
                        <div class="source-title">{{ source.title }}</div>
                        <div class="source-snippet">{{ source.snippet }}</div>
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>

                <!-- AI 消息：后续推荐问题 -->
                <!--
                  点击推荐问题可以直接提问（类似搜索引擎的"相关搜索"）
                -->
                <div v-if="msg.role === 'ASSISTANT' && msg.followUpQuestions && msg.followUpQuestions.length > 0" class="follow-ups">
                  <div class="follow-up-label">相关问题：</div>
                  <div
                    v-for="(question, qi) in msg.followUpQuestions"
                    :key="qi"
                    class="follow-up-item"
                    @click="askFollowUp(question)"
                  >
                    {{ question }}
                  </div>
                </div>
              </div>
            </div>

            <!-- ============================================================ -->
            <!-- 加载中指示器（三点跳动动画） -->
            <!-- ============================================================ -->
            <!--
              当 isAsking 为 true 时显示打字动画
              三个 .dot 元素配合 CSS @keyframes animation 实现跳动效果
            -->
            <div v-if="isAsking" class="message-wrapper assistant">
              <div class="message-avatar">
                <el-avatar :size="36" :icon="Monitor" style="background-color: #67c23a" />
              </div>
              <div class="message-body">
                <div class="message-content typing">
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="dot"></span>
                </div>
              </div>
            </div>
          </div>
        </el-scrollbar>

        <!-- ================================================================ -->
        <!-- 输入区域 -->
        <!-- ================================================================ -->
        <div class="chat-input-area">
          <!--
            el-input type="textarea" 多行文本输入框
            :rows="2" 默认显示 2 行
            resize="none" 禁止用户拖拽调整大小
            @keydown.enter.exact.prevent="sendMessage"
              - .exact 修饰符：只匹配单独按 Enter（不含 Shift/Ctrl/Alt）
              - .prevent 修饰符：阻止默认行为（换行）
              - 这样按 Enter 发送消息，Shift+Enter 换行
          -->
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            placeholder="输入您的问题..."
            resize="none"
            maxlength="2000"
            show-word-limit
            @keydown.enter.exact.prevent="sendMessage"
          />
          <!-- 发送按钮 -->
          <!--
            :disabled 条件：输入为空或正在等待 AI 回复时禁用
          -->
          <el-button
            type="primary"
            :icon="Promotion"
            :disabled="!inputText.trim() || isAsking"
            @click="sendMessage"
          >
            发送
          </el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
// 导入说明：
// - ref: 响应式引用
// - nextTick: DOM 更新后的回调（等待 Vue 完成 DOM 渲染）
// - onMounted: 生命周期钩子
import { ref, nextTick, onMounted } from 'vue'

// 导入路由
import { useRouter } from 'vue-router'

// 导入 Element Plus 的消息提示和确认弹窗
import { ElMessage, ElMessageBox } from 'element-plus'

// 导入图标
import {
  Plus,
  Delete,
  ChatDotRound,
  User,
  Monitor,
  Document,
  Promotion,
  QuestionFilled
} from '@element-plus/icons-vue'

// 导入聊天 API
import { chatApi } from '@/api/chat'

// 导入来源文档类型
import type { SourceDocument } from '@/types'

// ==================== 局部类型定义 ====================

/**
 * 对话摘要类型
 *
 * 在组件内部定义的接口，只在这个文件中使用。
 * 不需要放在 types/index.ts 中（局部类型，不跨组件共享）。
 */
interface ChatConversation {
  id: string
  title: string
  updatedAt: string
}

/**
 * 展示用消息类型
 *
 * 与后端的 ChatMessage 类似，但增加了 followUpQuestions 字段。
 * 这是因为 AI 回复可能包含推荐的后续问题。
 */
interface DisplayMessage {
  id: string
  role: 'USER' | 'ASSISTANT'
  content: string
  sources?: SourceDocument[]
  followUpQuestions?: string[]
  createdAt: string
}

const router = useRouter()

// ==================== 响应式状态 ====================

/** 对话列表 */
const conversations = ref<ChatConversation[]>([])

/** 当前选中的对话 ID（空字符串表示未选中） */
const currentConversationId = ref<string>('')

/** 当前对话的消息列表 */
const messages = ref<DisplayMessage[]>([])

/** 输入框文本 */
const inputText = ref('')

/** 是否正在等待 AI 回复（控制加载动画） */
const isAsking = ref(false)

/** 滚动条组件的引用（用于调用 scrollToBottom） */
const scrollbarRef = ref()

/** 快速提问建议列表 */
const welcomeSuggestions = [
  '公司的年假制度是怎样的？',
  '如何申请差旅报销？',
  '前端开发规范有哪些要求？',
  '新员工入职流程是什么？'
]

/**
 * 智能日期格式化
 *
 * 24 小时内显示时间（如 "14:30"）
 * 超过 24 小时显示日期（如 "5月28日" */
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  if (diff < 86400000) {  // 86400000ms = 24小时
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

/**
 * 滚动到消息列表底部
 *
 * 使用 nextTick() 确保 Vue 已完成 DOM 更新（新消息已渲染）后再滚动。
 * setScrollTop(99999) 设置一个很大的值，滚动到底部。
 *
 * nextTick 的必要性：
 * - Vue 的 DOM 更新是异步的
 * - 如果在 messages.value.push() 之后立即滚动，新消息可能还没渲染
 * - nextTick 确保在 DOM 更新完成后再执行滚动
 */
const scrollToBottom = async () => {
  await nextTick()
  if (scrollbarRef.value) {
    scrollbarRef.value.setScrollTop(99999)
  }
}

/** 获取对话列表 */
const fetchConversations = async () => {
  try {
    const res = await chatApi.getConversations({ page: 1, pageSize: 50 })
    conversations.value = res.data.items
  } catch {
    conversations.value = [
      { id: 'conv-1', title: '年假制度咨询', updatedAt: new Date().toISOString() },
      { id: 'conv-2', title: '报销流程提问', updatedAt: new Date(Date.now() - 86400000).toISOString() }
    ]
  }
}

/**
 * 选择对话
 *
 * 加载选中对话的消息历史，并滚动到底部
 */
const selectConversation = async (id: string) => {
  currentConversationId.value = id
  messages.value = []

  try {
    const res = await chatApi.getConversation(id)
    messages.value = res.data.messages.map((m: any) => ({
      id: m.id,
      role: m.role,
      content: m.content,
      sources: m.sources,
      followUpQuestions: m.followUpQuestions,
      createdAt: m.createdAt
    }))
  } catch {
    // Fallback mock messages
    if (id === 'conv-1') {
      messages.value = [
        {
          id: '1',
          role: 'USER',
          content: '公司的年假制度是怎样的？',
          createdAt: new Date().toISOString()
        },
        {
          id: '2',
          role: 'ASSISTANT',
          content: '根据公司规定，员工入职满一年后可享受带薪年假：\n\n- 工龄 1-5 年：每年 5 天\n- 工龄 5-10 年：每年 10 天\n- 工龄 10 年以上：每年 15 天\n\n年假需提前3个工作日通过 OA 系统申请。',
          sources: [
            { documentId: 1, title: '员工手册 - 年假制度', snippet: '根据公司规定，员工入职满一年后可享受带薪年假...', relevance: 0.95 }
          ],
          followUpQuestions: [
            '如何申请年假？',
            '年假可以累积到下一年吗？',
            '试用期有年假吗？'
          ],
          createdAt: new Date().toISOString()
        }
      ]
    }
  }
  scrollToBottom()
}

/**
 * 创建新对话
 *
 * 使用 Date.now() 生成临时 ID（实际项目中应由后端生成）
 */
const createConversation = () => {
  const newId = 'conv-' + Date.now()
  conversations.value.unshift({
    id: newId,
    title: '新对话',
    updatedAt: new Date().toISOString()
  })
  currentConversationId.value = newId
  messages.value = []
}

/**
 * 删除对话
 *
 * 先弹出确认框，确认后从列表中移除并调用 API
 */
const deleteConversation = async (id: string) => {
  try {
    await ElMessageBox.confirm('确定删除此对话？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    // 从本地列表中移除
    conversations.value = conversations.value.filter(c => c.id !== id)
    // 如果删除的是当前对话，清空消息
    if (currentConversationId.value === id) {
      currentConversationId.value = ''
      messages.value = []
    }
    // 尝试通知服务端删除
    try {
      await chatApi.deleteConversation(id)
    } catch {
      // already removed from local state
    }
  } catch {
    // cancelled
  }
}

/**
 * 发送消息
 *
 * 完整的消息发送流程：
 * 1. 验证输入非空
 * 2. 如果没有当前对话，自动创建
 * 3. 将用户消息添加到列表
 * 4. 更新对话标题（首次提问时用问题作标题）
 * 5. 滚动到底部
 * 6. 调用 AI API
 * 7. 将 AI 回复添加到列表
 * 8. 再次滚动到底部
 */
const sendMessage = async () => {
  const question = inputText.value.trim()
  if (!question || isAsking.value) return

  // If no conversation, create one
  if (!currentConversationId.value) {
    createConversation()
  }

  // 构建用户消息对象
  const userMsg: DisplayMessage = {
    id: 'msg-' + Date.now(),
    role: 'USER',
    content: question,
    createdAt: new Date().toISOString()
  }
  messages.value.push(userMsg)
  inputText.value = ''

  // Update conversation title with first message
  const conv = conversations.value.find(c => c.id === currentConversationId.value)
  if (conv && conv.title === '新对话') {
    // 取问题前 20 个字符作为对话标题
    conv.title = question.slice(0, 20) + (question.length > 20 ? '...' : '')
  }

  scrollToBottom()

  // Send to API
  isAsking.value = true
  try {
    const res = await chatApi.ask({
      question,
      conversationId: currentConversationId.value
    })
    const assistantMsg: DisplayMessage = {
      id: 'msg-' + Date.now(),
      role: 'ASSISTANT',
      content: res.data.answer,
      sources: res.data.sources,
      followUpQuestions: res.data.followUpQuestions,
      createdAt: new Date().toISOString()
    }
    messages.value.push(assistantMsg)
    currentConversationId.value = res.data.conversationId
  } catch {
    // Fallback mock response
    const mockResponse: DisplayMessage = {
      id: 'msg-' + Date.now(),
      role: 'ASSISTANT',
      content: `关于"${question}"，以下是我的回答：\n\n根据公司知识库中的相关文档，这个问题涉及到多个方面的内容。建议您查看相关文档以获取详细信息。\n\n如果还有其他问题，欢迎继续提问。`,
      sources: [
        { documentId: 1, title: '相关文档 - 规章制度', snippet: '本文档详细说明了公司的各项规章制度...', relevance: 0.88 }
      ],
      followUpQuestions: [
        '能否提供更详细的说明？',
        '相关的流程是怎样的？',
        '有没有具体的案例参考？'
      ],
      createdAt: new Date().toISOString()
    }
    messages.value.push(mockResponse)
  } finally {
    isAsking.value = false
    scrollToBottom()
  }
}

/** 点击推荐后续问题 */
const askFollowUp = (question: string) => {
  inputText.value = question
  sendMessage()
}

/** 快速提问（从欢迎页的建议卡片） */
const quickAsk = (question: string) => {
  createConversation()
  inputText.value = question
  sendMessage()
}

/** 跳转到文档详情 */
const goToDocument = (id: number) => {
  router.push(`/documents/${id}`)
}

/** 组件挂载时加载对话列表 */
onMounted(() => {
  fetchConversations()
})
</script>

<style scoped lang="scss">
/* ================================================================ */
/* 聊天页面整体布局 */
/* ================================================================ */
/* 使用 flex 实现左侧固定宽度 + 右侧自适应的布局 */
.chat-page {
  display: flex;
  height: calc(100vh - 120px);  /* 减去 header(60px) + footer(40px) + padding(20px) */
  margin: 0 auto;
  max-width: 1200px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
}

/* ================================================================ */
/* 左侧对话列表 */
/* ================================================================ */
.chat-sidebar {
  width: 260px;
  flex-shrink: 0;           /* 不收缩，保持固定宽度 */
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background: #fafafa;

  .sidebar-header {
    padding: 16px;

    .new-chat-btn {
      width: 100%;
    }
  }

  .conversation-list {
    flex: 1;                 /* 占据剩余高度 */
    overflow: hidden;

    .conversation-item {
      display: flex;
      align-items: center;
      padding: 12px 16px;
      cursor: pointer;
      transition: background 0.2s;
      border-left: 3px solid transparent;

      &:hover {
        background: #f0f2f5;
      }

      /* 选中状态：蓝色左边框 + 浅蓝背景 */
      &.active {
        background: #ecf5ff;
        border-left-color: #409eff;
      }

      .conv-icon {
        flex-shrink: 0;
        font-size: 18px;
        color: #909399;
        margin-right: 10px;
      }

      .conv-info {
        flex: 1;
        overflow: hidden;

        /* 文字超长时显示省略号 */
        .conv-title {
          font-size: 13px;
          color: #303133;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .conv-time {
          font-size: 11px;
          color: #c0c4cc;
          margin-top: 4px;
        }
      }

      /* 删除按钮：默认隐藏，悬浮时显示 */
      .conv-delete {
        flex-shrink: 0;
        opacity: 0;
        transition: opacity 0.2s;
      }

      &:hover .conv-delete {
        opacity: 1;
      }
    }
  }
}

/* ================================================================ */
/* 右侧聊天主区域 */
/* ================================================================ */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;              /* 防止 flex 子项溢出 */
}

/* ================================================================ */
/* 欢迎页 */
/* ================================================================ */
.chat-welcome {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;

  .welcome-title {
    font-size: 22px;
    font-weight: 600;
    color: #303133;
    margin: 20px 0 8px;
  }

  .welcome-desc {
    font-size: 14px;
    color: #909399;
    margin-bottom: 32px;
  }

  /* 2x2 网格布局的建议卡片 */
  .welcome-suggestions {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    max-width: 480px;

    .suggestion-card {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      background: #f5f7fa;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;
      font-size: 13px;
      color: #606266;

      &:hover {
        background: #ecf5ff;
        color: #409eff;
        border-color: #b3d8ff;
      }
    }
  }
}

/* ================================================================ */
/* 消息列表 */
/* ================================================================ */
.message-list {
  flex: 1;
  overflow: hidden;

  .message-list-inner {
    padding: 20px;
  }

  /* 消息项容器 */
  .message-wrapper {
    display: flex;
    gap: 12px;
    margin-bottom: 24px;

    /* 用户消息：靠右对齐 */
    &.user {
      flex-direction: row-reverse; /* 反转方向：头像在右，内容在左 */

      .message-body {
        align-items: flex-end;     /* 内容右对齐 */
      }

      .message-content {
        background: #409eff;       /* 蓝色气泡 */
        color: #fff;
        border-radius: 12px 2px 12px 12px; /* 右上角尖角 */
      }
    }

    /* AI 消息：靠左对齐 */
    &.assistant {
      .message-content {
        background: #f4f4f5;       /* 灰色气泡 */
        color: #303133;
        border-radius: 2px 12px 12px 12px; /* 左上角尖角 */
      }
    }

    .message-avatar {
      flex-shrink: 0;
    }

    .message-body {
      display: flex;
      flex-direction: column;
      max-width: 70%;              /* 消息最大宽度，避免占满整行 */

      /* 消息文本内容 */
      .message-content {
        padding: 12px 16px;
        font-size: 14px;
        line-height: 1.6;
        white-space: pre-wrap;     /* 保留换行和空格 */
        word-break: break-word;    /* 长单词换行 */

        /* 打字动画指示器 */
        &.typing {
          display: flex;
          gap: 6px;
          padding: 16px 20px;

          .dot {
            width: 8px;
            height: 8px;
            background: #c0c4cc;
            border-radius: 50%;
            /* 引用下方定义的 typingDot 关键帧动画 */
            animation: typingDot 1.4s infinite ease-in-out;

            /* 三个点错开动画时间 */
            &:nth-child(2) {
              animation-delay: 0.2s;
            }

            &:nth-child(3) {
              animation-delay: 0.4s;
            }
          }
        }
      }

      /* AI 消息的来源文档区域 */
      .message-sources {
        margin-top: 8px;
        max-width: 100%;

        /* 穿透修改折叠面板样式 */
        :deep(.el-collapse) {
          border: none;
        }

        :deep(.el-collapse-item__header) {
          background: transparent;
          border: none;
          height: 32px;
          line-height: 32px;
        }

        :deep(.el-collapse-item__wrap) {
          border: none;
          background: transparent;
        }

        .sources-title {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 12px;
          color: #909399;
        }

        .source-item {
          padding: 8px 12px;
          background: #fff;
          border: 1px solid #ebeef5;
          border-radius: 6px;
          margin-bottom: 8px;
          cursor: pointer;
          transition: border-color 0.2s;

          &:hover {
            border-color: #409eff;
          }

          .source-title {
            font-size: 13px;
            font-weight: 500;
            color: #303133;
            margin-bottom: 4px;
          }

          .source-snippet {
            font-size: 12px;
            color: #909399;
            line-height: 1.4;
          }
        }
      }

      /* 推荐后续问题 */
      .follow-ups {
        margin-top: 8px;

        .follow-up-label {
          font-size: 12px;
          color: #909399;
          margin-bottom: 6px;
        }

        .follow-up-item {
          font-size: 13px;
          color: #409eff;
          cursor: pointer;
          padding: 6px 10px;
          border-radius: 6px;
          transition: background 0.2s;
          margin-bottom: 4px;

          &:hover {
            background: #ecf5ff;
          }
        }
      }
    }
  }
}

/* ================================================================ */
/* 输入区域 */
/* ================================================================ */
.chat-input-area {
  display: flex;
  gap: 12px;
  align-items: flex-end;     /* 底部对齐（文本框多行时按钮在底部） */
  padding: 16px 20px;
  border-top: 1px solid #e4e7ed;
  background: #fafafa;

  :deep(.el-textarea__inner) {
    border-radius: 8px;
  }
}

/* ================================================================ */
/* 打字动画关键帧 */
/* ================================================================ */
/* 三个圆点依次上下跳动的动画效果 */
@keyframes typingDot {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-6px);
    opacity: 1;
  }
}
</style>
