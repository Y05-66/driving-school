<!--
  预约模块（学员/教练双端适配）
  - 学员端：选择日期、教练、时段进行预约
  - 教练端：查看预约列表、确认/完成预约
-->
<template>
  <view class="page">
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">预约练车</text>
      </view>
    </view>

    <scroll-view scroll-y class="scroll-area" :style="{ paddingTop: (statusBarHeight + 44) + 'px' }" refresher-enabled @refresherrefresh="onRefresh" :refresher-triggered="refreshing">
      <!-- 教练端：提示前往课程管理 -->
      <view v-if="role === 'COACH'" class="coach-notice">
        <view class="notice-icon">👨‍🏫</view>
        <text class="notice-title">教练端</text>
        <text class="notice-desc">预约管理请前往"我的课程"页面</text>
        <button class="btn-go-lessons" @click="goTo('/pages/coach/lessons')">前往我的课程</button>
      </view>

      <!-- 学员端：预约功能（教练角色被此守卫拦截，仅显示上方教练提示） -->
      <template v-if="role !== 'COACH'">
      <!-- 日期选择 -->
      <view class="date-section">
        <text class="section-title">选择日期</text>
        <scroll-view scroll-x class="date-scroll">
          <view class="date-list">
            <view v-for="d in dateList" :key="d.date" class="date-item" :class="{ active: selectedDate === d.date, today: d.isToday }" @click="selectDate(d.date)">
              <text class="date-week">{{ d.weekday }}</text>
              <text class="date-day">{{ d.day }}</text>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 教练选择 -->
      <view class="section">
        <text class="section-title">选择教练</text>
        <scroll-view scroll-x class="coach-scroll">
          <view class="coach-list">
            <view v-for="c in coachList" :key="c.id" class="coach-card" :class="{ active: selectedCoach === c.id }" @click="selectedCoach = c.id">
              <view class="coach-avatar">
                <text class="coach-letter">{{ (c.name || '?').charAt(0) }}</text>
              </view>
              <text class="coach-name">{{ c.name }}</text>
              <view class="coach-rating">
                <text class="rating-star">★</text>
                <text class="rating-num">{{ c.rating || '5.0' }}</text>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 课程选择 -->
      <view class="section" v-if="courseList.length > 1">
        <text class="section-title">选择课程</text>
        <scroll-view scroll-x class="coach-scroll">
          <view class="coach-list">
            <view v-for="c in courseList" :key="c.id" class="coach-card" :class="{ active: selectedCourse === c.id }" @click="selectedCourse = c.id">
              <text class="coach-name">{{ c.name }}</text>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 时段选择 -->
      <view class="section" v-if="selectedDate && selectedCoach">
        <text class="section-title">选择时段</text>
        <view v-if="timeSlots.length === 0" class="empty-slot">
          <text class="empty-text">该日期暂无可用时段</text>
        </view>
        <view v-else class="slot-grid">
          <view v-for="s in timeSlots" :key="s.id" class="slot-item" :class="{ active: selectedSlot === s.id, disabled: !s.available }" @click="s.available && (selectedSlot = s.id)">
            <text class="slot-time">{{ s.startTime }}-{{ s.endTime }}</text>
            <text class="slot-status">{{ s.available ? '可约' : '已满' }}</text>
          </view>
        </view>
      </view>

      <!-- 预约按钮 -->
      <view class="action-section" v-if="selectedDate && selectedCoach && selectedSlot && selectedCourse">
        <button class="btn-book" @click="submitBooking" :loading="booking" :disabled="booking">确认预约</button>
      </view>

      <!-- 我的预约 -->
      <view class="section">
        <view class="section-header">
          <text class="section-title">我的预约</text>
          <text class="section-more" @click="goTo('/pages/lesson/list')">全部 ›</text>
        </view>
        <view v-if="myBookings.length === 0" class="empty-booking">
          <text class="empty-icon">📅</text>
          <text class="empty-text">暂无预约</text>
        </view>
        <view v-else class="booking-list">
          <view v-for="b in myBookings" :key="b.id" class="booking-card">
            <view class="booking-date">
              <text class="booking-day">{{ b.lessonDate?.substring(5) }}</text>
            </view>
            <view class="booking-info">
              <text class="booking-time">{{ b.startTime }} - {{ b.endTime }}</text>
              <text class="booking-coach">{{ b.coachName || '待分配' }}</text>
            </view>
            <view class="booking-status" :class="'status-' + b.status">
              <text class="status-text">{{ statusText(b.status) }}</text>
            </view>
          </view>
        </view>
      </view>

      </template><!-- /学员端 -->
      <view style="height: 120rpx;"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../store/user'
import { get, post } from '../../utils/request'

// ========== 状态管理 ==========

/** @description 用户状态仓库实例 */
const userStore = useUserStore()

/** @description 当前用户角色：STUDENT / COACH / ADMIN / STAFF */
const role = computed(() => userStore.userInfo.role || '')

/** @description 系统状态栏高度（px），用于自定义导航栏定位 */
const statusBarHeight = ref(0)

/** @description 用户选中的日期（yyyy-MM-dd 格式） */
const selectedDate = ref('')

/** @description 用户选中的教练 ID */
const selectedCoach = ref(null)

/** @description 用户选中的课程 ID */
const selectedCourse = ref(null)

/** @description 用户选中的时段 ID */
const selectedSlot = ref(null)

/** @description 预约提交中的加载状态标识 */
const booking = ref(false)

/** @description 下拉刷新状态标识 */
const refreshing = ref(false)

/** @description 未来 14 天日期列表（含日期、星期、是否今天） */
const dateList = ref([])

/** @description 可选教练列表 */
const coachList = ref([])

/** @description 可选课程列表 */
const courseList = ref([])

/** @description 选中日期和教练对应的可用时段列表 */
const timeSlots = ref([])

/** @description 当前学员的预约记录列表 */
const myBookings = ref([])

// ========== 工具方法 ==========

/**
 * @description 课时状态码转中文文本
 * @param {number} s - 状态码（0待确认/1已确认/2进行中/3已完成/4已取消）
 * @returns {string} 状态中文文本
 */
const statusText = (s) => ({ 0: '待确认', 1: '已确认', 2: '进行中', 3: '已完成', 4: '已取消' }[s] || '')

/** @description Tab 栏页面路径列表 */
const tabPages = ['/pages/index/index', '/pages/learn/index', '/pages/booking/index', '/pages/message/index', '/pages/profile/index']

/**
 * @description 页面跳转：Tab 页使用 switchTab，其他页使用 navigateTo
 * @param {string} url - 目标页面路径
 * @returns {void}
 */
const goTo = (url) => { tabPages.includes(url) ? uni.switchTab({ url }) : uni.navigateTo({ url }) }

// ========== 日期与时段 ==========

/**
 * @description 生成未来 14 天日期列表并默认选中今天
 * @returns {void}
 */
const generateDates = () => {
  const weeks = ['日', '一', '二', '三', '四', '五', '六']
  const list = []
  const today = new Date()
  for (let i = 0; i < 14; i++) {
    const d = new Date(today)
    d.setDate(d.getDate() + i)
    const dateStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    list.push({
      date: dateStr,
      day: d.getDate(),
      weekday: i === 0 ? '今天' : '周' + weeks[d.getDay()],
      isToday: i === 0
    })
  }
  dateList.value = list
  selectedDate.value = list[0].date
}

/**
 * @description 选择日期，重置已选时段并重新加载时段列表
 * @param {string} date - 日期字符串（yyyy-MM-dd 格式）
 * @returns {void}
 */
const selectDate = (date) => {
  selectedDate.value = date
  selectedSlot.value = null
  loadTimeSlots()
}

/**
 * @description 根据选中日期和教练加载可用时段列表
 * @returns {Promise<void>}
 */
const loadTimeSlots = async () => {
  if (!selectedDate.value || !selectedCoach.value) { timeSlots.value = []; return }
  try {
    const res = await get('/lessons/available-time-slots', { coachId: selectedCoach.value, date: selectedDate.value })
    timeSlots.value = (res.data || []).map(s => ({ ...s, available: s.available !== false }))
  } catch { timeSlots.value = [] }
}

// ========== 预约操作 ==========

/**
 * @description 提交预约请求，包含课程、时段校验
 * @returns {Promise<void>}
 */
const submitBooking = async () => {
  // 课程校验：课程列表可能为空或用户未选择，需在此拦截以免提交无效数据
  if (!selectedCourse.value) {
    uni.showToast({ title: '请选择课程', icon: 'none' })
    return
  }
  // 从选中的时段中获取开始/结束时间
  const slot = timeSlots.value.find(s => s.id === selectedSlot.value)
  if (!slot) { uni.showToast({ title: '请选择时段', icon: 'none' }); return }

  booking.value = true
  try {
    await post('/lessons', {
      lessonDate: selectedDate.value,
      coachId: selectedCoach.value,
      courseId: selectedCourse.value,  // 用户选择的课程
      startTime: slot.startTime,
      endTime: slot.endTime
    })
    uni.showToast({ title: '预约成功', icon: 'success' })
    selectedSlot.value = null
    loadMyBookings()
  } catch (e) { console.error('预约失败', e) } finally { booking.value = false }
}

// ========== 数据加载 ==========

/**
 * @description 加载当前学员最近的预约记录
 * @returns {Promise<void>}
 */
const loadMyBookings = async () => {
  try {
    const res = await get('/lessons', { pageNum: 1, pageSize: 5 })
    myBookings.value = res.data?.records || []
  } catch {}
}

/** @description 监听教练选择变化，重置时段并重新加载 */
watch(selectedCoach, () => { selectedSlot.value = null; loadTimeSlots() })

/**
 * @description 下拉刷新回调，重新加载预约记录和时段列表
 * @returns {Promise<void>}
 */
const onRefresh = async () => {
  refreshing.value = true
  await loadMyBookings()
  if (selectedCoach.value && selectedDate.value) {
    await loadTimeSlots()
  }
  refreshing.value = false
}

// ========== 生命周期 ==========

/** @description 页面挂载时获取系统状态栏高度并生成日期列表 */
onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  generateDates()
})

/**
 * @description 每次显示页面时刷新数据：未登录则跳转登录页，否则加载教练、课程列表和预约记录
 * @returns {Promise<void>}
 */
onShow(async () => {
  // 未登录则跳转登录页
  if (!userStore.token) { uni.reLaunch({ url: '/pages/login/login' }); return }

  try {
    const [coachRes, courseRes] = await Promise.all([
      get('/coaches', { pageNum: 1, pageSize: 50 }),
      get('/courses', { pageNum: 1, pageSize: 50 })
    ])
    coachList.value = coachRes.data?.records || []
    courseList.value = courseRes.data?.records || []
    // 默认选中第一个课程
    if (courseList.value.length > 0) selectedCourse.value = courseList.value[0].id
  } catch {}
  loadMyBookings()
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f7fa; }
.nav-bar { position: fixed; top: 0; left: 0; right: 0; z-index: 100; background: linear-gradient(135deg, #4A7DFF, #6B9BFF); }
.nav-content { height: 44px; display: flex; align-items: center; justify-content: center; }
.nav-title { font-size: 34rpx; font-weight: 700; color: #fff; }
.scroll-area { height: 100vh; }

.date-section { padding: 30rpx 30rpx 0; }
.section { padding: 30rpx; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; }
.section-title { font-size: 28rpx; font-weight: 700; color: #1a1a2e; margin-bottom: 20rpx; }
.section-more { font-size: 24rpx; color: #4A7DFF; }

/* 日期选择 */
.date-scroll { white-space: nowrap; }
.date-list { display: inline-flex; gap: 16rpx; }
.date-item {
  width: 100rpx; padding: 16rpx 0; background: #fff; border-radius: 16rpx;
  text-align: center; border: 3rpx solid transparent;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
}
.date-item.active { border-color: #4A7DFF; background: #F0F4FF; }
.date-week { display: block; font-size: 22rpx; color: #999; }
.date-day { display: block; font-size: 32rpx; font-weight: 700; color: #1a1a2e; margin-top: 4rpx; }
.date-item.active .date-week { color: #4A7DFF; }
.date-item.active .date-day { color: #4A7DFF; }

/* 教练选择 */
.coach-scroll { white-space: nowrap; }
.coach-list { display: inline-flex; gap: 20rpx; }
.coach-card {
  width: 160rpx; padding: 24rpx 16rpx; background: #fff; border-radius: 20rpx;
  text-align: center; border: 3rpx solid transparent;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.04);
}
.coach-card.active { border-color: #4A7DFF; background: #F0F4FF; }
.coach-avatar {
  width: 80rpx; height: 80rpx; border-radius: 50%; background: #4A7DFF;
  display: flex; align-items: center; justify-content: center; margin: 0 auto 12rpx;
}
.coach-letter { font-size: 32rpx; font-weight: 700; color: #fff; }
.coach-name { display: block; font-size: 26rpx; font-weight: 600; color: #1a1a2e; }
.coach-rating { display: flex; align-items: center; justify-content: center; gap: 4rpx; margin-top: 6rpx; }
.rating-star { font-size: 20rpx; color: #FFD93D; }
.rating-num { font-size: 22rpx; color: #999; }

/* 时段选择 */
.slot-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.slot-item {
  width: calc(33.33% - 11rpx); padding: 20rpx; background: #fff; border-radius: 16rpx;
  text-align: center; border: 3rpx solid transparent;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
}
.slot-item.active { border-color: #4A7DFF; background: #F0F4FF; }
.slot-item.disabled { opacity: 0.5; }
.slot-time { display: block; font-size: 26rpx; font-weight: 600; color: #1a1a2e; }
.slot-status { display: block; font-size: 20rpx; color: #999; margin-top: 4rpx; }
.slot-item.active .slot-time { color: #4A7DFF; }
.slot-item.active .slot-status { color: #4A7DFF; }

.empty-slot, .empty-booking { text-align: center; padding: 40rpx 0; }
.empty-icon { font-size: 60rpx; display: block; margin-bottom: 12rpx; }
.empty-text { font-size: 26rpx; color: #999; }

.action-section { padding: 0 30rpx 30rpx; }
.btn-book {
  width: 100%; height: 88rpx; line-height: 88rpx;
  background: linear-gradient(135deg, #4A7DFF, #6B9BFF);
  color: #fff; font-size: 30rpx; font-weight: 600;
  border-radius: 24rpx; border: none;
}

/* 我的预约 */
.booking-list { display: flex; flex-direction: column; gap: 16rpx; }
.booking-card {
  background: #fff; border-radius: 20rpx; padding: 24rpx;
  display: flex; align-items: center; gap: 20rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.04);
}
.booking-date { width: 100rpx; text-align: center; }
.booking-day { font-size: 28rpx; font-weight: 700; color: #4A7DFF; }
.booking-info { flex: 1; }
.booking-time { display: block; font-size: 28rpx; font-weight: 600; color: #1a1a2e; }
.booking-coach { display: block; font-size: 22rpx; color: #999; margin-top: 6rpx; }
.booking-status { padding: 6rpx 16rpx; border-radius: 16rpx; }
.booking-status.status-0 { background: #E8F0FE; }
.booking-status.status-0 .status-text { color: #4A7DFF; }
.booking-status.status-1 { background: #E8F5E9; }
.booking-status.status-1 .status-text { color: #4CAF50; }
.booking-status.status-3 { background: #F5F5F5; }
.booking-status.status-3 .status-text { color: #999; }
.status-text { font-size: 22rpx; font-weight: 600; }

/* 教练端提示 */
.coach-notice {
  margin: 60rpx 30rpx; background: #fff; border-radius: 24rpx;
  padding: 60rpx 40rpx; text-align: center;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.coach-notice .notice-icon { font-size: 80rpx; display: block; margin-bottom: 20rpx; }
.coach-notice .notice-title { display: block; font-size: 36rpx; font-weight: 700; color: #1a1a2e; margin-bottom: 12rpx; }
.coach-notice .notice-desc { display: block; font-size: 26rpx; color: #999; margin-bottom: 30rpx; }
.btn-go-lessons {
  background: linear-gradient(135deg, #4A7DFF, #6B9BFF); color: #fff; border: none;
  border-radius: 24rpx; font-size: 28rpx; font-weight: 600; height: 80rpx; line-height: 80rpx;
  padding: 0 48rpx;
}
.btn-go-lessons::after { border: none; }
</style>
