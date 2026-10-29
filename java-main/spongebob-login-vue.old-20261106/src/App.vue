<template>
  <div class="page">
    <OceanDecor />
    <AuthCard />
  </div>
</template>

<script setup>
import OceanDecor from './components/OceanDecor.vue'
import AuthCard from './components/AuthCard.vue'
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
}

.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}
</style>
