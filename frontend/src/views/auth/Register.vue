<!--
================================================================================
注册页组件 - views/auth/Register.vue
================================================================================

【文件说明】
用户注册页面，提供用户名/邮箱/密码/确认密码的注册表单。
功能包括：
1. 表单输入和验证（用户名 3-20 字符、邮箱格式、密码强度、确认密码一致性）
2. 密码强度实时指示器（弱/中/强）
3. 注册成功后跳转到登录页
4. 错误提示和加载状态

【原型对照】
- 原型位置：prototype-pc.html「用户注册」屏幕（1690-1738 行）
- Logo 图标 + "创建账号" 标题 + "注册后即可使用企业知识库" 副标题
- 用户名/邮箱/密码/确认密码四个输入框
- 密码强度指示器（三段彩色条 + 强度文字）
- "注册" 按钮 + "已有账号？去登录" 底部链接
================================================================================
-->

<template>
  <div class="register-page">
    <div class="register-container">
      <!-- Logo 和标题区域 -->
      <div class="register-header">
        <el-icon :size="48" color="#409eff"><Reading /></el-icon>
        <h1 class="register-title">创建账号</h1>
        <p class="register-subtitle">注册后即可使用企业知识库</p>
      </div>

      <!-- 内联错误提示区域 -->
      <div v-if="errorMsg" class="error-message">
        <el-icon><CircleCloseFilled /></el-icon>
        {{ errorMsg }}
      </div>

      <!-- 注册表单 -->
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        size="large"
        class="register-form"
        @submit.prevent="handleRegister"
      >
        <!-- 用户名 -->
        <el-form-item prop="username">
          <el-input
            v-model="formData.username"
            placeholder="用户名（3-20个字符）"
            :prefix-icon="User"
            clearable
            maxlength="20"
          />
        </el-form-item>

        <!-- 邮箱 -->
        <el-form-item prop="email">
          <el-input
            v-model="formData.email"
            placeholder="邮箱地址"
            :prefix-icon="Message"
            clearable
          />
        </el-form-item>

        <!-- 密码 -->
        <el-form-item prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="密码（至少8位，含字母和数字）"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>

        <!-- 密码强度指示器（匹配原型：三段彩色条 + 强度文字） -->
        <div v-if="formData.password" class="password-strength">
          <div class="strength-label">
            <span class="strength-text">密码强度</span>
            <span :class="['strength-level', strengthClass]">{{ strengthText }}</span>
          </div>
          <div class="strength-bars">
            <div
              v-for="i in 3"
              :key="i"
              :class="['strength-bar', { filled: i <= passwordStrength }]"
            />
          </div>
        </div>

        <!-- 确认密码 -->
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="formData.confirmPassword"
            type="password"
            placeholder="确认密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <!-- 注册按钮 -->
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="register-button"
            @click="handleRegister"
          >
            {{ loading ? '注册中...' : '注册' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 底部链接：已有账号去登录 -->
      <div class="register-footer">
        已有账号？<router-link to="/login" class="login-link">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Reading, User, Lock, Message, CircleCloseFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const errorMsg = ref('')

const formData = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

// 自定义校验：确认密码必须与密码一致
const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== formData.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 自定义校验：密码强度（至少 8 位，包含字母和数字）
const validatePassword = (_rule: any, value: string, callback: any) => {
  if (value.length < 8) {
    callback(new Error('密码至少 8 个字符'))
  } else if (!/[a-zA-Z]/.test(value) || !/[0-9]/.test(value)) {
    callback(new Error('密码必须包含字母和数字'))
  } else {
    callback()
  }
}

const formRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 密码强度计算（0-3）
const passwordStrength = computed(() => {
  const pwd = formData.password
  if (!pwd) return 0
  let score = 0
  if (pwd.length >= 8) score++
  if (/[a-zA-Z]/.test(pwd) && /[0-9]/.test(pwd)) score++
  if (/[^a-zA-Z0-9]/.test(pwd) && pwd.length >= 12) score++
  return score
})

const strengthText = computed(() => {
  const labels = ['', '弱', '中', '强']
  return labels[passwordStrength.value] || ''
})

const strengthClass = computed(() => {
  const classes = ['', 'weak', 'medium', 'strong']
  return classes[passwordStrength.value] || ''
})

const handleRegister = async () => {
  if (!formRef.value) return

  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    loading.value = true
    errorMsg.value = ''

    await request.post('/api/auth/register', {
      username: formData.username,
      email: formData.email,
      password: formData.password
    })

    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error: any) {
    console.error('注册失败:', error)
    errorMsg.value = error.response?.data?.message || error.message || '注册失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
/* 注册页容器：全屏居中，紫色渐变背景（与登录页一致） */
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

/* 注册卡片容器 */
.register-container {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

/* 头部区域 */
.register-header {
  text-align: center;
  margin-bottom: 32px;

  .register-title {
    font-size: 24px;
    font-weight: 600;
    margin: 16px 0 8px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .register-subtitle {
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

/* 注册表单 */
.register-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }

  .register-button {
    width: 100%;
    height: 44px;
    font-size: 16px;
  }
}

/* 密码强度指示器（匹配原型：三段彩色条） */
.password-strength {
  margin: -12px 0 16px;

  .strength-label {
    display: flex;
    justify-content: space-between;
    font-size: 0.8rem;
    margin-bottom: 4px;

    .strength-text {
      color: #909399;
    }
  }

  .strength-bars {
    display: flex;
    gap: 4px;

    .strength-bar {
      flex: 1;
      height: 4px;
      border-radius: 2px;
      background: #e2e8f0;
      transition: background 0.3s ease;

      &.filled {
        background: #e2e8f0;

        &.weak-fill {
          background: #ef4444;
        }
      }
    }
  }
}

/* 动态强度条颜色 */
.strength-bar:nth-child(1).filled {
  background: v-bind("passwordStrength === 1 ? '#ef4444' : passwordStrength === 2 ? '#f59e0b' : '#10b981'");
}
.strength-bar:nth-child(2).filled {
  background: v-bind("passwordStrength === 2 ? '#f59e0b' : '#10b981'");
}
.strength-bar:nth-child(3).filled {
  background: #10b981;
}

/* 强度文字颜色 */
.strength-level {
  &.weak { color: #ef4444; }
  &.medium { color: #f59e0b; }
  &.strong { color: #10b981; }
}

/* 底部链接 */
.register-footer {
  text-align: center;
  font-size: 14px;
  color: #909399;
  margin-top: 20px;

  .login-link {
    color: #409eff;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}
</style>
