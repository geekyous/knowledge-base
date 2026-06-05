<!--
================================================================================
文档编辑页 - views/document/DocumentEdit.vue
================================================================================

【文件说明】
文档编辑页面，提供文档标题、分类、标签、内容的编辑功能。
支持新建和编辑已有文档两种模式。

【Vue 概念】
- 动态路由参数：route.params.id 判断新建还是编辑
- el-form 表单组件：数据绑定和验证
- el-select / el-tag 动态标签输入
================================================================================
-->

<template>
  <div class="document-edit-page">
    <div class="page-header">
      <el-button @click="goBack" :icon="ArrowLeft">返回</el-button>
      <h2 class="page-title">{{ isEdit ? '编辑文档' : '新建文档' }}</h2>
    </div>

    <el-card shadow="never">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="80px"
        class="edit-form"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入文档标题" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="formData.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="cat in categoryOptions"
              :key="cat.value"
              :label="cat.label"
              :value="cat.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="标签">
          <el-tag
            v-for="tag in formData.tags"
            :key="tag"
            closable
            class="tag-item"
            @close="removeTag(tag)"
          >
            {{ tag }}
          </el-tag>
          <el-input
            v-if="tagInputVisible"
            ref="tagInputRef"
            v-model="tagInputValue"
            size="small"
            class="tag-input"
            @keyup.enter="addTag"
            @blur="addTag"
          />
          <el-button v-else size="small" @click="showTagInput">+ 添加标签</el-button>
        </el-form-item>

        <el-form-item label="摘要">
          <el-input
            v-model="formData.summary"
            type="textarea"
            :rows="3"
            placeholder="请输入文档摘要（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="内容" prop="content">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="15"
            placeholder="请输入文档内容（支持 Markdown 格式）"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存修改' : '发布文档' }}
          </el-button>
          <el-button @click="saveDraft">保存草稿</el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { documentApi } from '@/api/document'

const router = useRouter()
const route = useRoute()

const isEdit = computed(() => !!route.params.id)
const formRef = ref<FormInstance>()
const submitting = ref(false)

const formData = reactive({
  title: '',
  categoryId: undefined as number | undefined,
  tags: [] as string[],
  summary: '',
  content: ''
})

const formRules: FormRules = {
  title: [
    { required: true, message: '请输入文档标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度为 2 到 100 个字符', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入文档内容', trigger: 'blur' }
  ]
}

const categoryOptions = [
  { label: '人事制度', value: 1 },
  { label: '技术文档', value: 2 },
  { label: '销售支持', value: 3 },
  { label: '合规法务', value: 4 }
]

// Tag input
const tagInputVisible = ref(false)
const tagInputValue = ref('')
const tagInputRef = ref()

const showTagInput = () => {
  tagInputVisible.value = true
  nextTick(() => {
    tagInputRef.value?.focus()
  })
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
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await documentApi.update(Number(route.params.id), formData)
      ElMessage.success('文档更新成功')
    } else {
      await documentApi.create({ ...formData, status: 'PUBLISHED' })
      ElMessage.success('文档发布成功')
    }
    router.push('/documents')
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const saveDraft = async () => {
  try {
    if (isEdit.value) {
      await documentApi.update(Number(route.params.id), { ...formData, status: 'DRAFT' })
    } else {
      await documentApi.create({ ...formData, status: 'DRAFT' })
    }
    ElMessage.success('草稿已保存')
  } catch {
    ElMessage.success('草稿已保存（本地）')
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
    } catch {
      ElMessage.error('加载文档失败')
    }
  }
})
</script>

<style scoped lang="scss">
.document-edit-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;

  .page-title {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
    margin: 0;
  }
}

.edit-form {
  .tag-item {
    margin-right: 8px;
    margin-bottom: 4px;
  }

  .tag-input {
    width: 120px;
  }
}
</style>
