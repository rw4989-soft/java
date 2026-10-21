<template>
  <div class="spongebob">
    <div class="eye left"></div>
    <div class="eye right"></div>
    <div class="nose"></div>
    <div class="mouth"></div>
    <div class="teeth"><span></span><span></span></div>
    <div class="pants"></div>
  </div>
</template>

<script setup>
// 纯 CSS 画的海绵宝宝
</script>

<style scoped>
.spongebob {
  position: absolute; top: -70px; left: 50%;
  transform: translateX(-50%);
  width: 130px; height: 150px;
  background: #ffeb3b;
  border: 4px solid #d4b800;
  border-radius: 16px;
  box-shadow: inset -6px -8px 0 rgba(0,0,0,.06);
  animation: bob 3.5s ease-in-out infinite;
  z-index: 6;
}
@keyframes bob {
  0%,100% { transform: translateX(-50%) rotate(-3deg); }
  50%     { transform: translateX(-50%) rotate(3deg) translateY(-6px); }
}
/* 海绵孔洞 */
.spongebob::before {
  content: ""; position: absolute; inset: 0;
  background:
    radial-gradient(circle 7px at 18% 22%, #e0c300 98%, transparent),
    radial-gradient(circle 5px at 82% 18%, #e0c300 98%, transparent),
    radial-gradient(circle 6px at 12% 62%, #e0c300 98%, transparent),
    radial-gradient(circle 5px at 88% 66%, #e0c300 98%, transparent),
    radial-gradient(circle 4px at 50% 8%,  #e0c300 98%, transparent);
}
.eye {
  position: absolute; top: 26px;
  width: 38px; height: 42px;
  background: #fff; border: 3px solid #333;
  border-radius: 50%;
}
.eye.left  { left: 20px; }
.eye.right { right: 20px; }
.eye::after {
  content: ""; position: absolute; top: 11px; left: 11px;
  width: 13px; height: 13px;
  background: radial-gradient(circle at 35% 35%, #4fc3f7 40%, #222 42%);
  border-radius: 50%;
}
.nose {
  position: absolute; top: 62px; left: 50%;
  transform: translateX(-50%);
  width: 16px; height: 20px;
  background: #ffeb3b; border: 3px solid #d4b800;
  border-radius: 50% 50% 60% 60%;
  z-index: 2;
}
.mouth {
  position: absolute; top: 84px; left: 50%;
  transform: translateX(-50%);
  width: 62px; height: 32px;
  border-bottom: 4px solid #333;
  border-radius: 0 0 60px 60px;
}
.teeth {
  position: absolute; top: 104px; left: 50%;
  transform: translateX(-50%);
  display: flex; gap: 4px;
}
.teeth span {
  width: 14px; height: 14px;
  background: #fff; border: 3px solid #333; border-top: none;
  border-radius: 0 0 3px 3px;
}
.pants {
  position: absolute; bottom: 0; left: -4px; right: -4px; height: 34px;
  background: linear-gradient(180deg, #fff 0 8px, #8d5524 8px);
  border: 4px solid #6d3f16; border-top: none;
  border-radius: 0 0 12px 12px;
}
</style>
