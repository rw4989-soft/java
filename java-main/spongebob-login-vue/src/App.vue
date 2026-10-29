<template>
  <!-- 路由出口：登录页 / 首页 由 router 决定 -->
  <router-view v-slot="{ Component }">
    <transition name="page" mode="out-in">
      <component :is="Component" @logout="handleLogout" />
    </transition>
  </router-view>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { clearSession, getToken } from './utils/auth.js'
import { logout as logoutApi } from './api/auth.js'

const router = useRouter()

function handleLogout() {
  const token = getToken()
  // 先清本地，界面立刻响应；再顺手通知后端让 token 失效（失败也不影响退出）
  clearSession()
  router.replace({ name: 'login' })
  if (token) logoutApi(token).catch(() => {})
}
</script>

<style>
/* 全局样式（非 scoped） */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: "Comic Sans MS", "Chalkboard SE", "PingFang SC", "Microsoft YaHei", sans-serif;
}

body {
  min-height: 100vh;
  background: linear-gradient(180deg, #4fc3f7 0%, #29b6f6 35%, #0288d1 75%, #01579b 100%);
  background-attachment: fixed;
}

/* 登录页容器（首页自带布局，不用这个类） */
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}

/* 登录页 ⇄ 首页 切换过渡 */
.page-enter-active,
.page-leave-active {
  transition: opacity .3s ease, transform .3s ease;
}
.page-enter-from {
  opacity: 0;
  transform: scale(.98);
}
.page-leave-to {
  opacity: 0;
  transform: scale(1.02);
}
</style>
