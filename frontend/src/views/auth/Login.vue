<!--
================================================================================
登录页组件 - views/auth/Login.vue
================================================================================

【文件说明】
这是用户登录页面，提供用户名/密码登录表单。
功能包括：
1. 表单输入和验证（用户名长度 3-20、密码长度 6-32）
2. 记住我复选框
3. 登录成功后跳转到原始页面或首页
4. 错误提示和加载状态

【Vue 概念】
- reactive() 和 ref() 的区别：reactive 适合对象，ref 适合基本类型
- 表单验证：Element Plus 的 el-form + FormRules
- FormInstance 类型：表单实例的 TypeScript 类型
- async/await 异步流程控制
- 路由查询参数：redirect 用于登录后跳回原始页面
- try/catch/finally 错误处理模式

💡 学习要点:
1. el-form 的 :model 绑定表单数据，:rules 绑定验证规则
2. el-form-item 的 prop 属性对应 formData 的字段名和 rules 的键名
3. formRef.value.validate() 返回 Promise，需要 await 等待验证结果
4. route.query.redirect 保存了被拦截前的目标路径，登录后跳转回去
5. loading 状态防止用户重复点击登录按钮
6. finally 块确保无论成功失败都会关闭 loading 状态
================================================================================
-->

<template>
  <div class="login-page">
    <div class="login-container">
      <!-- ================================================================ -->
      <!-- Logo 和标题区域 -->
      <!-- ================================================================ -->
      <div class="login-header">
        <el-icon :size="48" color="#409eff"><Reading /></el-icon>
        <h1 class="login-title">企业知识库</h1>
        <p class="login-subtitle">智能问答系统</p>
      </div>

      <!-- ================================================================ -->
      <!-- 登录表单 -->
      <!-- ================================================================ -->
      <!--
        el-form: Element Plus 的表单组件
        ref="formRef": 获取表单实例引用（用于调用 validate 方法）
        :model="formData": 绑定表单数据对象
        :rules="formRules": 绑定验证规则
        size="large": 表单控件的尺寸
        @submit.prevent: 阻止表单默认提交行为（防止页面刷新）
      -->
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        size="large"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <!-- prop="username" 对应 formData.username 和 formRules.username -->
        <el-form-item prop="username">
          <!--
            el-input 输入框组件
            v-model 双向绑定：输入框值变化 → formData.username 更新
            :prefix-icon 在输入框前显示图标
            clearable 显示清除按钮
          -->
          <el-input
            v-model="formData.username"
            placeholder="用户名"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <!--
            type="password": 密码输入框（显示为圆点）
            show-password: 显示密码切换按钮（眼睛图标）
            @keyup.enter: 按回车键触发登录
          -->
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <!-- 记住我和忘记密码 -->
        <el-form-item>
          <el-checkbox v-model="formData.remember">记住我</el-checkbox>
          <el-link type="primary" :underline="false" class="forgot-password">
            忘记密码？
          </el-link>
        </el-form-item>

        <!-- 登录按钮 -->
        <el-form-item>
          <!--
            :loading="loading" 显示加载动画（防止重复点击）
            按钮文字根据 loading 状态动态变化
          -->
          <el-button
            type="primary"
            :loading="loading"
            class="login-button"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- ================================================================ -->
      <!-- 其他登录方式 -->
      <!-- ================================================================ -->
      <div class="other-login">
        <el-divider>其他登录方式</el-divider>
        <div class="other-login-methods">
          <el-button circle>
            <el-icon><Platform /></el-icon>
          </el-button>
          <el-button circle>
            <el-icon><ChatDotRound /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
<!--
  导入说明：
  - reactive: 创建响应式对象（适合包含多个字段的表单数据）
  - ref: 创建响应式引用（适合基本类型值，如 boolean、string）
-->
import { reactive, ref } from 'vue'

<!-- 导入路由 API -->
import { useRouter, useRoute } from 'vue-router'

<!-- 导入用户状态 store -->
import { useUserStore } from '@/stores/user'

<!-- 导入 Element Plus 的消息提示组件 -->
import { ElMessage } from 'element-plus'

<!-- 导入表单相关的 TypeScript 类型 -->
<!-- FormInstance: 表单组件实例的类型（用于 ref 类型标注） -->
<!-- FormRules: 验证规则的类型 -->
import type { FormInstance, FormRules } from 'element-plus'

<!-- 导入页面中使用的图标 -->
import { Reading, User, Lock, Platform, ChatDotRound } from '@element-plus/icons-vue'

<!-- 获取路由实例和当前路由信息 -->
const router = useRouter()
const route = useRoute()

<!-- 获取用户状态 store -->
const userStore = useUserStore()

/**
 * 表单实例引用
 *
 * ref<FormInstance>() 类型标注确保 formRef.value 有 validate() 等方法。
 * 在模板中通过 ref="formRef" 绑定到 el-form 组件。
 * 初始值为 undefined（组件挂载后才有值）。
 */
const formRef = ref<FormInstance>()

/**
 * 加载状态
 *
 * 控制登录按钮的 loading 动画，防止用户重复提交。
 * 登录中时为 true，登录完成（成功或失败）后为 false。
 */
const loading = ref(false)

/**
 * 表单数据
 *
 * reactive() 创建响应式对象。
 * 与 ref() 不同，reactive 的属性可以直接访问（不需要 .value）。
 * 适合表单这种包含多个关联字段的数据结构。
 *
 * 为什么用 reactive 而不是 ref？
 * - reactive 更适合对象数据，访问属性不需要 .value
 * - ref 适合基本类型（如 loading = ref(false)）
 */
const formData = reactive({
  username: '',
  password: '',
  remember: false
})

/**
 * 表单验证规则
 *
 * FormRules 类型确保规则配置的正确性。
 * 每个字段的规则是一个数组，可以包含多个验证条件：
 * - required: 必填验证
 * - min/max: 长度验证
 * - trigger: 触发验证的时机（'blur' 失焦时、'change' 值变化时）
 */
const formRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6 到 32 个字符', trigger: 'blur' }
  ]
}

/**
 * 处理登录
 *
 * 登录流程：
 * 1. 验证表单（validate）
 * 2. 设置 loading 状态
 * 3. 调用 store 的 login 方法
 * 4. 登录成功 → 跳转到原始页面或首页
 * 5. 登录失败 → 显示错误信息
 *
 * try/catch/finally 模式：
 * - try: 尝试执行可能失败的操作
 * - catch: 捕获错误并提示用户
 * - finally: 无论成功失败都执行的清理操作（关闭 loading）
 */
const handleLogin = async () => {
  // 安全检查：确保表单引用存在
  if (!formRef.value) return

  try {
    // 调用 Element Plus 表单的 validate 方法进行表单验证
    // validate() 返回 Promise<boolean>，验证通过 resolve(true)，不通过 resolve(false)
    const valid = await formRef.value.validate()
    if (!valid) return

    // 开启加载状态
    loading.value = true

    // 调用 Pinia store 的 login 方法
    // 这个方法会调用后端 API，保存 Token 和用户信息
    await userStore.login({
      username: formData.username,
      password: formData.password
    })

    // 登录成功提示
    ElMessage.success('登录成功')

    // --------------------------------------------------------------------------
    // 跳转逻辑
    // --------------------------------------------------------------------------
    // route.query.redirect 是导航守卫在拦截未登录用户时保存的原始目标路径
    // 例如：用户访问 /admin → 被拦截到 /login?redirect=/admin
    // 登录成功后读取 redirect 参数，跳转回 /admin
    // 如果没有 redirect（直接访问登录页），则跳转到首页
    const redirect = (route.query.redirect as string) || '/home'
    router.push(redirect)
  } catch (error: any) {
    // 登录失败：显示错误信息
    // error.message 可能是后端返回的具体错误信息（如"密码错误"）
    console.error('登录失败:', error)
    ElMessage.error(error.message || '登录失败，请检查用户名和密码')
  } finally {
    // 无论登录成功还是失败，都关闭 loading 状态
    // finally 块总是会执行，确保按钮恢复可点击状态
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
/* 登录页容器：全屏高度，内容居中，紫色渐变背景 */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;      /* 垂直居中 */
  justify-content: center;   /* 水平居中 */
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

/* 登录卡片容器：白色背景，圆角，阴影 */
.login-container {
  width: 100%;
  max-width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

/* 头部区域 */
.login-header {
  text-align: center;
  margin-bottom: 40px;

  .login-title {
    font-size: 24px;
    font-weight: 600;
    margin: 16px 0 8px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .login-subtitle {
    font-size: 14px;
    color: #909399;
  }
}

/* 登录表单 */
.login-form {
  /* :deep() 穿透修改 Element Plus 表单项的间距 */
  :deep(.el-form-item) {
    margin-bottom: 24px;
  }

  .forgot-password {
    float: right;
    font-size: 14px;
  }

  /* 登录按钮：全宽，较高 */
  .login-button {
    width: 100%;
    height: 44px;
    font-size: 16px;
  }
}

/* 其他登录方式 */
.other-login {
  margin-top: 30px;

  :deep(.el-divider__text) {
    font-size: 12px;
    color: #909399;
  }

  .other-login-methods {
    display: flex;
    justify-content: center;
    gap: 16px;
  }
}
</style>
