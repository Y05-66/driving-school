<!--
  考试安排列表页
  功能：展示可报名的考试安排，学员可在线报名
  - 显示考试类型（科目一~四）、日期、地点、名额
  - 支持一键报名
-->
<template>
  <view class="container">
    <!-- 空状态 -->
    <view v-if="examList.length === 0" class="empty">
      <text class="empty-icon">📝</text>
      <text class="empty-text">暂无考试安排</text>
    </view>

    <!-- 考试卡片列表 -->
    <view v-for="item in examList" :key="item.id" class="card exam-card">
      <!-- 头部：考试类型标签 + 日期 -->
      <view class="exam-header">
        <view class="exam-type-tag" :class="'type-' + item.examType">
          {{ examTypeText(item.examType) }}
        </view>
        <text class="exam-date">{{ item.examDate }}</text>
      </view>

      <!-- 考试信息 -->
      <view class="exam-info">
        <text class="exam-location">📍 {{ item.examLocation || '待定' }}</text>
        <text class="exam-quota">👥 名额 {{ item.maxCandidates || '-' }}</text>
      </view>

      <!-- 报名按钮 -->
      <button class="btn-enroll" @click="handleEnroll(item)">报名</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { get, post } from '../../utils/request'

// 考试列表
const examList = ref([])

// 考试类型映射表
const examTypeMap = {
  SUBJECT_1: '科目一',
  SUBJECT_2: '科目二',
  SUBJECT_3: '科目三',
  SUBJECT_4: '科目四'
}

/**
 * 获取考试类型中文名
 * @param {string} type - 考试类型代码
 * @returns {string} 中文名
 */
const examTypeText = (type) => examTypeMap[type] || type

/**
 * 获取考试列表
 */
const fetchExams = async () => {
  try {
    const res = await get('/exams', { pageNum: 1, pageSize: 50 })
    examList.value = res.data?.records || []
  } catch (e) {
    console.error('获取考试列表失败', e)
  }
}

/**
 * 处理考试报名
 * @param {Object} item - 考试记录
 * @description 弹出确认框 → 调用报名接口 → 刷新列表
 */
const handleEnroll = async (item) => {
  uni.showModal({
    title: '考试报名',
    content: '确定报名' + examTypeText(item.examType) + '？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await post(`/exams/${item.id}/enroll`)
          uni.showToast({ title: '报名成功', icon: 'success' })
          fetchExams() // 刷新列表
        } catch {}
      }
    }
  })
}

// 页面显示时获取数据（从其他页面返回时也会刷新）
onShow(fetchExams)
</script>

<style scoped>
.exam-card { margin-bottom: 16rpx; }

/* 头部布局 */
.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

/* 考试类型标签（不同科目不同颜色） */
.exam-type-tag {
  font-size: 24rpx;
  padding: 6rpx 20rpx;
  border-radius: 20rpx;
  font-weight: 600;
}
.type-SUBJECT_1 { background: #eef2ff; color: #6366f1; }  /* 科目一：紫色 */
.type-SUBJECT_2 { background: #ecfdf5; color: #10b981; }  /* 科目二：绿色 */
.type-SUBJECT_3 { background: #fffbeb; color: #f59e0b; }  /* 科目三：黄色 */
.type-SUBJECT_4 { background: #fef2f2; color: #ef4444; }  /* 科目四：红色 */

.exam-date { font-size: 26rpx; color: #64748b; }
.exam-info { margin-bottom: 20rpx; }
.exam-location { display: block; font-size: 26rpx; color: #64748b; }
.exam-quota { display: block; font-size: 26rpx; color: #64748b; margin-top: 8rpx; }

/* 报名按钮 */
.btn-enroll {
  background: #eef2ff;
  color: #6366f1;
  border: none;
  border-radius: 12rpx;
  font-size: 26rpx;
  height: 64rpx;
  line-height: 64rpx;
}
.btn-enroll::after { border: none; }

/* 空状态 */
.empty { text-align: center; padding: 120rpx 0; }
.empty-icon { display: block; font-size: 80rpx; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: #94a3b8; }
</style>
