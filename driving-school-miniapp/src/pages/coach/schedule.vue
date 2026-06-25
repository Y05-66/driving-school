<!--
  教练排班页面
  功能：添加排班、查看排班、删除排班
-->
<template>
  <view class="page">
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">排班管理</text>
      </view>
    </view>

    <scroll-view scroll-y class="scroll-area" :style="{ paddingTop: (statusBarHeight + 44) + 'px' }">
      <!-- 快速排班 -->
      <view class="section">
        <text class="section-title">📅 快速排班</text>
        <view class="quick-form">
          <view class="form-row">
            <text class="form-label">日期范围</text>
            <view class="date-range">
              <picker mode="date" @change="e => batchForm.startDate = e.detail.value">
                <view class="date-box">{{ batchForm.startDate || '开始日期' }}</view>
              </picker>
              <text class="date-sep">至</text>
              <picker mode="date" @change="e => batchForm.endDate = e.detail.value">
                <view class="date-box">{{ batchForm.endDate || '结束日期' }}</view>
              </picker>
            </view>
          </view>
          <view class="form-row">
            <text class="form-label">时间段</text>
            <view class="date-range">
              <picker mode="time" @change="e => batchForm.startTime = e.detail.value">
                <view class="date-box">{{ batchForm.startTime || '开始时间' }}</view>
              </picker>
              <text class="date-sep">至</text>
              <picker mode="time" @change="e => batchForm.endTime = e.detail.value">
                <view class="date-box">{{ batchForm.endTime || '结束时间' }}</view>
              </picker>
            </view>
          </view>
          <view class="form-row">
            <text class="form-label">每时段最大学员数</text>
            <input v-model="batchForm.maxStudents" type="number" class="form-input" placeholder="1" />
          </view>
          <button class="btn-add" @click="batchAdd" :loading="submitting">批量添加排班</button>
        </view>
      </view>

      <!-- 我的排班列表 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">📋 我的排班</text>
          <text class="section-count">共 {{ scheduleList.length }} 条</text>
        </view>

        <view v-if="scheduleList.length === 0" class="empty">
          <text class="empty-icon">📅</text>
          <text class="empty-text">暂无排班，请先添加</text>
        </view>

        <view v-for="s in scheduleList" :key="s.id" class="schedule-card">
          <view class="schedule-date">
            <text class="date-day">{{ s.scheduleDate?.substring(5) }}</text>
            <text class="date-week">{{ getWeekday(s.scheduleDate) }}</text>
          </view>
          <view class="schedule-info">
            <text class="schedule-time">{{ s.startTime }} - {{ s.endTime }}</text>
            <view class="schedule-meta">
              <text class="meta-item">👥 {{ s.bookedCount || 0 }}/{{ s.maxStudents }}</text>
              <text class="meta-item" :class="'status-' + s.status">{{ statusText(s.status) }}</text>
            </view>
          </view>
          <view class="schedule-action" @click="deleteSchedule(s.id)">
            <text class="action-text">删除</text>
          </view>
        </view>
      </view>

      <view style="height: 120rpx;"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '../../store/user'
import { get, post, del } from '../../utils/request'

const userStore = useUserStore()

/** 状态栏高度，用于自定义导航栏定位 */
const statusBarHeight = ref(0)

/** 提交状态，防止重复点击 */
const submitting = ref(false)

/** 排班列表数据 */
const scheduleList = ref([])

/** 获取今天的日期字符串（本地时间），作为日期范围的默认起始值 */
const now = new Date()
const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`

/** 批量排班表单 */
const batchForm = reactive({
  startDate: today,   // 排班开始日期
  endDate: '',        // 排班结束日期
  startTime: '08:00', // 每日开始时间
  endTime: '12:00',   // 每日结束时间
  maxStudents: 1      // 每时段最大学员数
})

/**
 * 获取排班状态文本
 * @param {number} s - 状态码：0-可约 1-已满 2-已关闭
 * @returns {string} 状态文本
 */
const statusText = (s) => ({ 0: '可约', 1: '已满', 2: '已关闭' }[s] || '未知')

/**
 * 获取日期对应的星期几
 * @param {string} dateStr - 日期字符串，格式：YYYY-MM-DD
 * @returns {string} 星期几，如"周一"、"周二"
 */
const getWeekday = (dateStr) => {
  if (!dateStr) return ''
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return days[new Date(dateStr).getDay()]
}

/**
 * 加载当前教练的排班列表
 * 调用 GET /coach-schedules/my 获取我的排班数据
 */
const loadSchedules = async () => {
  try {
    const res = await get('/coach-schedules/my')
    scheduleList.value = res.data || []
  } catch {}
}

/**
 * 批量添加排班
 * @description 校验表单 → 调用批量排班接口 → 成功后刷新列表
 * 业务规则：日期范围和时间段必填，最大学员数必须大于0
 */
const batchAdd = async () => {
  // 校验日期范围
  if (!batchForm.startDate || !batchForm.endDate) return uni.showToast({ title: '请选择日期范围', icon: 'none' })
  // 校验时间段
  if (!batchForm.startTime || !batchForm.endTime) return uni.showToast({ title: '请选择时间段', icon: 'none' })
  // 校验最大学员数
  const max = parseInt(batchForm.maxStudents)
  if (!max || max < 1) return uni.showToast({ title: '请输入有效的最大学员数', icon: 'none' })
  submitting.value = true
  try {
    await post('/coach-schedules/batch', { ...batchForm, maxStudents: max })
    uni.showToast({ title: '排班添加成功', icon: 'success' })
    loadSchedules()
  } catch {} finally { submitting.value = false }
}

/**
 * 删除单条排班记录
 * @param {number} id - 排班记录ID
 * @description 弹出确认弹窗 → 确认后调用删除接口 → 刷新列表
 */
const deleteSchedule = async (id) => {
  uni.showModal({
    title: '删除排班', content: '确定删除该排班？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await del('/coach-schedules/' + id)
          uni.showToast({ title: '已删除', icon: 'success' })
          loadSchedules()
        } catch {}
      }
    }
  })
}

/**
 * 页面加载
 * @description 获取状态栏高度，校验登录状态，加载排班列表
 */
onMounted(async () => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  if (!userStore.token) { uni.navigateBack(); return }
  if (userStore.userInfo.role !== 'COACH') { uni.navigateBack(); return }
  loadSchedules()
})
</script>

<style scoped>
/* 页面容器 */
.page { min-height: 100vh; background: #f5f7fa; }

/* 自定义导航栏 */
.nav-bar { position: fixed; top: 0; left: 0; right: 0; z-index: 100; background: linear-gradient(135deg, #4A7DFF, #6B9BFF); }
.nav-content { height: 44px; display: flex; align-items: center; justify-content: center; }
.nav-title { font-size: 34rpx; font-weight: 700; color: #fff; }
.scroll-area { height: 100vh; }

/* 通用段落样式 */
.section { padding: 30rpx; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; }
.section-title { font-size: 30rpx; font-weight: 700; color: #1a1a2e; }
.section-count { font-size: 24rpx; color: #999; }

/* 快速排班表单 */
.quick-form { background: #fff; border-radius: 20rpx; padding: 28rpx; box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.04); }
.form-row { margin-bottom: 24rpx; }
.form-label { display: block; font-size: 26rpx; font-weight: 600; color: #475569; margin-bottom: 12rpx; }
.date-range { display: flex; align-items: center; gap: 12rpx; }
.date-box { flex: 1; height: 72rpx; line-height: 72rpx; text-align: center; background: #f5f7fa; border-radius: 12rpx; font-size: 26rpx; color: #333; }
.date-sep { font-size: 24rpx; color: #999; }
.form-input { width: 100%; height: 72rpx; padding: 0 20rpx; background: #f5f7fa; border-radius: 12rpx; font-size: 28rpx; }
.btn-add { width: 100%; height: 80rpx; line-height: 80rpx; background: linear-gradient(135deg, #4A7DFF, #6B9BFF); color: #fff; font-size: 28rpx; font-weight: 600; border-radius: 16rpx; border: none; }

/* 排班列表 */
.empty { text-align: center; padding: 60rpx 0; }
.empty-icon { font-size: 60rpx; display: block; margin-bottom: 12rpx; }
.empty-text { font-size: 26rpx; color: #999; }

.schedule-card {
  background: #fff; border-radius: 16rpx; padding: 24rpx;
  display: flex; align-items: center; gap: 20rpx;
  margin-bottom: 16rpx; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
}
.schedule-date { width: 100rpx; text-align: center; }
.date-day { display: block; font-size: 28rpx; font-weight: 700; color: #4A7DFF; }
.date-week { display: block; font-size: 22rpx; color: #999; }
.schedule-info { flex: 1; }
.schedule-time { display: block; font-size: 28rpx; font-weight: 600; color: #1a1a2e; }
.schedule-meta { display: flex; gap: 16rpx; margin-top: 8rpx; }
.meta-item { font-size: 22rpx; color: #999; }
.status-0 { color: #10b981; }
.status-1 { color: #f59e0b; }
.status-2 { color: #999; }
.schedule-action { padding: 12rpx; }
.action-text { font-size: 24rpx; color: #ef4444; }
</style>
