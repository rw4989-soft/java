<template>
  <form @submit.prevent="handleRegister">
    <div class="field">
      <span class="icon">🍍</span>
      <input
        type="text"
        v-model="username"
        placeholder="给自己起个海底名字"
        autocomplete="username"
      >
    </div>
    <div class="field">
      <span class="icon">⭐</span>
      <input
        type="text"
        v-model="nickname"
        placeholder="昵称（选填，派大星替你保密）"
      >
    </div>
    <div class="field">
      <span class="icon">🔑</span>
      <input
        type="password"
        v-model="password"
        placeholder="设置密码（至少 6 位）"
        autocomplete="new-password"
      >
    </div>
    <div class="field">
      <span class="icon">🔒</span>
      <input
        type="password"
        v-model="confirmPassword"
        placeholder="再输入一次密码"
        autocomplete="new-password"
      >
    </div>

    <label class="agree">
      <input type="checkbox" v-model="agree">
      我保证不偷蟹黄堡秘方 🍔
    </label>

    <button type="submit" class="btn" :disabled="loading">{{ loading ? '注册中…' : '注 册' }}</button>
    <div class="message" :class="messageType">{{ message }}</div>
  </form>
</template>

<script setup>
import { ref } from 'vue'
import { register } from '../api/auth.js'

const emit = defineEmits(['registered'])

const username = ref('')
const nickname = ref('')
const password = ref('')
const confirmPassword = ref('')
const agree = ref(false)
const message = ref('')
const messageType = ref('')
const loading = ref(false)

function fail(text) {
  messageType.value = 'err'
  message.value = text
}

async function handleRegister() {
  const u = username.value.trim()
  const p = password.value
  const cp = confirmPassword.value

  // 前端基础校验（与后端规则保持一致：用户名≥3、密码≥6）
  if (!u) return fail('章鱼哥说：连名字都不填还想加入比奇堡？')
  if (u.length < 3) return fail('名字太短啦，至少 3 个字符！')
  if (!p) return fail('密码不能空，不然痞老板就来了！')
  if (p.length < 6) return fail('密码至少 6 位，蟹老板的钱柜都比这安全！')
  if (p !== cp) return fail('两次密码不一样，派大星都看出来了！')
  if (!agree.value) return fail('先保证不偷蟹黄堡秘方才行 🍔')

  loading.value = true
  messageType.value = ''
  message.value = '正在向比奇堡报到…'
  try {
    const r = await register(u, p, nickname.value.trim())
    if (r.ok) {
      messageType.value = 'ok'
      message.value = r.message
      // 稍作停留后带着用户名跳回登录表单
      setTimeout(() => emit('registered', u), 1200)
    } else {
      messageType.value = 'err'
      message.value = r.message
    }
  } catch (e) {
    messageType.value = 'err'
    message.value = '🐠 网络开小差了，注册请求没发出去！请确认 Spring Boot 已启动在 8081。'
  } finally {
    loading.value = false
  }
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

.agree {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; color: #6d4c41;
  margin-bottom: 18px; cursor: pointer;
}

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
