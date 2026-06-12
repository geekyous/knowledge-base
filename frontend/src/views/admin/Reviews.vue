<!--
================================================================================
文档审核页面 - views/admin/Reviews.vue
================================================================================

【文件说明】
管理员文档审核页面，展示待审核、已通过、已拒绝的文档列表。
包含：
1. 页面标题 + "内容管理"标签
2. Tab 切换栏（待审核/已通过/已拒绝）
3. 审核卡片列表（文件图标 + 文档信息 + 操作按钮）
4. 底部批量操作栏

【Vue 概念】
- el-tabs / el-tab-pane 标签页切换
- el-tag 标签组件（带 count）
- 审核卡片布局：图标 + 信息 + 操作三栏 flex 布局
- ElMessage / ElMessageBox 消息提示与确认框
================================================================================
-->

<template>
  <div class="admin-reviews">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">
        <el-icon><Select /></el-icon>
        文档审核
      </h1>
      <el-tag type="warning" size="default">内容管理</el-tag>
    </div>

    <!-- Tab 切换栏 -->
    <el-tabs v-model="activeTab" class="review-tabs">
      <el-tab-pane name="pending">
        <template #label>
          <span>待审核</span>
          <el-badge :value="pendingCount" class="tab-badge" />
        </template>
      </el-tab-pane>
      <el-tab-pane label="已通过" name="approved" />
      <el-tab-pane label="已拒绝" name="rejected" />
    </el-tabs>

    <!-- 审核卡片列表 -->
    <div v-loading="loading" class="review-list">
      <el-card
        v-for="doc in currentList"
        :key="doc.id"
        class="review-card"
        shadow="hover"
      >
        <div class="review-card-body">
          <!-- 文件类型图标 -->
          <div class="file-icon" :style="{ background: doc.iconBg }">
            <el-icon :size="20"><component :is="doc.icon" /></el-icon>
          </div>

          <!-- 文档信息 -->
          <div class="review-info">
            <div class="review-title">{{ doc.title }}</div>
            <div class="review-meta">
              <span class="meta-item">
                <el-icon><Folder /></el-icon>
                {{ doc.category }}
              </span>
              <span class="meta-item">
                <el-icon><User /></el-icon>
                {{ doc.author }}
              </span>
              <span class="meta-item">
                <el-icon><Clock /></el-icon>
                {{ doc.date }}
              </span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="review-actions">
            <el-button size="small" @click="handlePreview(doc)">
              <el-icon><View /></el-icon>
              预览
            </el-button>
            <el-button type="success" size="small" @click="handleApprove(doc)">
              <el-icon><Check /></el-icon>
              通过
            </el-button>
            <el-button type="danger" size="small" plain @click="handleReject(doc)">
              <el-icon><Close /></el-icon>
              拒绝
            </el-button>
          </div>
        </div>
      </el-card>

      <!-- 空状态 -->
      <el-empty
        v-if="currentList.length === 0"
        :description="emptyText"
        :image-size="80"
      />
    </div>

    <!-- 底部批量操作栏 -->
    <div v-if="activeTab === 'pending' && pendingList.length > 0" class="batch-bar">
      <el-button plain @click="handleBatchApprove">
        <el-icon><Finished /></el-icon>
        批量通过
      </el-button>
      <el-button plain @click="handleViewAll">
        查看全部 ({{ pendingCount }})
        <el-icon><ArrowRight /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import {
  Select,
  View,
  Check,
  Close,
  Folder,
  User,
  Clock,
  Document,
  Finished,
  ArrowRight
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminReviewApi } from '@/api/admin'
import type { Document as DocType } from '@/types'

// ==================== 类型定义 ====================
interface ReviewDoc {
  id: number
  title: string
  category: string
  author: string
  date: string
  iconBg: string
  icon: typeof Document
  /** 保留原始 Document 引用，用于 API 操作 */
  _raw?: DocType
}

// ==================== Mock 数据（API 不可用时的降级数据）====================
const mockPendingList: ReviewDoc[] = [
  {
    id: 1,
    title: '员工手册 2025版',
    category: '人事制度',
    author: 'Alice Chen',
    date: '2026-06-01',
    iconBg: 'linear-gradient(135deg, #667eea, #764ba2)',
    icon: Document
  },
  {
    id: 2,
    title: '销售话术培训资料',
    category: '销售支持',
    author: 'Bob Wang',
    date: '2026-06-02',
    iconBg: 'linear-gradient(135deg, #409eff, #337ecc)',
    icon: Document
  },
  {
    id: 3,
    title: 'Q2 销售数据汇总',
    category: '销售支持',
    author: 'Carol Zhao',
    date: '2026-06-03',
    iconBg: 'linear-gradient(135deg, #67c23a, #529b2e)',
    icon: Document
  }
]

// ==================== 状态 ====================
const activeTab = ref('pending')
const loading = ref(false)

const pendingList = ref<ReviewDoc[]>([])
const approvedList = ref<ReviewDoc[]>([])
const rejectedList = ref<ReviewDoc[]>([])

/** 将 API Document 转换为页面展示用的 ReviewDoc */
const toReviewDoc = (doc: DocType): ReviewDoc => ({
  id: doc.id,
  title: doc.title,
  category: doc.category?.name ?? '未分类',
  author: doc.author?.username ?? '未知',
  date: doc.createdAt?.slice(0, 10) ?? '',
  iconBg: 'linear-gradient(135deg, #409eff, #337ecc)',
  icon: Document,
  _raw: doc
})

/** 加载当前 Tab 数据 */
const loadCurrentTab = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'pending') {
      const res = await adminReviewApi.listPending({ page: 1, size: 20 })
      pendingList.value = (res.data?.items ?? []).map(toReviewDoc)
    } else {
      const status = activeTab.value === 'approved' ? 'PUBLISHED' : 'REJECTED'
      const res = await adminReviewApi.listReviewed({ status, page: 1, size: 20 })
      const docs = (res.data?.items ?? []).map(toReviewDoc)
      if (activeTab.value === 'approved') {
        approvedList.value = docs
      } else {
        rejectedList.value = docs
      }
    }
  } catch {
    // API 不可用时降级到 mock 数据
    if (activeTab.value === 'pending' && pendingList.value.length === 0) {
      pendingList.value = mockPendingList
    }
  } finally {
    loading.value = false
  }
}

/** 待审核数量 */
const pendingCount = computed(() => pendingList.value.length)

/** 当前 Tab 对应的列表 */
const currentList = computed(() => {
  const map: Record<string, ReviewDoc[]> = {
    pending: pendingList.value,
    approved: approvedList.value,
    rejected: rejectedList.value
  }
  return map[activeTab.value] || []
})

/** 空状态文案 */
const emptyText = computed(() => {
  const map: Record<string, string> = {
    pending: '暂无待审核文档',
    approved: '暂无已通过文档',
    rejected: '暂无已拒绝文档'
  }
  return map[activeTab.value] || '暂无数据'
})

// ==================== 操作方法 ====================

/** 预览文档 */
const handlePreview = (doc: ReviewDoc) => {
  ElMessage.info(`预览文档：${doc.title}`)
}

/** 通过审核 */
const handleApprove = async (doc: ReviewDoc) => {
  try {
    await ElMessageBox.confirm(
      `确认通过文档「${doc.title}」？`,
      '审核确认',
      { confirmButtonText: '通过', cancelButtonText: '取消', type: 'success' }
    )
    await adminReviewApi.approve(doc.id)
    ElMessage.success(`已通过：${doc.title}`)
    await loadCurrentTab()
  } catch (e: unknown) {
    // ElMessageBox 取消时不报错；API 异常时提示
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error('审核操作失败，请稍后重试')
    }
  }
}

/** 拒绝审核 */
const handleReject = async (doc: ReviewDoc) => {
  try {
    await ElMessageBox.confirm(
      `确认拒绝文档「${doc.title}」？`,
      '审核确认',
      { confirmButtonText: '拒绝', cancelButtonText: '取消', type: 'warning' }
    )
    await adminReviewApi.reject(doc.id)
    ElMessage.warning(`已拒绝：${doc.title}`)
    await loadCurrentTab()
  } catch (e: unknown) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error('审核操作失败，请稍后重试')
    }
  }
}

/** 批量通过 */
const handleBatchApprove = async () => {
  try {
    await ElMessageBox.confirm(
      `确认批量通过全部 ${pendingList.value.length} 篇待审核文档？`,
      '批量审核',
      { confirmButtonText: '全部通过', cancelButtonText: '取消', type: 'success' }
    )
    const ids = pendingList.value.map(d => d.id)
    await adminReviewApi.batchApprove(ids)
    ElMessage.success(`已批量通过 ${ids.length} 篇文档`)
    await loadCurrentTab()
  } catch (e: unknown) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error('批量审核操作失败，请稍后重试')
    }
  }
}

/** 查看全部 */
const handleViewAll = () => {
  ElMessage.info('查看全部待审核文档')
}

// ==================== 生命周期 ====================

/** Tab 切换时重新加载 */
watch(activeTab, () => {
  loadCurrentTab()
})

/** 页面初始化加载待审核列表 */
onMounted(() => {
  loadCurrentTab()
})
</script>

<style scoped lang="scss">
/* 审核页面容器 */
.admin-reviews {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

/* 页面头部：标题 + 标签 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;

  .page-title {
    font-size: 22px;
    font-weight: 600;
    color: #303133;
    margin: 0;
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

/* Tab 栏样式 */
.review-tabs {
  margin-bottom: 20px;

  /* 徽章与 Tab 标签同行 */
  .tab-badge {
    margin-left: 6px;

    :deep(.el-badge__content) {
      font-size: 11px;
    }
  }
}

/* 审核卡片列表 */
.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 审核卡片 */
.review-card {
  :deep(.el-card__body) {
    padding: 16px 20px;
  }

  .review-card-body {
    display: flex;
    align-items: center;
    gap: 16px;
  }
}

/* 文件类型图标 */
.file-icon {
  width: 45px;
  height: 45px;
  min-width: 45px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

/* 文档信息 */
.review-info {
  flex: 1;
  min-width: 0;

  .review-title {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 6px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .review-meta {
    font-size: 13px;
    color: #909399;
    display: flex;
    flex-wrap: wrap;
    gap: 16px;

    .meta-item {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
  }
}

/* 操作按钮组 */
.review-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

/* 底部批量操作栏 */
.batch-bar {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f2f5;
}
</style>
