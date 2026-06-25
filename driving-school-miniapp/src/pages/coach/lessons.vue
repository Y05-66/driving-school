<!--
  我的课程页面（教练端）
  功能：教练查看自己的课程安排
  - 显示课程日期、时间段、学员ID、状态
  - 待确认：可确认或确认并完成
  - 已确认：可完成课时
-->
<template>
  <view class="container">
    <!-- 空状态 -->
    <view v-if="lessonList.length === 0" class="empty">
      <text class="empty-icon">📅</text>
      <text class="empty-text">暂无课程</text>
    </view>

    <!-- 课程卡片列表 -->
    <view v-for="item in lessonList" :key="item.id" class="card lesson-card">
      <!-- 头部：日期 + 状态标签 -->
      <view class="lesson-header">
        <text class="lesson-date">{{ item.lessonDate }}</text>
        <text class="lesson-status" :class="'status-' + item.status">
          {{ statusTextMap[item.status] || '未知' }}
        </text>
      </view>

      <!-- 信息：时间段 + 学员ID -->
      <view class="lesson-info">
        <text class="lesson-time">🕐 {{ item.startTime }} - {{ item.endTime }}</text>
        <text class="lesson-student">👤 学员ID: {{ item.studentId }}</text>
      </view>

      <!-- 待确认状态：确认 + 确认并完成 -->
      <view class="lesson-actions" v-if="item.status === 0">
        <button class="btn-action btn-confirm" @click="handleConfirm(item)">确认</button>
        <button class="btn-action btn-complete" @click="handleConfirmComplete(item)">确认并完成</button>
      </view>

      <!-- 已确认状态：完成课时 -->
      <view class="lesson-actions" v-else-if="item.status === 1">
        <button class="btn-action btn-complete" @click="handleComplete(item)">完成课时</button>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { get, put } from '../../utils/request'

// 课程列表
const lessonList = ref([])

// 状态文本映射
const statusTextMap = { 0: '待确认', 1: '已确认', 2: '进行中', 3: '已完成', 4: '已取消' }

/**
 * 获取课程列表
 */
const fetchLessons = async () => {
  try {
    const res = await get('/lessons', { pageNum: 1, pageSize: 50 })
    lessonList.value = res.data?.records || []
  } catch (e) {
    console.error('获取课程列表失败', e)
  }
}

/**
 * 确认预约
 * @param {Object} item - 课程记录
 */
const handleConfirm = async (item) => {
  try {
    await put(`/lessons/${item.id}/confirm`)
    uni.showToast({ title: '已确认', icon: 'success' })
    fetchLessons()
  } catch {}
}

/**
 * 完成课时（弹窗输入实际学时）
 * @param {Object} item - 课程记录
 */
const handleComplete = async (item) => {
  // #ifdef MP-WEIXIN
  // 微信小程序支持 editable 输入实际学时
  uni.showModal({
    title: '完成课时',
    content: '请输入实际学时',
    editable: true,
    placeholderText: '如: 1.5',
    success: async (res) => {
      if (res.confirm && res.content) {
        try {
          await put(`/lessons/${item.id}/complete`, { actualHours: parseFloat(res.content) })
          uni.showToast({ title: '已完成', icon: 'success' })
          fetchLessons()
        } catch {}
      } else if (res.confirm && !res.content) {
        uni.showToast({ title: '请输入实际学时', icon: 'none' })
      }
    }
  })
  // #endif
  // #ifndef MP-WEIXIN
  // 非微信平台不支持 editable，直接完成课时（后端根据起止时间计算学时）
  uni.showModal({
    title: '完成课时',
    content: '确定完成该课时？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await put(`/lessons/${item.id}/complete`)
          uni.showToast({ title: '已完成', icon: 'success' })
          fetchLessons()
        } catch {}
      }
    }
  })
  // #endif
}

/**
 * 确认并完成（一步到位）
 * @param {Object} item - 课程记录
 */
const handleConfirmComplete = async (item) => {
  // #ifdef MP-WEIXIN
  // 微信小程序支持 editable 输入实际学时
  uni.showModal({
    title: '确认并完成',
    content: '请输入实际学时',
    editable: true,
    placeholderText: '如: 1.5',
    success: async (res) => {
      if (res.confirm && res.content) {
        try {
          await put(`/lessons/${item.id}/confirm-complete`, { actualHours: parseFloat(res.content) })
          uni.showToast({ title: '已完成', icon: 'success' })
          fetchLessons()
        } catch {}
      } else if (res.confirm && !res.content) {
        uni.showToast({ title: '请输入实际学时', icon: 'none' })
      }
    }
  })
  // #endif
  // #ifndef MP-WEIXIN
  // 非微信平台不支持 editable，直接确认并完成（后端根据起止时间计算学时）
  uni.showModal({
    title: '确认并完成',
    content: '确定确认并完成该课时？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await put(`/lessons/${item.id}/confirm-complete`)
          uni.showToast({ title: '已完成', icon: 'success' })
          fetchLessons()
        } catch {}
      }
    }
  })
  // #endif
}

// 每次显示页面时刷新数据（navigateBack回来时也会触发，首次也会触发）
onShow(fetchLessons)
</script>

<style scoped>
.lesson-card { margin-bottom: 16rpx; }

/* 头部布局 */
.lesson-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.lesson-date { font-size: 30rpx; font-weight: 600; color: #1e293b; }

/* 状态标签 */
.lesson-status { font-size: 22rpx; padding: 6rpx 16rpx; border-radius: 20rpx; }
.status-0 { background: #fffbeb; color: #f59e0b; }  /* 待确认：黄色 */
.status-1 { background: #eef2ff; color: #6366f1; }  /* 已确认：紫色 */
.status-2 { background: #eff6ff; color: #3b82f6; }  /* 进行中：蓝色 */
.status-3 { background: #ecfdf5; color: #10b981; }  /* 已完成：绿色 */
.status-4 { background: #f1f5f9; color: #94a3b8; }  /* 已取消：灰色 */

/* 信息区域 */
.lesson-info { margin-bottom: 16rpx; }
.lesson-time { display: block; font-size: 26rpx; color: #64748b; }
.lesson-student { display: block; font-size: 26rpx; color: #64748b; margin-top: 8rpx; }

/* 操作按钮 */
.lesson-actions { display: flex; gap: 16rpx; }
.btn-action { flex: 1; height: 64rpx; line-height: 64rpx; font-size: 26rpx; border-radius: 12rpx; border: none; }
.btn-confirm { background: #eef2ff; color: #6366f1; }
.btn-complete { background: #ecfdf5; color: #10b981; }
.btn-action::after { border: none; }

/* 空状态 */
.empty { text-align: center; padding: 120rpx 0; }
.empty-icon { display: block; font-size: 80rpx; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: #94a3b8; }
</style>
