<template>
  <div class="decor">
    <div class="sand"></div>
    <div class="pineapple">🍍</div>
    <div class="rock">🪨</div>

    <div
      v-for="(j, i) in jellyfishes"
      :key="'j' + i"
      class="jellyfish"
      :style="j"
    >{{ j.emoji }}</div>

    <div
      v-for="(b, i) in bubbles"
      :key="'b' + i"
      class="bubble"
      :style="b"
    ></div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// 漂浮的水母 / 海洋生物
const jellyfishes = ref([
  { top: '12%', left: '12%', animationDelay: '0s', fontSize: '46px', emoji: '🪼' },
  { top: '22%', right: '14%', animationDelay: '2s', fontSize: '34px', emoji: '🪼' },
  { top: '60%', left: '8%', animationDelay: '4s', fontSize: '28px', emoji: '🐠' },
  { top: '55%', right: '9%', animationDelay: '1s', fontSize: '30px', emoji: '⭐' }
])

// 随机上升气泡
const bubbles = ref(
  Array.from({ length: 18 }, () => {
    const size = 8 + Math.random() * 30
    return {
      width: size + 'px',
      height: size + 'px',
      left: Math.random() * 100 + 'vw',
      animationDuration: 5 + Math.random() * 8 + 's',
      animationDelay: Math.random() * 8 + 's'
    }
  })
)
</script>

<style scoped>
.decor {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.sand {
  position: absolute; bottom: 0; left: 0; right: 0; height: 120px;
  background: linear-gradient(180deg, #f6e27a, #e8c95a);
  border-radius: 50% 50% 0 0 / 60px 60px 0 0;
  z-index: 0;
}

.bubble {
  position: absolute; bottom: -60px;
  background: radial-gradient(circle at 30% 30%, rgba(255,255,255,.9), rgba(255,255,255,.15));
  border-radius: 50%;
  animation: rise linear infinite;
  opacity: .7;
  z-index: 1;
}
@keyframes rise {
  0%   { transform: translateY(0) translateX(0) scale(1); }
  50%  { transform: translateY(-55vh) translateX(25px) scale(1.1); }
  100% { transform: translateY(-110vh) translateX(-15px) scale(.9); opacity: 0; }
}

.jellyfish {
  position: absolute; z-index: 1;
  animation: floaty 7s ease-in-out infinite;
  filter: drop-shadow(0 4px 8px rgba(0,0,0,.15));
}
@keyframes floaty {
  0%,100% { transform: translateY(0) rotate(-6deg); }
  50%     { transform: translateY(-30px) rotate(6deg); }
}

.pineapple {
  position: absolute; bottom: 70px; left: 5%;
  font-size: 80px; z-index: 1;
  filter: drop-shadow(0 6px 10px rgba(0,0,0,.2));
}
.rock {
  position: absolute; bottom: 60px; right: 6%;
  font-size: 64px; z-index: 1;
  filter: drop-shadow(0 6px 10px rgba(0,0,0,.2));
}
</style>
