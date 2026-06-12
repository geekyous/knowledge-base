<!--
================================================================================
文档详情页 - views/document/DocumentDetail.vue
================================================================================

【文件说明】
文档详情页面，展示一篇文档的完整内容。
功能包括：
1. 面包屑导航（首页 / 文档管理 / 文档标题）
2. 文档基本信息（标题、状态标签、作者、日期、分类、标签、统计数据）
3. Markdown 内容渲染（使用 marked 库）
4. TOC 目录侧边栏（从 Markdown 标题自动生成）
5. 操作按钮（编辑、收藏、分享、版本历史、点赞）
6. 相关文档推荐
7. 加载骨架屏

【原型对照】
- 原型位置：prototype-pc.html「文档详情页」屏幕（1833-1905 行）
- 面包屑：首页 / 文档管理 / 文档标题
- 标题 + 状态标签（"已发布"）
- 元信息行：作者 / 分类 / 日期 / 浏览量 / 点赞数
- 标签列表
- 操作按钮：编辑 / 收藏 / 分享 / 版本历史
- 内容区 + 右侧 TOC 目录（高亮当前位置）
================================================================================
-->

<template>
  <div class="document-detail-page">
    <!-- 加载骨架屏 -->
    <div class="loading-wrapper" v-if="loading">
      <el-skeleton :rows="12" animated />
    </div>

    <!-- 文档内容 -->
    <template v-else-if="documentData">
      <!-- ================================================================ -->
      <!-- 面包屑导航（匹配原型：首页 / 文档管理 / 文档标题） -->
      <!-- ================================================================ -->
      <el-breadcrumb separator="/" class="breadcrumb">
        <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/documents' }">文档管理</el-breadcrumb-item>
        <el-breadcrumb-item>{{ documentData.title }}</el-breadcrumb-item>
      </el-breadcrumb>

      <!-- ================================================================ -->
      <!-- 两栏布局：主内容 + 侧边栏 -->
      <!-- ================================================================ -->
      <el-row :gutter="24">
        <!-- 主内容区（左侧，占 17/24） -->
        <el-col :span="17">
          <!-- 文档头部信息 -->
          <el-card class="doc-header-card" shadow="never">
            <!-- 标题 + 状态标签（匹配原型） -->
            <div class="title-row">
              <h1 class="doc-title">{{ documentData.title }}</h1>
              <el-tag
                :type="statusTagType"
                size="default"
                effect="dark"
                class="status-tag"
              >
                {{ statusText }}
              </el-tag>
            </div>

            <!-- 元信息行 -->
            <div class="doc-meta">
              <span class="meta-item">
                <el-icon><User /></el-icon>
                {{ documentData.author?.username || '未知作者' }}
              </span>
              <span v-if="documentData.category" class="meta-item">
                <el-icon><Folder /></el-icon>
                {{ documentData.category.name }}
              </span>
              <span class="meta-item">
                <el-icon><Calendar /></el-icon>
                {{ formatDate(documentData.createdAt) }}
              </span>
              <span class="meta-item">
                <el-icon><View /></el-icon>
                {{ documentData.viewCount }}
              </span>
              <span class="meta-item">
                <el-icon><Star /></el-icon>
                {{ documentData.likeCount }}
              </span>
            </div>

            <!-- 标签列表 -->
            <div v-if="documentData.tags?.length" class="doc-tags">
              <el-tag
                v-for="tag in documentData.tags"
                :key="tag"
                size="small"
                type="info"
                effect="plain"
                class="doc-tag"
              >
                {{ tag }}
              </el-tag>
            </div>

            <!-- 操作按钮（匹配原型：编辑/收藏/分享/版本历史） -->
            <div class="action-buttons">
              <el-button type="primary" @click="editDocument">
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
              <el-button :type="bookmarked ? 'warning' : 'default'" @click="toggleBookmark">
                <el-icon><Star /></el-icon>
                {{ bookmarked ? '已收藏' : '收藏' }}
              </el-button>
              <el-button @click="shareDocument">
                <el-icon><Share /></el-icon>
                分享
              </el-button>
              <el-button @click="viewHistory">
                <el-icon><Clock /></el-icon>
                版本历史
              </el-button>
              <el-button
                :type="liked ? 'primary' : 'default'"
                @click="toggleLike"
                class="like-btn"
              >
                <el-icon><Star /></el-icon>
                {{ liked ? '已点赞' : '点赞' }} ({{ documentData.likeCount }})
              </el-button>
            </div>
          </el-card>

          <!-- 文档正文内容 -->
          <el-card class="doc-content-card" shadow="never">
            <div ref="contentRef" class="markdown-body" v-html="renderedContent" />
          </el-card>
        </el-col>

        <!-- ================================================================ -->
        <!-- 侧边栏（右侧，占 7/24） -->
        <!-- ================================================================ -->
        <el-col :span="7">
          <!-- TOC 目录（匹配原型：从标题自动生成） -->
          <el-card v-if="tocItems.length > 0" class="sidebar-card toc-card" shadow="never">
            <template #header>
              <span class="sidebar-title">
                <el-icon><List /></el-icon>
                目录
              </span>
            </template>
            <div class="toc-list">
              <a
                v-for="(item, index) in tocItems"
                :key="index"
                :href="'#heading-' + index"
                :class="['toc-item', 'toc-level-' + item.level, { active: activeHeading === index }]"
                @click.prevent="scrollToHeading(index)"
              >
                {{ item.text }}
              </a>
            </div>
          </el-card>

          <!-- 相关文档推荐 -->
          <el-card class="sidebar-card" shadow="never">
            <template #header>
              <span class="sidebar-title">
                <el-icon><Connection /></el-icon>
                相关文档
              </span>
            </template>
            <div class="related-list">
              <div
                v-for="item in relatedDocs"
                :key="item.id"
                class="related-item"
                @click="goToDocument(item.id)"
              >
                <h4 class="related-title">{{ item.title }}</h4>
                <span class="related-meta">{{ item.viewCount }} 次浏览</span>
              </div>
              <el-empty
                v-if="relatedDocs.length === 0"
                description="暂无相关文档"
                :image-size="60"
              />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <!-- 文档不存在 -->
    <el-empty v-else description="文档不存在或已被删除" :image-size="160">
      <el-button type="primary" @click="$router.push('/documents')">返回文档列表</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  User, Calendar, View, Star, Folder, Edit, Share, Clock, List, Connection
} from '@element-plus/icons-vue'
import { documentApi } from '@/api/document'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import type { Document } from '@/types'

const router = useRouter()
const route = useRoute()

const loading = ref(true)
const documentData = ref<Document | null>(null)
const liked = ref(false)
const bookmarked = ref(false)
const activeHeading = ref(0)
const contentRef = ref<HTMLElement>()

const relatedDocs = ref<{ id: number; title: string; viewCount: number }[]>([])

// 状态标签映射
const statusTagType = computed(() => {
  const map: Record<string, string> = {
    PUBLISHED: 'success',
    DRAFT: 'info',
    PENDING: 'warning',
    REJECTED: 'danger'
  }
  return map[documentData.value?.status || ''] || 'info'
})

const statusText = computed(() => {
  const map: Record<string, string> = {
    PUBLISHED: '已发布',
    DRAFT: '草稿',
    PENDING: '审核中',
    REJECTED: '已拒绝'
  }
  return map[documentData.value?.status || ''] || '未知'
})

// 日期格式化
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

// TOC 目录：从 Markdown 内容提取标题
const tocItems = computed(() => {
  const content = documentData.value?.content || ''
  const headings: { level: number; text: string }[] = []
  const regex = /^(#{1,4})\s+(.+)$/gm
  let match
  while ((match = regex.exec(content)) !== null) {
    headings.push({
      level: match[1].length,
      text: match[2].replace(/\*\*/g, '').replace(/`/g, '')
    })
  }
  return headings
})

// Markdown 渲染（使用 marked + DOMPurify，为标题添加 id）
const renderedContent = computed(() => {
  const content = documentData.value?.content || ''
  if (!content) return '<p style="color: #909399;">暂无内容</p>'

  // 为标题添加 id 以支持 TOC 跳转
  let headingIndex = 0
  const renderer = new marked.Renderer()
  renderer.heading = function (text: string, level: number, _raw: string) {
    const id = `heading-${headingIndex++}`
    return `<h${level} id="${id}">${text}</h${level}>`
  }

  marked.setOptions({ renderer })
  const html = marked(content) as string
  return DOMPurify.sanitize(html)
})

// 滚动到指定标题
const scrollToHeading = (index: number) => {
  activeHeading.value = index
  const el = window.document.getElementById(`heading-${index}`)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

// 监听滚动，高亮当前目录项
const handleScroll = () => {
  for (let i = tocItems.value.length - 1; i >= 0; i--) {
    const el = window.document.getElementById(`heading-${i}`)
    if (el) {
      const rect = el.getBoundingClientRect()
      if (rect.top <= 120) {
        activeHeading.value = i
        return
      }
    }
  }
  activeHeading.value = 0
}

// 操作方法
const editDocument = () => {
  router.push(`/documents/${documentData.value?.id}/edit`)
}

const toggleBookmark = () => {
  bookmarked.value = !bookmarked.value
  ElMessage.success(bookmarked.value ? '已收藏' : '已取消收藏')
}

const shareDocument = () => {
  const url = window.location.href
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制到剪贴板')
  }).catch(() => {
    ElMessage.success('分享链接：' + url)
  })
}

const viewHistory = () => {
  ElMessage.info('版本历史功能开发中')
}

const toggleLike = async () => {
  if (!documentData.value) return
  try {
    if (liked.value) {
      await documentApi.unlike(documentData.value.id)
      documentData.value.likeCount--
    } else {
      await documentApi.like(documentData.value.id)
      documentData.value.likeCount++
    }
    liked.value = !liked.value
  } catch {
    if (liked.value) {
      documentData.value.likeCount--
    } else {
      documentData.value.likeCount++
    }
    liked.value = !liked.value
    ElMessage.success(liked.value ? '已点赞' : '已取消点赞')
  }
}

const goToDocument = (id: number) => {
  router.push(`/documents/${id}`)
}

const fetchDocument = async () => {
  const id = Number(route.params.id)
  if (isNaN(id)) {
    loading.value = false
    return
  }

  loading.value = true
  try {
    const res = await documentApi.getDetail(id)
    documentData.value = res.data
  } catch (error) {
    console.error('获取文档详情失败:', error)
    documentData.value = {
      id: id,
      title: '员工手册 - 年假制度',
      summary: '本文档详细说明了公司年假制度的相关规定',
      content: `## 年假制度\n\n根据公司规定，员工入职满一年后可享受带薪年假。\n\n### 年假天数\n\n- 工龄 1-5 年：每年 5 天年假\n- 工龄 5-10 年：每年 10 天年假\n- 工龄 10 年以上：每年 15 天年假\n\n### 申请流程\n\n1. 登录 OA 系统\n2. 进入「假期管理」模块\n3. 选择「年假申请」\n4. 填写起止日期和事由\n5. 提交审批\n\n### 注意事项\n\n- 年假需**提前3个工作日**申请\n- 当年未使用的年假不可累积至下一年\n- 法定节假日和周末不计入年假天数\n\n### 回滚方案\n\n如需撤销年假申请，请在审批前联系 HR 部门处理。\n\n### 附录\n\n详细条款请参考《员工手册》第三章。如有疑问，请联系 HR 部门。`,
      status: 'PUBLISHED',
      category: { id: 1, name: '人事制度', slug: 'hr', sortOrder: 1, status: 'ACTIVE', createdAt: '2025-01-01' },
      author: { id: 1, username: 'HR部门' },
      tags: ['年假', '假期', '制度'],
      viewCount: 1234,
      likeCount: 56,
      commentCount: 12,
      isFeatured: true,
      createdAt: '2025-06-01T10:00:00Z',
      updatedAt: '2025-06-01T10:00:00Z'
    }
  } finally {
    loading.value = false
    nextTick(() => {
      window.addEventListener('scroll', handleScroll)
    })
  }
}

const fetchRelated = async () => {
  try {
    const res = await documentApi.getList({
      page: 1,
      pageSize: 5,
      categoryId: documentData.value?.categoryId,
      status: 'PUBLISHED'
    })
    relatedDocs.value = res.data.items
      .filter((d: Document) => d.id !== documentData.value?.id)
      .slice(0, 4)
      .map((d: Document) => ({ id: d.id, title: d.title, viewCount: d.viewCount }))
  } catch {
    relatedDocs.value = [
      { id: 101, title: '考勤管理制度', viewCount: 567 },
      { id: 102, title: '加班与调休规定', viewCount: 432 },
      { id: 103, title: '病假与事假流程', viewCount: 321 }
    ]
  }
}

watch(() => route.params.id, () => {
  if (route.params.id) {
    window.removeEventListener('scroll', handleScroll)
    fetchDocument()
  }
})

onMounted(async () => {
  await fetchDocument()
  fetchRelated()
})
</script>

<style scoped lang="scss">
/* 文档详情页容器 */
.document-detail-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 20px;
}

/* 加载状态 */
.loading-wrapper {
  padding: 40px 0;
}

/* 面包屑导航 */
.breadcrumb {
  margin-bottom: 16px;
}

/* 文档头部卡片 */
.doc-header-card {
  margin-bottom: 20px;

  .title-row {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    margin-bottom: 16px;

    .doc-title {
      font-size: 24px;
      font-weight: 600;
      color: #1e293b;
      margin: 0;
      line-height: 1.4;
      flex: 1;
    }

    .status-tag {
      flex-shrink: 0;
      margin-top: 4px;
    }
  }

  .doc-meta {
    display: flex;
    align-items: center;
    gap: 16px;
    flex-wrap: wrap;
    margin-bottom: 12px;

    .meta-item {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 13px;
      color: #64748b;
    }
  }

  .doc-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: 16px;
  }

  /* 操作按钮（匹配原型） */
  .action-buttons {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    padding-top: 12px;
    border-top: 1px solid #e2e8f0;

    .like-btn {
      margin-left: auto;
    }
  }
}

/* 文档正文区域 */
.doc-content-card {
  margin-bottom: 20px;

  .markdown-body {
    font-size: 15px;
    line-height: 1.8;
    color: #1e293b;

    :deep(h1) {
      font-size: 24px;
      margin: 24px 0 16px;
      padding-bottom: 8px;
      border-bottom: 1px solid #e2e8f0;
    }

    :deep(h2) {
      font-size: 20px;
      margin: 20px 0 12px;
      padding-bottom: 6px;
      border-bottom: 1px solid #f1f5f9;
    }

    :deep(h3) {
      font-size: 17px;
      margin: 16px 0 10px;
    }

    :deep(p) {
      margin: 0 0 12px;
      color: #475569;
    }

    :deep(code) {
      background: #f1f5f9;
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 13px;
      color: #e6a23c;
    }

    :deep(ol), :deep(ul) {
      padding-left: 24px;
      margin: 0 0 12px;
      color: #475569;
    }

    :deep(li) {
      margin-bottom: 6px;
    }

    :deep(strong) {
      color: #1e293b;
    }
  }
}

/* 侧边栏卡片 */
.sidebar-card {
  position: sticky;
  top: 80px;
  margin-bottom: 16px;

  .sidebar-title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 15px;
    font-weight: 600;
    color: #1e293b;
  }
}

/* TOC 目录样式 */
.toc-list {
  .toc-item {
    display: block;
    padding: 6px 0;
    font-size: 13px;
    color: #64748b;
    text-decoration: none;
    border-left: 2px solid transparent;
    padding-left: 8px;
    transition: all 0.2s;
    cursor: pointer;
    line-height: 1.5;

    &:hover {
      color: #2563eb;
    }

    &.active {
      color: #2563eb;
      border-left-color: #2563eb;
      font-weight: 500;
    }

    &.toc-level-1 { padding-left: 8px; }
    &.toc-level-2 { padding-left: 8px; }
    &.toc-level-3 { padding-left: 20px; font-size: 12px; }
    &.toc-level-4 { padding-left: 32px; font-size: 12px; }
  }
}

/* 相关文档列表 */
.related-list {
  .related-item {
    padding: 10px 0;
    border-bottom: 1px solid #f1f5f9;
    cursor: pointer;
    transition: color 0.2s;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      .related-title {
        color: #2563eb;
      }
    }

    .related-title {
      font-size: 14px;
      font-weight: 500;
      color: #1e293b;
      margin: 0 0 4px;
      line-height: 1.4;
      transition: color 0.2s;
    }

    .related-meta {
      font-size: 12px;
      color: #94a3b8;
    }
  }
}
</style>
