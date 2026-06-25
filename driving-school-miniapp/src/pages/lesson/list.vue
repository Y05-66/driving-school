<!--
  我的课时列表页（优化版）
  亮点：
  - 状态标签带圆点指示器
  - 时间信息更清晰
  - 操作按钮带图标
  - 空状态更友好
-->
<template>
  <view class="container">
    <!-- 空状态 -->
    <view v-if="lessonList.length === 0" class="empty-card">
      <view class="empty-illustration">
        <text class="empty-emoji">📅</text>
      </view>
      <text class="empty-title">暂无课时记录</text>
      <text class="empty-desc">快去预约你的第一节课吧</text>
      <button class="empty-btn" @click="goBook">去预约</button>
    </view>

    <!-- 课时列表 -->
    <view v-for="(item, idx) in lessonList" :key="item.id" class="lesson-card" :style="{ animationDelay: idx * 60 + 'ms' }">
      <!-- 顶部：日期 + 状态 -->
      <view class="lesson-top">
        <view class="lesson-date-row">
          <text class="lesson-date">{{ item.lessonDate }}</text>
          <text class="lesson-weekday">{{ getWeekday(item.lessonDate) }}</text>
        </view>
        <view class="status-badge" :class="'status-' + item.status">
          <view class="status-dot"></view>
          <text class="status-text">{{ statusTextMap[item.status] || '未知' }}</text>
        </view>
      </view>

      <!-- 时间信息 -->
      <view class="lesson-time-section">
        <view class="time-block">
          <text class="time-label">开始</text>
          <text class="time-value">{{ item.startTime }}</text>
        </view>
        <view class="time-arrow">→</view>
        <view class="time-block">
          <text class="time-label">结束</text>
          <text class="time-value">{{ item.endTime }}</text>
        </view>
        <view v-if="item.actualHours" class="time-block hours-block">
          <text class="time-label">实际</text>
          <text class="time-value highlight">{{ item.actualHours }}h</text>
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="lesson-actions" v-if="item.status <= 1">
        <button v-if="item.status === 0" class="action-btn btn-confirm" @click="handleConfirm(item)">
          <text class="btn-icon">✓</text> 确认
        </button>
        <button v-if="item.status === 1" class="action-btn btn-complete" @click="handleComplete(item)">
          <text class="btn-icon">✓</text> 完成
        </button>
        <button class="action-btn btn-cancel" @click="handleCancel(item)">
          <text class="btn-icon">✕</text> 取消
        </button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { get, put } from '../../utils/request'

// ========== 响应式状态 ==========

/** @description 课时记录列表 */
const lessonList = ref([])

/** @description 课时状态码与中文文本的映射表 */
const statusTextMap = { 0: '待确认', 1: '已确认', 2: '进行中', 3: '已完成', 4: '已取消' }

// ========== 工具方法 ==========

/**
 * @description 根据日期字符串获取星期几
 * @param {string} dateStr - 日期字符串（yyyy-MM-dd 格式）
 * @returns {string} 星期文本，如"周一"、"周日"；空字符串表示无效日期
 */
const getWeekday = (dateStr) => {
  if (!dateStr) return ''
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return days[new Date(dateStr).getDay()]
}

/**
 * @description 跳转到预约练车页面
 * @returns {void}
 */
const goBook = () => uni.navigateTo({ url: '/pages/lesson/book' })

// ========== 数据加载 ==========

/**
 * @description 从后端获取当前学员的课时列表
 * @returns {Promise<void>}
 */
const fetchLessons = async () => {
  try {
    const res = await get('/lessons', { pageNum: 1, pageSize: 50 })
    lessonList.value = res.data?.records || []
  } catch (e) { console.error('获取课时失败', e) }
}

// ========== 课时操作 ==========

/**
 * @description 确认课时（将状态从待确认改为已确认）
 * @param {Object} item - 课时记录对象
 * @param {number|string} item.id - 课时 ID
 * @returns {Promise<void>}
 */
const handleConfirm = async (item) => {
  try { await put(`/lessons/${item.id}/confirm`); uni.showToast({ title: '已确认', icon: 'success' }); fetchLessons() } catch {}
}

/**
 * @description 完成课时，微信平台支持输入实际学时，其他平台直接完成后端自动计算
 * @param {Object} item - 课时记录对象
 * @param {number|string} item.id - 课时 ID
 * @returns {Promise<void>}
 */
const handleComplete = async (item) => {
  // #ifdef MP-WEIXIN
  // 微信小程序支持 editable 输入实际学时
  uni.showModal({
    title: '完成课时', content: '请输入实际学时', editable: true, placeholderText: '如: 1.5',
    success: async (res) => {
      if (res.confirm && res.content) {
        try { await put(`/lessons/${item.id}/complete`, { actualHours: parseFloat(res.content) }); uni.showToast({ title: '已完成', icon: 'success' }); fetchLessons() } catch {}
      } else if (res.confirm && !res.content) {
        uni.showToast({ title: '请输入实际学时', icon: 'none' })
      }
    }
  })
  // #endif
  // #ifndef MP-WEIXIN
  // 非微信平台不支持 editable，直接完成课时（后端根据起止时间计算学时）
  uni.showModal({
    title: '完成课时', content: '确定完成该课时？',
    success: async (res) => {
      if (res.confirm) {
        try { await put(`/lessons/${item.id}/complete`); uni.showToast({ title: '已完成', icon: 'success' }); fetchLessons() } catch {}
      }
    }
  })
  // #endif
}

/**
 * @description 取消课时预约，需用户二次确认
 * @param {Object} item - 课时记录对象
 * @param {number|string} item.id - 课时 ID
 * @returns {Promise<void>}
 */
const handleCancel = async (item) => {
  uni.showModal({
    title: '取消预约', content: '确定取消该预约？',
    success: async (res) => {
      if (res.confirm) {
        try { await put(`/lessons/${item.id}/cancel`); uni.showToast({ title: '已取消', icon: 'success' }); fetchLessons() } catch {}
      }
    }
  })
}

// ========== 生命周期 ==========

/**
 * @description 每次显示页面时刷新数据（navigateBack回来时也会触发，首次也会触发）
 * @returns {Promise<void>}
 */
onShow(fetchLessons)
</script>

<style scoped>
/* ====== 课时卡片 ====== */
.lesson-card {
  background: #fff; border-radius: 20rpx; padding: 28rpx; margin-bottom: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
  animation: slideUp 0.4s ease forwards; opacity: 0;
}
@keyframes slideUp { from { opacity: 0; transform: translateY(30rpx); } to { opacity: 1; transform: translateY(0); } }

/* 顶部：日期 + 状态 */
.lesson-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; }
.lesson-date-row { display: flex; align-items: baseline; gap: 12rpx; }
.lesson-date { font-size: 30rpx; font-weight: 700; color: #1a1a2e; }
.lesson-weekday { font-size: 24rpx; color: #94a3b8; }

/* 状态徽章 */
.status-badge {
  display: flex; align-items: center; gap: 8rpx;
  padding: 8rpx 16rpx; border-radius: 20rpx;
}
.status-dot { width: 12rpx; height: 12rpx; border-radius: 50%; }
.status-0 .status-dot { background: #f59e0b; } .status-0 { background: #fffbeb; }
.status-1 .status-dot { background: #6366f1; } .status-1 { background: #eef2ff; }
.status-2 .status-dot { background: #3b82f6; } .status-2 { background: #eff6ff; }
.status-3 .status-dot { background: #10b981; } .status-3 { background: #ecfdf5; }
.status-4 .status-dot { background: #94a3b8; } .status-4 { background: #f1f5f9; }
.status-text { font-size: 22rpx; font-weight: 600; }
.status-0 .status-text { color: #f59e0b; }
.status-1 .status-text { color: #6366f1; }
.status-2 .status-text { color: #3b82f6; }
.status-3 .status-text { color: #10b981; }
.status-4 .status-text { color: #94a3b8; }

/* 时间信息 */
.lesson-time-section {
  display: flex; align-items: center; gap: 16rpx;
  background: #f8f9fc; border-radius: 14rpx; padding: 20rpx; margin-bottom: 20rpx;
}
.time-block { text-align: center; }
.time-label { display: block; font-size: 20rpx; color: #94a3b8; margin-bottom: 4rpx; }
.time-value { display: block; font-size: 30rpx; font-weight: 700; color: #1a1a2e; }
.time-arrow { font-size: 28rpx; color: #cbd5e1; margin: 0 8rpx; }
.hours-block { margin-left: auto; padding-left: 20rpx; border-left: 2rpx solid #e8eaf0; }
.highlight { color: #6366f1; }

/* 操作按钮 */
.lesson-actions { display: flex; gap: 16rpx; }
.action-btn {
  flex: 1; height: 72rpx; line-height: 72rpx; font-size: 26rpx; font-weight: 600;
  border-radius: 14rpx; border: none; display: flex; align-items: center; justify-content: center; gap: 8rpx;
}
.action-btn::after { border: none; }
.btn-icon { font-size: 24rpx; }
.btn-confirm { background: #eef2ff; color: #6366f1; }
.btn-complete { background: #ecfdf5; color: #10b981; }
.btn-cancel { background: #fef2f2; color: #ef4444; }

/* 空状态 */
.empty-card {
  background: #fff; border-radius: 24rpx; padding: 80rpx 40rpx; text-align: center;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.empty-illustration { margin-bottom: 24rpx; }
.empty-emoji { font-size: 120rpx; }
.empty-title { display: block; font-size: 32rpx; font-weight: 700; color: #1a1a2e; margin-bottom: 12rpx; }
.empty-desc { display: block; font-size: 26rpx; color: #94a3b8; margin-bottom: 32rpx; }
.empty-btn {
  background: linear-gradient(135deg, #6366f1, #818cf8); color: #fff; border: none;
  border-radius: 14rpx; font-size: 28rpx; font-weight: 600; height: 80rpx; line-height: 80rpx;
  padding: 0 48rpx; display: inline-block;
}
.empty-btn::after { border: none; }
</style>
