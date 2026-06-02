/**
 * ============================================================================
 * Vite 构建配置 - vite.config.ts
 * ============================================================================
 *
 * 【文件说明】
 * 这个文件是 Vite 构建工具的配置文件。Vite 是 Vue 3 官方推荐的构建工具
 * （替代 Vue CLI / Webpack）。配置内容包括：
 * 1. Vue 插件注册
 * 2. 路径别名（@ → src/）
 * 3. 开发服务器配置（端口、API 代理）
 * 4. 生产构建优化（代码分割）
 *
 * 【Vite 概念】
 * - Vite: 下一代前端构建工具，开发时使用原生 ES Module，生产用 Rollup 打包
 * - 插件(Plugin): vite-plugin-vue 提供 Vue SFC 的编译支持
 * - 路径别名(Alias): 用 @ 代替 src/，简化导入路径
 * - 代理(Proxy): 开发时将 API 请求代理到后端服务器，解决跨域问题
 * - 代码分割(Code Splitting): 将代码拆分为多个文件，优化加载性能
 *
 * 💡 学习要点:
 * 1. defineConfig() 提供配置项的 TypeScript 类型提示
 * 2. resolve.alias 设置 @ 为 src 目录的别名（需要 path.resolve 解析绝对路径）
 * 3. server.proxy 解决开发环境的跨域问题（生产环境需要 Nginx 反向代理）
 * 4. manualChunks 将第三方库拆分为独立文件（利用浏览器缓存）
 * 5. sourcemap: false 在生产环境中不生成源映射文件（减小构建体积）
 * ============================================================================
 */

// 导入 Vite 的配置定义辅助函数
// defineConfig 提供 TypeScript 类型提示，让 IDE 能自动补全配置项
import { defineConfig } from 'vite'

// 导入 Vue 插件
// @vitejs/plugin-vue 提供 Vue 单文件组件（.vue）的编译支持
import vue from '@vitejs/plugin-vue'

// 导入 Node.js 的 path 模块
// resolve 用于将相对路径转换为绝对路径
import { resolve } from 'path'

// ============================================================================
// 导出 Vite 配置
// ============================================================================
// https://vitejs.dev/config/
export default defineConfig({
  // --------------------------------------------------------------------------
  // 插件配置
  // --------------------------------------------------------------------------
  // plugins 数组中注册 Vite 插件
  // vue() 插件提供以下功能：
  // - 编译 .vue 单文件组件
  // - 支持 <script setup> 语法
  // - 支持 CSS scoped
  // - HMR（热模块替换）
  plugins: [vue()],

  // --------------------------------------------------------------------------
  // 路径解析配置
  // --------------------------------------------------------------------------
  resolve: {
    alias: {
      // 将 @ 映射到 src 目录
      // 这样在代码中可以用 import xxx from '@/utils/xxx'
      // 代替 import xxx from '../../../utils/xxx'
      //
      // __dirname 是 Node.js 全局变量，表示当前文件所在目录
      // resolve(__dirname, 'src') 解析为 src 目录的绝对路径
      '@': resolve(__dirname, 'src')
    }
  },

  // --------------------------------------------------------------------------
  // 开发服务器配置
  // --------------------------------------------------------------------------
  server: {
    // 开发服务器端口号
    // 访问 http://localhost:5173 即可查看应用
    port: 5173,

    // API 代理配置
    // 开发时前端（5173）和后端（8080/8000）端口不同，存在跨域问题
    // 代理将指定前缀的请求转发到后端服务器，避免跨域
    proxy: {
      // 将 /api 开头的请求代理到后端 Spring Boot 服务
      // 例如：fetch('/api/v1/documents') → http://localhost:8080/api/v1/documents
      '/api': {
        target: 'http://localhost:8080',  // 后端服务地址
        changeOrigin: true                 // 修改请求头中的 Origin，让后端认为请求来自同域
      },

      // 将 /ai-api 开头的请求代理到 AI 服务（Python/FastAPI）
      '/ai-api': {
        target: 'http://localhost:8000',   // AI 服务地址
        changeOrigin: true,
        // rewrite: 重写路径，去掉 /ai-api 前缀
        // 例如：/ai-api/chat → /chat（因为 AI 服务本身没有 /ai-api 路径）
        rewrite: (path) => path.replace(/^\/ai-api/, '')
      }
    }
  },

  // --------------------------------------------------------------------------
  // 生产构建配置
  // --------------------------------------------------------------------------
  build: {
    // 输出目录：构建产物放在 dist/ 文件夹
    outDir: 'dist',

    // sourcemap: 是否生成源映射文件（.map）
    // false 不生成，减小部署体积
    // 开发调试时可以设为 true，生产环境建议 false
    sourcemap: false,

    // Rollup 打包配置（Vite 生产构建使用 Rollup）
    rollupOptions: {
      output: {
        // manualChunks: 手动配置代码分割策略
        // 将第三方库拆分为独立的 chunk（代码块），优化缓存利用率
        //
        // 原理：第三方库很少变动，拆分后浏览器可以长期缓存这些文件
        // 只有业务代码变化时才需要重新下载
        manualChunks: {
          // element-plus 单独打包（体积较大，约 500KB+）
          'element-plus': ['element-plus'],
          // Vue 全家桶单独打包（vue + vue-router + pinia）
          'vue-vendor': ['vue', 'vue-router', 'pinia']
        }
      }
    }
  }
})
