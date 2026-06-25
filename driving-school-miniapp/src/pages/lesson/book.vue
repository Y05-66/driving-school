<!--
  预约课时页面（学员端）
  功能：学员在线预约练车课时
  - 选择教练（从教练列表）
  - 选择课程（科目一~四）
  - 选择日期（不能选过去）
  - 选择时间段（结束时间必须晚于开始时间）
-->
<template>
  <view class="container">
    <view class="card">
      <text class="card-title">📅 预约课时</text>

      <!-- 选择教练 -->
      <view class="form-item">
        <text class="form-label">选择教练</text>
        <picker :range="coachList" range-key="name" @change="onCoachChange">
          <view class="picker-value">{{ selectedCoach ? selectedCoach.name : '请选择教练' }} ▾</view>
        </picker>
      </view>

      <!-- 选择课程 -->
      <view class="form-item">
        <text class="form-label">选择课程</text>
        <picker :range="courseList" range-key="name" @change="onCourseChange">
          <view class="picker-value">{{ selectedCourse ? selectedCourse.name : '请选择课程' }} ▾</view>
        </picker>
      </view>

      <!-- 选择日期（限制不能选过去） -->
      <view class="form-item">
        <text class="form-label">预约日期</text>
        <picker mode="date" :start="today" @change="onDateChange">
          <view class="picker-value">{{ form.lessonDate || '请选择日期' }} ▾</view>
        </picker>
      </view>

      <!-- 开始时间 -->
      <view class="form-item">
        <text class="form-label">开始时间</text>
        <picker mode="time" @change="onStartTimeChange">
          <view class="picker-value">{{ form.startTime || '请选择时间' }} ▾</view>
        </picker>
      </view>

      <!-- 结束时间 -->
      <view class="form-item">
        <text class="form-label">结束时间</text>
        <picker mode="time" @change="onEndTimeChange">
          <view class="picker-value">{{ form.endTime || '请选择时间' }} ▾</view>
        </picker>
      </view>

      <!-- 提交按钮 -->
      <button class="btn-primary" @click="submitBook" :loading="loading">确认预约</button>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { get, post } from '../../utils/request'

// 提交按钮 loading 状态
const loading = ref(false)

// 教练列表和课程列表
const coachList = ref([])
const courseList = ref([])

// 当前选中的教练和课程
const selectedCoach = ref(null)
const selectedCourse = ref(null)

// 表单数据
const form = reactive({
  coachId: '',      // 教练ID
  courseId: '',     // 课程ID
  lessonDate: '',   // 预约日期
  startTime: '',    // 开始时间
  endTime: ''       // 结束时间
})

// 今天日期（用于限制日期选择器不能选过去）
const now = new Date()
const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`

/**
 * 教练选择回调
 */
const onCoachChange = (e) => {
  selectedCoach.value = coachList.value[e.detail.value]
  form.coachId = selectedCoach.value.id
}

/**
 * 课程选择回调
 */
const onCourseChange = (e) => {
  selectedCourse.value = courseList.value[e.detail.value]
  form.courseId = selectedCourse.value.id
}

/** 日期选择回调 */
const onDateChange = (e) => { form.lessonDate = e.detail.value }

/** 开始时间选择回调 */
const onStartTimeChange = (e) => { form.startTime = e.detail.value }

/** 结束时间选择回调 */
const onEndTimeChange = (e) => { form.endTime = e.detail.value }

/**
 * 提交预约
 * @description 校验表单 → 校验时间 → 调用预约接口 → 成功后返回上一页
 */
const submitBook = async () => {
  // 必填校验
  if (!form.coachId || !form.courseId || !form.lessonDate || !form.startTime || !form.endTime) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' })
    return
  }

  // 时间逻辑校验：结束时间必须晚于开始时间
  if (form.endTime <= form.startTime) {
    uni.showToast({ title: '结束时间必须晚于开始时间', icon: 'none' })
    return
  }

  loading.value = true
  try {
    await post('/lessons', form)
    uni.showToast({ title: '预约成功', icon: 'success' })
    // 返回上一页（如果无法返回则跳转首页）
    setTimeout(() => {
      uni.navigateBack({
        fail: () => uni.switchTab({ url: '/pages/index/index' })
      })
    }, 1000)
  } catch (e) {
    console.error('预约失败', e)
  } finally {
    loading.value = false
  }
}

/**
 * 页面加载
 * 获取教练列表和课程列表
 */
onMounted(async () => {
  try {
    const [coachRes, courseRes] = await Promise.all([
      get('/coaches', { pageNum: 1, pageSize: 50 }),
      get('/courses', { pageNum: 1, pageSize: 50 })
    ])
    coachList.value = coachRes.data?.records || []
    courseList.value = courseRes.data?.records || []
  } catch (e) {
    console.error('获取列表失败', e)
  }
})
</script>

<style scoped>
/* 表单项（每行一个） */
.form-item {
  padding: 24rpx 0;
  border-bottom: 2rpx solid #f1f5f9;
}
.form-label { display: block; font-size: 26rpx; color: #64748b; margin-bottom: 12rpx; }
.picker-value { font-size: 28rpx; color: #1e293b; padding: 16rpx 0; }

/* 提交按钮 */
.btn-primary { margin-top: 40rpx; }
</style>
