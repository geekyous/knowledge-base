<!--
================================================================================
文档列表页 - views/document/DocumentList.vue
================================================================================

【文件说明】
这是文档列表页面，展示知识库中的所有文档。
功能包括：
1. 精选文档展示（置顶推荐）
2. 分类筛选、状态筛选、关键词搜索
3. 文档卡片列表（显示标题、摘要、状态标签、元信息）
4. 分页导航（支持切换每页条数）
5. 空状态提示

【Vue 概念】
- reactive() 创建筛选条件对象
- el-select 的 @change 事件：筛选条件变化时重新获取数据
- v-model:current-page 和 v-model:page-size 双向绑定分页参数
- el-pagination 的完整功能：total、sizes、jumper 等
- Mock 数据兜底方案

💡 学习要点:
1. 筛选条件使用 reactive() 管理，所有字段都是可选类型
2. el-pagination 的 layout 属性控制分页组件显示哪些功能
3. @size-change 和 @current-change 分别处理页码和每页条数变化
4. Record<string, string> 类型用于键值映射表
5. 页面加载时从 route.query 恢复筛选条件
================================================================================
-->

<template>
  <div class="document-list-page">
    <!-- ================================================================== -->
    <!-- 精选文档区域 -->
    <!-- ================================================================== -->
    <!-- v-if 条件渲染：只有精选文档不为空时才显示 -->
    <div class="featured-section" v-if="featuredDocs.length > 0">
      <div class="section-header">
        <h2 class="section-title">
          <el-icon><Star /></el-icon>
          精选文档
        </h2>
      </div>
      <!-- CSS Grid 网格布局展示精选文档卡片 -->
      <div class="featured-grid">
        <el-card
          v-for="doc in featuredDocs"
          :key="doc.id"
          class="featured-card"
          shadow="hover"
          @click="goToDocument(doc.id)"
        >
          <div class="featured-tag">
            <el-tag type="warning" size="small" effect="dark">精选</el-tag>
          </div>
          <h3 class="featured-title">{{ doc.title }}</h3>
          <p class="featured-summary">{{ doc.summary }}</p>
          <div class="featured-meta">
            <!-- 可选链 ?. 安全访问嵌套属性 -->
            <span v-if="doc.category">
              <el-tag size="small" effect="plain">{{ doc.category.name }}</el-tag>
            </span>
            <span class="meta-item">
              <el-icon><View /></el-icon>
              {{ doc.viewCount }}
            </span>
          </div>
        </el-card>
      </div>
    </div>

    <!-- ================================================================== -->
    <!-- 操作栏 + 筛选区域（匹配原型：新建/上传按钮 + 筛选下拉 + 排序） -->
    <!-- ================================================================== -->
    <div class="filter-section">
      <div class="filter-bar">
        <!-- 左侧：操作按钮 -->
        <div class="filter-actions">
          <el-button type="primary" @click="createDocument">
            <el-icon><Plus /></el-icon>
            新建文档
          </el-button>
          <el-button @click="uploadDialogVisible = true">
            <el-icon><Upload /></el-icon>
            上传文件
          </el-button>
        </div>

        <!-- 右侧：筛选 + 搜索 + 排序 -->
        <div class="filter-controls">
          <el-select
            v-model="filters.categoryId"
            placeholder="全部分类"
            clearable
            @change="fetchDocuments"
          >
            <el-option
              v-for="cat in categoryOptions"
              :key="cat.value"
              :label="cat.label"
              :value="cat.value"
            />
          </el-select>

          <el-select
            v-model="filters.status"
            placeholder="全部状态"
            clearable
            @change="fetchDocuments"
          >
            <el-option label="草稿" value="DRAFT" />
            <el-option label="待审核" value="PENDING" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>

          <!-- 排序下拉（匹配原型：最新更新/最多浏览/最多点赞） -->
          <el-select
            v-model="sortBy"
            @change="fetchDocuments"
            style="width: 130px"
          >
            <el-option label="最新更新" value="updatedAt" />
            <el-option label="最多浏览" value="viewCount" />
            <el-option label="最多点赞" value="likeCount" />
          </el-select>

          <el-input
            v-model="filters.keyword"
            placeholder="搜索文档..."
            clearable
            class="filter-search"
            @keyup.enter="fetchDocuments"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </div>
    </div>

    <!-- ================================================================== -->
    <!-- 文档列表 -->
    <!-- ================================================================== -->
    <div class="document-list" v-if="documents.length > 0">
      <el-card
        v-for="doc in documents"
        :key="doc.id"
        class="document-card"
        shadow="hover"
        @click="goToDocument(doc.id)"
      >
        <div class="doc-main">
          <div class="doc-info">
            <div class="doc-title-row">
              <h3 class="doc-title">{{ doc.title }}</h3>
              <!-- 状态标签：动态类型和文本 -->
              <el-tag
                :type="statusTagType(doc.status)"
                size="small"
                effect="plain"
              >
                {{ statusLabel(doc.status) }}
              </el-tag>
              <!-- 操作按钮（匹配原型：编辑/删除，阻止冒泡） -->
              <div class="doc-actions" @click.stop>
                <el-button text size="small" @click="editDocument(doc.id)">
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button text size="small" class="delete-btn" @click="deleteDocument(doc)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
            <p class="doc-summary">{{ doc.summary || '暂无摘要' }}</p>
            <div class="doc-meta">
              <el-tag
                v-if="doc.category"
                size="small"
                effect="plain"
              >
                {{ doc.category.name }}
              </el-tag>
              <span class="meta-item">
                <el-icon><User /></el-icon>
                {{ doc.author?.username || '未知' }}
              </span>
              <span class="meta-item">
                <el-icon><View /></el-icon>
                {{ doc.viewCount }}
              </span>
              <span class="meta-item">
                <el-icon><ChatDotRound /></el-icon>
                {{ doc.commentCount }}
              </span>
              <span class="meta-item">
                <el-icon><Calendar /></el-icon>
                {{ formatDate(doc.createdAt) }}
              </span>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 空状态 -->
    <!-- v-else-if="!loading" 条件：无文档且不在加载中时显示 -->
    <el-empty v-else-if="!loading" description="暂无文档">
      <el-button type="primary" @click="resetFilters">重置筛选</el-button>
    </el-empty>

    <!-- ================================================================== -->
    <!-- 分页 -->
    <!-- ================================================================== -->
    <!--
      layout="total, sizes, prev, pager, next, jumper" 显示：
      - total: 总记录数
      - sizes: 每页条数切换
      - prev/next: 上一页/下一页按钮
      - pager: 页码
      - jumper: 跳转到指定页
    -->
    <div class="pagination-wrapper" v-if="documents.length > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 30, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchDocuments"
        @current-change="fetchDocuments"
      />
    </div>

    <!-- ================================================================== -->
    <!-- 文件上传弹窗 -->
    <!-- ================================================================== -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传文件"
      width="680px"
      destroy-on-close
    >
      <FileUpload />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
// 导入 Vue API
import { ref, reactive, onMounted } from 'vue'

// 导入路由 API
import { useRouter, useRoute } from 'vue-router'

// 导入图标
import {
  Search,
  Star,
  View,
  User,
  ChatDotRound,
  Calendar,
  Plus,
  Upload,
  Edit,
  Delete
} from '@element-plus/icons-vue'

// 导入文档 API
import { documentApi } from '@/api/document'

// 导入消息提示和确认框
import { ElMessage, ElMessageBox } from 'element-plus'

// 导入文件上传组件
import FileUpload from '@/components/common/FileUpload.vue'

// 导入文档类型
import type { Document } from '@/types'

// 路由实例
const router = useRouter()
const route = useRoute()

// ==================== 响应式状态 ====================

/** 加载状态 */
const loading = ref(false)

/** 文档列表数据 */
const documents = ref<Document[]>([])

/** 精选文档列表 */
const featuredDocs = ref<Document[]>([])

/** 总记录数（用于分页计算） */
const total = ref(0)

/** 当前页码 */
const currentPage = ref(1)

/** 每页条数 */
const pageSize = ref(10)

/**
 * 筛选条件
 *
 * 使用 reactive() 创建响应式对象。
 * 每个字段都用 as 明确类型，确保 undefined 值的类型安全。
 * 当筛选条件变化时（@change），会调用 fetchDocuments 重新获取数据。
 */
const filters = reactive({
  categoryId: undefined as number | undefined,
  status: undefined as string | undefined,
  keyword: ''
})

/** 排序方式（匹配原型：最新更新/最多浏览/最多点赞） */
const sortBy = ref('updatedAt')

/** 文件上传弹窗可见性 */
const uploadDialogVisible = ref(false)

/** 分类选项（静态数据） */
const categoryOptions = [
  { label: '人事制度', value: 1 },
  { label: '技术文档', value: 2 },
  { label: '销售支持', value: 3 },
  { label: '合规法务', value: 4 }
]

/**
 * 获取状态对应的标签类型
 *
 * Record<string, string> 类型定义键值映射表。
 * 不同状态显示不同颜色的标签：
 * - DRAFT(草稿) → info(灰色)
 * - PENDING(待审核) → warning(黄色)
 * - PUBLISHED(已发布) → success(绿色)
 * - ARCHIVED(已归档) → danger(红色)
 */
const statusTagType = (status: string) => {
  const map: Record<string, string> = {
    DRAFT: 'info',
    PENDING: 'warning',
    PUBLISHED: 'success',
    ARCHIVED: 'danger'
  }
  return map[status] || 'info'
}

/** 获取状态对应的中文标签 */
const statusLabel = (status: string) => {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PENDING: '待审核',
    PUBLISHED: '已发布',
    ARCHIVED: '已归档'
  }
  return map[status] || status
}

/** 日期格式化 */
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

/**
 * 获取文档列表
 *
 * 根据筛选条件和分页参数请求文档数据。
 * 使用 try/catch + Mock 数据兜底方案。
 */
const fetchDocuments = async () => {
  loading.value = true
  try {
    const res = await documentApi.getList({
      page: currentPage.value,
      pageSize: pageSize.value,
      categoryId: filters.categoryId,
      status: filters.status,
      keyword: filters.keyword || undefined
    })
    documents.value = res.data.items
    total.value = res.data.total
  } catch (error) {
    console.error('获取文档列表失败:', error)
    // Fallback mock data：API 失败时的模拟数据
    documents.value = [
      {
        id: 1,
        title: '员工手册 (2025版)',
        summary: '本手册涵盖公司各项规章制度，包括考勤、假期、薪酬福利等内容。',
        status: 'PUBLISHED',
        category: { id: 1, name: '人事制度', slug: 'hr', sortOrder: 1, status: 'ACTIVE', createdAt: '2025-01-01' },
        author: { id: 1, username: 'HR部门' },
        tags: ['手册', '制度'],
        viewCount: 1234,
        likeCount: 56,
        commentCount: 12,
        isFeatured: true,
        createdAt: '2025-06-01',
        updatedAt: '2025-06-01'
      },
      {
        id: 2,
        title: '前端开发规范 V3.0',
        summary: '统一前端开发标准，包括代码风格、组件库使用、项目架构等规范。',
        status: 'PUBLISHED',
        category: { id: 2, name: '技术文档', slug: 'tech', sortOrder: 2, status: 'ACTIVE', createdAt: '2025-01-01' },
        author: { id: 2, username: '技术部' },
        tags: ['前端', '规范'],
        viewCount: 856,
        likeCount: 34,
        commentCount: 8,
        isFeatured: false,
        createdAt: '2025-05-15',
        updatedAt: '2025-05-20'
      },
      {
        id: 3,
        title: '差旅报销流程 (修订中)',
        summary: '更新中...包含差旅申请、费用报销、审批流程等内容。',
        status: 'DRAFT',
        category: { id: 3, name: '销售支持', slug: 'sales', sortOrder: 3, status: 'ACTIVE', createdAt: '2025-01-01' },
        author: { id: 3, username: '财务部' },
        tags: ['报销', '流程'],
        viewCount: 432,
        likeCount: 12,
        commentCount: 3,
        isFeatured: false,
        createdAt: '2025-04-10',
        updatedAt: '2025-05-28'
      },
      {
        id: 4,
        title: '数据安全管理制度',
        summary: '为保障公司数据安全，制定数据分级、访问控制、加密存储等管理要求。',
        status: 'PENDING',
        category: { id: 4, name: '合规法务', slug: 'legal', sortOrder: 4, status: 'ACTIVE', createdAt: '2025-01-01' },
        author: { id: 4, username: '安全部' },
        tags: ['安全', '合规'],
        viewCount: 267,
        likeCount: 8,
        commentCount: 2,
        isFeatured: false,
        createdAt: '2025-03-22',
        updatedAt: '2025-05-18'
      }
    ]
    total.value = documents.value.length
  } finally {
    loading.value = false
  }
}

/** 获取精选文档 */
const fetchFeatured = async () => {
  try {
    const res = await documentApi.getList({
      page: 1,
      pageSize: 3,
      status: 'PUBLISHED'
    })
    featuredDocs.value = res.data.items.filter((d: Document) => d.isFeatured)
  } catch {
    featuredDocs.value = [
      {
        id: 100,
        title: '新员工入职指南',
        summary: '欢迎加入！这份指南将帮助你快速了解公司文化、办公环境、系统账号等基本信息。',
        status: 'PUBLISHED',
        category: { id: 1, name: '人事制度', slug: 'hr', sortOrder: 1, status: 'ACTIVE', createdAt: '2025-01-01' },
        author: { id: 1, username: 'HR部门' },
        tags: ['入职', '指南'],
        viewCount: 2380,
        likeCount: 120,
        commentCount: 28,
        isFeatured: true,
        createdAt: '2025-01-10',
        updatedAt: '2025-05-30'
      }
    ]
  }
}

/** 跳转到文档详情 */
const goToDocument = (id: number) => {
  router.push(`/documents/${id}`)
}

/** 新建文档 */
const createDocument = () => {
  router.push('/documents/new/edit')
}

/** 编辑文档 */
const editDocument = (id: number) => {
  router.push(`/documents/${id}/edit`)
}

/** 删除文档（确认弹窗） */
const deleteDocument = async (doc: Document) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文档「${doc.title}」吗？此操作不可恢复。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await documentApi.delete(doc.id)
    ElMessage.success('文档已删除')
    fetchDocuments()
  } catch {
    // 用户取消或删除失败
  }
}

/** 重置所有筛选条件 */
const resetFilters = () => {
  filters.categoryId = undefined
  filters.status = undefined
  filters.keyword = ''
  currentPage.value = 1
  fetchDocuments()
}

/**
 * 页面加载时初始化
 *
 * 1. 从 URL 查询参数恢复筛选条件（如从首页点击分类跳转过来）
 * 2. 获取精选文档和文档列表
 */
onMounted(() => {
  // 从 URL 恢复分类筛选（如 /documents?categoryId=1）
  if (route.query.categoryId) {
    filters.categoryId = Number(route.query.categoryId)
  }
  fetchFeatured()
  fetchDocuments()
})
</script>

<style scoped lang="scss">
/* 文档列表页容器 */
.document-list-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 20px;
}

/* 精选文档区域 */
.featured-section {
  margin-bottom: 32px;

  .section-header {
    margin-bottom: 16px;

    .section-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 20px;
      font-weight: 600;
      color: #303133;
    }
  }

  /* 精选卡片网格 */
  .featured-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 16px;
  }

  .featured-card {
    cursor: pointer;
    transition: all 0.3s;
    position: relative;

    &:hover {
      transform: translateY(-3px);
      border-color: #409eff;
    }

    .featured-tag {
      margin-bottom: 8px;
    }

    .featured-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin: 0 0 8px;
    }

    /* 摘要两行截断 */
    .featured-summary {
      font-size: 13px;
      color: #606266;
      line-height: 1.5;
      margin: 0 0 12px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .featured-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 12px;
      color: #909399;

      .meta-item {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }
}

/* 筛选区域 */
.filter-section {
  margin-bottom: 20px;

  .filter-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;

    .filter-actions {
      display: flex;
      gap: 8px;
    }

    .filter-controls {
      display: flex;
      gap: 12px;
      align-items: center;

      .filter-search {
        max-width: 240px;
      }
    }
  }
}

/* 文档列表 */
.document-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 文档卡片 */
.document-card {
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    border-color: #409eff;
  }

  .doc-main {
    .doc-info {
      /* 标题行：标题、状态标签和操作按钮 */
      .doc-title-row {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 8px;

        .doc-title {
          font-size: 16px;
          font-weight: 600;
          color: #303133;
          margin: 0;
          flex: 1;
        }

        .doc-actions {
          display: flex;
          gap: 0;
          opacity: 0;
          transition: opacity 0.2s;

          .delete-btn {
            color: #ef4444;
          }
        }
      }

      &:hover .doc-actions {
        opacity: 1;
      }

      /* 摘要两行截断 */
      .doc-summary {
        font-size: 14px;
        color: #606266;
        line-height: 1.5;
        margin: 0 0 12px;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      /* 元信息 */
      .doc-meta {
        display: flex;
        align-items: center;
        gap: 16px;
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
}

/* 分页居中 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}
</style>
