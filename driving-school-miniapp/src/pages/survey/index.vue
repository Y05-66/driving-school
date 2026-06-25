<!--
  满意度调查页面
  功能：对教练进行评分和评价
-->
<template>
  <view class="container">
    <view class="card form-card">
      <text class="card-title">📝 评价教练</text>
      <!-- 教练选择 -->
      <view class="form-item">
        <text class="form-label">选择教练</text>
        <picker :range="coachNames" @change="onCoachChange">
          <view class="picker-box">
            <text class="picker-text">{{ coachNames[coachIdx] || '请选择教练' }}</text>
            <text class="picker-arrow">▸</text>
          </view>
        </picker>
      </view>

      <!-- 评分项 -->
      <view v-for="item in rateItems" :key="item.key" class="form-item">
        <text class="form-label">{{ item.label }}</text>
        <view class="stars-row">
          <text v-for="s in 5" :key="s" class="star" :class="{ active: form[item.key] >= s }" @click="form[item.key] = s">★</text>
        </view>
      </view>

      <!-- 评价内容 -->
      <view class="form-item">
        <text class="form-label">评价内容</text>
        <textarea v-model="form.comment" class="textarea" placeholder="请输入您的评价（选填）" maxlength="500" />
      </view>

      <button class="btn-submit" @click="submit" :loading="submitting">提交评价</button>
    </view>

    <!-- 历史评价 -->
    <view class="card" style="margin-top:24rpx">
      <text class="card-title">📋 我的评价</text>
      <view v-if="surveyList.length === 0" class="empty-mini">
        <text class="empty-mini-text">暂无评价记录</text>
      </view>
      <view v-for="s in surveyList" :key="s.id" class="survey-item">
        <view class="survey-top">
          <text class="survey-coach">{{ s.coachName || '教练' }}</text>
          <text class="survey-score">{{ s.overallRating }} ★</text>
        </view>
        <text v-if="s.comment" class="survey-comment">{{ s.comment }}</text>
        <text class="survey-time">{{ s.createTime }}</text>
      </view>
    </view>
  </view>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { get, post } from '../../utils/request'

// ========== 状态变量 ==========

/** 教练列表 */
const coachList = ref([])

/** 教练姓名数组（用于picker组件） */
const coachNames = ref([])

/** 当前选中的教练索引，-1表示未选择 */
const coachIdx = ref(-1)

/** 提交中状态，防止重复提交 */
const submitting = ref(false)

/** 当前教练的历史评价列表 */
const surveyList = ref([])

/** 评价表单数据（各项评分默认5分） */
const form = reactive({ teachingQuality: 5, serviceAttitude: 5, teachingEnvironment: 5, overallRating: 5, comment: '' })

/** 评分项配置列表 */
const rateItems = [
  { key: 'teachingQuality', label: '教学质量' },
  { key: 'serviceAttitude', label: '服务态度' },
  { key: 'teachingEnvironment', label: '教学环境' },
  { key: 'overallRating', label: '综合评分' }
]

// ========== 事件处理 ==========

/**
 * 教练选择变更
 * @param {Object} e - picker组件的change事件对象
 * @description 更新选中的教练索引并加载该教练的历史评价
 */
const onCoachChange = (e) => {
  coachIdx.value = e.detail.value
  loadSurveys()
}

// ========== 数据操作 ==========

/**
 * 提交评价
 * @description 校验教练选择 → 调用接口提交评价 → 成功后清空评论并刷新历史评价列表
 */
const submit = async () => {
  if (coachIdx.value < 0) return uni.showToast({ title: '请选择教练', icon: 'none' })
  submitting.value = true
  try {
    await post('/surveys', { coachId: coachList.value[coachIdx.value].id, ...form })
    uni.showToast({ title: '评价成功', icon: 'success' })
    form.comment = ''
    loadSurveys()
  } catch {} finally { submitting.value = false }
}

/**
 * 加载指定教练的历史评价
 * @description 根据当前选中的教练ID查询评价记录，兼容分页和非分页返回格式
 */
const loadSurveys = async () => {
  if (coachIdx.value < 0 || !coachList.value[coachIdx.value]) {
    surveyList.value = []
    return
  }
  try {
    const res = await get('/surveys/coach/' + coachList.value[coachIdx.value].id)
    surveyList.value = res.data?.records || res.data || []
  } catch {}
}

// ========== 生命周期 ==========

/** 页面挂载：加载教练列表 */
onMounted(async () => {
  try {
    const res = await get('/coaches', { pageNum: 1, pageSize: 100 })
    coachList.value = res.data?.records || []
    coachNames.value = coachList.value.map(c => c.name)
  } catch {}
})
</script>
<style scoped>
.container { padding: 24rpx; min-height: 100vh; background: #f5f7fa; }
.card { background: #fff; border-radius: 20rpx; padding: 28rpx; box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.04); }
.card-title { font-size: 30rpx; font-weight: 700; color: #1e293b; display: block; margin-bottom: 24rpx; }
.form-item { margin-bottom: 28rpx; }
.form-label { font-size: 26rpx; font-weight: 600; color: #475569; display: block; margin-bottom: 12rpx; }
.picker-box { display: flex; justify-content: space-between; align-items: center; padding: 20rpx; background: #f8fafc; border-radius: 12rpx; border: 2rpx solid #e8ecf1; }
.picker-text { font-size: 28rpx; color: #334155; }
.picker-arrow { font-size: 24rpx; color: #94a3b8; }
.stars-row { display: flex; gap: 12rpx; }
.star { font-size: 48rpx; color: #e2e8f0; transition: color 0.2s; }
.star.active { color: #f59e0b; }
.textarea { width: 100%; height: 160rpx; padding: 20rpx; background: #f8fafc; border-radius: 12rpx; border: 2rpx solid #e8ecf1; font-size: 28rpx; color: #334155; }
.btn-submit { width: 100%; height: 88rpx; line-height: 88rpx; background: linear-gradient(135deg, #6366f1, #818cf8); color: #fff; font-size: 30rpx; font-weight: 600; border-radius: 16rpx; border: none; margin-top: 16rpx; }
.empty-mini { text-align: center; padding: 40rpx 0; }
.empty-mini-text { font-size: 26rpx; color: #94a3b8; }
.survey-item { padding: 20rpx 0; border-bottom: 2rpx solid #f1f5f9; }
.survey-item:last-child { border-bottom: none; }
.survey-top { display: flex; justify-content: space-between; margin-bottom: 8rpx; }
.survey-coach { font-size: 28rpx; font-weight: 600; color: #1e293b; }
.survey-score { font-size: 28rpx; color: #f59e0b; font-weight: 700; }
.survey-comment { font-size: 26rpx; color: #475569; line-height: 1.5; display: block; margin-bottom: 6rpx; }
.survey-time { font-size: 22rpx; color: #94a3b8; }
</style>
