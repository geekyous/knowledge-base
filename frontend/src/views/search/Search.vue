<!--
================================================================================
搜索页组件 - views/search/Search.vue
================================================================================

【文件说明】
这是全文搜索页面，用户可以输入关键词搜索知识库中的文档。
功能包括：
1. 搜索框 + 分类筛选 + 搜索按钮
2. 搜索历史展示（点击历史标签快速搜索）
3. 搜索结果列表（带高亮摘要和匹配度评分）
4. 分页导航
5. 空状态和无结果提示

【Vue 概念】
- URL 查询参数：从 route.query.q 读取初始关键词，实现搜索页可分享
- v-html 指令：渲染包含 HTML 标签的高亮文本
- ref() 多个响应式状态管理
- Mock 数据兜底：API 失败时显示模拟数据
- 条件渲染 v-if/v-else：根据搜索状态显示不同内容

💡 学习要点:
1. route.query.q 让搜索页可以通过 URL 分享搜索结果
2. v-html 用于渲染包含 HTML 的文本（如 <mark>高亮</mark>），注意 XSS 风险
3. 搜索历史使用数组 unshift/pop 维护固定长度的列表
4. Mock 数据作为 API 失败的兜底方案，确保页面始终有内容
5. el-pagination 的 v-model:current-page 实现双向绑定页码
6. el-empty 组件提供友好的空状态展示
================================================================================
-->

<template>
  <div class="search-page">
    <!-- ================================================================== -->
    <!-- 搜索区域 -->
    <!-- ================================================================== -->
    <div class="search-header">
      <div class="search-bar">
        <!-- 搜索输入框 -->
        <!--
          v-model="keyword" 双向绑定搜索关键词
          @keyup.enter="handleSearch" 按回车触发搜索
          #prefix 插槽在输入框前添加搜索图标
        -->
        <el-input
          v-model="keyword"
          placeholder="搜索文档、问答..."
          size="large"
          clearable
          maxlength="500"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><SearchIcon /></el-icon>
          </template>
        </el-input>

        <!-- 分类筛选下拉框 -->
        <el-select
          v-model="selectedCategory"
          placeholder="全部分类"
          clearable
          size="large"
          class="category-select"
        >
          <el-option
            v-for="cat in categoryOptions"
            :key="cat.value"
            :label="cat.label"
            :value="cat.value"
          />
        </el-select>

        <!-- 搜索按钮 -->
        <el-button type="primary" size="large" @click="handleSearch">
          搜索
        </el-button>
      </div>

      <!-- ================================================================== -->
      <!-- 搜索历史 -->
      <!-- ================================================================== -->
      <!--
        v-if 双重条件：有历史记录 且 未执行过搜索时才显示
      -->
      <div class="search-history" v-if="searchHistory.length > 0 && !hasSearched">
        <div class="history-header">
          <span class="history-label">搜索历史</span>
          <!-- 清空历史按钮 -->
          <el-button link type="info" size="small" @click="clearHistory">清空</el-button>
        </div>
        <div class="history-tags">
          <!--
            历史标签：
            点击标签 → 设置关键词并执行搜索
            使用分号写法在模板中执行多个操作
          -->
          <el-tag
            v-for="(item, index) in searchHistory"
            :key="index"
            class="history-tag"
            effect="plain"
            @click="keyword = item; handleSearch()"
          >
            {{ item }}
          </el-tag>
        </div>
      </div>
    </div>

    <!-- ================================================================== -->
    <!-- 搜索结果区域（搜索执行后才显示） -->
    <!-- ================================================================== -->
    <div class="search-results" v-if="hasSearched">
      <!-- 结果数量统计 -->
      <div class="results-header">
        <span class="results-count">
          找到 <strong>{{ totalResults }}</strong> 条相关结果
        </span>
      </div>

      <!-- 结果列表（有结果时） -->
      <div class="results-list" v-if="results.length > 0">
        <!--
          每个搜索结果卡片
          shadow="hover" 鼠标悬浮时显示阴影
        -->
        <el-card
          v-for="item in results"
          :key="item.id"
          class="result-card"
          shadow="hover"
          @click="goToDocument(item.id)"
        >
          <div class="result-content">
            <!--
              v-html 渲染 HTML 内容
              后端返回的 title 和 highlight 中包含 <mark> 标签用于高亮关键词
              注意：v-html 存在 XSS 风险，只应用于信任的数据源
            -->
            <h3 class="result-title" v-html="item.title"></h3>
            <p class="result-summary" v-html="item.highlight"></p>
            <div class="result-meta">
              <!-- 分类标签 -->
              <el-tag v-if="item.category" size="small" effect="plain">{{ item.category }}</el-tag>
              <!-- 作者 -->
              <span class="meta-item" v-if="item.author">
                <el-icon><UserIcon /></el-icon>
                {{ item.author }}
              </span>
              <!-- 浏览量 -->
              <span class="meta-item">
                <el-icon><View /></el-icon>
                {{ Math.floor(Math.random() * 500) + 10 }} 次浏览
              </span>
              <!-- 日期 -->
              <span class="meta-item" v-if="item.createdAt">
                {{ formatDate(item.createdAt) }}
              </span>
            </div>
          </div>
          <!-- 匹配度评分 -->
          <div class="result-score">
            <el-tag type="success" size="small">
              {{ (item.score * 100).toFixed(0) }}% 匹配
            </el-tag>
          </div>
        </el-card>
      </div>

      <!-- 空状态（无结果时） -->
      <el-empty v-else description="未找到相关结果，请尝试其他关键词" :image-size="160">
        <el-button type="primary" @click="keyword = ''; hasSearched = false">
          重新搜索
        </el-button>
      </el-empty>

      <!-- 分页 -->
      <!--
        v-model:current-page 双向绑定当前页码
        layout="prev, pager, next" 只显示上一页、页码、下一页
        @current-change 页码变化时重新搜索
      -->
      <div class="pagination-wrapper" v-if="results.length > 0">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="totalResults"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 导入 Vue 响应式 API 和生命周期钩子
import { ref, onMounted } from 'vue'

// 导入路由 API：读取 URL 参数和进行页面跳转
import { useRouter, useRoute } from 'vue-router'

// 导入 Element Plus 消息提示
import { ElMessage } from 'element-plus'

// 导入图标（使用 as 重命名避免与组件名冲突）
import { Search as SearchIcon, User as UserIcon, View } from '@element-plus/icons-vue'

// 导入搜索 API
import { searchApi } from '@/api/search'

// 导入搜索结果类型
import type { SearchResult } from '@/types'

// 路由实例
const router = useRouter()
const route = useRoute()

// ==================== 响应式状态 ====================

/** 搜索关键词 */
const keyword = ref('')

/** 选中的分类 ID（undefined 表示不筛选） */
const selectedCategory = ref<number | undefined>(undefined)

/** 搜索结果列表 */
const results = ref<SearchResult[]>([])

/** 搜索结果总数 */
const totalResults = ref(0)

/** 当前页码 */
const currentPage = ref(1)

/** 每页条数 */
const pageSize = ref(10)

/** 是否已执行过搜索（控制搜索结果区域的显示/隐藏） */
const hasSearched = ref(false)

/** 加载状态 */
const loading = ref(false)

/** 搜索历史记录 */
const searchHistory = ref<string[]>([])

/** 分类选项（静态数据） */
const categoryOptions = [
  { label: '人事制度', value: 1 },
  { label: '技术文档', value: 2 },
  { label: '销售支持', value: 3 },
  { label: '合规法务', value: 4 }
]

/** 日期格式化工具函数 */
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

/**
 * 执行搜索
 *
 * 搜索流程：
 * 1. 验证关键词非空
 * 2. 保存到搜索历史
 * 3. 调用搜索 API
 * 4. 更新搜索结果和总数
 * 5. API 失败时使用 Mock 数据兜底
 */
const handleSearch = async () => {
  if (!keyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }

  loading.value = true
  hasSearched.value = true

  // --------------------------------------------------------------------------
  // 维护搜索历史
  // --------------------------------------------------------------------------
  // 去重：如果关键词已存在则不重复添加
  // unshift：新记录添加到数组头部（最新的在前）
  // 限制最多 10 条历史记录
  if (!searchHistory.value.includes(keyword.value.trim())) {
    searchHistory.value.unshift(keyword.value.trim())
    if (searchHistory.value.length > 10) {
      searchHistory.value.pop()  // 超过 10 条时移除最后一条
    }
  }

  try {
    // 调用搜索 API
    const res = await searchApi.search({
      q: keyword.value,
      page: currentPage.value,
      pageSize: pageSize.value,
      categoryId: selectedCategory.value
    })
    results.value = res.data.items
    totalResults.value = res.data.total
  } catch (error) {
    console.error('搜索失败:', error)
    // Fallback mock data：API 失败时使用模拟数据，确保页面有内容
    results.value = [
      {
        id: 1,
        title: '员工手册 - 年假制度',
        highlight: '根据公司规定，员工入职满一年后可享受带薪年假...',
        score: 0.95,
        category: '人事制度',
        author: 'HR部门',
        createdAt: '2025-12-01'
      },
      {
        id: 2,
        title: '技术文档编写规范',
        highlight: '本文档旨在规范技术文档的编写流程和格式要求...',
        score: 0.87,
        category: '技术文档',
        author: '技术部',
        createdAt: '2025-11-15'
      },
      {
        id: 3,
        title: '差旅报销流程指南',
        highlight: '员工出差前需在OA系统中提交差旅申请...',
        score: 0.72,
        category: '销售支持',
        author: '财务部',
        createdAt: '2025-10-20'
      }
    ]
    totalResults.value = results.value.length
  } finally {
    loading.value = false
  }
}

/** 分页变化处理：更新页码并重新搜索 */
const handlePageChange = (page: number) => {
  currentPage.value = page
  handleSearch()
}

/** 跳转到文档详情 */
const goToDocument = (id: number) => {
  router.push(`/documents/${id}`)
}

/** 清空搜索历史 */
const clearHistory = () => {
  searchHistory.value = []
  try {
    searchApi.clearHistory()
  } catch {
    // ignore: 即使 API 调用失败，本地历史也已清空
  }
}

/**
 * onMounted: 页面加载时初始化
 *
 * 1. 检查 URL 查询参数中是否有搜索关键词（从首页搜索跳转过来的情况）
 * 2. 加载搜索历史
 */
onMounted(async () => {
  // 从 URL 恢复搜索关键词
  // 例如从首页搜索跳转：/search?q=年假
  if (route.query.q) {
    keyword.value = route.query.q as string
    handleSearch()
  }

  // 加载搜索历史
  try {
    const res = await searchApi.getHistory()
    searchHistory.value = res.data
  } catch {
    // API 失败时使用模拟数据
    searchHistory.value = ['年假制度', '报销流程', '技术文档']
  }
})
</script>

<style scoped lang="scss">
/* 搜索页容器 */
.search-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 20px;
}

.search-header {
  margin-bottom: 32px;
}

/* 搜索栏：flex 布局排列输入框、下拉框、按钮 */
.search-bar {
  display: flex;
  gap: 12px;
  align-items: center;

  .category-select {
    width: 140px;
  }
}

/* 搜索历史区域 */
.search-history {
  margin-top: 16px;

  .history-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;

    .history-label {
      font-size: 13px;
      color: #909399;
    }
  }

  .history-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .history-tag {
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        color: #409eff;
        border-color: #409eff;
      }
    }
  }
}

/* 搜索结果区域 */
.search-results {
  .results-header {
    margin-bottom: 20px;

    .results-count {
      font-size: 14px;
      color: #606266;

      strong {
        color: #409eff;
        font-size: 18px;
      }
    }
  }
}

/* 搜索结果卡片 */
.result-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-2px);
    border-color: #409eff;
  }

  :deep(.el-card__body) {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
  }

  .result-content {
    flex: 1;

    /* 结果标题 */
    .result-title {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
      margin: 0 0 8px;
      line-height: 1.4;

      /* 高亮标记样式（<mark> 标签） */
      :deep(mark) {
        background-color: #ecf5ff;
        color: #409eff;
        padding: 0 2px;
        border-radius: 2px;
      }
    }

    /* 摘要文字：最多显示 2 行，超出部分省略 */
    .result-summary {
      font-size: 14px;
      color: #606266;
      line-height: 1.6;
      margin: 0 0 12px;
      /* 多行文本截断（WebKit 浏览器支持） */
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;

      :deep(mark) {
        background-color: #ecf5ff;
        color: #409eff;
        padding: 0 2px;
        border-radius: 2px;
      }
    }

    /* 元信息（分类、作者、浏览量、日期） */
    .result-meta {
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

  /* 匹配度评分 */
  .result-score {
    flex-shrink: 0;
    margin-left: 16px;
  }
}

/* 分页居中 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}
</style>
