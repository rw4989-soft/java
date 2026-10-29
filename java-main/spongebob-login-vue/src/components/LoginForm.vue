<template>
  <form @submit.prevent="handleLogin">
    <div class="field">
      <span class="icon">🍍</span>
      <input
        type="text"
        v-model="username"
        placeholder="用户名（试试 spongebob）"
        autocomplete="username"
      >
    </div>
    <div class="field">
      <span class="icon">🔑</span>
      <input
        type="password"
        v-model="password"
        placeholder="密码（试试 123456）"
        autocomplete="current-password"
      >
    </div>
    <div class="options">
      <label><input type="checkbox" v-model="remember"> 记住我</label>
      <a href="#" @click.prevent="forgot">忘记密码？</a>
    </div>
    <button type="submit" class="btn" :disabled="loading">{{ loading ? '登录中…' : '登 录' }}</button>
    <div class="message" :class="messageType">{{ message }}</div>
  </form>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { login } from '../api/auth.js'
import { setSession, getUsername } from '../utils/auth.js'

const props = defineProps({
  presetUsername: { type: String, default: '' },
  flashMessage: { type: String, default: '' }
})

const emit = defineEmits(['logged-in'])

const username = ref('')
const password = ref('')
const remember = ref(true)
const message = ref('')
const messageType = ref('')
const loading = ref(false)

onMounted(() => {
  if (props.presetUsername) {
    username.value = props.presetUsername
  } else {
    // 上次勾了「记住我」就自动填上
    username.value = getUsername()
  }
  if (props.flashMessage) {
    messageType.value = 'ok'
    message.value = props.flashMessage
  }
})

async function handleLogin() {
  const u = username.value.trim()
  const p = password.value.trim()

  if (!u || !p) {
    messageType.value = 'err'
    message.value = '章鱼哥说：用户名和密码都不能空！'
    return
  }

  loading.value = true
  messageType.value = ''
  message.value = '正在连接蟹堡王后厨…'
  try {
    const r = await login(u, p)
    if (r.ok) {
      // 后端返回 { token, tokenType, expiresIn, username, nickname }
      const token = r.data?.token
      if (!token) {
        messageType.value = 'err'
        message.value = '登录成功但后端没返回 token，请确认后端已是最新代码'
        return
      }

      // 1) 存 token + 昵称，登录态交给 localStorage
      setSession({
        token,
        nickname: r.data?.nickname || u,
        username: r.data?.username || u
      })

      // 2) 记住我：勾了就留用户名方便下次自动填，不勾就清掉
      if (remember.value) {
        localStorage.setItem('sb_username', u)
      } else {
        localStorage.removeItem('sb_username')
      }

      messageType.value = 'ok'
      message.value = '登录成功，正在进入比奇堡…'

      // 3) 通知外层跳转（LoginView 里 push 到首页 / 原目标页）
      emit('logged-in', r.data?.nickname || u)
    } else {
      messageType.value = 'err'
      message.value = r.message
    }
  } catch (e) {
    messageType.value = 'err'
    message.value = '🐠 网络开小差了，连不上后端！请确认 Spring Boot 已启动在 8081。'
  } finally {
    loading.value = false
  }
}

function forgot() {
  messageType.value = 'err'
  message.value = '去问问蟹老板吧，他记性最好了 🦀'
}
</script>

<style scoped>
.field { position: relative; margin-bottom: 16px; }
.field input {
  width: 100%; padding: 13px 14px 13px 44px;
  border: 3px solid #ffe082; border-radius: 14px;
  background: #fffde7; font-size: 15px; color: #4e342e;
  outline: none; transition: border-color .2s, box-shadow .2s;
}
.field input:focus {
  border-color: #ffb300;
  box-shadow: 0 0 0 4px rgba(255, 179, 0, .2);
}
.field .icon {
  position: absolute; left: 14px; top: 50%;
  transform: translateY(-50%);
  font-size: 18px;
}

.options {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 13px; color: #6d4c41; margin-bottom: 18px;
}
.options label { display: flex; align-items: center; gap: 5px; cursor: pointer; }
.options a { color: #0288d1; text-decoration: none; }
.options a:hover { text-decoration: underline; }

.btn {
  width: 100%; padding: 14px;
  border: none; border-radius: 16px;
  background: linear-gradient(180deg, #ffd54f, #ffb300);
  color: #5d3700; font-size: 17px; font-weight: bold; letter-spacing: 3px;
  cursor: pointer;
  box-shadow: 0 5px 0 #c17900, 0 8px 14px rgba(0,0,0,.15);
  transition: transform .1s, box-shadow .1s;
}
.btn:hover { filter: brightness(1.05); }
.btn:active {
  transform: translateY(4px);
  box-shadow: 0 1px 0 #c17900, 0 3px 6px rgba(0,0,0,.15);
}

.message {
  text-align: center; margin-top: 12px;
  font-size: 14px; font-weight: bold; min-height: 20px;
}
.message.ok { color: #2e7d32; }
.message.err { color: #c62828; }
</style>
