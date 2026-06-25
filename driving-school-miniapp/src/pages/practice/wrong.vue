<!--
  错题本页面
  功能：查看做错的题目，支持重新练习
-->
<template>
  <view class="container">
    <view v-if="wrongList.length === 0" class="empty">
      <text class="empty-icon">🎉</text>
      <text class="empty-text">暂无错题，继续保持！</text>
    </view>
    <view v-for="(q, idx) in wrongList" :key="q.id" class="question-card">
      <view class="q-header">
        <text class="q-index">{{ idx + 1 }}</text>
        <text class="q-type">{{ q.questionType === 1 ? '判断题' : '单选题' }}</text>
      </view>
      <text class="q-title">{{ q.title }}</text>
      <view class="q-answer">
        <text class="answer-label">正确答案：</text>
        <text class="answer-value">{{ q.correctAnswer }}</text>
      </view>
      <view v-if="q.explanation" class="q-explain">
        <text class="explain-text">{{ q.explanation }}</text>
      </view>
    </view>
  </view>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '../../store/user'
import { get } from '../../utils/request'

const userStore = useUserStore()

// ========== 状态变量 ==========

/** 错题列表 */
const wrongList = ref([])

// ========== 生命周期 ==========

/** 页面挂载：校验登录状态和角色，加载错题列表 */
onMounted(async () => {
  // 未登录或非学员，返回上一页
  if (!userStore.token || userStore.userInfo.role !== 'STUDENT') {
    uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/index/index' }) })
    return
  }
  try { const res = await get('/practice/wrong-questions'); wrongList.value = res.data || [] } catch {}
})
</script>
<style scoped>
.container { padding: 24rpx; min-height: 100vh; background: #f5f7fa; }
.empty { text-align: center; padding: 120rpx 0; }
.empty-icon { font-size: 80rpx; display: block; margin-bottom: 16rpx; }
.empty-text { font-size: 28rpx; color: #94a3b8; }
.question-card { background: #fff; border-radius: 16rpx; padding: 28rpx; margin-bottom: 20rpx; box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.04); }
.q-header { display: flex; justify-content: space-between; margin-bottom: 16rpx; }
.q-index { font-size: 24rpx; color: #6366f1; font-weight: 700; }
.q-type { font-size: 22rpx; color: #94a3b8; background: #f1f5f9; padding: 4rpx 12rpx; border-radius: 8rpx; }
.q-title { font-size: 28rpx; color: #1e293b; line-height: 1.6; display: block; margin-bottom: 16rpx; }
.q-answer { display: flex; align-items: center; gap: 8rpx; margin-bottom: 12rpx; }
.answer-label { font-size: 24rpx; color: #64748b; }
.answer-value { font-size: 28rpx; color: #10b981; font-weight: 700; }
.q-explain { background: #fffbeb; border-radius: 8rpx; padding: 16rpx; border-left: 4rpx solid #f59e0b; }
.explain-text { font-size: 24rpx; color: #78350f; line-height: 1.5; }
</style>
