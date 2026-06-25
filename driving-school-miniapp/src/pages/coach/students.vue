<!--
  我的学员页面（教练端）
  功能：教练查看自己名下的学员列表
  - 显示学员姓名、电话、报名类型
  - 彩色头像（首字母）
-->
<template>
  <view class="container">
    <!-- 空状态 -->
    <view v-if="studentList.length === 0" class="empty">
      <text class="empty-icon">👥</text>
      <text class="empty-text">暂无学员</text>
    </view>

    <!-- 学员卡片列表 -->
    <view v-for="item in studentList" :key="item.id" class="card student-card">
      <view class="student-header">
        <!-- 学员头像（首字母） -->
        <view class="student-avatar">
          <text class="avatar-text">{{ (item.name || '?').charAt(0) }}</text>
        </view>

        <!-- 学员信息 -->
        <view class="student-info">
          <text class="student-name">{{ item.name }}</text>
          <text class="student-phone">{{ item.phone || '-' }}</text>
        </view>

        <!-- 报名类型标签 -->
        <text class="student-type">{{ item.applyType || '-' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '../../utils/request'
import { useUserStore } from '../../store/user'

const userStore = useUserStore()

// 学员列表
const studentList = ref([])

/**
 * 页面加载
 * 调用教练端专属接口获取名下学员
 */
onMounted(async () => {
  if (!userStore.token) { uni.navigateBack(); return }
  if (userStore.userInfo.role !== 'COACH') { uni.navigateBack(); return }
  try {
    const res = await get('/coaches/me/students')
    studentList.value = res.data || []
  } catch (e) {
    console.error('获取学员列表失败', e)
  }
})
</script>

<style scoped>
.student-card { margin-bottom: 16rpx; }

/* 学员项布局 */
.student-header { display: flex; align-items: center; gap: 20rpx; }

/* 学员头像（紫色圆形 + 首字母） */
.student-avatar {
  width: 72rpx;
  height: 72rpx;
  background: #6366f1;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-text { font-size: 32rpx; font-weight: 700; color: #fff; }

/* 学员姓名和电话 */
.student-info { flex: 1; }
.student-name { display: block; font-size: 30rpx; font-weight: 600; color: #1e293b; }
.student-phone { display: block; font-size: 24rpx; color: #94a3b8; margin-top: 4rpx; }

/* 报名类型标签 */
.student-type {
  font-size: 24rpx;
  padding: 4rpx 16rpx;
  background: #eef2ff;
  color: #6366f1;
  border-radius: 16rpx;
}

/* 空状态 */
.empty { text-align: center; padding: 120rpx 0; }
.empty-icon { display: block; font-size: 80rpx; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: #94a3b8; }
</style>
