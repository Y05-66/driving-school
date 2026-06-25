/**
 * 通知状态管理模块
 * 功能：管理未读通知数量，提供刷新方法
 * 供 TabBar 角标和通知页面使用
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { get } from '../utils/request'

// 消息 Tab 在 tabBar.list 中的索引，需与 pages.json 中的顺序保持一致
const MESSAGE_TAB_INDEX = 3

export const useNotificationStore = defineStore('notification', () => {
  /** 未读通知数量 */
  const unreadCount = ref(0)

  /**
   * 获取未读通知数量
   * @description 调用后端接口刷新未读数，同步更新 TabBar 角标
   */
  async function fetchUnread() {
    try {
      const res = await get('/notifications/unread-count')
      unreadCount.value = Number(res.data?.count) || 0
      // 同步设置 TabBar 角标
      if (unreadCount.value > 0) {
        uni.setTabBarBadge({ index: MESSAGE_TAB_INDEX, text: String(unreadCount.value > 99 ? '99+' : unreadCount.value) })
      } else {
        uni.removeTabBarBadge({ index: MESSAGE_TAB_INDEX })
      }
    } catch (e) {
      console.error('获取未读数失败', e)
    }
  }

  return { unreadCount, fetchUnread }
})
