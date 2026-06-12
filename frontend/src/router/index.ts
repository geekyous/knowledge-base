/**
 * ============================================================================
 * 路由配置 - router/index.ts
 * ============================================================================
 *
 * 【文件说明】
 * 这个文件定义了 Vue Router 的路由配置，包括：
 * - 路由表（URL 路径与页面组件的映射关系）
 * - 路由模式（HTML5 History 模式）
 * - 导航守卫（登录验证、权限检查、页面标题设置）
 * - 路由懒加载（按需加载页面组件，优化首屏性能）
 *
 * 【Vue 概念】
 * - Vue Router: Vue.js 官方的路由管理器，实现 SPA（单页应用）的页面导航
 * - History 模式: 使用 HTML5 pushState API，URL 不带 # 号（对比 Hash 模式）
 * - 导航守卫(Navigation Guards): 路由跳转前/后执行的钩子函数
 * - 懒加载(Lazy Loading): 使用 () => import() 动态导入，实现代码分割
 * - 嵌套路由(Nested Routes): children 配置实现布局嵌套
 *
 * 💡 学习要点:
 * 1. 路由懒加载使用 () => import() 语法，每个页面会被打包为独立的 JS 文件
 * 2. meta 字段可以存储自定义数据（如 title、requiresAuth），在导航守卫中使用
 * 3. 嵌套路由配合 <router-view /> 实现布局复用（Header/Footer 不变）
 * 4. beforeEach 导航守卫常用于权限控制和页面标题设置
 * 5. redirect 用于设置路由重定向（访问 / 自动跳转到 /home）
 * ============================================================================
 */

// 导入 Vue Router 的核心函数和类型
// createRouter: 创建路由实例的工厂函数
// createWebHistory: 创建 HTML5 History 模式的路由（URL 无 # 号）
import { createRouter, createWebHistory } from 'vue-router'

// 导入路由记录的类型定义，用于 TypeScript 类型检查
// RouteRecordRaw 定义了路由对象的类型结构（path、name、component、meta 等）
import type { RouteRecordRaw } from 'vue-router'

// 导入用户状态管理 store，用于在导航守卫中检查登录状态和用户角色
import { useUserStore } from '@/stores/user'

// ============================================================================
// 路由表定义
// ============================================================================
// RouteRecordRaw[] 类型确保每个路由对象都有正确的结构
// 路由匹配按照定义顺序，从上到下匹配
const routes: RouteRecordRaw[] = [
  // --------------------------------------------------------------------------
  // 登录页路由（独立页面，不使用 MainLayout 布局）
  // --------------------------------------------------------------------------
  {
    path: '/login',
    name: 'Login',
    // 懒加载：只有访问 /login 时才会下载 Login.vue 的代码
    // 这比直接 import 节省首屏加载时间
    component: () => import('@/views/auth/Login.vue'),
    // meta 字段存储路由元信息，可在导航守卫中通过 to.meta 访问
    meta: { title: '登录', requiresAuth: false }
  },

  // --------------------------------------------------------------------------
  // 注册页路由（独立页面，与登录页同级）
  // --------------------------------------------------------------------------
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册', requiresAuth: false }
  },

  // --------------------------------------------------------------------------
  // 密码修改路由（独立页面，需登录后访问）
  // --------------------------------------------------------------------------
  {
    path: '/change-password',
    name: 'PasswordChange',
    component: () => import('@/views/auth/PasswordChange.vue'),
    meta: { title: '修改密码', requiresAuth: false }
  },

  // --------------------------------------------------------------------------
  // 主布局路由（包含 Header + Main + Footer 的嵌套布局）
  // --------------------------------------------------------------------------
  {
    path: '/',
    // 主布局组件作为父路由的组件，提供统一的页面框架
    component: () => import('@/components/layout/MainLayout.vue'),
    // 重定向：访问根路径 / 时自动跳转到 /home
    redirect: '/home',
    // children 定义嵌套的子路由，它们会渲染在 MainLayout 的 <router-view /> 中
    children: [
      {
        // 完整路径为 /home（父路由 path '/' + 子路由 path 'home'）
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/Home.vue'),
        meta: { title: '首页', requiresAuth: true }
      },
      {
        path: 'search',
        name: 'Search',
        component: () => import('@/views/search/Search.vue'),
        meta: { title: '搜索', requiresAuth: true }
      },
      {
        path: 'documents',
        name: 'DocumentList',
        component: () => import('@/views/document/DocumentList.vue'),
        meta: { title: '文档列表', requiresAuth: true }
      },
      {
        // :id 是动态路由参数（路由参数），可以通过 route.params.id 获取
        // 例如访问 /documents/123 时，route.params.id === '123'
        path: 'documents/:id',
        name: 'DocumentDetail',
        component: () => import('@/views/document/DocumentDetail.vue'),
        meta: { title: '文档详情', requiresAuth: true }
      },
      {
        path: 'documents/:id/edit',
        name: 'DocumentEdit',
        component: () => import('@/views/document/DocumentEdit.vue'),
        meta: { title: '编辑文档', requiresAuth: true }
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('@/views/chat/Chat.vue'),
        meta: { title: '智能问答', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/Profile.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      },
      {
        path: 'admin',
        name: 'Admin',
        component: () => import('@/views/admin/Dashboard.vue'),
        // requiresRole: 额外的权限要求，只有 ADMIN 角色才能访问
        meta: { title: '管理后台', requiresAuth: true, requiresRole: 'ADMIN' }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理', requiresAuth: true, requiresRole: 'ADMIN' }
      },
      {
        path: 'admin/reviews',
        name: 'AdminReviews',
        component: () => import('@/views/admin/Reviews.vue'),
        meta: { title: '文档审核', requiresAuth: true, requiresRole: 'ADMIN' }
      },
      {
        path: 'admin/categories',
        name: 'AdminCategories',
        component: () => import('@/views/admin/Categories.vue'),
        meta: { title: '分类管理', requiresAuth: true, requiresRole: 'ADMIN' }
      },
      {
        path: 'admin/tags',
        name: 'AdminTags',
        component: () => import('@/views/admin/Tags.vue'),
        meta: { title: '标签管理', requiresAuth: true, requiresRole: 'ADMIN' }
      },
      {
        path: 'admin/settings',
        name: 'AdminSettings',
        component: () => import('@/views/admin/Settings.vue'),
        meta: { title: '系统设置', requiresAuth: true, requiresRole: 'ADMIN' }
      },
      {
        path: 'documents/:id/versions',
        name: 'DocumentVersions',
        component: () => import('@/views/document/DocumentVersions.vue'),
        meta: { title: '版本对比', requiresAuth: true }
      }
    ]
  },

  // --------------------------------------------------------------------------
  // 错误页面路由
  // --------------------------------------------------------------------------
  {
    path: '/401',
    name: 'Unauthorized',
    component: () => import('@/views/error/Unauthorized.vue'),
    meta: { title: '请先登录' }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/Forbidden.vue'),
    meta: { title: '没有权限' }
  },
  // --------------------------------------------------------------------------
  // 404 兜底路由（必须放在最后）
  // --------------------------------------------------------------------------
  {
    // :pathMatch(.*)* 是 Vue Router 4 的通配符语法，匹配所有未定义的路径
    // 这与 Vue Router 3 的 path: '*' 语法不同
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '页面未找到' }
  }
]

// ============================================================================
// 创建路由实例
// ============================================================================
// createWebHistory() 使用 HTML5 的 History API（pushState/replaceState）
// 优点：URL 更干净，没有 # 号（如 /home 而不是 /#/home）
// 注意：部署时需要服务器配置回退到 index.html（否则刷新页面会 404）
const router = createRouter({
  history: createWebHistory(),
  routes
})

// ============================================================================
// 全局前置守卫（Navigation Guard）
// ============================================================================
// beforeEach 在每次路由跳转之前执行
// 参数说明：
// - to: 即将进入的目标路由对象
// - from: 当前导航正要离开的路由对象
// - next: 放行函数，必须调用才能完成导航（Vue Router 4 也支持 return 替代 next）
router.beforeEach((to, from, next) => {
  // 在守卫中使用 Pinia store 必须在 router 创建之后，否则会报错
  const userStore = useUserStore()

  // --------------------------------------------------------------------------
  // 设置页面标题
  // --------------------------------------------------------------------------
  // 动态修改 document.title，让浏览器标签页显示当前页面名称
  document.title = `${to.meta.title || '企业知识库'} - 知识库问答系统`

  // --------------------------------------------------------------------------
  // 认证检查
  // --------------------------------------------------------------------------
  // 如果目标路由需要登录（requiresAuth: true）且用户未登录
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    // 重定向到登录页，并通过 query 参数保存原始目标路径
    // 登录成功后可以读取 redirect 参数跳转回原始页面
    next({
      name: 'Login',
      query: { redirect: to.fullPath }
    })
    return
  }

  // --------------------------------------------------------------------------
  // 角色权限检查
  // --------------------------------------------------------------------------
  // 如果路由要求特定角色（如 requiresRole: 'ADMIN'），检查用户是否拥有该角色
  if (to.meta.requiresRole && userStore.currentUser?.role !== to.meta.requiresRole) {
    // 权限不足时重定向到首页
    next({ name: 'Home' })
    return
  }

  // 所有检查通过，放行导航
  next()
})

// 导出路由实例，供 main.ts 中的 app.use(router) 使用
export default router
