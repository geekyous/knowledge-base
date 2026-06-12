<!--
================================================================================
403 无权限错误页面 - views/error/Forbidden.vue
================================================================================

【文件说明】
当用户已登录但没有足够权限时，跳转到此页面。
原型设计：危险色 (red) 大数字 + "没有权限" + 返回首页按钮。
================================================================================
-->

<template>
  <div class="error-page">
    <div class="error-content">
      <h1 class="error-code">403</h1>
      <h2 class="error-title">没有权限</h2>
      <p class="error-desc">您没有权限访问此页面，请联系管理员</p>
      <el-button plain size="large" @click="$router.push('/home')">
        <el-icon><HomeFilled /></el-icon>
        返回首页
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { HomeFilled } from '@element-plus/icons-vue'
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
    color: var(--el-color-danger);
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
