<!--
================================================================================
密码修改页组件 - views/auth/PasswordChange.vue
================================================================================

【文件说明】
密码修改页面，提供当前密码/新密码/确认新密码的修改表单。
功能包括：
1. 表单输入和验证（当前密码必填、新密码强度、确认密码一致性）
2. 密码要求说明框（匹配原型）
3. 取消和确认修改双按钮
4. 修改成功后跳转

【原型对照】
- 原型位置：prototype-pc.html「密码修改」屏幕（1740-1778 行）
- 钥匙图标（警告色）+ "修改密码" 标题 + "请输入当前密码并设置新密码" 副标题
- 当前密码/新密码/确认新密码三个输入框
- 密码要求说明框（灰色背景）：至少8字符/含字母和数字/不能与当前密码相同
- 取消（outline）+ 确认修改（primary）双按钮并排
================================================================================
-->

<template>
  <div class="password-page">
    <div class="password-container">
      <!-- Logo 和标题区域（匹配原型：钥匙图标，警告色） -->
      <div class="password-header">
        <el-icon :size="48" color="#e6a23c"><Key /></el-icon>
        <h1 class="password-title">修改密码</h1>
        <p class="password-subtitle">请输入当前密码并设置新密码</p>
      </div>

      <!-- 内联错误/成功提示 -->
      <div v-if="errorMsg" class="error-message">
        <el-icon><CircleCloseFilled /></el-icon>
        {{ errorMsg }}
      </div>

      <!-- 修改密码表单 -->
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        size="large"
        class="password-form"
        @submit.prevent="handleSubmit"
      >
        <!-- 当前密码 -->
        <el-form-item prop="currentPassword">
          <el-input
            v-model="formData.currentPassword"
            type="password"
            placeholder="当前密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <!-- 新密码 -->
        <el-form-item prop="newPassword">
          <el-input
            v-model="formData.newPassword"
            type="password"
            placeholder="新密码（至少8位，含字母和数字）"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <!-- 确认新密码 -->
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="formData.confirmPassword"
            type="password"
            placeholder="确认新密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleSubmit"
          />
        </el-form-item>

        <!-- 密码要求说明框（匹配原型：灰色背景 + 要求列表） -->
        <div class="password-requirements">
          <div class="requirements-title">密码要求：</div>
          <div :class="['requirement-item', { met: hasMinLength }]">
            <el-icon v-if="hasMinLength"><CircleCheckFilled /></el-icon>
            <span v-else>✓</span>
            至少 8 个字符
          </div>
          <div :class="['requirement-item', { met: hasLetterAndNumber }]">
            <el-icon v-if="hasLetterAndNumber"><CircleCheckFilled /></el-icon>
            <span v-else>✓</span>
            包含字母和数字
          </div>
          <div :class="['requirement-item', { met: isDifferentFromCurrent }]">
            <el-icon v-if="isDifferentFromCurrent"><CircleCheckFilled /></el-icon>
            <span v-else>✓</span>
            不能与当前密码相同
          </div>
        </div>

        <!-- 按钮区域（匹配原型：取消 outline + 确认修改 primary 并排） -->
        <el-form-item>
          <div class="button-group">
            <el-button class="action-button" @click="handleCancel">
              取消
            </el-button>
            <el-button
              type="primary"
              :loading="loading"
              class="action-button"
              @click="handleSubmit"
            >
              {{ loading ? '提交中...' : '确认修改' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Key, Lock, CircleCloseFilled, CircleCheckFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const errorMsg = ref('')

const formData = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 新密码强度校验
const validateNewPassword = (_rule: any, value: string, callback: any) => {
  if (value.length < 8) {
    callback(new Error('新密码至少 8 个字符'))
  } else if (!/[a-zA-Z]/.test(value) || !/[0-9]/.test(value)) {
    callback(new Error('新密码必须包含字母和数字'))
  } else if (value === formData.currentPassword) {
    callback(new Error('新密码不能与当前密码相同'))
  } else {
    callback()
  }
}

// 确认密码一致性校验
const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== formData.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const formRules: FormRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { validator: validateNewPassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 密码要求实时状态
const hasMinLength = computed(() => formData.newPassword.length >= 8)
const hasLetterAndNumber = computed(
  () => /[a-zA-Z]/.test(formData.newPassword) && /[0-9]/.test(formData.newPassword)
)
const isDifferentFromCurrent = computed(
  () => formData.newPassword.length > 0 && formData.newPassword !== formData.currentPassword
)

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    loading.value = true
    errorMsg.value = ''

    await request.put('/api/auth/password', {
      currentPassword: formData.currentPassword,
      newPassword: formData.newPassword
    })

    ElMessage.success('密码修改成功，请重新登录')
    router.push('/login')
  } catch (error: any) {
    console.error('密码修改失败:', error)
    errorMsg.value = error.response?.data?.message || error.message || '密码修改失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  router.back()
}
</script>

<style scoped lang="scss">
/* 密码修改页容器：全屏居中，紫色渐变背景（与登录页一致） */
.password-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

/* 卡片容器 */
.password-container {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

/* 头部区域 */
.password-header {
  text-align: center;
  margin-bottom: 32px;

  .password-title {
    font-size: 24px;
    font-weight: 600;
    margin: 16px 0 8px;
    color: #1e293b;
  }

  .password-subtitle {
    font-size: 14px;
    color: #909399;
  }
}

/* 内联错误提示 */
.error-message {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 0.9rem;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 表单 */
.password-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }
}

/* 密码要求说明框（匹配原型：灰色背景 + 要求列表） */
.password-requirements {
  background: #f8fafc;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 0.85rem;
  color: #64748b;

  .requirements-title {
    font-weight: 600;
    margin-bottom: 6px;
    color: #475569;
  }

  .requirement-item {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 2px 0;
    transition: color 0.3s ease;

    &.met {
      color: #10b981;

      .el-icon {
        color: #10b981;
      }
    }
  }
}

/* 双按钮并排（匹配原型：各占 50%） */
.button-group {
  display: flex;
  gap: 10px;
  width: 100%;

  .action-button {
    flex: 1;
    height: 44px;
    font-size: 16px;
  }
}
</style>
