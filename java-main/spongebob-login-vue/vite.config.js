import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  // 生产构建直接输出到 Spring Boot 的静态资源目录，
  // 这样后端一个端口（8081）就能同时提供页面和 /api 接口，无需再起 nginx。
  build: {
    outDir: '../springBootTest/src/main/resources/static',
    emptyOutDir: true
  },
  server: {
    port: 5173,
    open: true,
    // 开发态把 /api 代理到 Spring Boot 后端（http://localhost:8081），避免跨域问题
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
