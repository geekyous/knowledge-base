<!--
================================================================================
管理后台仪表盘 - views/admin/Dashboard.vue
================================================================================

【文件说明】
这是管理员后台的仪表盘页面，提供系统运营数据的概览。
包含：
1. 统计卡片（文档总数、用户总数、提问总数、回答准确率）
2. 图表区域占位（待集成 ECharts）
3. 最近文档表格
4. 待审核文档列表（可执行通过/拒绝操作）

【Vue 概念】
- el-row/el-col 栅格布局系统（4 个等宽统计卡片）
- el-table 数据表格组件
- el-table-column 自定义列模板（#default 插槽）
- 仪表盘卡片模式：图标 + 数值 + 趋势

💡 学习要点:
1. el-row :gutter="20" 设置列之间的间距
2. el-col :span="6" 将 24 栏分为 4 等份（6*4=24）
3. el-table-column 的 #default="{ row }" 插槽获取当前行数据
4. Record<string, string> 类型用于状态映射表
5. onMounted 中获取最新数据覆盖 Mock 数据
================================================================================
-->

<template>
  <div class="admin-dashboard">
    <h1 class="page-title">管理后台</h1>

    <!-- ================================================================== -->
    <!-- 统计卡片行（4 等分） -->
    <!-- ================================================================== -->
    <!--
      el-row :gutter="20" 列间距 20px
      el-col :span="6" 每列占 6 栏（24/4 = 6，即 4 等分）
    -->
    <el-row :gutter="20" class="stats-row">
      <!-- 文档总数卡片 -->
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon blue">
              <el-icon :size="28"><Document /></el-icon>
            </div>
            <div class="stat-detail">
              <div class="stat-value">{{ stats.totalDocuments }}</div>
              <div class="stat-label">文档总数</div>
            </div>
          </div>
          <div class="stat-footer">
            <span class="stat-trend up">
              <el-icon><Top /></el-icon>
              +12%
            </span>
            <span class="stat-period">较上月</span>
          </div>
        </el-card>
      </el-col>

      <!-- 用户总数卡片 -->
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon green">
              <el-icon :size="28"><User /></el-icon>
            </div>
            <div class="stat-detail">
              <div class="stat-value">{{ stats.totalUsers }}</div>
              <div class="stat-label">用户总数</div>
            </div>
          </div>
          <div class="stat-footer">
            <span class="stat-trend up">
              <el-icon><Top /></el-icon>
              +8%
            </span>
            <span class="stat-period">较上月</span>
          </div>
        </el-card>
      </el-col>

      <!-- 提问总数卡片 -->
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon orange">
              <el-icon :size="28"><ChatDotRound /></el-icon>
            </div>
            <div class="stat-detail">
              <div class="stat-value">{{ stats.totalQuestions }}</div>
              <div class="stat-label">提问总数</div>
            </div>
          </div>
          <div class="stat-footer">
            <span class="stat-trend up">
              <el-icon><Top /></el-icon>
              +23%
            </span>
            <span class="stat-period">较上月</span>
          </div>
        </el-card>
      </el-col>

      <!-- 回答准确率卡片 -->
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon red">
              <el-icon :size="28"><TrendCharts /></el-icon>
            </div>
            <div class="stat-detail">
              <div class="stat-value">{{ stats.accuracyRate }}%</div>
              <div class="stat-label">回答准确率</div>
            </div>
          </div>
          <div class="stat-footer">
            <span class="stat-trend up">
              <el-icon><Top /></el-icon>
              +2.1%
            </span>
            <span class="stat-period">较上月</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ================================================================== -->
    <!-- 图表区域占位（待集成 ECharts） -->
    <!-- ================================================================== -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">文档发布趋势</span>
          </template>
          <!-- 占位区域，后续集成 ECharts 图表 -->
          <div class="chart-placeholder">
            <el-icon :size="48" color="#dcdfe6"><TrendCharts /></el-icon>
            <p>图表区域（待集成 ECharts）</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">用户活跃度</span>
          </template>
          <div class="chart-placeholder">
            <el-icon :size="48" color="#dcdfe6"><DataLine /></el-icon>
            <p>图表区域（待集成 ECharts）</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ================================================================== -->
    <!-- 底部区域：最近文档 + 待审核 -->
    <!-- ================================================================== -->
    <el-row :gutter="20" class="bottom-row">
      <!-- 最近文档表格 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-title">最近文档</span>
              <el-button link type="primary" @click="$router.push('/documents')">查看全部</el-button>
            </div>
          </template>
          <!--
            el-table: 数据表格组件
            :data="recentDocuments" 绑定数据源
            stripe 斑马纹样式
          -->
          <el-table :data="recentDocuments" stripe style="width: 100%">
            <!-- prop 属性直接读取数据对象的字段 -->
            <el-table-column prop="title" label="标题" min-width="200" />
            <el-table-column prop="author" label="作者" width="100" />
            <!-- 自定义列模板：使用插槽渲染状态标签 -->
            <el-table-column label="状态" width="90">
              <!--
                #default="{ row }" 解构获取当前行数据
                row 对象包含该行的所有字段
              -->
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small">
                  {{ statusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="viewCount" label="浏览" width="80" />
            <el-table-column prop="createdAt" label="日期" width="110" />
          </el-table>
        </el-card>
      </el-col>

      <!-- 待审核文档列表 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <div class="card-header-row">
              <span class="card-title">待审核文档</span>
              <!-- 显示待审核数量 -->
              <el-tag type="warning" size="small">{{ pendingDocs.length }} 篇</el-tag>
            </div>
          </template>
          <div class="pending-list">
            <div
              v-for="doc in pendingDocs"
              :key="doc.id"
              class="pending-item"
            >
              <div class="pending-info">
                <h4 class="pending-title">{{ doc.title }}</h4>
                <span class="pending-meta">
                  {{ doc.author }} · {{ doc.createdAt }}
                </span>
              </div>
              <!-- 操作按钮组 -->
              <div class="pending-actions">
                <el-button type="success" size="small" plain>通过</el-button>
                <el-button type="danger" size="small" plain>拒绝</el-button>
              </div>
            </div>
            <!-- 空状态 -->
            <el-empty
              v-if="pendingDocs.length === 0"
              description="暂无待审核文档"
              :image-size="60"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
// 导入 Vue API
import { ref, onMounted } from 'vue'

// 导入图标
import {
  Document,
  User,
  ChatDotRound,
  TrendCharts,
  Top,
  DataLine
} from '@element-plus/icons-vue'

// 导入文档 API
import { documentApi } from '@/api/document'

// ==================== 统计数据（Mock） ====================
/** 系统统计数据 */
const stats = ref({
  totalDocuments: 1256,
  totalUsers: 328,
  totalQuestions: 4892,
  accuracyRate: 94.7
})

// ==================== 最近文档（Mock） ====================
/** 最近文档表格数据 */
const recentDocuments = ref([
  { id: 1, title: '员工手册 (2025版)', author: 'HR部门', status: 'PUBLISHED', viewCount: 1234, createdAt: '06-01' },
  { id: 2, title: '前端开发规范 V3.0', author: '技术部', status: 'PUBLISHED', viewCount: 856, createdAt: '05-30' },
  { id: 3, title: '数据安全管理制度', author: '安全部', status: 'PENDING', viewCount: 267, createdAt: '05-28' },
  { id: 4, title: '差旅报销流程', author: '财务部', status: 'DRAFT', viewCount: 432, createdAt: '05-25' },
  { id: 5, title: '新员工入职指南', author: 'HR部门', status: 'PUBLISHED', viewCount: 2380, createdAt: '05-20' }
])

// ==================== 待审核文档（Mock） ====================
/** 待审核文档列表 */
const pendingDocs = ref([
  { id: 101, title: 'API 接口设计规范', author: '架构组', createdAt: '05-31' },
  { id: 102, title: '数据库运维手册', author: 'DBA团队', createdAt: '05-29' },
  { id: 103, title: '客户服务流程优化方案', author: '客服部', createdAt: '05-28' }
])

/** 获取状态对应的标签类型（颜色） */
const statusTagType = (status: string) => {
  const map: Record<string, string> = {
    DRAFT: 'info',
    PENDING: 'warning',
    PUBLISHED: 'success',
    ARCHIVED: 'danger'
  }
  return map[status] || 'info'
}

/** 获取状态对应的中文文本 */
const statusLabel = (status: string) => {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PENDING: '待审核',
    PUBLISHED: '已发布',
    ARCHIVED: '已归档'
  }
  return map[status] || status
}

/**
 * 组件挂载时获取最新数据
 *
 * 尝试从 API 获取真实数据来替换 Mock 数据。
 * API 失败则保持 Mock 数据不变。
 */
onMounted(async () => {
  try {
    const res = await documentApi.getList({ page: 1, pageSize: 5 })
    recentDocuments.value = res.data.items.map((d: any) => ({
      id: d.id,
      title: d.title,
      author: d.author?.username || '未知',
      status: d.status,
      viewCount: d.viewCount,
      createdAt: new Date(d.createdAt).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
    }))
  } catch {
    // keep mock data: API 失败时保留初始 Mock 数据
  }
})
</script>

<style scoped lang="scss">
/* 仪表盘容器 */
.admin-dashboard {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 24px;
}

/* 统计卡片行 */
.stats-row {
  margin-bottom: 20px;
}

/* 统计卡片样式 */
.stat-card {
  .stat-content {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 12px;

    /* 图标容器：固定大小圆角方块 */
    .stat-icon {
      width: 52px;
      height: 52px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      /* 四种颜色主题 */
      &.blue { background: #ecf5ff; color: #409eff; }
      &.green { background: #f0f9eb; color: #67c23a; }
      &.orange { background: #fdf6ec; color: #e6a23c; }
      &.red { background: #fef0f0; color: #f56c6c; }
    }

    .stat-detail {
      .stat-value {
        font-size: 28px;
        font-weight: 700;
        color: #303133;
        line-height: 1.2;
      }

      .stat-label {
        font-size: 13px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }

  /* 趋势信息 */
  .stat-footer {
    display: flex;
    align-items: center;
    gap: 8px;
    padding-top: 12px;
    border-top: 1px solid #f0f2f5;

    .stat-trend {
      font-size: 13px;
      display: flex;
      align-items: center;
      gap: 2px;

      &.up { color: #67c23a; }
      &.down { color: #f56c6c; }
    }

    .stat-period {
      font-size: 12px;
      color: #c0c4cc;
    }
  }
}

/* 图表区域 */
.charts-row {
  margin-bottom: 20px;

  .chart-placeholder {
    height: 240px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #c0c4cc;
    font-size: 14px;
    background: #fafafa;
    border-radius: 6px;

    p {
      margin-top: 12px;
    }
  }
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

/* 卡片头部：标题和操作左右排列 */
.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bottom-row {
  margin-bottom: 20px;
}

/* 待审核列表 */
.pending-list {
  .pending-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 0;
    border-bottom: 1px solid #f0f2f5;

    &:last-child {
      border-bottom: none;
    }

    .pending-info {
      flex: 1;
      min-width: 0;

      .pending-title {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
        margin: 0 0 4px;
        /* 文字超长截断 */
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .pending-meta {
        font-size: 12px;
        color: #909399;
      }
    }

    .pending-actions {
      display: flex;
      gap: 8px;
      flex-shrink: 0;
      margin-left: 12px;
    }
  }
}
</style>
