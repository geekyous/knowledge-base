<!--
================================================================================
用户管理页面 - views/admin/Users.vue
================================================================================

【文件说明】
管理员后台的用户管理页面，提供用户列表的查看、搜索、筛选与操作。
包含：
1. 页面标题 + "管理后台"徽章
2. 工具栏：搜索框、角色筛选、状态筛选、新建用户按钮
3. 用户数据表格（含复选框、角色/状态标签、操作按钮）
4. 分页组件

【Vue 概念】
- el-table + el-table-column 数据表格
- el-select 下拉筛选
- el-input 搜索输入
- el-tag / el-badge 标签与徽章
- ElMessageBox.confirm 确认对话框
- Composition API 响应式数据管理

💡 学习要点:
1. el-table 的 selection 列实现复选框
2. 多条件筛选：computed 过滤 + watch 联动
3. 操作按钮根据用户状态动态显示（禁用/锁定/解锁）
4. ElMessageBox.confirm 实现删除确认
================================================================================
-->

<template>
  <div class="admin-users">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">用户管理</h1>
      <el-tag type="warning" effect="plain" size="small">管理后台</el-tag>
    </div>

    <!-- 工具栏：搜索 + 筛选 + 操作 -->
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-filters">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索用户名或邮箱"
            :prefix-icon="Search"
            clearable
            style="width: 250px"
          />
          <el-select v-model="roleFilter" placeholder="所有角色" clearable style="width: 130px">
            <el-option label="所有角色" value="" />
            <el-option label="USER" value="USER" />
            <el-option label="EDITOR" value="EDITOR" />
            <el-option label="ADMIN" value="ADMIN" />
          </el-select>
          <el-select v-model="statusFilter" placeholder="所有状态" clearable style="width: 130px">
            <el-option label="所有状态" value="" />
            <el-option label="活跃" value="ACTIVE" />
            <el-option label="已禁用" value="INACTIVE" />
            <el-option label="已锁定" value="LOCKED" />
          </el-select>
        </div>
        <el-button type="primary" :icon="Plus" @click="handleCreate">新建用户</el-button>
      </div>
    </el-card>

    <!-- 用户表格 -->
    <el-card shadow="never" class="table-card" v-loading="loading">
      <el-table
        ref="tableRef"
        :data="users"
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <!-- 复选框列 -->
        <el-table-column type="selection" width="50" />

        <!-- 用户名列 -->
        <el-table-column prop="username" label="用户名" min-width="140">
          <template #default="{ row }">
            <span class="username-text">{{ row.username }}</span>
          </template>
        </el-table-column>

        <!-- 邮箱列 -->
        <el-table-column prop="email" label="邮箱" min-width="200">
          <template #default="{ row }">
            <span class="email-text">{{ row.email }}</span>
          </template>
        </el-table-column>

        <!-- 角色列（Badge 标签） -->
        <el-table-column prop="role" label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small" effect="plain">
              {{ row.role }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 状态列（Badge 标签） -->
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 最后登录列 -->
        <el-table-column prop="lastLoginAt" label="最后登录" width="170">
          <template #default="{ row }">
            <span class="login-time-text">{{ row.lastLoginAt || '—' }}</span>
          </template>
        </el-table-column>

        <!-- 操作列 -->
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <!-- 编辑 -->
              <el-tooltip content="编辑" placement="top">
                <el-button link type="primary" :icon="Edit" @click="handleEdit(row)" />
              </el-tooltip>
              <!-- 重置密码 -->
              <el-tooltip content="重置密码" placement="top">
                <el-button link type="warning" :icon="Key" @click="handleResetPassword(row)" />
              </el-tooltip>
              <!-- 禁用（仅活跃/锁定用户显示） -->
              <el-tooltip v-if="row.status === 'ACTIVE'" content="禁用" placement="top">
                <el-button link type="warning" :icon="Lock" @click="handleDisable(row)" />
              </el-tooltip>
              <!-- 解锁（仅锁定用户显示） -->
              <el-tooltip v-if="row.status === 'LOCKED'" content="解锁" placement="top">
                <el-button link type="success" :icon="Unlock" @click="handleUnlock(row)" />
              </el-tooltip>
              <!-- 删除 -->
              <el-tooltip content="删除" placement="top">
                <el-button link type="danger" :icon="Delete" @click="handleDelete(row)" />
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <span class="total-text">共 {{ total }} 条记录</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="sizes, prev, pager, next"
          background
          small
        />
      </div>
    </el-card>

    <!-- 新建/编辑用户对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '新建用户'"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="80px"
        label-position="right"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="formData.username"
            placeholder="请输入用户名"
            maxlength="30"
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            show-password
            placeholder="请输入密码"
            maxlength="50"
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="formData.email"
            placeholder="请输入邮箱"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="formData.phone"
            placeholder="请输入手机号"
            maxlength="20"
          />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="formData.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="普通用户" value="USER" />
            <el-option label="编辑者" value="EDITOR" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态" prop="status">
          <el-select v-model="formData.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="活跃" value="ACTIVE" />
            <el-option label="已禁用" value="INACTIVE" />
            <el-option label="已锁定" value="LOCKED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  Search,
  Plus,
  Edit,
  Key,
  Lock,
  Unlock,
  Delete
} from '@element-plus/icons-vue'
import { adminUserApi } from '@/api/admin'
import type { AdminUser, CreateUserRequest, UpdateUserRequest } from '@/types'

// ==================== 筛选状态 ====================
const searchKeyword = ref('')
const roleFilter = ref('')
const statusFilter = ref('')

// ==================== 分页状态 ====================
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// ==================== 加载状态 ====================
const loading = ref(false)

// ==================== 表格引用 ====================
const tableRef = ref()
const selectedUsers = ref<AdminUser[]>([])

// ==================== 用户数据（服务端加载） ====================
const users = ref<AdminUser[]>([])

/** 从后端加载用户列表 */
const loadUsers = async () => {
  loading.value = true
  try {
    const res = await adminUserApi.list({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      role: roleFilter.value || undefined,
      status: statusFilter.value || undefined
    })
    users.value = res.data.items
    total.value = res.data.total
  } catch {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

// ==================== 筛选/分页变化时重新请求 ====================

// 搜索、角色、状态变化 → 重置到第 1 页并重新加载
watch([searchKeyword, roleFilter, statusFilter], () => {
  currentPage.value = 1
  loadUsers()
})

// 分页参数变化 → 直接重新加载
watch([currentPage, pageSize], () => {
  loadUsers()
})

onMounted(() => {
  loadUsers()
})

// ==================== 标签映射 ====================

/** 角色对应的 Tag 类型 */
const roleTagType = (role: string): string => {
  const map: Record<string, string> = {
    ADMIN: 'danger',
    EDITOR: '',
    USER: 'info'
  }
  return map[role] || 'info'
}

/** 状态对应的 Tag 类型 */
const statusTagType = (status: string): string => {
  const map: Record<string, string> = {
    ACTIVE: 'success',
    INACTIVE: 'warning',
    LOCKED: 'danger'
  }
  return map[status] || 'info'
}

/** 状态对应的中文文本 */
const statusLabel = (status: string): string => {
  const map: Record<string, string> = {
    ACTIVE: '活跃',
    INACTIVE: '已禁用',
    LOCKED: '已锁定'
  }
  return map[status] || status
}

// ==================== 事件处理 ====================

/** 表格选择变更 */
const handleSelectionChange = (selection: AdminUser[]) => {
  selectedUsers.value = selection
}

// ==================== 新建/编辑对话框 ====================

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive({
  id: 0 as number,
  username: '',
  password: '',
  email: '',
  phone: '',
  role: 'USER' as string,
  status: 'ACTIVE' as string
})

/** 表单校验规则 */
const formRules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 30, message: '长度在 2 到 30 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 50, message: '长度在 6 到 50 个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
})

/** 重置表单数据 */
const resetForm = () => {
  formData.id = 0
  formData.username = ''
  formData.password = ''
  formData.email = ''
  formData.phone = ''
  formData.role = 'USER'
  formData.status = 'ACTIVE'
  formRef.value?.resetFields()
}

/** 新建用户 */
const handleCreate = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

/** 编辑用户 */
const handleEdit = (row: AdminUser) => {
  isEdit.value = true
  formData.id = row.id
  formData.username = row.username
  formData.password = ''
  formData.email = row.email || ''
  formData.phone = row.phone || ''
  formData.role = row.role
  formData.status = row.status
  dialogVisible.value = true
}

/** 提交表单（新建或编辑） */
const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      const data: UpdateUserRequest = {
        email: formData.email || undefined,
        phone: formData.phone || undefined,
        role: formData.role as UpdateUserRequest['role'],
        status: formData.status as UpdateUserRequest['status']
      }
      await adminUserApi.update(formData.id, data)
      ElMessage.success(`用户「${formData.username}」更新成功`)
    } else {
      const data: CreateUserRequest = {
        username: formData.username,
        password: formData.password,
        email: formData.email || undefined,
        phone: formData.phone || undefined,
        role: formData.role as CreateUserRequest['role']
      }
      await adminUserApi.create(data)
      ElMessage.success(`用户「${formData.username}」创建成功`)
    }
    dialogVisible.value = false
    loadUsers()
  } catch {
    ElMessage.error(isEdit.value ? '更新用户失败' : '创建用户失败')
  } finally {
    submitting.value = false
  }
}

/** 重置密码 */
const handleResetPassword = (row: AdminUser) => {
  ElMessageBox.prompt('请输入新密码', `重置用户「${row.username}」的密码`, {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputType: 'password',
    inputPattern: /^.{6,}$/,
    inputErrorMessage: '密码长度至少 6 位'
  }).then(async ({ value }) => {
    try {
      await adminUserApi.resetPassword(row.id, { newPassword: value })
      ElMessage.success(`已重置「${row.username}」的密码`)
    } catch {
      ElMessage.error('重置密码失败')
    }
  }).catch(() => {
    // 用户取消操作
  })
}

/** 禁用用户 */
const handleDisable = (row: AdminUser) => {
  ElMessageBox.confirm(
    `确认禁用用户「${row.username}」？禁用后该用户将无法登录系统。`,
    '禁用用户',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await adminUserApi.disable(row.id)
      ElMessage.success(`已禁用用户「${row.username}」`)
      loadUsers()
    } catch {
      ElMessage.error('禁用用户失败')
    }
  }).catch(() => {
    // 用户取消操作
  })
}

/** 解锁用户 */
const handleUnlock = (row: AdminUser) => {
  ElMessageBox.confirm(
    `确认解锁用户「${row.username}」？`,
    '解锁用户',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(async () => {
    try {
      await adminUserApi.unlock(row.id)
      ElMessage.success(`已解锁用户「${row.username}」`)
      loadUsers()
    } catch {
      ElMessage.error('解锁用户失败')
    }
  }).catch(() => {
    // 用户取消操作
  })
}

/** 删除用户 */
const handleDelete = (row: AdminUser) => {
  ElMessageBox.confirm(
    `确认删除用户「${row.username}」？此操作不可恢复。`,
    '删除用户',
    {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'error',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(async () => {
    try {
      await adminUserApi.delete(row.id)
      ElMessage.success(`已删除用户「${row.username}」`)
      loadUsers()
    } catch {
      ElMessage.error('删除用户失败')
    }
  }).catch(() => {
    // 用户取消操作
  })
}
</script>

<style scoped lang="scss">
/* 页面容器 */
.admin-users {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

/* 页面标题区域 */
.page-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

/* 工具栏卡片 */
.toolbar-card {
  margin-bottom: 16px;

  :deep(.el-card__body) {
    padding: 16px 20px;
  }
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.toolbar-filters {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

/* 表格卡片 */
.table-card {
  margin-bottom: 16px;
}

/* 用户名加粗 */
.username-text {
  font-weight: 600;
  color: #303133;
}

/* 邮箱次要颜色 */
.email-text {
  color: #909399;
}

/* 最后登录时间次要颜色 */
.login-time-text {
  color: #909399;
  font-size: 0.9rem;
}

/* 操作按钮组 */
.action-buttons {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 分页区域 */
.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f2f5;
}

.total-text {
  font-size: 0.85rem;
  color: #909399;
}
</style>
