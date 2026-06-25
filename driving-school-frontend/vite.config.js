import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        // 修复：使用函数形式的 manualChunks，将大型第三方库拆分为独立 chunk
        // 目的：避免单个 bundle 过大，提升首屏加载速度，同时利用浏览器缓存减少重复下载
        manualChunks(id) {
          if (id.includes('element-plus')) return 'element-plus'
          if (id.includes('echarts')) return 'echarts'
          if (id.includes('node_modules/vue') || id.includes('vue-router') || id.includes('pinia') || id.includes('axios')) return 'vendor'
        }
      }
    }
  }
})
