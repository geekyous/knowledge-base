<!--
================================================================================
页脚组件 - components/layout/Footer.vue
================================================================================

【文件说明】
这是页面底部的页脚组件，显示版权信息和版本号。
作为一个简单的展示型组件，它不需要复杂的脚本逻辑。

【Vue 概念】
- 无状态组件：不需要响应式数据，只有静态模板
- <script setup> 即使为空也需要存在，因为这是 Vue SFC 的标准结构

💡 学习要点:
1. 简单的展示型组件可以只有 template 和 style，script 可以为空
2. flex 布局配合 justify-content: center 实现内容水平居中
3. scoped 样式确保只影响当前组件，不会污染其他组件
================================================================================
-->

<template>
  <div class="app-footer">
    <div class="footer-content">
      <!-- 版权信息 -->
      <span class="copyright">
        © 2024 企业知识库问答系统. All rights reserved.
      </span>
      <!-- 版本号 -->
      <span class="version">v1.0.0</span>
    </div>
  </div>
</template>

<script setup lang="ts">
// Footer 是一个纯展示型组件，不需要任何脚本逻辑
// <script setup> 标签仍需保留，因为 Vue SFC 规范要求
</script>

<style scoped lang="scss">
/* 页脚容器：全宽、固定高度 40px、内容居中 */
.app-footer {
  width: 100%;
  height: 40px;
  display: flex;
  align-items: center;      /* 垂直居中 */
  justify-content: center;   /* 水平居中 */
  font-size: 13px;
  color: #909399;

  /* 内容容器：flex 布局排列版权和版本号 */
  .footer-content {
    display: flex;
    align-items: center;
    gap: 20px;               /* 版权和版本号之间的间距 */
  }

  /* 版权文字颜色（比版本号更深，突出主要信息） */
  .copyright {
    color: #606266;
  }

  /* 版本号颜色（较浅，作为次要信息） */
  .version {
    color: #909399;
  }
}
</style>
