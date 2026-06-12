<!--
================================================================================
头部导航组件 - components/layout/Header.vue
================================================================================

【文件说明】
这是页面顶部的导航栏组件，显示在所有使用 MainLayout 的页面中。
包含：Logo、导航菜单、通知徽章、用户头像下拉菜单。

【Vue 概念】
- 计算属性 computed: 根据当前路由路径动态确定活跃菜单项
- Pinia Store: 读取用户登录状态和用户信息
- Element Plus 组件: el-menu、el-dropdown、el-badge、el-avatar 等
- 条件渲染 v-if/v-else: 根据登录状态显示不同内容
- 路由导航: 使用 router.push() 进行编程式导航

💡 学习要点:
1. el-menu 的 router 属性让菜单项自动根据 index 路径进行路由跳转
2. computed(() => route.path) 实现菜单高亮跟随路由变化
3. el-dropdown 的 @command 事件处理下拉菜单的点击操作
4. ElMessageBox.confirm() 提供确认弹窗（异步操作，需要 await）
5. try/catch 捕获用户取消确认弹窗的操作
================================================================================
-->

<template>
  <div class="app-header">
    <div class="header-container">
      <!-- ================================================================== -->
      <!-- Logo 区域 -->
      <!-- ================================================================== -->
      <div class="logo">
        <!-- Reading 是 Element Plus 的图标组件，已在 main.ts 全局注册 -->
        <el-icon :size="28"><Reading /></el-icon>
        <span class="logo-text">知识库</span>
      </div>

      <!-- ================================================================== -->
      <!-- 导航菜单 -->
      <!-- ================================================================== -->
      <!--
        el-menu: Element Plus 的导航菜单组件
        :default-active="activeMenu" 控制当前高亮的菜单项（根据路由路径动态计算）
        mode="horizontal" 水平模式（适合顶部导航）
        :ellipsis="false" 禁止菜单项溢出时折叠
        router 属性：启用 vue-router 模式，点击菜单项自动跳转到 index 指定的路径
      -->
      <el-menu
        :default-active="activeMenu"
        mode="horizontal"
        :ellipsis="false"
        class="header-menu"
        router
      >
        <!-- index 值就是路由路径，配合 router 属性实现点击跳转 -->
        <el-menu-item index="/home">首页</el-menu-item>
        <el-menu-item index="/search">搜索</el-menu-item>
        <el-menu-item index="/documents">文档</el-menu-item>
        <el-menu-item index="/chat">智能问答</el-menu-item>
      </el-menu>

      <!-- ================================================================== -->
      <!-- 用户操作区域 -->
      <!-- ================================================================== -->
      <div class="user-actions">
        <!-- v-if/v-else 条件渲染：根据登录状态显示不同内容 -->
        <template v-if="userStore.isLoggedIn">
          <!-- 通知图标（接入 NotificationPanel 组件） -->
          <NotificationPanel
            v-model:visible="notificationVisible"
            :count="notificationCount"
            @update:count="notificationCount = $event"
            @view-all="handleViewAllNotifications"
          >
            <el-badge :value="notificationCount" :hidden="notificationCount === 0" class="notification-badge">
              <el-button :icon="Bell" circle />
            </el-badge>
          </NotificationPanel>

          <!-- 用户下拉菜单 -->
          <!--
            el-dropdown: 下拉菜单组件
            @command 事件：当下拉菜单项被点击时触发，参数是 el-dropdown-item 的 command 值
          -->
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <!--
                el-avatar: 头像组件
                :src 绑定头像 URL，如果 URL 无效则显示文字占位
                文字占位显示用户名的首字母（大写）
              -->
              <el-avatar :size="32" :src="userStore.currentUser?.avatar">
                {{ userStore.userName.charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="user-name">{{ userStore.userName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <!-- #dropdown 插槽：定义下拉菜单的内容 -->
            <template #dropdown>
              <el-dropdown-menu>
                <!-- command 属性：点击时传递给 @command 处理函数的值 -->
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon>
                  设置
                </el-dropdown-item>
                <!-- divided 属性：在此项前添加分割线 -->
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>

        <!-- 未登录时显示登录按钮 -->
        <template v-else>
          <!-- $router 是 Vue Router 注入的全局属性，可以直接在模板中使用 -->
          <el-button type="primary" @click="$router.push('/login')">登录</el-button>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 导入 Vue 的计算属性 API
import { computed, ref } from 'vue'

// 导入 Vue Router 的组合式 API
// useRouter: 获取路由实例（用于编程式导航）
// useRoute: 获取当前路由信息（路径、参数、查询参数等）
import { useRouter, useRoute } from 'vue-router'

// 导入用户状态 store，用于获取登录状态和用户信息
import { useUserStore } from '@/stores/user'

// 导入 Element Plus 的消息提示和确认弹窗组件
import { ElMessage, ElMessageBox } from 'element-plus'

// 导入通知面板组件
import NotificationPanel from '@/components/common/NotificationPanel.vue'

// 导入 Element Plus 图标组件
// 注意：虽然图标已在 main.ts 全局注册，这里显式导入是为了在 JS 中引用（如 :icon="Bell"）
import {
  Reading,
  Bell,
  User,
  Setting,
  SwitchButton,
  ArrowDown
} from '@element-plus/icons-vue'

// 获取路由实例和当前路由信息
const router = useRouter()
const route = useRoute()

// 获取用户状态 store 实例
const userStore = useUserStore()

/**
 * 活跃菜单项（计算属性）
 *
 * 根据当前路由路径自动确定哪个菜单项应该高亮。
 * 例如：当前路径是 /documents，则 index="/documents" 的菜单项会高亮。
 */
const activeMenu = computed(() => route.path)

/**
 * 通知面板可见性
 */
const notificationVisible = ref(false)

/**
 * 通知未读数量（响应式，由 NotificationPanel 更新）
 */
const notificationCount = ref(2)

/** 查看全部通知 */
const handleViewAllNotifications = () => {
  ElMessage.info('通知列表页开发中')
}

/**
 * 处理用户下拉菜单的命令
 *
 * @param command - 下拉菜单项的 command 属性值
 *
 * 使用 switch 语句分发不同的操作：
 * - profile: 跳转到个人中心
 * - settings: 跳转到个人中心的设置标签页
 * - logout: 确认后执行登出操作
 */
const handleCommand = async (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      // 跳转到个人中心并带上 tab 参数，Profile 组件会读取 query.tab 切换标签页
      router.push('/profile?tab=settings')
      break
    case 'logout':
      try {
        // ElMessageBox.confirm() 显示确认对话框，返回 Promise
        // 用户点击"确定"→ Promise resolve；点击"取消"→ Promise reject
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        // 用户确认后执行登出
        userStore.logout()
        router.push('/login')
        ElMessage.success('已退出登录')
      } catch {
        // 用户取消操作，不需要做任何处理
        // catch 块捕获的是 ElMessageBox 的 reject（用户取消）
      }
      break
  }
}
</script>

<style scoped lang="scss">
/* scoped: 样式只作用于当前组件，不会影响其他组件 */
/* lang="scss": 使用 SCSS 预处理器，支持嵌套、变量等特性 */

/* 头部容器：固定高度 60px，白色背景，底部边框分隔 */
.app-header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;

  /* flex 布局使 Logo、菜单、用户操作水平排列 */
  .header-container {
    max-width: 1400px;    /* 限制最大宽度，居中显示 */
    margin: 0 auto;       /* 水平居中 */
    height: 100%;
    display: flex;
    align-items: center;  /* 垂直居中 */
    padding: 0 20px;
  }

  /* Logo 区域样式 */
  .logo {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 18px;
    font-weight: 600;
    color: #409eff;
    margin-right: 40px;   /* 与菜单拉开距离 */

    /* 文字渐变效果（纯 CSS 渐变文字技巧） */
    .logo-text {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
  }

  /* 导航菜单样式 */
  .header-menu {
    flex: 1;              /* 占据剩余空间 */
    border-bottom: none;  /* 移除默认底部边框 */

    /* :deep() 穿透 scoped 样式，修改 Element Plus 组件内部样式 */
    :deep(.el-menu-item) {
      font-size: 15px;
    }
  }

  /* 用户操作区域 */
  .user-actions {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-left: 20px;

    /* 通知徽章位置微调 */
    .notification-badge {
      :deep(.el-badge__content) {
        transform: translateY(-5px) translateX(5px);
      }
    }

    /* 用户信息区域（头像 + 名字 + 箭头） */
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 4px 12px;
      border-radius: 4px;
      transition: background-color 0.3s;

      &:hover {
        background-color: #f5f7fa;
      }

      /* 用户名文字截断（超长时显示省略号） */
      .user-name {
        font-size: 14px;
        color: #606266;
        max-width: 100px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }
}
</style>
