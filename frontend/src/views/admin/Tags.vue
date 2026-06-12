<!--
================================================================================
标签管理页面 - views/admin/Tags.vue
================================================================================

【文件说明】
管理员标签管理页面，用于管理知识库文档的内容标记标签。
包含：
1. 标签云展示（按关联文档数量动态调整字号）
2. 标签数据表格（名称、颜色、关联文档数、创建时间、操作）

【Vue 概念】
- 标签云：根据文档关联数动态计算字号和字重
- el-table 数据表格 + 自定义列插槽
- ElMessage / ElMessageBox 用于操作反馈和确认

💡 学习要点:
1. computed 计算属性根据文档数映射字号范围
2. el-popconfirm 或 ElMessageBox.confirm 二次确认删除
3. CSS 变量实现标签颜色主题化
================================================================================
-->

<template>
  <div class="admin-tags">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-title-row">
        <h1 class="page-title">标签管理</h1>
        <el-tag type="info" size="small">内容标记</el-tag>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        新建标签
      </el-button>
    </div>

    <!-- ================================================================== -->
    <!-- 标签云区域 -->
    <!-- ================================================================== -->
    <el-card shadow="never" class="tag-cloud-card">
      <template #header>
        <div class="card-header-row">
          <span class="card-title">标签云</span>
          <span class="card-subtitle">共 {{ tags.length }} 个标签</span>
        </div>
      </template>
      <div class="tag-cloud">
        <span
          v-for="tag in tags"
          :key="tag.id"
          class="tag-cloud-item"
          :style="{
            fontSize: getCloudFontSize(tag.docCount),
            fontWeight: tag.docCount >= 30 ? '600' : '400',
            color: tag.color
          }"
        >
          {{ tag.name }}
        </span>
      </div>
    </el-card>

    <!-- ================================================================== -->
    <!-- 标签数据表格 -->
    <!-- ================================================================== -->
    <el-card shadow="never" class="tag-table-card">
      <template #header>
        <div class="card-header-row">
          <span class="card-title">标签列表</span>
        </div>
      </template>
      <el-table :data="tags" stripe style="width: 100%">
        <!-- 标签名称列：渲染为带背景色的标签徽章 -->
        <el-table-column label="标签名称" min-width="160">
          <template #default="{ row }">
            <el-tag
              :style="{
                backgroundColor: row.bgColor,
                color: row.color,
                borderColor: row.borderColor
              }"
              effect="light"
              size="default"
            >
              {{ row.name }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 颜色列：小色块圆点 -->
        <el-table-column label="颜色" width="80" align="center">
          <template #default="{ row }">
            <span
              class="color-swatch"
              :style="{
                backgroundColor: row.bgColor,
                borderColor: row.borderColor
              }"
            ></span>
          </template>
        </el-table-column>

        <!-- 关联文档数 -->
        <el-table-column prop="docCount" label="关联文档数" width="120" align="center" />

        <!-- 创建时间 -->
        <el-table-column prop="createdAt" label="创建时间" width="140" />

        <!-- 操作列 -->
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :icon="Edit"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="danger"
              :icon="Delete"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'

// ==================== 类型定义 ====================
/** 标签数据结构 */
interface TagItem {
  id: number
  name: string
  color: string
  bgColor: string
  borderColor: string
  docCount: number
  createdAt: string
}

// ==================== Mock 标签数据 ====================
const tags = ref<TagItem[]>([
  { id: 1,  name: '制度', color: '#1e40af', bgColor: '#dbeafe', borderColor: '#93c5fd', docCount: 56, createdAt: '2025-09-01' },
  { id: 2,  name: '规范', color: '#166534', bgColor: '#dcfce7', borderColor: '#86efac', docCount: 42, createdAt: '2025-09-05' },
  { id: 3,  name: '前端', color: '#7c3aed', bgColor: '#ede9fe', borderColor: '#c4b5fd', docCount: 35, createdAt: '2025-09-10' },
  { id: 4,  name: '培训', color: '#b45309', bgColor: '#fef3c7', borderColor: '#fcd34d', docCount: 31, createdAt: '2025-09-12' },
  { id: 5,  name: '安全', color: '#dc2626', bgColor: '#fee2e2', borderColor: '#fca5a5', docCount: 28, createdAt: '2025-09-15' },
  { id: 6,  name: '流程', color: '#0369a1', bgColor: '#e0f2fe', borderColor: '#7dd3fc', docCount: 23, createdAt: '2025-09-18' },
  { id: 7,  name: '合规', color: '#0d9488', bgColor: '#ccfbf1', borderColor: '#5eead4', docCount: 22, createdAt: '2025-09-22' },
  { id: 8,  name: '发布', color: '#15803d', bgColor: '#dcfce7', borderColor: '#86efac', docCount: 18, createdAt: '2025-10-01' },
  { id: 9,  name: '手册', color: '#a16207', bgColor: '#fef9c3', borderColor: '#fde047', docCount: 19, createdAt: '2025-10-05' },
  { id: 10, name: '标准', color: '#92400e', bgColor: '#fef3c7', borderColor: '#fcd34d', docCount: 15, createdAt: '2025-10-10' },
  { id: 11, name: '新人', color: '#6d28d9', bgColor: '#ede9fe', borderColor: '#c4b5fd', docCount: 12, createdAt: '2025-10-15' },
  { id: 12, name: 'v2.0', color: '#be185d', bgColor: '#fce7f3', borderColor: '#f9a8d4', docCount: 8,  createdAt: '2025-10-20' }
])

// ==================== 辅助函数 ====================

/**
 * 根据关联文档数量计算标签云字号
 *
 * 文档数范围 8-56，映射到字号 14px-28px。
 * docCount 越大字号越大，视觉上体现标签热度。
 */
const getCloudFontSize = (docCount: number): string => {
  const minSize = 14
  const maxSize = 28
  const minCount = 5
  const maxCount = 60
  // 线性映射，clamp 到合理范围
  const ratio = Math.min(Math.max((docCount - minCount) / (maxCount - minCount), 0), 1)
  const size = Math.round(minSize + ratio * (maxSize - minSize))
  return `${size}px`
}

// ==================== 操作处理 ====================

/** 新建标签 */
const handleCreate = () => {
  ElMessage.info('新建标签功能开发中')
}

/** 编辑标签 */
const handleEdit = (row: TagItem) => {
  ElMessage.info(`编辑标签: ${row.name}`)
}

/** 删除标签（二次确认） */
const handleDelete = (row: TagItem) => {
  ElMessageBox.confirm(
    `确定要删除标签「${row.name}」吗？该标签关联了 ${row.docCount} 篇文档。`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    tags.value = tags.value.filter(t => t.id !== row.id)
    ElMessage.success(`标签「${row.name}」已删除`)
  }).catch(() => {
    // 用户取消，无需处理
  })
}
</script>

<style scoped lang="scss">
/* 标签管理容器 */
.admin-tags {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

/* 页面头部：标题和操作按钮 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.card-subtitle {
  font-size: 13px;
  color: #909399;
}

/* 卡片头部：标题和操作左右排列 */
.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 标签云卡片 */
.tag-cloud-card {
  margin-bottom: 20px;
}

/* 标签云布局 */
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
  align-items: baseline;
  padding: 8px 0;
  line-height: 1.8;
}

/* 标签云单个标签 */
.tag-cloud-item {
  display: inline-block;
  cursor: default;
  transition: transform 0.2s, opacity 0.2s;

  &:hover {
    transform: scale(1.1);
    opacity: 0.8;
  }
}

/* 标签表格卡片 */
.tag-table-card {
  margin-bottom: 20px;
}

/* 颜色色块 */
.color-swatch {
  display: inline-block;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  vertical-align: middle;
}
</style>
