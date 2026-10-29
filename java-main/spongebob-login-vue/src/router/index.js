import { createRouter, createWebHashHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import HomePage from '../components/HomePage.vue'
import WorkPage from '../components/WorkPage.vue'
import GamePage from '../components/GamePage.vue'
import { getToken, getNickname } from '../utils/auth.js'

const routes = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    // 已登录的用户不该再看到登录页
    meta: { guestOnly: true, title: '比奇堡登录' }
  },
  {
    path: '/home',
    name: 'home',
    component: HomePage,
    // 没 token 进不来
    meta: { requiresAuth: true, title: '比奇堡首页' },
    // 首页需要的昵称直接从本地存储取
    props: () => ({ nickname: getNickname() || '海绵宝宝' })
  },
  {
    path: '/work',
    name: 'work',
    component: WorkPage,
    meta: { requiresAuth: true, title: '打工日常' },
    props: () => ({ nickname: getNickname() || '海绵宝宝' })
  },
  {
    path: '/game',
    name: 'game',
    component: GamePage,
    meta: { requiresAuth: true, title: '出餐大作战' },
    props: () => ({ nickname: getNickname() || '海绵宝宝' })
  },
  {
    // 兜底：乱敲地址一律回首页，再由守卫决定要不要踢去登录
    path: '/:pathMatch(.*)*',
    redirect: '/home'
  }
]

const router = createRouter({
  // 用 hash 模式：Spring Boot 里不需要额外的 history fallback 配置，
  // 直接访问 /#/home 也能打开
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

/**
 * 全局前置守卫
 * 1. 目标页要登录、但本地没 token → 拦截，跳登录页，并记住原本想去哪
 * 2. 已登录还想去登录页 → 直接送去首页
 */
router.beforeEach((to) => {
  const hasToken = Boolean(getToken())

  if (to.meta.requiresAuth && !hasToken) {
    return {
      name: 'login',
      query: to.fullPath && to.fullPath !== '/home' ? { redirect: to.fullPath } : {}
    }
  }

  if (to.meta.guestOnly && hasToken) {
    return { name: 'home' }
  }

  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 海绵宝宝` : '比奇堡'
})

export default router
