<!--
================================================================================
401 未认证错误页面 - views/error/Unauthorized.vue
================================================================================

【文件说明】
当用户未登录或 Token 过期时，跳转到此页面。
原型设计：警告色 (amber) 大数字 + "请先登录" + 去登录按钮。
================================================================================
-->

<template>
  <div class="error-page">
    <div class="error-content">
      <h1 class="error-code">401</h1>
      <h2 class="error-title">请先登录</h2>
      <p class="error-desc">您需要登录后才能访问此页面</p>
      <el-button type="primary" size="large" @click="$router.push('/login')">
        <el-icon><Right /></el-icon>
        去登录
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Right } from '@element-plus/icons-vue'
</script>

<style scoped lang="scss">
.error-page {
  min-height: 80vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.error-content {
  text-align: center;

  .error-code {
    font-size: 120px;
    font-weight: 700;
    margin: 0;
    color: var(--el-color-warning);
    line-height: 1;
  }

  .error-title {
    font-size: 24px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    margin: 16px 0 8px;
  }

  .error-desc {
    font-size: 14px;
    color: var(--el-text-color-secondary);
    margin: 0 0 32px;
  }
}
</style>
