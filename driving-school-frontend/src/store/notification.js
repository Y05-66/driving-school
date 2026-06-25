/**
 * @description 通知状态管理模块 (Pinia Store)
 * 管理当前用户的未读通知数量
 * 侧边栏和顶栏通过此 Store 显示未读通知角标
 * 定时从后端拉取最新未读数，保持角标实时更新
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUnreadCount } from '../api'

/**
 * @description 定义通知状态 Store
 * Store ID 为 'notification'，通过 useNotificationStore() 调用
 */
export const useNotificationStore = defineStore('notification', () => {
  /**
   * @description 未读通知数量
   * 用于侧边栏菜单和顶栏通知图标的角标显示
   * 默认为 0，通过 fetchUnread() 方法从后端获取最新值
   */
  const unreadCount = ref(0)

  /**
   * @description 获取未读通知数量
   * 从后端 /notifications/unread-count 接口拉取当前用户未读通知数
   * 使用 try-catch 静默处理请求失败，避免影响页面正常渲染
   * 在 Layout 组件中以 60 秒间隔定时调用
   */
  async function fetchUnread() {
    try {
      const res = await getUnreadCount()
      // 确保返回值为数字类型，避免 null/undefined 导致显示异常
      unreadCount.value = Number(res.data?.count) || 0
    } catch {}
  }

  return { unreadCount, fetchUnread }
})
