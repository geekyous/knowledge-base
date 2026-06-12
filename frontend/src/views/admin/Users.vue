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
            <el-option label="已禁用" value="DISABLED" />
            <el-option label="已锁定" value="LOCKED" />
          </el-select>
        </div>
        <el-button type="primary" :icon="Plus" @click="handleCreate">新建用户</el-button>
      </div>
    </el-card>

    <!-- 用户表格 -->
    <el-card shadow="never" class="table-card">
      <el-table
        ref="tableRef"
        :data="pagedUsers"
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
        <el-table-column prop="lastLogin" label="最后登录" width="170">
          <template #default="{ row }">
            <span class="login-time-text">{{ row.lastLogin }}</span>
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
        <span class="total-text">共 {{ filteredUsers.length }} 条记录</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="filteredUsers.length"
          layout="sizes, prev, pager, next"
          background
          small
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Plus,
  Edit,
  Key,
  Lock,
  Unlock,
  Delete
} from '@element-plus/icons-vue'

// ==================== 类型定义 ====================
/** 用户数据类型 */
interface UserItem {
  id: number
  username: string
  email: string
  role: 'USER' | 'EDITOR' | 'ADMIN'
  status: 'ACTIVE' | 'DISABLED' | 'LOCKED'
  lastLogin: string
}

// ==================== 筛选状态 ====================
const searchKeyword = ref('')
const roleFilter = ref('')
const statusFilter = ref('')

// ==================== 分页状态 ====================
const currentPage = ref(1)
const pageSize = ref(10)

// ==================== 表格引用 ====================
const tableRef = ref()
const selectedUsers = ref<UserItem[]>([])

// ==================== Mock 数据 ====================
const mockUsers = ref<UserItem[]>([
  {
    id: 1,
    username: 'Geekyous Guo',
    email: 'zhangsan@company.com',
    role: 'ADMIN',
    status: 'ACTIVE',
    lastLogin: '2026-06-04 09:30'
  },
  {
    id: 2,
    username: 'Alice Chen',
    email: 'lisi@company.com',
    role: 'EDITOR',
    status: 'ACTIVE',
    lastLogin: '2026-06-03 14:20'
  },
  {
    id: 3,
    username: 'Bob Wang',
    email: 'wangwu@company.com',
    role: 'USER',
    status: 'LOCKED',
    lastLogin: '2026-05-28 10:15'
  }
])

// ==================== 计算属性 ====================

/** 根据搜索和筛选条件过滤后的用户列表 */
const filteredUsers = computed(() => {
  return mockUsers.value.filter((user) => {
    // 关键字搜索：匹配用户名或邮箱
    const keyword = searchKeyword.value.trim().toLowerCase()
    const matchKeyword =
      !keyword ||
      user.username.toLowerCase().includes(keyword) ||
      user.email.toLowerCase().includes(keyword)

    // 角色筛选
    const matchRole = !roleFilter.value || user.role === roleFilter.value

    // 状态筛选
    const matchStatus = !statusFilter.value || user.status === statusFilter.value

    return matchKeyword && matchRole && matchStatus
  })
})

/** 当前页的用户列表 */
const pagedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredUsers.value.slice(start, start + pageSize.value)
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
    DISABLED: 'warning',
    LOCKED: 'danger'
  }
  return map[status] || 'info'
}

/** 状态对应的中文文本 */
const statusLabel = (status: string): string => {
  const map: Record<string, string> = {
    ACTIVE: '活跃',
    DISABLED: '已禁用',
    LOCKED: '已锁定'
  }
  return map[status] || status
}

// ==================== 事件处理 ====================

/** 表格选择变更 */
const handleSelectionChange = (selection: UserItem[]) => {
  selectedUsers.value = selection
}

/** 新建用户 */
const handleCreate = () => {
  ElMessage.info('新建用户功能开发中')
}

/** 编辑用户 */
const handleEdit = (row: UserItem) => {
  ElMessage.info(`编辑用户: ${row.username}`)
}

/** 重置密码 */
const handleResetPassword = (row: UserItem) => {
  ElMessageBox.confirm(
    `确认重置用户「${row.username}」的密码？`,
    '重置密码',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.success(`已重置「${row.username}」的密码`)
  }).catch(() => {
    // 用户取消操作
  })
}

/** 禁用用户 */
const handleDisable = (row: UserItem) => {
  ElMessageBox.confirm(
    `确认禁用用户「${row.username}」？禁用后该用户将无法登录系统。`,
    '禁用用户',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    row.status = 'DISABLED'
    ElMessage.success(`已禁用用户「${row.username}」`)
  }).catch(() => {
    // 用户取消操作
  })
}

/** 解锁用户 */
const handleUnlock = (row: UserItem) => {
  ElMessageBox.confirm(
    `确认解锁用户「${row.username}」？`,
    '解锁用户',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(() => {
    row.status = 'ACTIVE'
    ElMessage.success(`已解锁用户「${row.username}」`)
  }).catch(() => {
    // 用户取消操作
  })
}

/** 删除用户 */
const handleDelete = (row: UserItem) => {
  ElMessageBox.confirm(
    `确认删除用户「${row.username}」？此操作不可恢复。`,
    '删除用户',
    {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'error',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(() => {
    const index = mockUsers.value.findIndex((u) => u.id === row.id)
    if (index !== -1) {
      mockUsers.value.splice(index, 1)
      ElMessage.success(`已删除用户「${row.username}」`)
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
