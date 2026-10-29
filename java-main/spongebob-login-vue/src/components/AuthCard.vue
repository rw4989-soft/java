<template>
  <div class="card">
    <SpongeBob />

    <h1>比奇堡</h1>

    <!-- 登录 / 注册 选项卡 -->
    <div class="tabs">
      <button
        type="button"
        :class="{ active: tab === 'login' }"
        @click="switchTab('login')"
      >登 录</button>
      <button
        type="button"
        :class="{ active: tab === 'register' }"
        @click="switchTab('register')"
      >注 册</button>
      <span class="tab-slider" :class="{ right: tab === 'register' }"></span>
    </div>

    <p class="subtitle">
      {{ tab === 'login' ? 'Bikini Bottom Login · 我准备好啦！' : 'Bikini Bottom Register · 一起去抓水母吧！' }}
    </p>

    <transition name="swap" mode="out-in">
      <LoginForm
        v-if="tab === 'login'"
        key="login"
        :preset-username="presetUsername"
        :flash-message="flashMessage"
        @logged-in="handleLoggedIn"
      />
      <RegisterForm
        v-else
        key="register"
        @registered="handleRegistered"
      />
    </transition>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import SpongeBob from './SpongeBob.vue'
import LoginForm from './LoginForm.vue'
import RegisterForm from './RegisterForm.vue'

const tab = ref('login')
const presetUsername = ref('')
const flashMessage = ref('')

// 登录成功后交给父组件 App 切换到首页
const emit = defineEmits(['logged-in'])

function switchTab(target) {
  if (target === 'register') {
    presetUsername.value = ''
    flashMessage.value = ''
  }
  tab.value = target
}

function handleLoggedIn(name) {
  emit('logged-in', name)
}

// 注册成功后切回登录表单，自动填好用户名并提示
function handleRegistered(username) {
  presetUsername.value = username
  flashMessage.value = '🎉 注册成功！快登录看看你的菠萝屋吧'
  tab.value = 'login'
}
</script>

<style scoped>
.card {
  position: relative; z-index: 5;
  width: 380px; max-width: 92vw;
  background: rgba(255, 253, 240, .96);
  border: 4px solid #f9a825;
  border-radius: 28px;
  padding: 110px 32px 28px;
  box-shadow: 0 20px 50px rgba(1, 60, 110, .45);
  animation: cardIn .7s cubic-bezier(.2, 1.4, .4, 1);
}
@keyframes cardIn {
  from { transform: translateY(60px) scale(.85); opacity: 0; }
  to   { transform: translateY(0) scale(1); opacity: 1; }
}

h1 {
  text-align: center; color: #f57f17;
  font-size: 26px; letter-spacing: 6px;
  text-shadow: 2px 2px 0 #fff3c4;
  margin-bottom: 14px;
}

/* 选项卡 */
.tabs {
  position: relative;
  display: flex;
  background: #fff3c4;
  border: 3px solid #ffe082;
  border-radius: 14px;
  padding: 4px;
  margin-bottom: 14px;
}
.tabs button {
  flex: 1;
  position: relative; z-index: 2;
  padding: 9px 0;
  border: none; background: transparent;
  font-size: 15px; font-weight: bold; letter-spacing: 4px;
  color: #8d6e63; cursor: pointer;
  transition: color .25s;
}
.tabs button.active { color: #5d3700; }
.tab-slider {
  position: absolute; z-index: 1;
  top: 4px; bottom: 4px; left: 4px;
  width: calc(50% - 4px);
  background: linear-gradient(180deg, #ffd54f, #ffb300);
  border-radius: 10px;
  box-shadow: 0 2px 0 #c17900;
  transition: transform .28s cubic-bezier(.3, 1.3, .4, 1);
}
.tab-slider.right { transform: translateX(100%); }

.subtitle {
  text-align: center; color: #0288d1;
  font-size: 13px; margin-bottom: 22px;
}

/* 表单切换过渡 */
.swap-enter-active,
.swap-leave-active {
  transition: opacity .22s ease, transform .22s ease;
}
.swap-enter-from {
  opacity: 0;
  transform: translateX(24px);
}
.swap-leave-to {
  opacity: 0;
  transform: translateX(-24px);
}
</style>
