<!--
  消息模块
  - 通知列表
  - 已读/未读状态
  - 全部已读
-->
<template>
  <view class="page">
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">消息</text>
        <view class="nav-right" @click="markAllRead" v-if="notifyStore.unreadCount > 0">
          <text class="read-all">全部已读</text>
        </view>
      </view>
    </view>

    <scroll-view scroll-y class="scroll-area" :style="{ paddingTop: (statusBarHeight + 44) + 'px' }" @scrolltolower="loadMore">
      <view v-if="notifyList.length === 0" class="empty">
        <text class="empty-icon">🔔</text>
        <text class="empty-text">暂无消息</text>
      </view>

      <view v-else class="msg-list">
        <view v-for="n in notifyList" :key="n.id" class="msg-card" :class="{ unread: !n.isRead }" @click="readNotify(n)">
          <view class="msg-dot" v-if="!n.isRead"></view>
          <view class="msg-icon-wrap" :class="'type-' + (n.type || 1)">
            <text class="msg-icon-emoji">{{ typeIcon(n.type) }}</text>
          </view>
          <view class="msg-content">
            <text class="msg-title">{{ n.title }}</text>
            <text class="msg-body">{{ n.content }}</text>
            <text class="msg-time">{{ n.createTime }}</text>
          </view>
        </view>
      </view>

      <view v-if="loading" class="loading-tip">
        <text class="loading-text">加载中...</text>
      </view>
      <view v-if="noMore && notifyList.length > 0" class="no-more">
        <text class="no-more-text">没有更多了</text>
      </view>

      <view style="height: 120rpx;"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../store/user'
import { useNotificationStore } from '../../store/notification'
import { get, put } from '../../utils/request'

const userStore = useUserStore()
const notifyStore = useNotificationStore()

// ========== 状态变量 ==========

/** 状态栏高度（用于自定义导航栏适配） */
const statusBarHeight = ref(0)

/** 通知列表 */
const notifyList = ref([])

/** 加载中状态 */
const loading = ref(false)

/** 是否已加载全部数据 */
const noMore = ref(false)

/** 当前分页页码 */
const pageNum = ref(1)

// ========== 工具函数 ==========

/**
 * 获取通知类型图标
 * @param {number} t - 通知类型：1-公告 2-日程 3-提醒 4-财务 5-定时
 * @returns {string} 对应的emoji图标
 */
const typeIcon = (t) => ({ 1: '📢', 2: '📅', 3: '📝', 4: '💰', 5: '⏰' }[t] || '📢')

// ========== 数据操作 ==========

/**
 * 加载通知列表
 * @param {boolean} [reset=false] - 是否重置列表（下拉刷新时为true，上拉加载更多时为false）
 * @description 支持分页加载，每页20条，加载完成后自动递增页码
 */
const loadNotify = async (reset = false) => {
  if (loading.value) return
  if (reset) { pageNum.value = 1; noMore.value = false; notifyList.value = [] }
  loading.value = true
  try {
    const res = await get('/notifications', { pageNum: pageNum.value, pageSize: 20 })
    const list = res.data?.records || []
    if (reset) notifyList.value = list
    else notifyList.value.push(...list)
    if (list.length < 20) noMore.value = true
    pageNum.value++
  } catch {} finally { loading.value = false }
}

/**
 * 上拉加载更多
 * @description 触底时触发，如果没有加载完则加载下一页
 */
const loadMore = () => { if (!noMore.value) loadNotify() }

/**
 * 标记单条通知为已读
 * @param {Object} n - 通知对象
 * @description 未读通知点击后调用接口标记已读，同步更新全局未读数
 */
const readNotify = async (n) => {
  if (!n.isRead) {
    try {
      await put('/notifications/' + n.id + '/read')
      n.isRead = 1
      // 同步更新全局未读数
      notifyStore.fetchUnread()
    } catch {}
  }
}

/**
 * 全部标记已读
 * @description 调用接口将所有通知标记为已读，同步更新列表状态和全局未读数
 */
const markAllRead = async () => {
  try {
    await put('/notifications/read-all')
    notifyList.value.forEach(n => n.isRead = 1)
    // 同步更新全局未读数
    notifyStore.fetchUnread()
    uni.showToast({ title: '已全部标记已读', icon: 'success' })
  } catch {}
}

// ========== 生命周期 ==========

/** 页面挂载：获取状态栏高度 */
onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
})

/**
 * 每次显示页面时刷新数据
 * @description 校验登录状态，未登录则跳转登录页；已登录则刷新通知列表和未读数
 */
onShow(async () => {
  // 未登录则跳转登录页
  if (!userStore.token) { uni.reLaunch({ url: '/pages/login/login' }); return }

  loadNotify(true)
  notifyStore.fetchUnread()
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f7fa; }
.nav-bar { position: fixed; top: 0; left: 0; right: 0; z-index: 100; background: linear-gradient(135deg, #4A7DFF, #6B9BFF); }
.nav-content { height: 44px; display: flex; align-items: center; justify-content: space-between; padding: 0 30rpx; }
.nav-title { font-size: 34rpx; font-weight: 700; color: #fff; }
.read-all { font-size: 24rpx; color: rgba(255,255,255,0.8); }
.scroll-area { height: 100vh; }

.empty { text-align: center; padding: 120rpx 0; }
.empty-icon { font-size: 80rpx; display: block; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: #999; }

.msg-list { padding: 20rpx 30rpx; }
.msg-card {
  background: #fff; border-radius: 20rpx; padding: 24rpx;
  display: flex; align-items: flex-start; gap: 20rpx;
  margin-bottom: 16rpx; position: relative;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}
.msg-card.unread { background: #F8FAFF; }
.msg-dot { position: absolute; top: 20rpx; left: 12rpx; width: 12rpx; height: 12rpx; border-radius: 50%; background: #FF4757; }
.msg-icon-wrap {
  width: 72rpx; height: 72rpx; border-radius: 20rpx;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.type-1 { background: #E8F0FE; }
.type-2 { background: #E8F5E9; }
.type-3 { background: #FFF3E0; }
.type-4 { background: #FCE4EC; }
.type-5 { background: #F3E5F5; }
.msg-icon-emoji { font-size: 32rpx; }
.msg-content { flex: 1; min-width: 0; }
.msg-title { display: block; font-size: 28rpx; font-weight: 600; color: #1a1a2e; }
.msg-body { display: block; font-size: 24rpx; color: #666; margin-top: 8rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.msg-time { display: block; font-size: 22rpx; color: #bbb; margin-top: 8rpx; }

.loading-tip, .no-more { text-align: center; padding: 20rpx 0; }
.loading-text, .no-more-text { font-size: 24rpx; color: #ccc; }
</style>
