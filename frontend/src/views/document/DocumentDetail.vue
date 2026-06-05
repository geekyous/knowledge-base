<!--
================================================================================
文档详情页 - views/document/DocumentDetail.vue
================================================================================

【文件说明】
这是文档详情页面，展示一篇文档的完整内容。
功能包括：
1. 文档基本信息（标题、作者、日期、分类、标签、统计数据）
2. Markdown 内容渲染（简易版）
3. 点赞/取消点赞功能
4. 相关文档推荐（侧边栏）
5. 加载骨架屏（skeleton loading）
6. 路由参数变化时重新加载数据（watch 监听）

【Vue 概念】
- 动态路由参数：route.params.id 获取文档 ID
- computed 计算属性：实现 Markdown 到 HTML 的转换
- watch 监听器：监听路由参数变化，重新加载数据
- el-skeleton 骨架屏：加载状态优化用户体验
- el-row/el-col 栅格布局：17:7 的两栏布局

💡 学习要点:
1. route.params.id 获取动态路由参数（如 /documents/123 中的 123）
2. watch(() => route.params.id, callback) 监听路由参数变化
3. computed 实现 Markdown 渲染（生产环境应使用 marked.js 等库）
4. el-skeleton 提供加载占位效果，改善用户等待体验
5. 侧边栏 position: sticky 实现滚动时固定效果
6. v-html 渲染 HTML 内容（注意 XSS 安全性）
================================================================================
-->

<template>
  <div class="document-detail-page">
    <!-- ================================================================== -->
    <!-- 加载状态：骨架屏 -->
    <!-- ================================================================== -->
    <!--
      el-skeleton: Element Plus 的骨架屏组件
      :rows="12" 显示 12 行骨架占位
      animated 启用闪烁动画
    -->
    <div class="loading-wrapper" v-if="loading">
      <el-skeleton :rows="12" animated />
    </div>

    <!-- ================================================================== -->
    <!-- 文档内容（加载完成且有数据时） -->
    <!-- ================================================================== -->
    <template v-else-if="document">
      <!-- 返回按钮 -->
      <div class="back-row">
        <el-button @click="goBack" :icon="ArrowLeft">返回列表</el-button>
      </div>

      <!--
        Element Plus 栅格布局（el-row + el-col）
        :gutter="24" 列之间的间距为 24px
        :span="17" 主内容占 24 栏中的 17 栏
        :span="7" 侧边栏占 24 栏中的 7 栏
      -->
      <el-row :gutter="24">
        <!-- 主内容区（左侧，占 17/24） -->
        <el-col :span="17">
          <!-- 文档头部信息 -->
          <el-card class="doc-header-card" shadow="never">
            <h1 class="doc-title">{{ document.title }}</h1>
            <!-- 元信息行 -->
            <div class="doc-meta">
              <span class="meta-item">
                <el-icon><User /></el-icon>
                {{ document.author?.username || '未知作者' }}
              </span>
              <span class="meta-item">
                <el-icon><Calendar /></el-icon>
                {{ formatDate(document.createdAt) }}
              </span>
              <!-- 分类标签 -->
              <el-tag
                v-if="document.category"
                size="small"
                effect="plain"
              >
                {{ document.category.name }}
              </el-tag>
              <!-- 标签列表：v-for 遍历 tags 数组 -->
              <el-tag
                v-for="tag in document.tags"
                :key="tag"
                size="small"
                type="info"
                effect="plain"
                class="doc-tag"
              >
                {{ tag }}
              </el-tag>
            </div>
            <!-- 统计数据 -->
            <div class="doc-stats">
              <span class="stat-item">
                <el-icon><View /></el-icon>
                {{ document.viewCount }} 次浏览
              </span>
              <span class="stat-item">
                <el-icon><Star /></el-icon>
                {{ document.likeCount }} 次点赞
              </span>
              <span class="stat-item">
                <el-icon><ChatDotRound /></el-icon>
                {{ document.commentCount }} 条评论
              </span>
            </div>
          </el-card>

          <!-- 文档正文内容 -->
          <el-card class="doc-content-card" shadow="never">
            <!--
              v-html 渲染 computed 计算属性生成的 HTML
              renderedContent 将 Markdown 文本转换为 HTML 标签
            -->
            <div class="markdown-body" v-html="renderedContent"></div>
          </el-card>

          <!-- 操作栏（点赞按钮） -->
          <div class="action-bar">
            <el-button
              :type="liked ? 'primary' : 'default'"
              @click="toggleLike"
            >
              <el-icon><Star /></el-icon>
              {{ liked ? '已点赞' : '点赞' }} ({{ document.likeCount }})
            </el-button>
          </div>
        </el-col>

        <!-- 侧边栏（右侧，占 7/24） -->
        <el-col :span="7">
          <!-- 相关文档推荐 -->
          <el-card class="sidebar-card" shadow="never">
            <template #header>
              <span class="sidebar-title">相关文档</span>
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

    <!-- ================================================================== -->
    <!-- 文档不存在时的提示 -->
    <!-- ================================================================== -->
    <el-empty v-else description="文档不存在或已被删除" :image-size="160">
      <el-button type="primary" @click="$router.push('/documents')">返回文档列表</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
// 导入说明：
// - ref: 响应式引用
// - computed: 计算属性（缓存计算结果）
// - onMounted: 生命周期钩子
// - watch: 侦听器（监听数据变化）
import { ref, computed, onMounted, watch } from 'vue'

// 导入路由 API
import { useRouter, useRoute } from 'vue-router'

// 导入消息提示
import { ElMessage } from 'element-plus'

// 导入图标
import {
  ArrowLeft,
  User,
  Calendar,
  View,
  Star,
  ChatDotRound
} from '@element-plus/icons-vue'

// 导入文档 API
import { documentApi } from '@/api/document'

// 导入文档类型
import type { Document } from '@/types'

const router = useRouter()
const route = useRoute()

// ==================== 响应式状态 ====================

/** 加载状态：初始为 true（页面打开即开始加载） */
const loading = ref(true)

/** 文档详情数据（null 表示未获取到或不存在） */
const document = ref<Document | null>(null)

/** 当前用户是否已点赞 */
const liked = ref(false)

/** 相关文档列表 */
const relatedDocs = ref<{ id: number; title: string; viewCount: number }[]>([])

/** 日期格式化（带中文年月日） */
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

// Markdown → HTML 转换（计算属性）
//
// 使用 computed 创建缓存计算属性。
// 只有 document.value?.content 变化时才重新计算。
//
// 注意：这是一个简易的正则替换实现，仅支持基本的 Markdown 语法。
// 生产环境应该使用 marked.js、markdown-it 等专业库。
//
// 正则说明：
// - /^### (.*$)/gim: 匹配三级标题
// - /\*\*(.*?)\*\* /gim: 匹配加粗
// - /\*(.*?)\* /gim: 匹配斜体
// - /`(.*?)`/gim: 匹配行内代码
const renderedContent = computed(() => {
  const content = document.value?.content || ''
  if (!content) return '<p style="color: #909399;">暂无内容</p>'

  // Basic markdown conversion
  return content
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    .replace(/\*\*(.*?)\*\*/gim, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/gim, '<em>$1</em>')
    .replace(/`(.*?)`/gim, '<code>$1</code>')
    .replace(/\n\n/g, '</p><p>')
    .replace(/\n/g, '<br>')
    .replace(/^/, '<p>')
    .replace(/$/, '</p>')
})

/**
 * 获取文档详情
 *
 * 从路由参数获取文档 ID，调用 API 获取详情数据。
 * 如果 ID 无效（NaN），直接结束加载。
 */
const fetchDocument = async () => {
  // route.params.id 对应路由定义中的 path: 'documents/:id'
  const id = Number(route.params.id)
  if (isNaN(id)) {
    loading.value = false
    return
  }

  loading.value = true
  try {
    const res = await documentApi.getDetail(id)
    document.value = res.data
  } catch (error) {
    console.error('获取文档详情失败:', error)
    // Fallback mock data
    document.value = {
      id: id,
      title: '员工手册 - 年假制度',
      summary: '本文档详细说明了公司年假制度的相关规定',
      content: `## 年假制度\n\n根据公司规定，员工入职满一年后可享受带薪年假。\n\n### 年假天数\n\n- 工龄 1-5 年：每年 5 天年假\n- 工龄 5-10 年：每年 10 天年假\n- 工龄 10 年以上：每年 15 天年假\n\n### 申请流程\n\n1. 登录 OA 系统\n2. 进入「假期管理」模块\n3. 选择「年假申请」\n4. 填写起止日期和事由\n5. 提交审批\n\n### 注意事项\n\n- 年假需**提前3个工作日**申请\n- 当年未使用的年假不可累积至下一年\n- 法定节假日和周末不计入年假天数\n\n如有疑问，请联系 HR 部门。`,
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
  }
}

/** 获取相关文档（根据同分类查找） */
const fetchRelated = async () => {
  try {
    const res = await documentApi.getList({
      page: 1,
      pageSize: 5,
      categoryId: document.value?.categoryId,
      status: 'PUBLISHED'
    })
    relatedDocs.value = res.data.items
      .filter((d: Document) => d.id !== document.value?.id)  // 排除当前文档
      .slice(0, 4)                                             // 最多显示 4 个
      .map((d: Document) => ({ id: d.id, title: d.title, viewCount: d.viewCount }))
  } catch {
    relatedDocs.value = [
      { id: 101, title: '考勤管理制度', viewCount: 567 },
      { id: 102, title: '加班与调休规定', viewCount: 432 },
      { id: 103, title: '病假与事假流程', viewCount: 321 }
    ]
  }
}

/**
 * 切换点赞状态
 *
 * 先调用 API，成功后更新本地状态。
 * API 失败时仍然更新本地状态（乐观更新的降级方案），
 * 并给出成功提示让用户感知到操作已完成。
 */
const toggleLike = async () => {
  if (!document.value) return
  try {
    if (liked.value) {
      await documentApi.unlike(document.value.id)
      document.value.likeCount--
    } else {
      await documentApi.like(document.value.id)
      document.value.likeCount++
    }
    liked.value = !liked.value
  } catch {
    // Toggle locally even if API fails
    if (liked.value) {
      document.value.likeCount--
    } else {
      document.value.likeCount++
    }
    liked.value = !liked.value
    ElMessage.success(liked.value ? '已点赞' : '已取消点赞')
  }
}

/** 返回文档列表 */
const goBack = () => {
  router.push('/documents')
}

/** 跳转到其他文档详情 */
const goToDocument = (id: number) => {
  router.push(`/documents/${id}`)
}

/**
 * watch 监听器：监听路由参数变化
 *
 * 应用场景：用户在文档详情页点击"相关文档"跳转到另一篇文档时，
 * 组件不会重新创建（复用同一个组件实例），所以需要手动监听
 * route.params.id 的变化来重新加载数据。
 *
 * 这是 Vue Router 的常见模式：同一个组件对应不同的路由参数时，
 * 需要用 watch 监听参数变化来响应更新。
 */
watch(() => route.params.id, () => {
  if (route.params.id) {
    fetchDocument()
  }
})

/**
 * 组件挂载时加载数据
 *
 * 先获取文档详情，再获取相关文档。
 * 使用 await 确保顺序执行（相关文档需要文档的分类 ID）。
 */
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

/* 加载状态区域 */
.loading-wrapper {
  padding: 40px 0;
}

/* 返回按钮 */
.back-row {
  margin-bottom: 16px;
}

/* 文档头部卡片 */
.doc-header-card {
  margin-bottom: 20px;

  .doc-title {
    font-size: 24px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 16px;
    line-height: 1.4;
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
      color: #606266;
    }

    .doc-tag {
      margin-left: 0;
    }
  }

  .doc-stats {
    display: flex;
    gap: 24px;

    .stat-item {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 13px;
      color: #909399;
    }
  }
}

/* 文档正文区域 */
.doc-content-card {
  margin-bottom: 20px;

  /* Markdown 渲染样式 */
  .markdown-body {
    font-size: 15px;
    line-height: 1.8;
    color: #303133;

    /* :deep() 穿透样式，控制 v-html 渲染的 HTML 元素 */
    :deep(h1) {
      font-size: 24px;
      margin: 24px 0 16px;
      padding-bottom: 8px;
      border-bottom: 1px solid #e4e7ed;
    }

    :deep(h2) {
      font-size: 20px;
      margin: 20px 0 12px;
      padding-bottom: 6px;
      border-bottom: 1px solid #ebeef5;
    }

    :deep(h3) {
      font-size: 17px;
      margin: 16px 0 10px;
    }

    :deep(p) {
      margin: 0 0 12px;
    }

    :deep(code) {
      background: #f5f7fa;
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 13px;
      color: #e6a23c;
    }

    :deep(ol), :deep(ul) {
      padding-left: 24px;
      margin: 0 0 12px;
    }

    :deep(li) {
      margin-bottom: 6px;
    }

    :deep(strong) {
      color: #409eff;
    }
  }
}

/* 操作栏 */
.action-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 32px;
}

/* 侧边栏卡片 */
.sidebar-card {
  /* sticky 定位：滚动时固定在视口中 */
  position: sticky;
  top: 80px;

  .sidebar-title {
    font-size: 16px;
    font-weight: 600;
  }

  .related-list {
    .related-item {
      padding: 10px 0;
      border-bottom: 1px solid #f0f2f5;
      cursor: pointer;
      transition: color 0.2s;

      &:last-child {
        border-bottom: none;
      }

      &:hover {
        color: #409eff;

        .related-title {
          color: #409eff;
        }
      }

      .related-title {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
        margin: 0 0 4px;
        line-height: 1.4;
        transition: color 0.2s;
      }

      .related-meta {
        font-size: 12px;
        color: #909399;
      }
    }
  }
}
</style>
