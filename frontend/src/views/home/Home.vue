<!--
================================================================================
首页组件 - views/home/Home.vue
================================================================================

【文件说明】
这是应用的首页（Home Page），用户登录后看到的第一个页面。
页面包含以下模块：
1. 欢迎区域：标题和副标题
2. 搜索框：快速搜索文档
3. 快捷分类：常用分类的卡片入口
4. 热门文档：浏览量最高的文档列表
5. 最近访问：用户最近浏览过的文档

【Vue 概念】
- onMounted 生命周期：页面加载时获取热门文档和最近访问数据
- ref() 响应式数据：存储分类列表、文档列表等
- 组件通信：通过 @search 事件接收 SearchBox 组件的搜索关键词
- v-for 列表渲染：循环显示分类卡片和文档卡片
- 路由导航：使用 router.push() 跳转到搜索页或文档详情页

💡 学习要点:
1. 页面组件通常在 onMounted 中发起初始数据请求
2. 使用 router.push({ path: '/search', query: { q: keyword } }) 传递搜索参数
3. v-for 必须配合 :key 使用，Vue 使用 key 追踪列表项的身份
4. 动态组件 <component :is="category.icon" /> 根据数据渲染不同的图标
5. try/catch 包裹 API 调用，防止接口报错导致页面崩溃
6. CSS Grid 布局（grid-template-columns）实现自适应卡片排列
================================================================================
-->

<template>
  <div class="home-page">
    <!-- ================================================================== -->
    <!-- 欢迎区域 -->
    <!-- ================================================================== -->
    <div class="welcome-section">
      <!-- 文字渐变效果：使用 CSS background-clip: text 技巧 -->
      <h1 class="welcome-title">欢迎来到企业知识库</h1>
      <p class="welcome-subtitle">智能搜索，快速获取所需知识</p>
    </div>

    <!-- ================================================================== -->
    <!-- 搜索区域 -->
    <!-- ================================================================== -->
    <!--
      SearchBox 是自定义组件（搜索框）
      @search 事件：用户按回车或点击搜索按钮时触发
      handleSearch 方法接收搜索关键词并跳转到搜索页
    -->
    <div class="search-section">
      <search-box placeholder="搜索文档、问答或输入您的问题..." @search="handleSearch" />
    </div>

    <!-- ================================================================== -->
    <!-- 快捷分类 -->
    <!-- ================================================================== -->
    <!--
      v-for 列表渲染：
      - v-for="category in categories" 遍历分类数组
      - :key="category.id" 为每个列表项提供唯一标识（Vue 要求）
      - @click="goToCategory(category.id)" 点击卡片跳转到对应分类
    -->
    <div class="quick-categories">
      <div
        v-for="category in categories"
        :key="category.id"
        class="category-card"
        @click="goToCategory(category.id)"
      >
        <!--
          动态组件渲染：
          <component :is="category.icon" /> 根据 category.icon 的值渲染不同的组件
          例如 icon 是 Briefcase 时渲染 <Briefcase /> 图标
        -->
        <el-icon :size="32" :color="category.color">
          <component :is="category.icon" />
        </el-icon>
        <div class="category-name">{{ category.name }}</div>
        <div class="category-count">{{ category.count }} 篇文档</div>
      </div>
    </div>

    <!-- ================================================================== -->
    <!-- 热门文档 -->
    <!-- ================================================================== -->
    <div class="hot-documents">
      <div class="section-header">
        <h2 class="section-title">热门文档</h2>
        <!-- "查看更多"链接，跳转到文档列表页 -->
        <el-link type="primary" @click="$router.push('/documents')">查看更多</el-link>
      </div>

      <!--
        DocumentCard 是自定义的业务组件（文档卡片）
        v-for 遍历热门文档列表，:document="doc" 将文档数据传递给子组件
      -->
      <div class="document-list">
        <document-card
          v-for="doc in hotDocuments"
          :key="doc.id"
          :document="doc"
          @click="goToDocument(doc.id)"
        />
      </div>
    </div>

    <!-- ================================================================== -->
    <!-- 最近访问 -->
    <!-- ================================================================== -->
    <!-- v-if 条件渲染：只有在有最近访问记录时才显示这个区域 -->
    <div class="recent-documents" v-if="recentDocuments.length > 0">
      <div class="section-header">
        <h2 class="section-title">最近访问</h2>
      </div>

      <div class="document-list">
        <document-card
          v-for="doc in recentDocuments"
          :key="doc.id"
          :document="doc"
          @click="goToDocument(doc.id)"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 导入 Vue 响应式 API 和生命周期钩子
import { ref, onMounted } from 'vue'

// 导入 Vue Router 的组合式 API
import { useRouter } from 'vue-router'

// 导入自定义组件
// SearchBox: 搜索框组件
// DocumentCard: 文档卡片组件
import SearchBox from '@/components/common/SearchBox.vue'
import DocumentCard from '@/components/business/DocumentCard.vue'

// 导入文档 API 模块
import { documentApi } from '@/api/document'

// 导入文档类型定义
import type { Document } from '@/types'

// 导入分类卡片使用的图标
// 这些图标组件会在动态组件 <component :is="..."> 中使用
import {
  Briefcase,
  Monitor,
  TrendCharts,
  Key
} from '@element-plus/icons-vue'

// 获取路由实例
const router = useRouter()

/**
 * 分类数据（本地静态数据）
 *
 * 使用 ref() 创建响应式数组。
 * 每个分类包含：id、名称、图标组件、颜色、文档数量。
 * icon 字段存储的是 Vue 组件引用（不是字符串），可以直接传给 <component :is="">
 */
const categories = ref([
  { id: 1, name: '人事制度', icon: Briefcase, color: '#409eff', count: 128 },
  { id: 2, name: '技术文档', icon: Monitor, color: '#67c23a', count: 356 },
  { id: 3, name: '销售支持', icon: TrendCharts, color: '#e6a23c', count: 89 },
  { id: 4, name: '合规法务', icon: Key, color: '#f56c6c', count: 67 }
])

/**
 * 热门文档列表
 *
 * ref<Document[]> 泛型指定数组元素的类型为 Document。
 * 初始为空数组，在 onMounted 中通过 API 获取数据。
 */
const hotDocuments = ref<Document[]>([])

/** 最近访问的文档列表 */
const recentDocuments = ref<Document[]>([])

/**
 * 获取热门文档
 *
 * 调用文档列表 API，按发布状态筛选，获取前 5 条。
 * try/catch 包裹 API 调用，防止接口报错导致页面崩溃。
 */
const fetchHotDocuments = async () => {
  try {
    const res = await documentApi.getList({
      page: 1,
      pageSize: 5,
      status: 'PUBLISHED'
    })
    hotDocuments.value = res.data.items
  } catch (error) {
    console.error('获取热门文档失败:', error)
  }
}

/** 获取最近访问的文档 */
const fetchRecentDocuments = async () => {
  // TODO: 从本地存储或API获取
  try {
    const res = await documentApi.getList({
      page: 1,
      pageSize: 5
    })
    recentDocuments.value = res.data.items
  } catch (error) {
    console.error('获取最近文档失败:', error)
  }
}

/**
 * 处理搜索
 *
 * @param keyword - 用户输入的搜索关键词
 *
 * 使用 router.push() 跳转到搜索页，并通过 query 参数传递关键词。
 * 搜索页组件会读取 route.query.q 来初始化搜索。
 */
const handleSearch = (keyword: string) => {
  if (keyword.trim()) {
    router.push({ path: '/search', query: { q: keyword } })
  }
}

/**
 * 跳转到分类页
 *
 * @param categoryId - 分类 ID
 *
 * 跳转到文档列表页，并通过 query 参数传递分类筛选条件
 */
const goToCategory = (categoryId: number) => {
  router.push({ path: '/documents', query: { categoryId } })
}

/**
 * 跳转到文档详情页
 *
 * @param documentId - 文档 ID
 *
 * 路由路径使用模板字符串动态拼接 ID
 */
const goToDocument = (documentId: number) => {
  router.push({ path: `/documents/${documentId}` })
}

/**
 * onMounted 生命周期钩子
 *
 * 组件挂载后（DOM 已渲染）执行。
 * 在这里发起初始数据请求，获取热门文档和最近访问记录。
 */
onMounted(() => {
  fetchHotDocuments()
  fetchRecentDocuments()
})
</script>

<style scoped lang="scss">
/* 首页容器：限制最大宽度，水平居中 */
.home-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 欢迎区域：居中布局，渐变标题 */
.welcome-section {
  text-align: center;
  margin-bottom: 40px;

  .welcome-title {
    font-size: 32px;
    font-weight: 600;
    margin-bottom: 12px;
    /* 渐变文字效果：背景渐变 + background-clip: text + text-fill-color: transparent */
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .welcome-subtitle {
    font-size: 16px;
    color: #909399;
  }
}

.search-section {
  margin-bottom: 40px;
}

/* 分类卡片网格布局 */
.quick-categories {
  /* CSS Grid 自适应布局：每列最小 200px，自动填充 */
  /* auto-fit + minmax 实现响应式卡片布局，无需媒体查询 */
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 40px;

  .category-card {
    background: #fff;
    border-radius: 12px;
    padding: 24px;
    text-align: center;
    cursor: pointer;
    transition: all 0.3s;
    border: 1px solid #e4e7ed;

    /* 悬浮效果：上移 4px + 阴影 + 边框变色 */
    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
      border-color: #409eff;
    }

    .category-name {
      font-size: 16px;
      font-weight: 500;
      margin: 12px 0 8px;
    }

    .category-count {
      font-size: 13px;
      color: #909399;
    }
  }
}

/* 文档列表区域 */
.hot-documents,
.recent-documents {
  margin-bottom: 40px;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .section-title {
      font-size: 20px;
      font-weight: 600;
    }
  }

  /* 文档卡片网格 */
  .document-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 20px;
  }
}
</style>
