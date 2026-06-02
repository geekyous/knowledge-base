/**
 * ============================================================================
 * 应用入口文件 - main.ts
 * ============================================================================
 *
 * 【文件说明】
 * 这是整个 Vue 应用的入口点（entry point）。类似于 C 语言的 main() 函数，
 * 这里负责创建 Vue 应用实例、注册全局插件、并挂载到 DOM 中。
 *
 * 【Vue 概念】
 * - createApp(): Vue 3 的新 API，用于创建一个应用实例（Vue 2 使用 new Vue()）
 * - 插件注册（Plugin）：通过 app.use() 安装插件，为全局添加功能
 * - 挂载（Mount）：将 Vue 应用绑定到 HTML 中的 DOM 节点
 *
 * 💡 学习要点:
 * 1. Vue 3 使用 createApp() 创建应用实例，支持多个独立的 Vue 应用共存
 * 2. Pinia 是 Vue 3 推荐的状态管理库（替代 Vuex），需要在应用创建后注册
 * 3. Element Plus 是基于 Vue 3 的 UI 组件库，需要全局注册
 * 4. 图标组件需要单独从 @element-plus/icons-vue 引入并注册
 * 5. app.mount('#app') 将 Vue 应用挂载到 index.html 中 id="app" 的 DOM 元素
 * ============================================================================
 */

// 从 Vue 中导入 createApp 工厂函数，用于创建 Vue 应用实例
import { createApp } from 'vue'

// 导入 Pinia 状态管理库 —— Vue 3 官方推荐的状态管理方案（Vuex 5 的替代品）
import { createPinia } from 'pinia'

// 导入 Element Plus UI 组件库（基于 Vue 3 的桌面端组件库，类似 Ant Design）
import ElementPlus from 'element-plus'

// 导入 Element Plus 的样式文件，必须导入才能正确显示组件样式
import 'element-plus/dist/index.css'

// 导入 Element Plus 的所有图标组件，这些图标以 Vue 组件形式提供
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 导入路由配置，Vue Router 用于实现单页应用（SPA）的页面导航
import router from './router'

// 导入根组件 App.vue，它是整个组件树的根节点
import App from './App.vue'

// 导入全局样式文件，定义了整个应用的通用样式
import './assets/styles/main.css'

// ============================================================================
// 创建 Vue 应用实例
// ============================================================================
// createApp(App) 接收根组件作为参数，返回一个应用实例
// 这与 Vue 2 的 new Vue({ render: h => h(App) }) 不同，Vue 3 的方式更加清晰
const app = createApp(App)

// ============================================================================
// 注册 Element Plus 图标组件
// ============================================================================
// 遍历所有图标组件并全局注册，这样在模板中就可以直接使用 <Edit />、<Delete /> 等图标
// Object.entries() 将对象转为 [key, value] 数组，便于遍历
// app.component() 的第一个参数是组件名，第二个参数是组件定义
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// ============================================================================
// 注册插件（Plugin）
// ============================================================================
// app.use() 用于注册插件，插件可以为整个应用添加全局功能
// 注意：注册顺序有时很重要，例如 router 应该在 pinia 之后注册

// createPinia() 创建 Pinia 实例 —— 用于管理全局状态（用户信息、应用状态等）
app.use(createPinia())

// 注册 Vue Router —— 用于页面导航和 URL 管理
app.use(router)

// 注册 Element Plus —— 全局注册所有 Element Plus 组件（el-button、el-input 等）
app.use(ElementPlus)

// ============================================================================
// 挂载应用
// ============================================================================
// app.mount('#app') 将 Vue 应用挂载到 public/index.html 中 <div id="app"></div>
// 这一步会触发 Vue 的渲染流程，将组件树渲染为真实 DOM
app.mount('#app')
