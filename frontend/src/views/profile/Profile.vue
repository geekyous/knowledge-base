<!--
================================================================================
个人中心页面 - views/profile/Profile.vue
================================================================================

【文件说明】
这是用户的个人中心页面，展示用户信息和相关操作。
页面分为两栏布局：
- 左栏：用户头像和基本信息、统计数据
- 右栏：标签页（最近活动时间线、我的文档列表、个人设置表单）

【Vue 概念】
- computed 计算属性：根据用户角色动态计算标签文本和样式
- el-tabs 标签页组件：多内容区域切换
- el-timeline 时间线组件：展示活动历史
- Pinia Store 数据读取：从 userStore 获取用户信息

💡 学习要点:
1. el-tabs + v-model="activeTab" 实现标签页切换
2. el-timeline 组件用于展示按时间排列的事件
3. computed 根据数据动态计算 UI 展示值（如角色映射为中文）
4. el-form 的 :model-value 单向绑定（不需要双向绑定的只读字段）
5. 使用 as const 进行类型断言，确保字面量类型的精确性
================================================================================
-->

<template>
  <div class="profile-page">
    <!-- Element Plus 栅格布局：左 8 右 16（共 24 栏） -->
    <el-row :gutter="24">
      <!-- ================================================================ -->
      <!-- 左栏：用户信息 -->
      <!-- ================================================================ -->
      <el-col :span="8">
        <!-- 用户信息卡片 -->
        <el-card class="user-card" shadow="never">
          <div class="user-info">
            <!-- 用户头像 -->
            <el-avatar :size="80" :icon="UserFilled" style="background-color: #409eff" />
            <!-- 用户名：从 Pinia store 读取 -->
            <h2 class="user-name">{{ userStore.userName || '用户' }}</h2>
            <!-- 角色标签：computed 计算属性动态决定文本和类型 -->
            <el-tag
              :type="roleTagType"
              effect="plain"
              size="small"
              class="user-role"
            >
              {{ roleLabel }}
            </el-tag>
            <p class="user-department">技术部</p>
            <!-- 可选链 ?. 安全读取邮箱 -->
            <p class="user-email">{{ userStore.currentUser?.email || 'user@example.com' }}</p>
          </div>
        </el-card>

        <!-- 统计卡片组 -->
        <div class="stats-cards">
          <!-- 文档数 -->
          <el-card class="stat-card" shadow="hover">
            <div class="stat-icon" style="background: #ecf5ff; color: #409eff;">
              <el-icon :size="24"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.documents }}</div>
              <div class="stat-label">我的文档</div>
            </div>
          </el-card>
          <!-- 问答数 -->
          <el-card class="stat-card" shadow="hover">
            <div class="stat-icon" style="background: #f0f9eb; color: #67c23a;">
              <el-icon :size="24"><ChatDotRound /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.answers }}</div>
              <div class="stat-label">我的问答</div>
            </div>
          </el-card>
          <!-- 点赞数 -->
          <el-card class="stat-card" shadow="hover">
            <div class="stat-icon" style="background: #fdf6ec; color: #e6a23c;">
              <el-icon :size="24"><Star /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.likes }}</div>
              <div class="stat-label">获得点赞</div>
            </div>
          </el-card>
        </div>
      </el-col>

      <!-- ================================================================ -->
      <!-- 右栏：内容标签页 -->
      <!-- ================================================================ -->
      <el-col :span="16">
        <el-card shadow="never">
          <!--
            el-tabs: 标签页组件
            v-model="activeTab" 双向绑定当前激活的标签名
          -->
          <el-tabs v-model="activeTab">
            <!-- ------------------------------------------------------------ -->
            <!-- 标签页 1：最近活动 -->
            <!-- ------------------------------------------------------------ -->
            <el-tab-pane label="最近活动" name="activity">
              <!--
                el-timeline: 时间线组件
                用于按时间顺序展示用户的活动记录
                :timestamp 显示时间戳
                :type 控制节点颜色
                placement="top" 时间戳显示在上方
              -->
              <el-timeline class="activity-timeline">
                <el-timeline-item
                  v-for="(item, index) in activities"
                  :key="index"
                  :timestamp="item.time"
                  :type="item.type"
                  placement="top"
                >
                  <div class="activity-content">
                    <span class="activity-text">{{ item.content }}</span>
                  </div>
                </el-timeline-item>
              </el-timeline>
            </el-tab-pane>

            <!-- ------------------------------------------------------------ -->
            <!-- 标签页 2：我的文档 -->
            <!-- ------------------------------------------------------------ -->
            <el-tab-pane label="我的文档" name="documents">
              <div class="my-documents">
                <!--
                  文档列表项
                  @click="$router.push(...)" 直接在模板中使用路由导航
                -->
                <div
                  v-for="doc in myDocuments"
                  :key="doc.id"
                  class="my-doc-item"
                  @click="$router.push(`/documents/${doc.id}`)"
                >
                  <div class="doc-info">
                    <h4 class="doc-title">{{ doc.title }}</h4>
                    <span class="doc-date">{{ doc.date }}</span>
                  </div>
                  <!-- 状态标签 -->
                  <el-tag :type="doc.statusType" size="small">{{ doc.statusLabel }}</el-tag>
                </div>
                <!-- 空状态 -->
                <el-empty v-if="myDocuments.length === 0" description="暂无文档" :image-size="80" />
              </div>
            </el-tab-pane>

            <!-- ------------------------------------------------------------ -->
            <!-- 标签页 3：个人设置 -->
            <!-- ------------------------------------------------------------ -->
            <el-tab-pane label="个人设置" name="settings">
              <div class="settings-section">
                <h3 class="settings-title">基本信息</h3>
                <!--
                  el-form: 表单组件
                  label-width="80px" 标签宽度固定为 80px
                  :model-value 单向绑定（只读字段，不需要 v-model 双向绑定）
                  disabled 属性让输入框不可编辑
                -->
                <el-form label-width="80px" class="settings-form">
                  <el-form-item label="用户名">
                    <!-- disabled：用户名不可修改 -->
                    <el-input :model-value="userStore.userName" disabled />
                  </el-form-item>
                  <el-form-item label="邮箱">
                    <el-input :model-value="userStore.currentUser?.email || ''" />
                  </el-form-item>
                  <el-form-item label="部门">
                    <el-input model-value="技术部" />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary">保存修改</el-button>
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
<!-- 导入 Vue 响应式 API -->
import { ref, computed } from 'vue'

<!-- 导入图标 -->
import {
  UserFilled,
  Document,
  ChatDotRound,
  Star
} from '@element-plus/icons-vue'

<!-- 导入用户状态 store -->
import { useUserStore } from '@/stores/user'

<!-- 获取 store 实例 -->
const userStore = useUserStore()

/** 当前激活的标签页名称（对应 el-tab-pane 的 name 属性） */
const activeTab = ref('activity')

/**
 * 角色标签文本（计算属性）
 *
 * 将英文角色代码映射为中文显示文本。
 * Record<string, string> 类型定义键值映射表。
 */
const roleLabel = computed(() => {
  const map: Record<string, string> = {
    USER: '普通用户',
    EDITOR: '编辑者',
    ADMIN: '管理员'
  }
  return map[userStore.userRole || 'USER'] || '普通用户'
})

/**
 * 角色标签类型（计算属性）
 *
 * 根据角色返回不同的 Element Plus Tag 类型（颜色）：
 * - USER → info（灰色）
 * - EDITOR → warning（黄色）
 * - ADMIN → danger（红色）
 */
const roleTagType = computed(() => {
  const map: Record<string, string> = {
    USER: 'info',
    EDITOR: 'warning',
    ADMIN: 'danger'
  }
  return map[userStore.userRole || 'USER'] || 'info'
})

/** 统计数据 */
const stats = ref({
  documents: 12,
  answers: 48,
  likes: 156
})

/**
 * 活动记录列表
 *
 * 每条记录包含：
 * - content: 活动内容描述
 * - time: 活动时间
 * - type: 时间线节点颜色类型
 *
 * as const 类型断言确保 type 的值是精确的字面量类型
 * （如 'primary' 而不是 string），符合 Element Plus 的类型要求
 */
const activities = ref([
  {
    content: '发布了文档《前端开发规范 V3.0》',
    time: '2025-05-30 14:30',
    type: 'primary' as const
  },
  {
    content: '提问了「如何申请差旅报销？」',
    time: '2025-05-29 10:15',
    type: 'success' as const
  },
  {
    content: '点赞了文档《新员工入职指南》',
    time: '2025-05-28 16:45',
    type: 'warning' as const
  },
  {
    content: '更新了文档《技术架构设计文档》',
    time: '2025-05-27 09:20',
    type: 'primary' as const
  },
  {
    content: '评论了文档《API 接口规范》',
    time: '2025-05-26 11:00',
    type: '' as const
  },
  {
    content: '创建了文档《项目部署手册》',
    time: '2025-05-25 15:30',
    type: 'primary' as const
  }
])

/** 我的文档列表 */
const myDocuments = ref([
  { id: 1, title: '前端开发规范 V3.0', date: '2025-05-30', statusType: 'success', statusLabel: '已发布' },
  { id: 2, title: '技术架构设计文档', date: '2025-05-27', statusType: 'warning', statusLabel: '待审核' },
  { id: 3, title: '项目部署手册', date: '2025-05-25', statusType: 'success', statusLabel: '已发布' },
  { id: 4, title: '代码审查规范', date: '2025-05-20', statusType: 'info', statusLabel: '草稿' }
])
</script>

<style scoped lang="scss">
/* 个人中心页面容器 */
.profile-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 20px;
}

/* 用户信息卡片 */
.user-card {
  text-align: center;

  .user-info {
    display: flex;
    flex-direction: column;
    align-items: center;

    .user-name {
      font-size: 20px;
      font-weight: 600;
      color: #303133;
      margin: 16px 0 8px;
    }

    .user-role {
      margin-bottom: 8px;
    }

    .user-department {
      font-size: 14px;
      color: #606266;
      margin: 0 0 4px;
    }

    .user-email {
      font-size: 13px;
      color: #909399;
      margin: 0;
    }
  }
}

/* 统计卡片组：垂直排列 */
.stats-cards {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;

  .stat-card {
    /* 穿透修改卡片内容为 flex 水平布局 */
    :deep(.el-card__body) {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 16px;
    }

    /* 图标容器：固定大小，圆角背景 */
    .stat-icon {
      width: 48px;
      height: 48px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .stat-info {
      .stat-value {
        font-size: 24px;
        font-weight: 600;
        color: #303133;
      }

      .stat-label {
        font-size: 13px;
        color: #909399;
        margin-top: 2px;
      }
    }
  }
}

/* 活动时间线 */
.activity-timeline {
  padding: 16px 0;

  .activity-content {
    .activity-text {
      font-size: 14px;
      color: #303133;
    }
  }
}

/* 我的文档列表 */
.my-documents {
  .my-doc-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 0;
    border-bottom: 1px solid #f0f2f5;
    cursor: pointer;
    transition: background 0.2s;

    &:last-child {
      border-bottom: none;
    }

    /* 悬浮时标题变色 */
    &:hover {
      .doc-info .doc-title {
        color: #409eff;
      }
    }

    .doc-info {
      .doc-title {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
        margin: 0 0 4px;
        transition: color 0.2s;
      }

      .doc-date {
        font-size: 12px;
        color: #909399;
      }
    }
  }
}

/* 个人设置区域 */
.settings-section {
  .settings-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 20px;
  }

  /* 限制表单宽度，避免过宽 */
  .settings-form {
    max-width: 400px;
  }
}
</style>
