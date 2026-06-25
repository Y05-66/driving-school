<!--
  通知公告列表页（优化版）
  亮点：
  - 未读卡片有紫色渐变左边框
  - 类型标签带图标
  - 点击涟漪反馈
  - 空状态插画
-->
<template>
  <view class="container">
    <!-- 空状态 -->
    <view v-if="notifyList.length === 0" class="empty-card">
      <text class="empty-emoji">🔔</text>
      <text class="empty-title">暂无通知</text>
      <text class="empty-desc">有新消息会在这里通知你</text>
    </view>

    <!-- 通知列表 -->
    <view v-for="(item, idx) in notifyList" :key="item.id"
      class="notify-card" :class="{ unread: !item.isRead }"
      :style="{ animationDelay: idx * 50 + 'ms' }"
      @click="handleRead(item)">

      <!-- 类型图标 -->
      <view class="notify-icon-wrapper" :class="'icon-type-' + item.type">
        <text class="notify-icon">{{ typeIconMap[item.type] || '📌' }}</text>
      </view>

      <!-- 内容区 -->
      <view class="notify-body">
        <view class="notify-header">
          <text class="notify-title" :style="{ fontWeight: !item.isRead ? 700 : 500 }">{{ item.title }}</text>
          <view v-if="!item.isRead" class="unread-badge">
            <text class="badge-dot">●</text>
          </view>
        </view>
        <text class="notify-content" v-if="item.content">{{ item.content }}</text>
        <view class="notify-footer">
          <view class="type-tag" :class="'tag-' + item.type">
            <text class="tag-text">{{ typeTextMap[item.type] || '其他' }}</text>
          </view>
          <text class="notify-time">{{ item.createTime }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { get, put } from '../../utils/request'
import { useNotificationStore } from '../../store/notification'
import { useUserStore } from '../../store/user'

// ========== 状态管理 ==========

/** @description 通知状态仓库实例，用于同步未读数 */
const notifyStore = useNotificationStore()

/** @description 用户状态仓库实例，用于登录态校验 */
const userStore = useUserStore()

/** @description 通知公告列表 */
const notifyList = ref([])

/** @description 通知类型码与中文文本的映射表 */
const typeTextMap = { 1: '系统', 2: '考试', 3: '课程', 4: '紧急' }

/** @description 通知类型码与 emoji 图标的映射表 */
const typeIconMap = { 1: '⚙️', 2: '📝', 3: '📚', 4: '🚨' }

// ========== 数据加载 ==========

/**
 * @description 从后端获取通知公告列表
 * @returns {Promise<void>}
 */
const fetchNotifications = async () => {
  try {
    const res = await get('/notifications', { pageNum: 1, pageSize: 50 })
    notifyList.value = res.data?.records || []
  } catch (e) { console.error('获取通知失败', e) }
}

// ========== 通知操作 ==========

/**
 * @description 标记单条通知为已读，同时刷新全局未读数
 * @param {Object} item - 通知对象
 * @param {number|string} item.id - 通知 ID
 * @param {number} item.isRead - 已读状态（0 未读，1 已读）
 * @returns {Promise<void>}
 */
const handleRead = async (item) => {
  if (!item.isRead) {
    try {
      await put(`/notifications/${item.id}/read`)
      item.isRead = 1
      notifyStore.fetchUnread()
    } catch {}
  }
}

// ========== 生命周期 ==========

/**
 * @description 页面显示时校验登录态并加载通知列表，未登录则返回上一页
 * @returns {void}
 */
onShow(() => {
  if (!userStore.token) { uni.navigateBack(); return }
  fetchNotifications()
})
</script>

<style scoped>
/* ====== 通知卡片 ====== */
.notify-card {
  display: flex; gap: 20rpx; background: #fff; border-radius: 20rpx;
  padding: 24rpx; margin-bottom: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
  animation: slideUp 0.4s ease forwards; opacity: 0;
  transition: transform 0.2s;
}
.notify-card:active { transform: scale(0.98); }
.notify-card.unread { border-left: 6rpx solid #6366f1; }

@keyframes slideUp { from { opacity: 0; transform: translateY(20rpx); } to { opacity: 1; transform: translateY(0); } }

/* 类型图标 */
.notify-icon-wrapper {
  width: 72rpx; height: 72rpx; border-radius: 18rpx;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.icon-type-1 { background: #eef2ff; }
.icon-type-2 { background: #ecfdf5; }
.icon-type-3 { background: #fffbeb; }
.icon-type-4 { background: #fef2f2; }
.notify-icon { font-size: 32rpx; }

/* 内容区 */
.notify-body { flex: 1; min-width: 0; }
.notify-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8rpx; }
.notify-title { font-size: 28rpx; color: #1a1a2e; line-height: 1.4; flex: 1; }
.unread-badge { margin-left: 8rpx; }
.badge-dot { font-size: 20rpx; color: #ef4444; }

.notify-content {
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  font-size: 24rpx; color: #64748b; line-height: 1.5; margin-bottom: 12rpx;
}

.notify-footer { display: flex; justify-content: space-between; align-items: center; }
.type-tag { padding: 4rpx 12rpx; border-radius: 10rpx; }
.tag-1 { background: #eef2ff; } .tag-1 .tag-text { color: #6366f1; }
.tag-2 { background: #ecfdf5; } .tag-2 .tag-text { color: #10b981; }
.tag-3 { background: #fffbeb; } .tag-3 .tag-text { color: #f59e0b; }
.tag-4 { background: #fef2f2; } .tag-4 .tag-text { color: #ef4444; }
.tag-text { font-size: 20rpx; font-weight: 600; }
.notify-time { font-size: 22rpx; color: #94a3b8; }

/* 空状态 */
.empty-card {
  background: #fff; border-radius: 24rpx; padding: 100rpx 40rpx; text-align: center;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.empty-emoji { display: block; font-size: 120rpx; margin-bottom: 24rpx; }
.empty-title { display: block; font-size: 32rpx; font-weight: 700; color: #1a1a2e; margin-bottom: 12rpx; }
.empty-desc { display: block; font-size: 26rpx; color: #94a3b8; }
</style>
