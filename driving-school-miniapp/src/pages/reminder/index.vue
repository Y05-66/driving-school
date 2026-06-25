<!--
  我的提醒页面
  功能：查看提醒列表、取消提醒
-->
<template>
  <view class="container">
    <view v-if="reminderList.length === 0" class="empty">
      <text class="empty-icon">🔔</text>
      <text class="empty-text">暂无提醒</text>
    </view>
    <view v-for="r in reminderList" :key="r.id" class="reminder-card">
      <view class="reminder-top">
        <view class="reminder-type-tag" :class="'type-' + r.type">
          <text class="type-text">{{ typeText(r.type) }}</text>
        </view>
        <view class="reminder-status" :class="'status-' + r.status">
          <text class="status-text">{{ statusText(r.status) }}</text>
        </view>
      </view>
      <text class="reminder-content">{{ r.content }}</text>
      <view class="reminder-bottom">
        <text class="reminder-time">⏰ {{ r.remindTime }}</text>
        <text v-if="r.status === 0" class="cancel-btn" @click="cancel(r.id)">取消</text>
      </view>
    </view>
  </view>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { get, put } from '../../utils/request'

// ========== 状态变量 ==========

/** 提醒列表 */
const reminderList = ref([])

// ========== 工具函数 ==========

/**
 * 获取提醒类型文本
 * @param {number} t - 提醒类型：1-课时提醒 2-考试提醒 3-缴费提醒 4-其他
 * @returns {string} 类型对应的中文文本
 */
const typeText = (t) => ({ 1: '课时提醒', 2: '考试提醒', 3: '缴费提醒', 4: '其他' }[t] || '提醒')

/**
 * 获取提醒状态文本
 * @param {number} s - 状态：0-待发送 1-已发送 2-已取消
 * @returns {string} 状态对应的中文文本
 */
const statusText = (s) => ({ 0: '待发送', 1: '已发送', 2: '已取消' }[s] || '未知')

// ========== 数据操作 ==========

/**
 * 加载提醒列表
 * @description 从后端获取当前学员的所有提醒，兼容分页和非分页返回格式
 */
const load = async () => {
  try { const res = await get('/reminders/my'); reminderList.value = Array.isArray(res.data) ? res.data : (res.data?.records || []) } catch {}
}

/**
 * 取消提醒
 * @param {number} id - 提醒ID
 * @description 弹出确认弹窗，确认后调用后端接口取消提醒并刷新列表
 */
const cancel = async (id) => {
  uni.showModal({
    title: '取消提醒', content: '确定取消该提醒？',
    success: async (res) => {
      if (res.confirm) {
        try { await put('/reminders/' + id + '/cancel'); uni.showToast({ title: '已取消', icon: 'success' }); load() } catch {}
      }
    }
  })
}

// ========== 生命周期 ==========

/** 页面挂载：加载提醒列表 */
onMounted(() => { load() })
</script>
<style scoped>
.container { padding: 24rpx; min-height: 100vh; background: #f5f7fa; }
.empty { text-align: center; padding: 120rpx 0; }
.empty-icon { font-size: 80rpx; display: block; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: #94a3b8; }
.reminder-card { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 16rpx; box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.04); }
.reminder-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.reminder-type-tag { padding: 4rpx 14rpx; border-radius: 8rpx; }
.type-1 { background: #eef2ff; } .type-1 .type-text { color: #6366f1; }
.type-2 { background: #fffbeb; } .type-2 .type-text { color: #f59e0b; }
.type-3 { background: #ecfdf5; } .type-3 .type-text { color: #10b981; }
.type-4 { background: #f1f5f9; } .type-4 .type-text { color: #64748b; }
.type-text { font-size: 22rpx; font-weight: 600; }
.reminder-status { padding: 4rpx 12rpx; border-radius: 8rpx; }
.status-0 { background: #eef2ff; } .status-0 .status-text { color: #6366f1; }
.status-1 { background: #ecfdf5; } .status-1 .status-text { color: #10b981; }
.status-2 { background: #f1f5f9; } .status-2 .status-text { color: #94a3b8; }
.status-text { font-size: 22rpx; font-weight: 600; }
.reminder-content { font-size: 28rpx; color: #1e293b; line-height: 1.5; display: block; margin-bottom: 12rpx; }
.reminder-bottom { display: flex; justify-content: space-between; align-items: center; }
.reminder-time { font-size: 24rpx; color: #94a3b8; }
.cancel-btn { font-size: 24rpx; color: #ef4444; font-weight: 600; }
</style>
