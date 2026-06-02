<!--
================================================================================
根组件 - App.vue
================================================================================

【文件说明】
这是 Vue 应用的根组件（Root Component）。所有页面和组件都渲染在这个根组件内部。
它主要负责：
1. 配置 Element Plus 的国际化语言（中文）
2. 提供 <router-view /> 作为页面渲染的出口
3. 定义全局基础样式

【Vue 概念】
- SFC（Single File Component）：Vue 的单文件组件，将 template/script/style 写在一个文件中
- router-view：路由视图组件，根据当前 URL 显示对应的页面组件
- el-config-provider：Element Plus 的全局配置组件，用于设置语言、尺寸等

💡 学习要点:
1. App.vue 是整个组件树的根，所有页面都通过 <router-view /> 渲染
2. <script setup lang="ts"> 是 Vue 3.2+ 的语法糖，自动处理 setup() 的返回值
3. el-config-provider 的 :locale 属性用于配置 Element Plus 的语言包
4. 全局样式（不带 scoped）会影响所有组件，需要谨慎使用
================================================================================
-->

<template>
  <!--
    el-config-provider: Element Plus 的全局配置提供者组件
    :locale="locale" 将 Element Plus 的语言设置为中文
    这会影响所有 Element Plus 组件的内置文本（如分页的"下一页"、日期选择器的月份名等）
  -->
  <el-config-provider :locale="locale">
    <!--
      router-view: 路由视图出口
      这是 Vue Router 的核心组件，它会根据当前浏览器的 URL 路径，
      自动渲染对应的路由组件。例如：
      - 访问 /home → 渲染 Home.vue
      - 访问 /login → 渲染 Login.vue
      - 访问 /documents → 渲染 DocumentList.vue
    -->
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
<!--
  <script setup lang="ts"> 说明：
  - setup: Vue 3 Composition API 的编译期语法糖，无需手动 return 变量
  - lang="ts": 启用 TypeScript 支持，可以获得类型检查和智能提示
-->

<!-- ref() 是 Vue 3 中创建响应式数据的基本方法 -->
import { ref } from 'vue'

<!-- 导入 Element Plus 的中文语言包，使组件显示中文（默认是英文） -->
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

<!-- 使用 ref() 创建响应式的语言配置 -->
<!-- ref() 返回一个包含 .value 属性的响应式引用对象 -->
<!-- 当 locale.value 改变时，模板中引用它的地方会自动更新 -->
const locale = ref(zhCn)
</script>

<style>
<!--
  注意：这里的 <style> 没有 scoped 属性，说明这些样式是全局的
  全局样式会影响所有组件，适合定义基础样式重置（CSS Reset）
-->

/* 根应用容器样式：设置字体族和字体渲染优化 */
/* -webkit-font-smoothing 和 -moz-osx-font-smoothing 让文字在 Retina 屏幕上更清晰 */
#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 全局样式重置（CSS Reset）：统一不同浏览器的默认样式 */
/* box-sizing: border-box 让 width/height 包含 padding 和 border，更直观 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 页面主体背景色：浅灰色背景是后台管理系统的常见设计 */
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    sans-serif;
  background-color: #f5f7fa;
}
</style>
