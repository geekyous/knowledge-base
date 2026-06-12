<!--
================================================================================
分类管理 - views/admin/Categories.vue
================================================================================

【文件说明】
管理员后台的分类管理页面，用于维护文档分类体系。
包含：
1. 左侧分类树结构（el-tree），支持展开/折叠、选中、拖拽排序
2. 右侧分类详情编辑表单（分类名称、上级分类、图标、排序）
3. 新建/保存/取消/删除操作

【Vue 概念】
- el-tree 树形组件：:data 绑定树数据，node-key 唯一标识，default-expand-all 展开全部
- el-tree 自定义节点内容：#default="{ node, data }" 插槽
- el-form 表单组件：:model 绑定表单数据，:rules 验证规则
- el-input-number 数字输入框：:min/:max 限制范围
- el-select 下拉选择器：:options 绑定选项列表

💡 学习要点:
1. el-tree 的 props 属性配置 children/label 字段映射
2. handleNodeClick 事件处理树节点选中
3. ref() 响应式数据驱动表单双向绑定
4. el-row/el-col :span="8"/:span="16" 实现 1:2 两栏布局
================================================================================
-->

<template>
  <div class="admin-categories">
    <div class="page-header">
      <h1 class="page-title">分类管理</h1>
      <el-tag type="info" size="small">内容组织</el-tag>
    </div>

    <el-row :gutter="20" class="category-content">
      <!-- ================================================================== -->
      <!-- 左侧：分类结构树 -->
      <!-- ================================================================== -->
      <el-col :span="8">
        <el-card shadow="never" class="tree-card">
          <template #header>
            <div class="card-header-row">
              <span class="card-title">分类结构</span>
              <el-button type="primary" size="small" @click="handleCreate">
                <el-icon><Plus /></el-icon>
                新建分类
              </el-button>
            </div>
          </template>

          <!--
            el-tree 树形组件
            :data 绑定扁平/嵌套的树数据
            :props 配置数据字段映射
            node-key 指定唯一标识字段
            highlight-current 高亮当前选中节点
            default-expand-all 默认展开所有节点
            @node-click 节点点击事件
          -->
          <el-tree
            ref="treeRef"
            :data="categoryTree"
            :props="treeProps"
            node-key="id"
            highlight-current
            default-expand-all
            :expand-on-click-node="false"
            @node-click="handleNodeClick"
          >
            <!-- 自定义树节点内容 -->
            <template #default="{ node, data }">
              <div class="tree-node">
                <el-icon class="tree-icon">
                  <FolderOpened v-if="node.expanded && data.children?.length" />
                  <Folder v-else-if="data.children?.length" />
                  <Document v-else />
                </el-icon>
                <span class="tree-label">{{ data.label }}</span>
                <span class="tree-count">{{ data.docCount }}篇</span>
              </div>
            </template>
          </el-tree>

          <div class="tree-hint">
            <el-icon :size="14"><InfoFilled /></el-icon>
            拖拽分类可调整排序
          </div>
        </el-card>
      </el-col>

      <!-- ================================================================== -->
      <!-- 右侧：分类详情编辑表单 -->
      <!-- ================================================================== -->
      <el-col :span="16">
        <el-card shadow="never" class="detail-card">
          <template #header>
            <span class="card-title">分类详情</span>
          </template>

          <el-form
            ref="formRef"
            :model="formData"
            :rules="formRules"
            label-width="90px"
            label-position="right"
            class="category-form"
          >
            <!-- 分类名称 -->
            <el-form-item label="分类名称" prop="name">
              <el-input
                v-model="formData.name"
                placeholder="请输入分类名称"
                :maxlength="20"
                show-word-limit
              />
            </el-form-item>

            <!-- 上级分类 -->
            <el-form-item label="上级分类" prop="parentId">
              <el-select
                v-model="formData.parentId"
                placeholder="请选择上级分类"
                clearable
                style="width: 100%"
              >
                <el-option
                  label="无（顶级分类）"
                  :value="0"
                />
                <el-option
                  v-for="cat in topLevelCategories"
                  :key="cat.id"
                  :label="cat.label"
                  :value="cat.id"
                  :disabled="cat.id === formData.id"
                />
              </el-select>
            </el-form-item>

            <!-- 分类图标 -->
            <el-form-item label="分类图标" prop="icon">
              <div class="icon-selector">
                <div
                  v-for="icon in iconOptions"
                  :key="icon.value"
                  class="icon-option"
                  :class="{ active: formData.icon === icon.value }"
                  @click="formData.icon = icon.value"
                >
                  <el-icon :size="18">
                    <component :is="icon.component" />
                  </el-icon>
                </div>
              </div>
            </el-form-item>

            <!-- 排序 -->
            <el-form-item label="排序" prop="sort">
              <el-input-number
                v-model="formData.sort"
                :min="0"
                :max="999"
                controls-position="right"
                style="width: 160px"
              />
            </el-form-item>

            <!-- 操作按钮 -->
            <el-form-item>
              <div class="form-actions">
                <el-button type="primary" @click="handleSave">
                  <el-icon><Check /></el-icon>
                  保存
                </el-button>
                <el-button @click="handleCancel">
                  <el-icon><Close /></el-icon>
                  取消
                </el-button>
                <el-button
                  type="danger"
                  plain
                  @click="handleDelete"
                  :disabled="!formData.id"
                >
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  Folder,
  FolderOpened,
  Document,
  Plus,
  Delete,
  Check,
  Close,
  Briefcase,
  Files,
  Notebook,
  Setting,
  InfoFilled
} from '@element-plus/icons-vue'
import { adminCategoryApi } from '@/api/admin'
import type { CategoryTreeNode } from '@/types'

// ==================== 类型定义 ====================

/** el-tree 节点数据结构（API 返回的 name 映射为 label） */
interface CategoryNode {
  id: number
  label: string
  docCount: number
  icon?: string
  sort?: number
  children?: CategoryNode[]
}

/** 表单数据结构 */
interface CategoryForm {
  id: number
  name: string
  parentId: number
  icon: string
  sort: number
}

// ==================== Mock 分类树数据（API 失败时的 fallback） ====================

const mockTree: CategoryNode[] = [
  {
    id: 1,
    label: '人事制度',
    docCount: 128,
    icon: 'briefcase',
    sort: 1,
    children: [
      { id: 11, label: '考勤管理', docCount: 45, icon: 'files', sort: 1 },
      { id: 12, label: '薪酬福利', docCount: 32, icon: 'files', sort: 2 },
      { id: 13, label: '假期制度', docCount: 28, icon: 'files', sort: 3 }
    ]
  },
  {
    id: 2,
    label: '技术文档',
    docCount: 356,
    icon: 'briefcase',
    sort: 2,
    children: [
      { id: 21, label: '开发规范', docCount: 120, icon: 'files', sort: 1 },
      { id: 22, label: '架构设计', docCount: 89, icon: 'files', sort: 2 },
      { id: 23, label: '运维手册', docCount: 78, icon: 'files', sort: 3 }
    ]
  },
  {
    id: 3,
    label: '销售支持',
    docCount: 89,
    icon: 'briefcase',
    sort: 3
  },
  {
    id: 4,
    label: '合规法务',
    docCount: 67,
    icon: 'briefcase',
    sort: 4
  }
]

const categoryTree = ref<CategoryNode[]>([])

// ==================== 树配置 ====================

/** el-tree 字段映射配置 */
const treeProps = {
  children: 'children',
  label: 'label'
}

const treeRef = ref<InstanceType<typeof import('element-plus')['ElTree']>>()

// ==================== 图标选项 ====================

/** 分类图标可选列表 */
const iconOptions = [
  { value: 'briefcase', label: '公文包', component: Briefcase },
  { value: 'files', label: '文档', component: Files },
  { value: 'notebook', label: '笔记本', component: Notebook },
  { value: 'setting', label: '设置', component: Setting }
]

// ==================== 表单数据 ====================

const formRef = ref<FormInstance>()

/** 分类详情表单 */
const formData = ref<CategoryForm>({
  id: 0,
  name: '',
  parentId: 0,
  icon: 'briefcase',
  sort: 0
})

/** 表单验证规则 */
const formRules: FormRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { max: 20, message: '分类名称不能超过20个字符', trigger: 'blur' }
  ]
}

// ==================== API 数据加载 ====================

/** 将 API 返回的 CategoryTreeNode 递归映射为 el-tree 的 CategoryNode */
function mapTreeNode(node: CategoryTreeNode): CategoryNode {
  return {
    id: node.id,
    label: node.name,
    docCount: node.docCount,
    icon: node.icon,
    sort: node.sortOrder,
    children: node.children?.map(mapTreeNode)
  }
}

/** 加载分类树数据 */
async function loadTree() {
  try {
    const res = await adminCategoryApi.getTree()
    categoryTree.value = (res.data ?? []).map(mapTreeNode)
  } catch {
    // API 失败时使用 Mock 数据
    categoryTree.value = mockTree
  }
}

onMounted(() => {
  loadTree()
})

// ==================== 计算属性 ====================

/** 顶级分类列表（用于上级分类下拉选项） */
const topLevelCategories = computed(() => {
  return categoryTree.value.map(cat => ({
    id: cat.id,
    label: cat.label
  }))
})

// ==================== 事件处理 ====================

/** 树节点点击：加载分类详情到表单 */
const handleNodeClick = (data: CategoryNode) => {
  formData.value = {
    id: data.id,
    name: data.label,
    parentId: findParentId(data.id),
    icon: data.icon || 'briefcase',
    sort: data.sort || 0
  }
}

/** 查找某个节点的父节点 ID */
const findParentId = (nodeId: number): number => {
  for (const parent of categoryTree.value) {
    if (parent.children) {
      for (const child of parent.children) {
        if (child.id === nodeId) {
          return parent.id
        }
      }
    }
  }
  return 0
}

/** 新建分类 */
const handleCreate = () => {
  formData.value = {
    id: 0,
    name: '',
    parentId: 0,
    icon: 'briefcase',
    sort: categoryTree.value.length + 1
  }
}

/** 保存分类 */
const handleSave = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      if (formData.value.id) {
        // 编辑模式：调用更新 API
        await adminCategoryApi.update(formData.value.id, {
          name: formData.value.name,
          parentId: formData.value.parentId || undefined,
          icon: formData.value.icon,
          sortOrder: formData.value.sort
        })
        ElMessage.success(`分类「${formData.value.name}」已保存`)
      } else {
        // 新建模式：调用创建 API
        await adminCategoryApi.create({
          name: formData.value.name,
          parentId: formData.value.parentId || undefined,
          icon: formData.value.icon,
          sortOrder: formData.value.sort
        })
        ElMessage.success(`分类「${formData.value.name}」已创建`)
      }
      await loadTree()
    } catch {
      // API 失败时仍显示消息（拦截器已提示），不额外处理
    }
  })
}

/** 取消编辑 */
const handleCancel = () => {
  if (formData.value.id) {
    // 还原为选中节点的数据
    const node = findNodeById(formData.value.id)
    if (node) {
      handleNodeClick(node)
    }
  } else {
    // 新建模式：重置表单
    handleCreate()
  }
}

/** 删除分类 */
const handleDelete = async () => {
  if (!formData.value.id) return

  try {
    await ElMessageBox.confirm(
      `确定要删除分类「${formData.value.name}」吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await adminCategoryApi.delete(formData.value.id)
    ElMessage.success(`分类「${formData.value.name}」已删除`)
    handleCreate()
    await loadTree()
  } catch {
    // 用户取消删除或 API 失败，不做操作
  }
}

/** 根据 ID 在树中查找节点 */
const findNodeById = (id: number): CategoryNode | null => {
  for (const node of categoryTree.value) {
    if (node.id === id) return node
    if (node.children) {
      for (const child of node.children) {
        if (child.id === id) return child
      }
    }
  }
  return null
}
</script>

<style scoped lang="scss">
/* 分类管理页面容器 */
.admin-categories {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

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

/* ==================== 左侧树结构 ==================== */

.tree-card {
  min-height: 400px;

  /* 自定义树节点样式 */
  .tree-node {
    display: flex;
    align-items: center;
    gap: 6px;
    flex: 1;
    font-size: 14px;

    .tree-icon {
      color: #e6a23c;
      flex-shrink: 0;
    }

    .tree-label {
      font-weight: 500;
      color: #303133;
    }

    .tree-count {
      margin-left: auto;
      font-size: 12px;
      color: #909399;
      flex-shrink: 0;
    }
  }

  .tree-hint {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-top: 16px;
    padding-top: 12px;
    border-top: 1px solid #f0f2f5;
    font-size: 13px;
    color: #909399;
  }
}

/* ==================== 右侧详情表单 ==================== */

.detail-card {
  min-height: 400px;
}

.category-form {
  max-width: 500px;
}

/* 图标选择器网格 */
.icon-selector {
  display: flex;
  gap: 8px;

  .icon-option {
    width: 40px;
    height: 40px;
    border-radius: 8px;
    border: 1px solid #dcdfe6;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #909399;
    transition: all 0.2s;

    &:hover {
      border-color: #409eff;
      color: #409eff;
    }

    &.active {
      background: #409eff;
      border-color: #409eff;
      color: #fff;
    }
  }
}

/* 操作按钮组 */
.form-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}
</style>
