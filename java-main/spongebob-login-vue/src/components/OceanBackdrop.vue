<template>
  <!-- 纯装饰层：固定铺满视口，不响应鼠标 -->
  <div class="sea" aria-hidden="true">
    <div class="water"></div>

    <!-- 海面：水面亮带 + 波浪边缘 -->
    <svg class="surface" viewBox="0 0 1440 80" preserveAspectRatio="none">
      <path
        d="M0,0 L1440,0 L1440,42 C1320,64 1200,28 1080,46 C960,64 840,32 720,48 C600,64 480,30 360,46 C240,62 120,32 0,48 Z"
        fill="#bdf2ff"
        opacity=".42"
      />
      <path
        d="M0,20 C140,44 260,4 400,24 C540,44 660,8 800,26 C940,44 1060,10 1200,28 C1300,40 1380,24 1440,34"
        fill="none"
        stroke="#ffffff"
        stroke-width="2"
        opacity=".25"
      />
    </svg>

    <!-- 阳光光柱 -->
    <div class="rays">
      <span v-for="(r, i) in rays" :key="'r' + i" class="ray" :style="r"></span>
    </div>

    <!-- 水波焦散 -->
    <div class="caustics"></div>

    <!-- 中层水体雾光，制造纵深 -->
    <div class="haze"></div>

    <!-- 浮游微粒 -->
    <div class="plankton">
      <span v-for="(p, i) in plankton" :key="'p' + i" class="mote" :style="p"></span>
    </div>

    <!-- 水母 -->
    <div v-for="(j, i) in jellyfish" :key="'j' + i" class="jelly" :style="j">
      <svg viewBox="0 0 60 96" width="100%" height="100%">
        <ellipse cx="30" cy="30" rx="23" ry="19" fill="rgba(255,255,255,.34)" stroke="rgba(255,255,255,.6)" stroke-width="1.6" />
        <path
          d="M13,44 q4,20 -2,34 M24,48 q3,18 0,30 M36,48 q-3,18 0,30 M47,44 q-4,20 2,34"
          stroke="rgba(255,255,255,.5)"
          stroke-width="2.6"
          fill="none"
          stroke-linecap="round"
        />
      </svg>
    </div>

    <!-- 游过的鱼 -->
    <div v-for="(f, i) in fishes" :key="'f' + i" class="fish" :style="f.style">
      <span class="fish-body" :style="{ fontSize: f.size, scale: f.dir }">{{ f.emoji }}</span>
    </div>

    <!-- 海床：礁石 + 珊瑚 + 海草 + 沙地 -->
    <div class="seabed">
      <div class="kelp kelp-a"></div>
      <div class="kelp kelp-b"></div>
      <div class="kelp kelp-c"></div>
      <div class="kelp kelp-d"></div>

      <svg class="reef" viewBox="0 0 1440 300" preserveAspectRatio="xMidYMax slice">
        <defs>
          <linearGradient id="sbSand" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stop-color="#f7e8ad" />
            <stop offset="55%" stop-color="#e3c976" />
            <stop offset="100%" stop-color="#c5a44c" />
          </linearGradient>
        </defs>

        <!-- 远景礁石 -->
        <path
          d="M0,196 C150,168 250,208 400,190 C560,170 640,212 800,192 C960,172 1060,214 1220,194 C1320,182 1390,198 1440,188 L1440,300 L0,300 Z"
          fill="#075b86"
          opacity=".45"
        />

        <!-- 远景珊瑚 -->
        <g stroke="#0b6d95" stroke-width="10" stroke-linecap="round" fill="none" opacity=".55">
          <path d="M180,252 C176,208 192,188 186,154 M184,206 C162,192 154,174 150,152 M188,190 C212,176 220,160 224,140" />
          <path d="M980,246 C976,206 992,188 986,158 M984,204 C962,190 954,172 950,150 M988,190 C1012,176 1020,158 1024,138" />
        </g>

        <!-- 近景珊瑚（暖色，呼应蟹堡王） -->
        <g stroke="#e07a5f" stroke-width="9" stroke-linecap="round" fill="none" opacity=".62">
          <path d="M420,260 C416,222 430,204 426,176 M424,216 C404,204 396,188 392,168 M428,202 C450,190 458,174 462,156" />
          <path d="M1322,254 C1318,220 1332,204 1328,178 M1326,214 C1306,202 1298,186 1294,166 M1330,200 C1352,188 1360,172 1364,154" />
        </g>

        <!-- 海草剪影 -->
        <g stroke="#127a5e" stroke-width="11" stroke-linecap="round" fill="none" opacity=".45">
          <path d="M760,256 C752,222 772,198 762,168" />
          <path d="M790,258 C784,228 800,208 792,184" />
          <path d="M1130,256 C1122,224 1140,202 1132,174" />
        </g>

        <!-- 沙丘 -->
        <path
          d="M0,236 C140,212 250,258 400,244 C560,228 640,264 800,250 C960,236 1060,270 1220,252 C1330,240 1390,254 1440,246 L1440,300 L0,300 Z"
          fill="url(#sbSand)"
        />
        <!-- 沙地高光 -->
        <path
          d="M0,254 C150,234 260,272 410,258 C570,244 660,276 820,262 C980,248 1080,278 1240,262 C1340,252 1400,264 1440,258 L1440,300 L0,300 Z"
          fill="#fff2cb"
          opacity=".32"
        />

        <!-- 石头 -->
        <ellipse cx="648" cy="272" rx="46" ry="19" fill="#0a5c86" opacity=".38" />
        <ellipse cx="1190" cy="278" rx="34" ry="14" fill="#0a5c86" opacity=".3" />
      </svg>
    </div>

    <!-- 上升气泡 -->
    <div class="bubbles">
      <span v-for="(b, i) in bubbles" :key="'b' + i" class="bubble" :style="b"></span>
    </div>

    <!-- 底部压暗，保证内容可读 -->
    <div class="depth-shade"></div>
  </div>
</template>

<script setup>
/* 光柱：角度、位置错开，避免看出规律 */
const rays = [
  { left: '4%', width: '14vw', '--rot': '-9deg', animationDelay: '0s', animationDuration: '13s' },
  { left: '22%', width: '9vw', '--rot': '-4deg', animationDelay: '2.5s', animationDuration: '16s' },
  { left: '40%', width: '16vw', '--rot': '3deg', animationDelay: '1.2s', animationDuration: '18s' },
  { left: '62%', width: '10vw', '--rot': '7deg', animationDelay: '4s', animationDuration: '15s' },
  { left: '80%', width: '13vw', '--rot': '11deg', animationDelay: '3s', animationDuration: '17s' }
]

/* 浮游微粒 */
const plankton = Array.from({ length: 26 }, () => ({
  left: Math.random() * 100 + '%',
  top: Math.random() * 100 + '%',
  animationDelay: (Math.random() * 8).toFixed(1) + 's',
  animationDuration: (4 + Math.random() * 5).toFixed(1) + 's'
}))

/* 水母：左边大、右边小，做景深 */
const jellyfish = [
  { top: '14%', left: '5%', width: '64px', height: '96px', animationDelay: '0s', animationDuration: '9s', opacity: 0.75 },
  { top: '30%', right: '7%', width: '44px', height: '66px', animationDelay: '2.4s', animationDuration: '11s', opacity: 0.6 },
  { top: '58%', left: '11%', width: '34px', height: '52px', animationDelay: '4s', animationDuration: '13s', opacity: 0.45 }
]

/* 鱼：dir = -1 表示朝左游 */
const fishes = [
  { top: '18%', emoji: '🐠', size: '26px', dir: 1, duration: '48s', delay: '0s' },
  { top: '38%', emoji: '🐟', size: '20px', dir: -1, duration: '62s', delay: '6s' },
  { top: '66%', emoji: '🐡', size: '22px', dir: 1, duration: '70s', delay: '12s' },
  { top: '82%', emoji: '🐟', size: '16px', dir: 1, duration: '80s', delay: '3s' }
].map((f) => ({
  emoji: f.emoji,
  size: f.size,
  // 朝左的鱼：水平翻转 + 动画反向，否则会"倒着游"
  dir: f.dir === -1 ? '-1 1' : '1 1',
  style: {
    top: f.top,
    animationDuration: f.duration,
    animationDelay: f.delay,
    animationDirection: f.dir === -1 ? 'reverse' : 'normal'
  }
}))

/* 气泡 */
const bubbles = Array.from({ length: 18 }, () => {
  const size = 6 + Math.random() * 26
  return {
    width: size + 'px',
    height: size + 'px',
    left: Math.random() * 100 + '%',
    animationDuration: (7 + Math.random() * 9).toFixed(1) + 's',
    animationDelay: (Math.random() * 10).toFixed(1) + 's'
  }
})
</script>

<style scoped>
.sea {
  position: fixed;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
}

/* ============ 水体：从水面亮蓝到深海墨蓝 ============ */
.water {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(130% 70% at 50% -8%, rgba(205, 247, 255, 0.55), transparent 58%),
    linear-gradient(180deg, #74d6ff 0%, #34b0ef 16%, #108fcb 38%, #0a6ea6 60%, #04476f 82%, #022b46 100%);
}

/* ============ 海面 ============ */
.surface {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 84px;
  filter: blur(0.4px);
}

/* ============ 阳光光柱 ============ */
.rays {
  position: absolute;
  inset: 0;
}
.ray {
  position: absolute;
  top: -14vh;
  height: 165vh;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.46), rgba(255, 255, 255, 0.1) 58%, rgba(255, 255, 255, 0.04) 82%, transparent 96%);
  filter: blur(11px);
  mix-blend-mode: screen;
  transform-origin: top center;
  animation: raySway ease-in-out infinite;
}
@keyframes raySway {
  0%, 100% { transform: rotate(var(--rot)) translateX(0) scaleY(1); opacity: 0.5; }
  50%      { transform: rotate(var(--rot)) translateX(3vw) scaleY(1.06); opacity: 0.85; }
}

/* 中层雾光 */
.haze {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(60% 34% at 50% 34%, rgba(170, 240, 255, 0.16), transparent 70%),
    radial-gradient(50% 30% at 18% 62%, rgba(126, 226, 255, 0.12), transparent 72%);
  pointer-events: none;
}

/* ============ 焦散 ============ */
.caustics {
  position: absolute;
  inset: 0;
  opacity: 0.5;
  mix-blend-mode: soft-light;
  background-image:
    radial-gradient(circle at 18% 26%, rgba(255, 255, 255, 0.5), transparent 22%),
    radial-gradient(circle at 68% 14%, rgba(255, 255, 255, 0.42), transparent 18%),
    radial-gradient(circle at 42% 52%, rgba(255, 255, 255, 0.32), transparent 26%),
    radial-gradient(circle at 86% 44%, rgba(255, 255, 255, 0.36), transparent 20%),
    radial-gradient(circle at 8% 70%, rgba(255, 255, 255, 0.26), transparent 24%);
  animation: causticDrift 22s ease-in-out infinite alternate;
}
@keyframes causticDrift {
  0%   { transform: translate3d(0, 0, 0) scale(1); }
  100% { transform: translate3d(-3%, 2%, 0) scale(1.08); }
}

/* ============ 浮游微粒 ============ */
.mote {
  position: absolute;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 0 6px rgba(255, 255, 255, 0.7);
  animation: twinkle ease-in-out infinite;
}
@keyframes twinkle {
  0%, 100% { opacity: 0.12; transform: translateY(0); }
  50%      { opacity: 0.9; transform: translateY(-10px); }
}

/* ============ 水母 ============ */
.jelly {
  position: absolute;
  animation: jellyFloat ease-in-out infinite;
  filter: drop-shadow(0 6px 14px rgba(4, 60, 96, 0.35));
}
@keyframes jellyFloat {
  0%, 100% { transform: translateY(0) rotate(-5deg); }
  50%      { transform: translateY(-34px) rotate(5deg); }
}

/* ============ 鱼 ============ */
.fish {
  position: absolute;
  left: -12vw;
  animation: swim linear infinite;
  opacity: 0.8;
}
.fish-body {
  display: inline-block;
  filter: drop-shadow(0 3px 6px rgba(2, 43, 70, 0.35));
  animation: bob 3.4s ease-in-out infinite;
}
@keyframes swim {
  0%   { transform: translateX(0); }
  100% { transform: translateX(124vw); }
}
@keyframes bob {
  0%, 100% { transform: translateY(0) rotate(-4deg); }
  50%      { transform: translateY(-6px) rotate(4deg); }
}

/* ============ 海床 ============ */
.seabed {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 300px;
}
.reef {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

/* 会摇摆的海草 */
.kelp {
  position: absolute;
  bottom: 34px;
  width: 13px;
  border-radius: 60% 60% 30% 30% / 90% 90% 12% 12%;
  background: linear-gradient(180deg, #2fbf8a, #0d6b52);
  transform-origin: bottom center;
  animation: sway 6.5s ease-in-out infinite;
  opacity: 0.85;
  box-shadow: inset -3px 0 0 rgba(0, 0, 0, 0.12);
}
.kelp-a { left: 16%; height: 150px; animation-delay: 0s; }
.kelp-b { left: 20%; height: 108px; animation-delay: 1.4s; opacity: 0.7; }
.kelp-c { right: 15%; height: 168px; animation-delay: 0.8s; }
.kelp-d { right: 21%; height: 120px; animation-delay: 2.1s; opacity: 0.68; }
@keyframes sway {
  0%, 100% { transform: rotate(-7deg) scaleY(1); }
  50%      { transform: rotate(8deg) scaleY(1.04); }
}

/* ============ 气泡 ============ */
.bubbles {
  position: absolute;
  inset: 0;
}
.bubble {
  position: absolute;
  bottom: -70px;
  border-radius: 50%;
  background: radial-gradient(circle at 32% 28%, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.1) 62%, rgba(255, 255, 255, 0.32));
  border: 1px solid rgba(255, 255, 255, 0.45);
  animation: rise linear infinite;
  opacity: 0.75;
}
@keyframes rise {
  0%   { transform: translateY(0) translateX(0) scale(0.9); }
  45%  { transform: translateY(-48vh) translateX(26px) scale(1.05); }
  100% { transform: translateY(-108vh) translateX(-18px) scale(0.85); opacity: 0; }
}

/* ============ 底部压暗 ============ */
.depth-shade {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(120% 60% at 50% 120%, rgba(1, 26, 44, 0.62), transparent 62%),
    linear-gradient(180deg, transparent 60%, rgba(2, 34, 56, 0.3) 100%);
}

@media (prefers-reduced-motion: reduce) {
  .ray, .caustics, .mote, .jelly, .fish, .fish-body, .kelp, .bubble {
    animation: none !important;
  }
}
</style>
