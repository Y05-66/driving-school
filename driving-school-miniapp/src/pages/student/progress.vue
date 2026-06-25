<!--
  学习进度页面（学员端）
  功能：展示学员的四科学习进度
  - 每科显示：科目名、完成学时/要求学时、进度条、状态
  - 状态：未开始（灰色）、进行中（紫色）、已完成（绿色）
-->
<template>
  <view class="container">
    <view class="card">
      <text class="card-title">📚 我的学习进度</text>

      <!-- 空状态 -->
      <view v-if="progressList.length === 0" class="empty">
        <text class="empty-icon">📚</text>
        <text class="empty-text">暂无进度数据</text>
      </view>

      <!-- 进度列表 -->
      <view v-for="p in progressList" :key="p.id" class="progress-item">
        <!-- 科目名 + 状态标签 -->
        <view class="progress-header">
          <view class="progress-left">
            <text class="progress-name">{{ p.subjectName }}</text>
            <text class="progress-status" :class="'status-' + p.status">
              {{ statusTextMap[p.status] || '未知' }}
            </text>
          </view>
          <!-- 学时显示 -->
          <text class="progress-hours">{{ p.completedHours || 0 }}/{{ p.requiredHours || 0 }} 小时</text>
        </view>

        <!-- 进度条 -->
        <view class="progress-bar">
          <view class="progress-fill" :style="{ width: percentage(p) + '%' }"
            :class="{ 'fill-success': p.status === 2, 'fill-primary': p.status === 1 }"></view>
        </view>

        <!-- 百分比 -->
        <text class="progress-percent">{{ percentage(p) }}%</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '../../utils/request'

// 进度列表
const progressList = ref([])

// 状态文本映射
const statusTextMap = { 0: '未开始', 1: '进行中', 2: '已完成' }

/**
 * 计算进度百分比
 * @param {Object} p - 进度记录
 * @returns {number} 百分比（0-100）
 */
const percentage = (p) => Math.min(100, Math.round((p.completedHours || 0) / (p.requiredHours || 1) * 100))

/**
 * 页面加载
 * 获取学员的四科学习进度
 */
onMounted(async () => {
  try {
    const res = await get('/students/me/progress')
    progressList.value = res.data || []
  } catch (e) {
    console.error('获取进度失败', e)
  }
})
</script>

<style scoped>
/* 进度项 */
.progress-item {
  padding: 24rpx 0;
  border-bottom: 2rpx solid #f1f5f9;
}
.progress-item:last-child { border-bottom: none; }

/* 头部：科目名 + 学时 */
.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.progress-left { display: flex; align-items: center; gap: 12rpx; }
.progress-name { font-size: 30rpx; font-weight: 600; color: #1e293b; }
.progress-hours { font-size: 24rpx; color: #94a3b8; }

/* 状态标签 */
.progress-status {
  font-size: 20rpx;
  padding: 4rpx 12rpx;
  border-radius: 16rpx;
}
.status-0 { background: #f1f5f9; color: #94a3b8; }  /* 未开始：灰色 */
.status-1 { background: #eef2ff; color: #6366f1; }  /* 进行中：紫色 */
.status-2 { background: #ecfdf5; color: #10b981; }  /* 已完成：绿色 */

/* 进度条 */
.progress-bar {
  height: 16rpx;
  background: #f1f5f9;
  border-radius: 8rpx;
  overflow: hidden;
  margin-bottom: 8rpx;
}
.progress-fill { height: 100%; border-radius: 8rpx; }
/* 进行中：紫色渐变 */
.fill-primary { background: linear-gradient(90deg, #6366f1, #818cf8); }
/* 已完成：绿色渐变 */
.fill-success { background: linear-gradient(90deg, #10b981, #34d399); }

/* 百分比文字 */
.progress-percent { font-size: 22rpx; color: #64748b; }

/* 空状态 */
.empty { text-align: center; padding: 60rpx 0; }
.empty-icon { display: block; font-size: 80rpx; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: #94a3b8; }
</style>
