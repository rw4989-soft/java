<template>
  <header class="nav">
    <div class="nav-inner">
      <router-link to="/home" class="brand">
        <span class="brand-logo">🍍</span>
        <span class="brand-text">
          <strong>比奇堡</strong>
          <em>BIKINI BOTTOM</em>
        </span>
      </router-link>

      <nav class="links">
        <router-link
          v-for="item in items"
          :key="item.key"
          :to="item.to"
          class="link"
          :class="{ active: active === item.key }"
        >
          <span class="link-icon">{{ item.icon }}</span>{{ item.label }}
        </router-link>
      </nav>

      <div class="right">
        <button
          type="button"
          class="sound"
          :title="muted ? '打开音效' : '关闭音效'"
          :aria-label="muted ? '打开音效' : '关闭音效'"
          @click="onToggleSound"
        >{{ muted ? '🔇' : '🔊' }}</button>

        <div class="user-chip" :title="nickname">
          <span class="avatar">🧽</span>
          <span class="user-name">{{ nickname }}</span>
        </div>
        <button class="logout" type="button" @click="$emit('logout')">退出</button>
      </div>
    </div>
    <div class="line"></div>
  </header>
</template>

<script setup>
import { ref } from 'vue'
import { sfx } from '../utils/sound.js'

defineProps({
  active: { type: String, default: 'home' },
  nickname: { type: String, default: '海绵宝宝' }
})

defineEmits(['logout'])

const items = [
  { key: 'home', to: '/home', label: '首页', icon: '🏠' },
  { key: 'work', to: '/work', label: '打工日常', icon: '🧽' },
  { key: 'game', to: '/game', label: '出餐作战', icon: '🍔' }
]

const muted = ref(sfx.isMuted())

function onToggleSound() {
  muted.value = sfx.toggle()
}
</script>

<style scoped>
.nav {
  position: sticky;
  top: 0;
  z-index: 50;
  background: linear-gradient(180deg, rgba(3, 52, 84, 0.88), rgba(3, 45, 74, 0.76));
  backdrop-filter: blur(14px) saturate(140%);
  box-shadow: 0 6px 24px rgba(1, 26, 44, 0.35);
}

.line {
  height: 3px;
  background: linear-gradient(90deg, transparent, rgba(126, 226, 255, 0.75), rgba(255, 213, 79, 0.85), rgba(126, 226, 255, 0.75), transparent);
  box-shadow: 0 0 12px rgba(126, 226, 255, 0.55);
}

.nav-inner {
  max-width: 1120px;
  margin: 0 auto;
  padding: 10px 20px;
  display: flex;
  align-items: center;
  gap: 18px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  text-decoration: none;
}

.brand-logo {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  font-size: 22px;
  background: linear-gradient(180deg, #ffe082, #ffb300);
  border: 3px solid #ffd54f;
  border-radius: 14px;
  box-shadow: 0 3px 0 #c17900, 0 0 18px rgba(255, 193, 7, 0.45);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}
.brand-text strong {
  color: #fff8e1;
  font-size: 18px;
  letter-spacing: 3px;
  text-shadow: 0 2px 0 rgba(0, 0, 0, 0.25), 0 0 16px rgba(255, 213, 79, 0.45);
}
.brand-text em {
  font-style: normal;
  font-size: 10px;
  letter-spacing: 1.5px;
  color: #8fdcff;
}

.links {
  display: flex;
  gap: 4px;
  margin-left: 8px;
}

.link {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 15px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: bold;
  color: #b9e6ff;
  text-decoration: none;
  transition: background 0.2s, color 0.2s, transform 0.15s;
}
.link:hover {
  background: rgba(126, 226, 255, 0.16);
  color: #fff;
  transform: translateY(-1px);
}
.link.active {
  background: linear-gradient(180deg, #ffd54f, #ffb300);
  color: #5d3700;
  box-shadow: 0 3px 0 #c17900, 0 0 16px rgba(255, 193, 7, 0.45);
}
.link-icon {
  font-size: 15px;
}

.right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
}

.sound {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  font-size: 15px;
  border-radius: 50%;
  border: 2px solid rgba(126, 226, 255, 0.4);
  background: rgba(126, 226, 255, 0.1);
  cursor: pointer;
  transition: all 0.2s;
}
.sound:hover {
  background: rgba(126, 226, 255, 0.22);
  transform: scale(1.08);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 5px 14px 5px 6px;
  background: rgba(126, 226, 255, 0.12);
  border: 2px solid rgba(126, 226, 255, 0.45);
  border-radius: 999px;
  max-width: 190px;
}
.avatar {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  flex-shrink: 0;
}
.user-name {
  font-size: 14px;
  font-weight: bold;
  color: #eaf7ff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout {
  border: 2px solid rgba(126, 226, 255, 0.45);
  background: transparent;
  color: #b9e6ff;
  padding: 6px 14px;
  border-radius: 999px;
  font-family: inherit;
  font-size: 13px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s;
}
.logout:hover {
  background: rgba(255, 205, 210, 0.18);
  border-color: #ff8a80;
  color: #ffcdd2;
}

@media (max-width: 880px) {
  .brand-text,
  .user-name {
    display: none;
  }
  .nav-inner {
    gap: 10px;
    padding: 10px 12px;
  }
  .link {
    padding: 7px 10px;
    font-size: 13px;
  }
  .link-icon {
    display: none;
  }
}
</style>
