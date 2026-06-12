<!--
================================================================================
文档编辑页 - views/document/DocumentEdit.vue
================================================================================

【文件说明】
文档编辑页面，提供 Markdown 编辑器、实时预览、AI 辅助建议等功能。
支持新建和编辑已有文档两种模式。

【原型对照】
- 原型位置：prototype-pc.html「文档编辑器」屏幕（2312-2409 行）
- 行内标题编辑 + "上次保存" 时间戳
- 预览/保存草稿/提交审核 三按钮
- Markdown 工具栏（加粗/斜体/下划线/标题/列表/链接/图片/表格/代码/AI辅助）
- 分栏编辑区（编辑+预览）
- AI 建议浮层（蓝色左边框 + 灯泡图标）
- 底部信息栏（分类/标签/作者/字数）

【Vue 概念】
- 动态路由参数：route.params.id 判断新建还是编辑
- el-form 表单组件：数据绑定和验证
- marked 库：Markdown → HTML 实时渲染
- computed 计算属性：字数统计、预览 HTML
================================================================================
-->

<template>
  <div class="document-edit-page">
    <!-- ================================================================ -->
    <!-- 编辑器头部：行内标题 + 时间戳 + 操作按钮（匹配原型） -->
    <!-- ================================================================ -->
    <div class="editor-header">
      <div class="header-left">
        <el-button text @click="goBack" :icon="ArrowLeft" class="back-btn" />
        <div class="title-area">
          <input
            v-model="formData.title"
            class="inline-title"
            placeholder="请输入文档标题"
            maxlength="100"
          />
          <div class="save-info">
            <el-icon><Clock /></el-icon>
            <span>{{ lastSaveText }}</span>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <el-button @click="togglePreview">
          <el-icon><View /></el-icon>
          {{ showPreview ? '关闭预览' : '预览' }}
        </el-button>
        <el-button @click="saveDraft" :loading="savingDraft">
          <el-icon><FolderOpened /></el-icon>
          保存草稿
        </el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          <el-icon><Promotion /></el-icon>
          {{ isEdit ? '保存修改' : '提交审核' }}
        </el-button>
      </div>
    </div>

    <div class="editor-body">
      <!-- ============================================================== -->
      <!-- 左侧：编辑区 -->
      <!-- ============================================================== -->
      <div class="editor-main" :class="{ 'full-width': !showPreview }">
        <!-- Markdown 工具栏（匹配原型） -->
        <div class="toolbar">
          <el-tooltip content="加粗" placement="bottom">
            <el-button text @click="insertMarkdown('**', '**')">
              <el-icon><bold /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="斜体" placement="bottom">
            <el-button text @click="insertMarkdown('*', '*')">
              <el-icon><italic /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="删除线" placement="bottom">
            <el-button text @click="insertMarkdown('~~', '~~')">
              <el-icon><strikethrough /></el-icon>
            </el-button>
          </el-tooltip>
          <el-divider direction="vertical" />
          <el-tooltip content="标题" placement="bottom">
            <el-button text @click="insertMarkdown('\n## ', '\n')">
              <el-icon><heading /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="无序列表" placement="bottom">
            <el-button text @click="insertMarkdown('\n- ', '\n')">
              <el-icon><list /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="有序列表" placement="bottom">
            <el-button text @click="insertMarkdown('\n1. ', '\n')">
              <el-icon><list-ordered /></el-icon>
            </el-button>
          </el-tooltip>
          <el-divider direction="vertical" />
          <el-tooltip content="链接" placement="bottom">
            <el-button text @click="insertMarkdown('[', '](url)')">
              <el-icon><link /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="图片" placement="bottom">
            <el-button text @click="insertMarkdown('![alt](', ')')">
              <el-icon><picture-filled /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="表格" placement="bottom">
            <el-button text @click="insertTable">
              <el-icon><grid /></el-icon>
            </el-button>
          </el-tooltip>
          <el-divider direction="vertical" />
          <el-tooltip content="代码块" placement="bottom">
            <el-button text @click="insertMarkdown('\n```\n', '\n```\n')">
              <el-icon><document /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="AI 辅助" placement="bottom">
            <el-button text class="ai-btn" @click="showAiSuggestion = !showAiSuggestion">
              <el-icon><magic-stick /></el-icon>
              <span class="ai-label">AI辅助</span>
            </el-button>
          </el-tooltip>
          <el-divider direction="vertical" />
          <el-tooltip content="上传附件" placement="bottom">
            <el-button text @click="uploadDialogVisible = true">
              <el-icon><upload-filled /></el-icon>
            </el-button>
          </el-tooltip>

          <!-- 右侧快捷操作 -->
          <div class="toolbar-right">
            <span class="word-count">
              <el-icon><Document /></el-icon>
              {{ wordCount }} 字
            </span>
          </div>
        </div>

        <!-- AI 建议浮层（匹配原型：蓝色左边框 + 灯泡图标） -->
        <div v-if="showAiSuggestion" class="ai-suggestion">
          <div class="ai-suggestion-header">
            <el-icon color="#2563eb"><MagicStick /></el-icon>
            <span>AI 建议</span>
            <el-button text size="small" @click="showAiSuggestion = false">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
          <div class="ai-suggestion-body">
            建议添加发布风险评估部分的详细说明，包含风险等级划分和应对策略。
          </div>
          <div class="ai-suggestion-actions">
            <el-button size="small" type="primary" @click="applyAiSuggestion">采纳建议</el-button>
            <el-button size="small" @click="showAiSuggestion = false">忽略</el-button>
          </div>
        </div>

        <!-- Markdown 编辑区 -->
        <div class="editor-content">
          <textarea
            ref="editorRef"
            v-model="formData.content"
            class="markdown-editor"
            placeholder="请输入文档内容（支持 Markdown 格式）"
            @input="onContentChange"
          />
        </div>
      </div>

      <!-- ============================================================== -->
      <!-- 右侧：实时预览（可切换显隐） -->
      <!-- ============================================================== -->
      <div v-if="showPreview" class="preview-panel">
        <div class="preview-header">预览</div>
        <div class="preview-content markdown-body" v-html="previewHtml" />
      </div>
    </div>

    <!-- ================================================================ -->
    <!-- 底部信息栏（匹配原型：分类/标签/作者/字数） -->
    <!-- ================================================================ -->
    <div class="editor-footer">
      <div class="footer-item">
        <el-icon><Folder /></el-icon>
        <span>分类：</span>
        <el-select
          v-model="formData.categoryId"
          placeholder="选择分类"
          size="small"
          style="width: 140px"
        >
          <el-option
            v-for="cat in categoryOptions"
            :key="cat.value"
            :label="cat.label"
            :value="cat.value"
          />
        </el-select>
      </div>
      <div class="footer-item">
        <el-icon><PriceTag /></el-icon>
        <span>标签：</span>
        <el-tag
          v-for="tag in formData.tags"
          :key="tag"
          closable
          size="small"
          class="footer-tag"
          @close="removeTag(tag)"
        >
          {{ tag }}
        </el-tag>
        <el-input
          v-if="tagInputVisible"
          ref="tagInputRef"
          v-model="tagInputValue"
          size="small"
          class="footer-tag-input"
          @keyup.enter="addTag"
          @blur="addTag"
        />
        <el-button v-else size="small" text @click="showTagInput">+ 标签</el-button>
      </div>
      <div class="footer-item">
        <el-icon><User /></el-icon>
        <span>作者：{{ authorName }}</span>
      </div>
      <div class="footer-item">
        <el-icon><Document /></el-icon>
        <span>字数：{{ wordCount }}</span>
      </div>
    </div>

    <!-- ================================================================ -->
    <!-- 文件上传弹窗 -->
    <!-- ================================================================ -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传附件"
      width="680px"
      destroy-on-close
    >
      <FileUpload />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, Clock, View, FolderOpened, Promotion, Close,
  Folder, PriceTag, User, Document, PictureFilled, Grid,
  MagicStick, UploadFilled
} from '@element-plus/icons-vue'
import { documentApi } from '@/api/document'
import { useUserStore } from '@/stores/user'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import FileUpload from '@/components/common/FileUpload.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isEdit = computed(() => !!route.params.id)
const submitting = ref(false)
const savingDraft = ref(false)
const showPreview = ref(false)
const showAiSuggestion = ref(false)
const uploadDialogVisible = ref(false)
const editorRef = ref<HTMLTextAreaElement>()

const formData = reactive({
  title: '',
  categoryId: undefined as number | undefined,
  tags: [] as string[],
  summary: '',
  content: ''
})

const categoryOptions = [
  { label: '人事制度', value: 1 },
  { label: '技术文档', value: 2 },
  { label: '销售支持', value: 3 },
  { label: '合规法务', value: 4 }
]

// 作者名（从 store 读取）
const authorName = computed(() => userStore.currentUser?.username || '未知')

// 字数统计
const wordCount = computed(() => {
  if (!formData.content) return 0
  return formData.content.replace(/\s/g, '').length
})

// 上次保存文案
const lastSaveText = ref('尚未保存')

// Markdown 实时预览（使用 marked 渲染 + DOMPurify 防XSS）
const previewHtml = computed(() => {
  if (!formData.content) return '<p style="color:#909399;">暂无内容</p>'
  const raw = marked(formData.content) as string
  return DOMPurify.sanitize(raw)
})

// 工具栏：插入 Markdown 语法
const insertMarkdown = (before: string, after: string) => {
  const textarea = editorRef.value
  if (!textarea) return

  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selected = formData.content.substring(start, end)
  const replacement = before + (selected || '文本') + after

  formData.content =
    formData.content.substring(0, start) + replacement + formData.content.substring(end)

  nextTick(() => {
    textarea.focus()
    textarea.setSelectionRange(
      start + before.length,
      start + before.length + (selected || '文本').length
    )
  })
}

// 插入表格模板
const insertTable = () => {
  const table = '\n| 列1 | 列2 | 列3 |\n|------|------|------|\n| 内容 | 内容 | 内容 |\n'
  insertMarkdown(table, '')
}

// AI 辅助：采纳建议插入内容
const applyAiSuggestion = () => {
  const suggestion = '\n## 发布风险评估\n\n### 风险等级划分\n- **高风险**：影响核心功能，需 CEO 审批\n- **中风险**：影响部分功能，需产品总监审批\n- **低风险**：仅 UI 优化，需产品经理确认\n\n### 应对策略\n1. 建立回滚预案\n2. 灰度发布验证\n3. 监控关键指标\n'
  formData.content += suggestion
  showAiSuggestion.value = false
  ElMessage.success('已采纳 AI 建议')
}

const onContentChange = () => {
  // 内容变化时触发（可用于自动保存）
}

const togglePreview = () => {
  showPreview.value = !showPreview.value
}

// Tag 输入
const tagInputVisible = ref(false)
const tagInputValue = ref('')
const tagInputRef = ref()

const showTagInput = () => {
  tagInputVisible.value = true
  nextTick(() => tagInputRef.value?.focus())
}

const addTag = () => {
  const val = tagInputValue.value.trim()
  if (val && !formData.tags.includes(val)) {
    formData.tags.push(val)
  }
  tagInputVisible.value = false
  tagInputValue.value = ''
}

const removeTag = (tag: string) => {
  formData.tags = formData.tags.filter(t => t !== tag)
}

const goBack = () => {
  router.push('/documents')
}

const handleSubmit = async () => {
  if (!formData.title.trim()) {
    ElMessage.warning('请输入文档标题')
    return
  }
  if (!formData.content.trim()) {
    ElMessage.warning('请输入文档内容')
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await documentApi.update(Number(route.params.id), formData)
      ElMessage.success('文档更新成功')
    } else {
      await documentApi.create({ ...formData, status: 'PUBLISHED' })
      ElMessage.success('文档已提交审核')
    }
    router.push('/documents')
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const saveDraft = async () => {
  savingDraft.value = true
  try {
    if (isEdit.value) {
      await documentApi.update(Number(route.params.id), { ...formData, status: 'DRAFT' })
    } else {
      await documentApi.create({ ...formData, status: 'DRAFT' })
    }
    lastSaveText.value = '刚刚保存'
    ElMessage.success('草稿已保存')
  } catch {
    lastSaveText.value = '保存失败'
    ElMessage.warning('草稿保存失败（本地已保留）')
  } finally {
    savingDraft.value = false
  }
}

onMounted(async () => {
  if (isEdit.value) {
    try {
      const res = await documentApi.getDetail(Number(route.params.id))
      const doc = res.data
      formData.title = doc.title
      formData.categoryId = doc.category?.id
      formData.tags = doc.tags || []
      formData.summary = doc.summary || ''
      formData.content = doc.content || ''
      lastSaveText.value = '已加载最新版本'
    } catch {
      ElMessage.error('加载文档失败')
    }
  }
})
</script>

<style scoped lang="scss">
/* 页面容器 */
.document-edit-page {
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
  background: #f8fafc;
}

/* ================================================================ */
/* 编辑器头部（匹配原型：行内标题 + 时间戳 + 操作按钮） */
/* ================================================================ */
.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;

  .header-left {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    min-width: 0;

    .back-btn {
      flex-shrink: 0;
    }

    .title-area {
      flex: 1;
      min-width: 0;

      .inline-title {
        display: block;
        width: 100%;
        font-size: 1.3rem;
        font-weight: 600;
        border: none;
        outline: none;
        color: #1e293b;
        background: transparent;
        padding: 0;

        &::placeholder {
          color: #c0c4cc;
        }
      }

      .save-info {
        display: flex;
        align-items: center;
        gap: 4px;
        color: #909399;
        font-size: 0.85rem;
        margin-top: 4px;
      }
    }
  }

  .header-actions {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
  }
}

/* ================================================================ */
/* 编辑器主体（左编辑 + 右预览 分栏） */
/* ================================================================ */
.editor-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.editor-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;

  &.full-width {
    flex: 1;
  }
}

/* 工具栏 */
.toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 6px 12px;
  background: #f1f5f9;
  border-bottom: 1px solid #e2e8f0;
  flex-wrap: wrap;
  flex-shrink: 0;

  :deep(.el-button) {
    padding: 6px 8px;
    font-size: 14px;
    border-radius: 4px;
  }

  :deep(.el-divider--vertical) {
    height: 20px;
    margin: 0 4px;
  }

  .ai-btn {
    color: #2563eb;

    .ai-label {
      font-size: 13px;
      margin-left: 2px;
    }
  }

  .toolbar-right {
    margin-left: auto;
    display: flex;
    align-items: center;

    .word-count {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 0.85rem;
      color: #909399;
    }
  }
}

/* AI 建议浮层（匹配原型：蓝色左边框 + 灯泡图标） */
.ai-suggestion {
  background: rgba(37, 99, 235, 0.05);
  border-left: 3px solid #2563eb;
  padding: 12px 16px;
  margin: 0;
  flex-shrink: 0;

  .ai-suggestion-header {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 600;
    color: #2563eb;
    margin-bottom: 6px;
    font-size: 0.9rem;

    .el-button {
      margin-left: auto;
    }
  }

  .ai-suggestion-body {
    font-size: 0.9rem;
    color: #64748b;
    line-height: 1.6;
  }

  .ai-suggestion-actions {
    margin-top: 8px;
    display: flex;
    gap: 8px;
  }
}

/* Markdown 编辑区 */
.editor-content {
  flex: 1;
  overflow: hidden;

  .markdown-editor {
    width: 100%;
    height: 100%;
    padding: 20px;
    border: none;
    outline: none;
    resize: none;
    font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
    font-size: 14px;
    line-height: 1.8;
    color: #1e293b;
    background: #fff;
    box-sizing: border-box;
  }
}

/* ================================================================ */
/* 预览面板 */
/* ================================================================ */
.preview-panel {
  width: 50%;
  border-left: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  background: #fff;
  flex-shrink: 0;

  .preview-header {
    padding: 10px 20px;
    font-weight: 600;
    font-size: 0.9rem;
    color: #64748b;
    background: #f8fafc;
    border-bottom: 1px solid #e2e8f0;
  }

  .preview-content {
    flex: 1;
    padding: 20px;
    overflow-y: auto;
  }
}

/* Markdown 渲染样式 */
.markdown-body {
  font-size: 15px;
  line-height: 1.8;
  color: #1e293b;

  :deep(h1), :deep(h2), :deep(h3) {
    margin: 20px 0 10px;
    font-weight: 600;
  }

  :deep(h2) { font-size: 1.3rem; }
  :deep(h3) { font-size: 1.1rem; }

  :deep(p) {
    margin-bottom: 12px;
    color: #475569;
  }

  :deep(ul), :deep(ol) {
    margin-left: 20px;
    margin-bottom: 12px;
    color: #475569;
  }

  :deep(code) {
    background: #f1f5f9;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 0.9em;
  }

  :deep(pre) {
    background: #1e293b;
    color: #e2e8f0;
    padding: 16px;
    border-radius: 8px;
    overflow-x: auto;
    margin: 12px 0;

    code {
      background: none;
      padding: 0;
      color: inherit;
    }
  }

  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: 12px 0;

    th, td {
      border: 1px solid #e2e8f0;
      padding: 8px 12px;
      text-align: left;
    }

    th {
      background: #f8fafc;
      font-weight: 600;
    }
  }

  :deep(blockquote) {
    border-left: 3px solid #2563eb;
    padding: 8px 16px;
    margin: 12px 0;
    background: rgba(37, 99, 235, 0.05);
    color: #475569;
  }
}

/* ================================================================ */
/* 底部信息栏（匹配原型：分类/标签/作者/字数） */
/* ================================================================ */
.editor-footer {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 8px 20px;
  background: #f1f5f9;
  border-top: 1px solid #e2e8f0;
  font-size: 0.85rem;
  color: #64748b;
  flex-wrap: wrap;
  flex-shrink: 0;

  .footer-item {
    display: flex;
    align-items: center;
    gap: 4px;

    .footer-tag {
      margin-right: 4px;
    }

    .footer-tag-input {
      width: 100px;
    }
  }
}
</style>
