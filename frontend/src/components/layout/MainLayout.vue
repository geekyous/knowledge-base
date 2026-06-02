<!--
================================================================================
主布局组件 - components/layout/MainLayout.vue
================================================================================

【文件说明】
这是应用的主布局组件，提供了统一的页面骨架：
- 顶部导航栏（Header）
- 中间内容区域（通过 <router-view /> 动态渲染）
- 底部页脚（Footer）

大多数页面都使用这个布局，通过 Vue Router 的嵌套路由（children）实现。

【Vue 概念】
- 布局组件模式：将公共的页面结构抽取为布局组件，避免重复代码
- <router-view />：路由出口，根据 URL 渲染对应的子页面组件
- onMounted 生命周期钩子：组件挂载后执行初始化逻辑
- 嵌套路由：子路由的组件会渲染在父路由组件的 <router-view /> 中

【布局架构】
  el-container（垂直布局）
  ├── el-header（60px 固定高度）
  │   └── AppHeader（导航栏）
  ├── el-main（自适应高度）
  │   └── <router-view />（子页面）
  └── el-footer（40px 固定高度）
      └── AppFooter（页脚）

💡 学习要点:
1. 布局组件通过 <router-view /> 提供页面渲染出口
2. 嵌套路由的 children 组件会渲染在父路由组件的 <router-view /> 中
3. onMounted 中初始化用户信息，确保页面加载时就能获取到用户数据
4. Element Plus 的 Container 布局组件（el-container/el-header/el-main/el-footer）
5. :deep() 穿透 scoped 样式，修改 Element Plus 组件的内部样式
================================================================================
-->

<template>
  <!--
    el-container: Element Plus 的布局容器组件
    默认为垂直方向排列子元素（header/main/footer 从上到下）
  -->
  <el-container class="main-layout">
    <!-- 头部区域：固定高度 60px -->
    <el-header height="60px">
      <app-header />
    </el-header>

    <!-- 主要内容区域：自适应高度（flex: 1 填满剩余空间） -->
    <!--
      <router-view /> 是子路由的渲染出口
      例如访问 /home 时，这里会渲染 Home.vue 组件
      访问 /documents 时，这里会渲染 DocumentList.vue 组件
    -->
    <el-main>
      <router-view />
    </el-main>

    <!-- 底部区域：固定高度 40px -->
    <el-footer height="40px">
      <app-footer />
    </el-footer>
  </el-container>
</template>

<script setup lang="ts">
<!-- 导入布局的子组件 -->
<!-- Header 和 Footer 是当前目录下的兄弟组件 -->
import AppHeader from './Header.vue'
import AppFooter from './Footer.vue'

<!-- 导入 onMounted 生命周期钩子 -->
<!-- onMounted: 组件挂载到 DOM 后调用，适合执行初始化操作（如 API 请求） -->
import { onMounted } from 'vue'

<!-- 导入用户状态 store -->
import { useUserStore } from '@/stores/user'

<!-- 获取用户 store 实例 -->
const userStore = useUserStore()

/**
 * 组件挂载时初始化用户信息
 *
 * 如果用户已登录（localStorage 中有 Token），则用 Token 获取最新的用户信息。
 * 这解决了"页面刷新后内存中用户信息丢失"的问题。
 *
 * initialize() 方法会：
 * 1. 用保存的 Token 调用 /v1/users/me 接口
 * 2. 成功：更新 store 中的用户信息
 * 3. 失败（Token 过期）：清除 Token，要求重新登录
 */
onMounted(async () => {
  if (userStore.isLoggedIn) {
    await userStore.initialize()
  }
})
</script>

<style scoped lang="scss">
/* 主布局容器：全屏高度，垂直 flex 布局 */
.main-layout {
  min-height: 100vh;       /* 最小高度为视口高度，确保页脚在底部 */
  display: flex;
  flex-direction: column;  /* 垂直排列 header、main、footer */
}

/* :deep() 穿透 scoped 限制，修改 Element Plus 组件内部样式 */
/* 头部：移除默认内边距，设置背景色和底部边框 */
:deep(.el-header) {
  padding: 0;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

/* 主内容区：设置内边距，flex: 1 填满 header 和 footer 之间的空间 */
:deep(.el-main) {
  padding: 20px;
  flex: 1;                 /* 关键：占据剩余所有高度 */
  background: #f5f7fa;
}

/* 底部：居中显示，设置背景色和顶部边框 */
:deep(.el-footer) {
  padding: 0;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
