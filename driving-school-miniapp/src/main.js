/**
 * 应用入口文件
 * 创建 Vue SSR 应用实例并注册 Pinia 状态管理
 */
import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

/**
 * 创建应用实例（uni-app 要求导出此函数）
 * @returns {{ app: import('vue').App }} Vue 应用实例
 */
export function createApp() {
  const app = createSSRApp(App)
  app.use(createPinia())
  return { app }
}
