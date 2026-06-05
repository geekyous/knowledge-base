<!--
================================================================================
文档卡片组件 - components/business/DocumentCard.vue
================================================================================

【文件说明】
文档卡片展示组件，用于在首页、搜索结果等页面展示文档摘要信息。
接收一个 Document 对象作为 prop，展示标题、摘要、分类、浏览量等。

【Vue 概念】
- defineProps: 接收父组件传递的文档数据
- TypeScript 泛型: 约束 prop 类型
================================================================================
-->

<template>
  <el-card class="document-card" shadow="hover">
    <div class="card-content">
      <h3 class="card-title">{{ document.title }}</h3>
      <p class="card-summary">{{ document.summary || '暂无摘要' }}</p>
      <div class="card-meta">
        <el-tag v-if="document.category" size="small" effect="plain">
          {{ document.category.name }}
        </el-tag>
        <span class="meta-item">
          <el-icon><View /></el-icon>
          {{ document.viewCount || 0 }}
        </span>
        <span class="meta-item">
          <el-icon><Star /></el-icon>
          {{ document.likeCount || 0 }}
        </span>
        <span v-if="document.author" class="meta-item">
          <el-icon><User /></el-icon>
          {{ document.author.username }}
        </span>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { View, Star, User } from '@element-plus/icons-vue'
import type { Document } from '@/types'

interface Props {
  document: Document
}

defineProps<Props>()
</script>

<style scoped lang="scss">
.document-card {
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-3px);
    border-color: #409eff;
  }

  .card-content {
    .card-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin: 0 0 8px;
      line-height: 1.4;
      display: -webkit-box;
      -webkit-line-clamp: 1;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .card-summary {
      font-size: 13px;
      color: #606266;
      line-height: 1.5;
      margin: 0 0 12px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .card-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;

      .meta-item {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: #909399;
      }
    }
  }
}
</style>
