<!--
================================================================================
系统设置页面 - views/admin/Settings.vue
================================================================================

【文件说明】
管理员系统设置页面，包含 AI 智能配置、权限管理和存储管理三个板块。

【Vue 概念】
- el-switch 开关组件用于布尔设置项
- el-progress 进度条展示存储用量
- el-card 卡片容器按功能分区
- reactive 集中管理所有设置项

💡 学习要点:
1. el-switch 的 v-model 双向绑定布尔值
2. el-progress 的 percentage 属性控制进度百分比
3. 使用 reactive 管理表单状态，便于后续对接 API
================================================================================
-->

<template>
  <div class="admin-settings">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">系统设置</h1>
      <el-tag type="info" size="small">配置管理</el-tag>
    </div>

    <!-- ================================================================== -->
    <!-- AI 智能设置 -->
    <!-- ================================================================== -->
    <el-card shadow="never" class="settings-card">
      <template #header>
        <div class="section-title">
          <el-icon :size="20" class="section-icon ai"><Monitor /></el-icon>
          <span>AI 智能设置</span>
        </div>
      </template>

      <div class="setting-item">
        <div class="setting-info">
          <div class="setting-label">启用智能问答</div>
          <div class="setting-desc">开启后将使用AI自动回答用户问题</div>
        </div>
        <el-switch v-model="settings.ai.enableQA" @change="(val: boolean) => handleSwitchChange('ai.enableQA', val)" />
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <div class="setting-label">自动文档分类</div>
          <div class="setting-desc">使用AI自动为新上传的文档分类</div>
        </div>
        <el-switch v-model="settings.ai.autoClassify" @change="(val: boolean) => handleSwitchChange('ai.autoClassify', val)" />
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <div class="setting-label">智能建议</div>
          <div class="setting-desc">在用户搜索时提供相关建议</div>
        </div>
        <el-switch v-model="settings.ai.smartSuggestion" @change="(val: boolean) => handleSwitchChange('ai.smartSuggestion', val)" />
      </div>
    </el-card>

    <!-- ================================================================== -->
    <!-- 权限管理 -->
    <!-- ================================================================== -->
    <el-card shadow="never" class="settings-card">
      <template #header>
        <div class="section-title">
          <el-icon :size="20" class="section-icon permission"><Lock /></el-icon>
          <span>权限管理</span>
        </div>
      </template>

      <div class="setting-item">
        <div class="setting-info">
          <div class="setting-label">公开搜索</div>
          <div class="setting-desc">允许未登录用户搜索公开文档</div>
        </div>
        <el-switch v-model="settings.permission.publicSearch" @change="(val: boolean) => handleSwitchChange('permission.publicSearch', val)" />
      </div>

      <div class="setting-item">
        <div class="setting-info">
          <div class="setting-label">审核机制</div>
          <div class="setting-desc">新文档发布需要审核</div>
        </div>
        <el-switch v-model="settings.permission.requireReview" @change="(val: boolean) => handleSwitchChange('permission.requireReview', val)" />
      </div>
    </el-card>

    <!-- ================================================================== -->
    <!-- 存储管理 -->
    <!-- ================================================================== -->
    <el-card shadow="never" class="settings-card">
      <template #header>
        <div class="section-title">
          <el-icon :size="20" class="section-icon storage"><Coin /></el-icon>
          <span>存储管理</span>
        </div>
      </template>

      <div class="storage-info">
        <div class="storage-label">存储用量</div>
        <div class="storage-detail">
          <span class="storage-value">{{ settings.storage.used }}</span>
          <span class="storage-unit"> / {{ settings.storage.total }} GB</span>
        </div>
      </div>

      <el-progress
        :percentage="storagePercentage"
        :stroke-width="12"
        :color="progressColor"
        class="storage-progress"
      />

      <div class="storage-actions">
        <el-button plain @click="handleClearCache">清理缓存</el-button>
        <el-button type="primary" @click="handleExpand">扩容</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Monitor, Lock, Coin } from '@element-plus/icons-vue'
import { adminSettingsApi } from '@/api/admin'

/** 所有设置项集中管理，API 加载前使用默认值 */
const settings = reactive({
  ai: {
    enableQA: true,
    autoClassify: true,
    smartSuggestion: false
  },
  permission: {
    publicSearch: true,
    requireReview: true
  },
  storage: {
    used: 85,
    total: 100
  }
})

// ==================== API 数据加载 ====================

/** 从 API 加载设置并填充到 reactive 对象 */
async function loadSettings() {
  try {
    const res = await adminSettingsApi.getAll()
    const data = res.data
    if (data) {
      // AI 设置
      if (data.ai) {
        settings.ai.enableQA = data.ai.enableQA ?? settings.ai.enableQA
        settings.ai.autoClassify = data.ai.autoClassify ?? settings.ai.autoClassify
        settings.ai.smartSuggestion = data.ai.smartSuggestion ?? settings.ai.smartSuggestion
      }
      // 权限设置
      if (data.permission) {
        settings.permission.publicSearch = data.permission.publicSearch ?? settings.permission.publicSearch
        settings.permission.requireReview = data.permission.requireReview ?? settings.permission.requireReview
      }
      // 存储信息
      if (data.storage) {
        settings.storage.used = data.storage.used_gb ?? settings.storage.used
        settings.storage.total = data.storage.total_gb ?? settings.storage.total
      }
    }
  } catch {
    // API 失败时保留默认值
  }
}

onMounted(() => {
  loadSettings()
})

/** 存储用量百分比 */
const storagePercentage = computed(() => {
  return Math.round((settings.storage.used / settings.storage.total) * 100)
})

/** 根据用量返回进度条颜色 */
const progressColor = computed(() => {
  const pct = storagePercentage.value
  if (pct >= 90) return '#f56c6c'
  if (pct >= 70) return '#e6a23c'
  return '#409eff'
})

/** el-switch 变更时同步到后端 */
const handleSwitchChange = async (key: string, value: boolean) => {
  try {
    await adminSettingsApi.update({ [key]: String(value) })
    ElMessage.success('设置已保存')
  } catch {
    // API 失败时回滚开关状态
    const keys = key.split('.')
    if (keys.length === 2) {
      const [group, prop] = keys
      ;(settings as any)[group][prop] = !value
    }
  }
}

/** 清理缓存 */
const handleClearCache = () => {
  ElMessage.success('缓存清理成功')
}

/** 扩容操作 */
const handleExpand = () => {
  ElMessage.info('请联系管理员进行扩容操作')
}
</script>

<style scoped lang="scss">
/* 页面容器 */
.admin-settings {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

/* 页面标题行 */
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

/* 设置卡片 */
.settings-card {
  margin-bottom: 20px;

  /* 区块标题 */
  .section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;

    .section-icon {
      border-radius: 6px;
      padding: 4px;

      &.ai {
        background: #ecf5ff;
        color: #409eff;
      }

      &.permission {
        background: #f0f9eb;
        color: #67c23a;
      }

      &.storage {
        background: #fdf6ec;
        color: #e6a23c;
      }
    }
  }
}

/* 单条设置项 */
.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
  border-bottom: 1px solid #f0f2f5;

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }

  &:first-child {
    padding-top: 0;
  }
}

.setting-info {
  flex: 1;
  min-width: 0;

  .setting-label {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
    margin-bottom: 4px;
  }

  .setting-desc {
    font-size: 13px;
    color: #909399;
  }
}

/* 存储管理区域 */
.storage-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;

  .storage-label {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
  }

  .storage-detail {
    font-size: 14px;

    .storage-value {
      font-weight: 600;
      color: #303133;
    }

    .storage-unit {
      color: #909399;
    }
  }
}

.storage-progress {
  margin-bottom: 20px;
}

.storage-actions {
  display: flex;
  gap: 12px;
}
</style>
