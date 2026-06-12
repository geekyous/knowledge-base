<!--
================================================================================
404 错误页面 - views/error/NotFound.vue
================================================================================

【文件说明】
这是 404 页面未找到的错误页面。当用户访问不存在的 URL 路径时，
Vue Router 的通配路由（/:pathMatch(.*)*）会匹配并渲染此页面。

【Vue 概念】
- 错误页面组件：简单的展示型组件
- $router.push('/home') 直接在模板中使用路由导航
- 渐变文字效果：CSS background-clip: text 技巧

💡 学习要点:
1. 404 页面是 Web 应用的标准页面，提供友好的错误提示
2. Vue Router 4 的通配路由语法是 /:pathMatch(.*)*（Vue Router 3 是 *）
3. 使用 flex 布局实现内容垂直和水平居中
4. 即使是简单组件也需要 <script setup lang="ts"> 来导入图标
================================================================================
-->

<template>
  <!-- flex 布局实现内容居中 -->
  <div class="not-found-page">
    <div class="not-found-content">
      <!-- 404 大数字：渐变色效果 -->
      <h1 class="error-code">404</h1>
      <h2 class="error-title">页面未找到</h2>
      <p class="error-desc">抱歉，您访问的页面不存在或已被移除。</p>
      <!-- 返回首页按钮 -->
      <el-button plain size="large" @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回上一页
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
// 导入首页图标
import { ArrowLeft } from '@element-plus/icons-vue'
</script>

<style scoped lang="scss">
/* 404 页面：全屏居中 */
.not-found-page {
  min-height: 80vh;
  display: flex;
  align-items: center;      /* 垂直居中 */
  justify-content: center;   /* 水平居中 */
}

.not-found-content {
  text-align: center;

  /* 404 大数字：渐变色效果 */
  .error-code {
    font-size: 120px;
    font-weight: 700;
    margin: 0;
    color: var(--el-text-color-secondary);
    line-height: 1;
  }

  .error-title {
    font-size: 24px;
    font-weight: 600;
    color: #303133;
    margin: 16px 0 8px;
  }

  .error-desc {
    font-size: 14px;
    color: #909399;
    margin: 0 0 32px;
  }
}
</style>
