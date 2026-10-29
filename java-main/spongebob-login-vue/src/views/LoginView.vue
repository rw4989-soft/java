<template>
  <div class="page">
    <OceanDecor />
    <AuthCard @logged-in="handleLoggedIn" />
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import OceanDecor from '../components/OceanDecor.vue'
import AuthCard from '../components/AuthCard.vue'

const router = useRouter()
const route = useRoute()

/**
 * 登录表单已经写过 token 了，这里只负责跳转。
 * 带 redirect 参数时（被守卫拦截过来的）跳回原来想去的页面。
 */
function handleLoggedIn() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  // 只接受站内路径，防止 ?redirect=https://evil.com 这种跳转
  const target = redirect.startsWith('/') ? redirect : '/home'
  router.replace(target)
}
</script>
