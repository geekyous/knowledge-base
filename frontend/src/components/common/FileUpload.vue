<!--
================================================================================
文件上传组件 - components/common/FileUpload.vue
================================================================================

【文件说明】
通用文件上传组件，支持拖拽上传和点击选择文件。
功能包括：
1. 拖拽上传区域（云图标 + 提示文字 + 格式说明）
2. 上传队列展示三种状态：上传中、已完成、上传失败
3. 上传设置面板（目标分类、标签、自动提取、通知选项）

【Vue 概念】
- el-upload: Element Plus 文件上传组件，支持 drag 拖拽模式
- el-progress: 进度条展示上传进度
- el-select / el-option: 下拉选择分类
- el-checkbox: 复选框控制上传选项
- ref() / reactive(): 管理上传队列和设置项的响应式状态
================================================================================
-->

<template>
  <div class="file-upload">
    <!-- ================================================================== -->
    <!-- 拖拽上传区域 -->
    <!-- ================================================================== -->
    <el-upload
      ref="uploadRef"
      class="upload-dragger"
      drag
      multiple
      action="#"
      :auto-upload="false"
      :show-file-list="false"
      :on-change="handleFileChange"
      accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.md"
    >
      <div class="dragger-content">
        <el-icon class="dragger-icon"><UploadFilled /></el-icon>
        <div class="dragger-text">
          <p class="dragger-main-text">拖拽文件到此处，或点击选择文件</p>
          <p class="dragger-hint">支持格式：PDF、Word、Excel、PPT、Markdown</p>
        </div>
        <el-button type="primary" class="dragger-btn">
          <el-icon><Plus /></el-icon>
          选择文件
        </el-button>
      </div>
    </el-upload>

    <!-- ================================================================== -->
    <!-- 上传队列 -->
    <!-- ================================================================== -->
    <div class="upload-queue" v-if="uploadQueue.length > 0">
      <div class="queue-header">
        <span class="queue-title">上传队列</span>
        <span class="queue-count">{{ uploadQueue.length }} 个文件</span>
      </div>
      <div class="queue-list">
        <div
          v-for="file in uploadQueue"
          :key="file.uid"
          class="queue-item"
          :class="`queue-item--${file.status}`"
        >
          <!-- 文件图标 + 文件名 -->
          <div class="queue-file-info">
            <el-icon class="file-icon"><Document /></el-icon>
            <span class="file-name" :title="file.name">{{ file.name }}</span>
          </div>

          <!-- 上传中：进度条 + 加载动画 -->
          <div class="queue-status queue-status--uploading" v-if="file.status === 'uploading'">
            <el-progress
              :percentage="file.percentage"
              :stroke-width="6"
              :show-text="false"
              class="upload-progress"
            />
            <span class="percentage-text">{{ file.percentage }}%</span>
            <el-icon class="is-loading status-icon spinning"><Loading /></el-icon>
          </div>

          <!-- 已完成：绿色对勾徽章 -->
          <div class="queue-status queue-status--success" v-else-if="file.status === 'success'">
            <el-tag type="success" size="small" effect="light">
              <el-icon><CircleCheck /></el-icon>
              已完成
            </el-tag>
          </div>

          <!-- 上传失败：红色叉号徽章 -->
          <div class="queue-status queue-status--fail" v-else-if="file.status === 'fail'">
            <el-tag type="danger" size="small" effect="light">
              <el-icon><CircleClose /></el-icon>
              {{ file.errorMsg }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- ================================================================== -->
    <!-- 上传设置面板 -->
    <!-- ================================================================== -->
    <div class="upload-settings">
      <div class="settings-title">上传设置</div>

      <div class="settings-form">
        <!-- 目标分类 -->
        <div class="settings-row">
          <label class="settings-label">目标分类</label>
          <el-select
            v-model="uploadSettings.categoryId"
            placeholder="请选择分类"
            clearable
            class="settings-select"
          >
            <el-option
              v-for="cat in categoryOptions"
              :key="cat.value"
              :label="cat.label"
              :value="cat.value"
            />
          </el-select>
        </div>

        <!-- 标签 -->
        <div class="settings-row">
          <label class="settings-label">标签</label>
          <el-input
            v-model="uploadSettings.tags"
            placeholder="输入标签，多个标签用逗号分隔"
            clearable
            class="settings-input"
          />
        </div>

        <!-- 复选框选项 -->
        <div class="settings-checkboxes">
          <el-checkbox v-model="uploadSettings.autoExtract">
            自动提取文档内容
          </el-checkbox>
          <el-checkbox v-model="uploadSettings.notifyUsers">
            上传后通知相关人员
          </el-checkbox>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'

// 导入图标
import {
  UploadFilled,
  Plus,
  Document,
  Loading,
  CircleCheck,
  CircleClose
} from '@element-plus/icons-vue'

// 导入 Element Plus 类型
import type { UploadFile, UploadInstance } from 'element-plus'

// ==================== 类型定义 ====================

/** 上传队列中的文件项 */
interface QueueFileItem {
  /** 唯一标识 */
  uid: string
  /** 文件名 */
  name: string
  /** 文件大小（字节） */
  size: number
  /** 上传状态 */
  status: 'uploading' | 'success' | 'fail'
  /** 上传进度百分比（0-100） */
  percentage: number
  /** 失败时的错误信息 */
  errorMsg: string
}

// ==================== 响应式状态 ====================

/** el-upload 组件实例引用 */
const uploadRef = ref<UploadInstance>()

/** 上传队列 */
const uploadQueue = ref<QueueFileItem[]>([])

/** 上传设置 */
const uploadSettings = reactive({
  /** 目标分类 ID */
  categoryId: undefined as number | undefined,
  /** 标签（逗号分隔） */
  tags: '',
  /** 自动提取文档内容 */
  autoExtract: true,
  /** 上传后通知相关人员 */
  notifyUsers: false
})

/** 分类选项 */
const categoryOptions = [
  { label: '人事制度', value: 1 },
  { label: '技术文档', value: 2 },
  { label: '销售支持', value: 3 },
  { label: '合规法务', value: 4 }
]

// ==================== 方法 ====================

/**
 * 文件选择变化回调
 *
 * 将用户选择的文件添加到上传队列。
 * 实际项目中此处会调用上传 API，当前仅做模拟演示。
 */
const handleFileChange = (uploadFile: UploadFile) => {
  if (!uploadFile.name || !uploadFile.raw) return

  const queueItem: QueueFileItem = {
    uid: String(uploadFile.uid || Date.now()),
    name: uploadFile.name,
    size: uploadFile.raw?.size || 0,
    status: 'uploading',
    percentage: 0,
    errorMsg: ''
  }

  uploadQueue.value.unshift(queueItem)
  simulateUpload(queueItem)
}

/**
 * 模拟上传进度
 *
 * 实际项目中应替换为真实的上传 API 调用。
 * 这里用 setInterval 模拟进度推进，最终随机决定成功或失败。
 */
const simulateUpload = (item: QueueFileItem) => {
  let progress = 0
  const interval = setInterval(() => {
    progress += Math.floor(Math.random() * 15) + 5
    if (progress >= 100) {
      progress = 100
      clearInterval(interval)

      const idx = uploadQueue.value.findIndex(f => f.uid === item.uid)
      if (idx !== -1) {
        uploadQueue.value[idx].percentage = 100
        // 模拟：90% 概率成功，10% 概率失败
        if (Math.random() > 0.1) {
          uploadQueue.value[idx].status = 'success'
        } else {
          uploadQueue.value[idx].status = 'fail'
          uploadQueue.value[idx].errorMsg = '文件过大'
        }
      }
    } else {
      const idx = uploadQueue.value.findIndex(f => f.uid === item.uid)
      if (idx !== -1) {
        uploadQueue.value[idx].percentage = progress
      }
    }
  }, 300)
}

// ==================== 初始化 Mock 数据 ====================

/**
 * 加载 Mock 队列数据，展示三种上传状态
 *
 * 实际项目中此函数应删除，队列由用户操作驱动。
 */
const loadMockQueue = () => {
  uploadQueue.value = [
    {
      uid: 'mock-uploading-1',
      name: '2025年Q2季度报告.xlsx',
      size: 2457600,
      status: 'uploading',
      percentage: 68,
      errorMsg: ''
    },
    {
      uid: 'mock-success-1',
      name: '前端开发规范V3.0.pdf',
      size: 1048576,
      status: 'success',
      percentage: 100,
      errorMsg: ''
    },
    {
      uid: 'mock-fail-1',
      name: '大型数据集备份.zip',
      size: 524288000,
      status: 'fail',
      percentage: 45,
      errorMsg: '文件过大'
    }
  ]

  // 模拟上传中文件的进度持续推进
  simulateUpload(uploadQueue.value[0])
}

onMounted(() => {
  loadMockQueue()
})
</script>

<style scoped lang="scss">
/* 组件容器 */
.file-upload {
  max-width: 720px;
  margin: 0 auto;
}

/* ================================================================== */
/* 拖拽上传区域 */
/* ================================================================== */
.upload-dragger {
  margin-bottom: 24px;

  /* 覆盖 el-upload 内部 dragger 样式 */
  :deep(.el-upload-dragger) {
    padding: 40px 20px;
    border: 2px dashed #dcdfe6;
    border-radius: 8px;
    background: #fafafa;
    transition: border-color 0.3s;

    &:hover {
      border-color: #409eff;
    }
  }

  :deep(.el-upload) {
    width: 100%;
  }

  .dragger-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;

    .dragger-icon {
      font-size: 48px;
      color: #c0c4cc;
    }

    .dragger-text {
      text-align: center;

      .dragger-main-text {
        font-size: 16px;
        color: #606266;
        margin: 0 0 4px;
      }

      .dragger-hint {
        font-size: 13px;
        color: #909399;
        margin: 0;
      }
    }

    .dragger-btn {
      margin-top: 4px;
    }
  }
}

/* ================================================================== */
/* 上传队列 */
/* ================================================================== */
.upload-queue {
  margin-bottom: 24px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;

  .queue-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    background: #f5f7fa;
    border-bottom: 1px solid #e4e7ed;

    .queue-title {
      font-size: 14px;
      font-weight: 600;
      color: #303133;
    }

    .queue-count {
      font-size: 12px;
      color: #909399;
    }
  }

  .queue-list {
    padding: 8px 0;
  }

  .queue-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 16px;
    transition: background-color 0.2s;

    &:hover {
      background: #f5f7fa;
    }

    &:not(:last-child) {
      border-bottom: 1px solid #f0f2f5;
    }
  }

  /* 文件信息（图标 + 文件名） */
  .queue-file-info {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    min-width: 0;

    .file-icon {
      font-size: 18px;
      color: #909399;
      flex-shrink: 0;
    }

    .file-name {
      font-size: 14px;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  /* 状态区域 */
  .queue-status {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
    margin-left: 16px;
  }

  /* 上传中状态 */
  .queue-status--uploading {
    min-width: 180px;

    .upload-progress {
      flex: 1;
    }

    .percentage-text {
      font-size: 12px;
      color: #409eff;
      font-weight: 600;
      min-width: 32px;
      text-align: right;
    }

    .status-icon {
      font-size: 16px;
      color: #409eff;
    }

    .spinning {
      animation: spin 1.5s linear infinite;
    }
  }

  /* 已完成状态 */
  .queue-status--success {
    :deep(.el-tag) {
      .el-icon {
        margin-right: 4px;
        vertical-align: middle;
      }
    }
  }

  /* 失败状态 */
  .queue-status--fail {
    :deep(.el-tag) {
      .el-icon {
        margin-right: 4px;
        vertical-align: middle;
      }
    }
  }
}

/* ================================================================== */
/* 上传设置面板 */
/* ================================================================== */
.upload-settings {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;

  .settings-title {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 16px;
  }

  .settings-form {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .settings-row {
    display: flex;
    align-items: center;
    gap: 12px;

    .settings-label {
      font-size: 14px;
      color: #606266;
      min-width: 70px;
      flex-shrink: 0;
    }

    .settings-select {
      flex: 1;
    }

    .settings-input {
      flex: 1;
    }
  }

  .settings-checkboxes {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding-left: 82px;
  }
}

/* ================================================================== */
/* 动画 */
/* ================================================================== */
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
