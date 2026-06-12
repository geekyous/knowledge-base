<!--
================================================================================
通知面板组件 - components/common/NotificationPanel.vue
================================================================================

【文件说明】
通知下拉面板组件，点击头部通知铃铛时弹出。
显示用户的通知列表，支持标记已读和查看全部通知。

【Vue 概念】
- el-popover: 弹出面板容器，定位在触发元素附近
- el-scrollbar: 滚动容器，限制列表高度并显示滚动条
- defineProps / defineEmits: 组件 props 和事件定义
- v-for 列表渲染: 遍历通知数据生成列表项
- 条件类绑定 :class: 根据 read 状态显示未读圆点
================================================================================
-->

<template>
  <el-popover
    :visible="visible"
    placement="bottom-end"
    :width="380"
    trigger="click"
    @update:visible="(val: boolean) => emit('update:visible', val)"
  >
    <!-- 触发元素：由父组件通过默认插槽传入（通常是通知铃铛按钮） -->
    <template #reference>
      <slot />
    </template>

    <!-- 通知面板主体 -->
    <div class="notification-panel">
      <!-- ================================================================ -->
      <!-- 头部：标题 + 全部已读 -->
      <!-- ================================================================ -->
      <div class="panel-header">
        <span class="panel-title">通知</span>
        <el-button link type="primary" size="small" @click="markAllRead">
          全部已读
        </el-button>
      </div>

      <!-- ================================================================ -->
      <!-- 通知列表 -->
      <!-- ================================================================ -->
      <el-scrollbar max-height="400px">
        <div class="notification-list">
          <div
            v-for="item in notifications"
            :key="item.id"
            class="notification-item"
            :class="{ unread: !item.read }"
            @click="handleItemClick(item)"
          >
            <!-- 左侧：类型图标 -->
            <div class="item-icon" :style="{ backgroundColor: item.iconBg }">
              <el-icon :size="20" :color="item.iconColor">
                <component :is="item.icon" />
              </el-icon>
            </div>

            <!-- 中间：标题 + 时间 -->
            <div class="item-content">
              <p class="item-title">{{ item.title }}</p>
              <p class="item-time">{{ item.time }}</p>
            </div>

            <!-- 右侧：未读圆点 -->
            <div v-if="!item.read" class="item-dot" />
          </div>
        </div>
      </el-scrollbar>

      <!-- ================================================================ -->
      <!-- 底部：查看全部通知 -->
      <!-- ================================================================ -->
      <div class="panel-footer">
        <el-button link type="primary" @click="viewAll">查看全部通知</el-button>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Document,
  Comment,
  Goods,
  UserFilled,
  Warning
} from '@element-plus/icons-vue'
import type { Component } from 'vue'

// ---------------------------------------------------------------------------
// Props & Emits
// ---------------------------------------------------------------------------

interface Props {
  /** 未读通知数量（用于父组件显示徽章） */
  count?: number
  /** 控制面板显示/隐藏 */
  visible?: boolean
}

withDefaults(defineProps<Props>(), {
  count: 0,
  visible: false
})

const emit = defineEmits<{
  (e: 'update:count', count: number): void
  (e: 'update:visible', val: boolean): void
  (e: 'viewAll'): void
}>()

// ---------------------------------------------------------------------------
// 通知类型定义
// ---------------------------------------------------------------------------

/** 单条通知的数据结构 */
interface NotificationItem {
  id: number
  type: string
  icon: Component
  iconBg: string
  iconColor: string
  title: string
  time: string
  read: boolean
}

// ---------------------------------------------------------------------------
// Mock 通知数据
// ---------------------------------------------------------------------------

const notifications = ref<NotificationItem[]>([
  {
    id: 1,
    type: 'doc_approved',
    icon: Document,
    iconBg: '#ecf5ff',
    iconColor: '#409eff',
    title: '您的文档《产品发布流程》已通过审核',
    time: '刚刚',
    read: false
  },
  {
    id: 2,
    type: 'comment',
    icon: Comment,
    iconBg: '#fdf6ec',
    iconColor: '#e6a23c',
    title: 'Alice Chen评论了您的文档',
    time: '5分钟前',
    read: false
  },
  {
    id: 3,
    type: 'like',
    icon: Goods,
    iconBg: '#f0f9eb',
    iconColor: '#67c23a',
    title: '您的答案获得了10个点赞',
    time: '1小时前',
    read: true
  },
  {
    id: 4,
    type: 'follow',
    icon: UserFilled,
    iconBg: '#ecf5ff',
    iconColor: '#409eff',
    title: 'Bob Wang开始关注您',
    time: '昨天',
    read: true
  },
  {
    id: 5,
    type: 'system',
    icon: Warning,
    iconBg: '#fef0f0',
    iconColor: '#f56c6c',
    title: '系统将于今晚进行维护',
    time: '2天前',
    read: true
  }
])

// ---------------------------------------------------------------------------
// 操作方法
// ---------------------------------------------------------------------------

/** 标记所有通知为已读 */
const markAllRead = () => {
  notifications.value.forEach((n) => {
    n.read = true
  })
  emit('update:count', 0)
  ElMessage.success('已全部标记为已读')
}

/** 点击单条通知 */
const handleItemClick = (item: NotificationItem) => {
  if (!item.read) {
    item.read = true
    const newCount = notifications.value.filter((n) => !n.read).length
    emit('update:count', newCount)
  }
  // TODO: 根据 item.type 跳转到对应页面
}

/** 查看全部通知 */
const viewAll = () => {
  emit('update:visible', false)
  emit('viewAll')
  // TODO: 路由跳转到通知列表页
}
</script>

<style scoped lang="scss">
.notification-panel {
  margin: -12px; /* 抵消 el-popover 默认内边距 */
}

/* 头部 */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 12px;
  border-bottom: 1px solid #f0f0f0;

  .panel-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }
}

/* 通知列表 */
.notification-list {
  .notification-item {
    display: flex;
    align-items: flex-start;
    padding: 14px 20px;
    cursor: pointer;
    transition: background-color 0.2s;
    gap: 12px;

    &:hover {
      background-color: #f5f7fa;
    }

    /* 左侧图标圆形背景 */
    .item-icon {
      flex-shrink: 0;
      width: 40px;
      height: 40px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    /* 中间内容 */
    .item-content {
      flex: 1;
      min-width: 0; /* 允许文本截断 */

      .item-title {
        font-size: 14px;
        color: #303133;
        line-height: 1.5;
        margin: 0;
        /* 超出两行时截断 */
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .item-time {
        font-size: 12px;
        color: #909399;
        margin: 4px 0 0;
        line-height: 1;
      }
    }

    /* 右侧未读圆点 */
    .item-dot {
      flex-shrink: 0;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background-color: #409eff;
      margin-top: 6px;
    }

    /* 未读项标题加粗 */
    &.unread .item-title {
      font-weight: 600;
    }
  }
}

/* 底部 */
.panel-footer {
  padding: 12px 20px;
  border-top: 1px solid #f0f0f0;
  text-align: center;
}
</style>
