<!--
  首页（学员/教练双端适配）
  - 学员端：进度卡片、刷题入口、预约练车、今日课程
  - 教练端：今日课程、学员管理、工作统计
-->
<template>
  <view class="page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-left">
          <view class="logo-icon"><text class="logo-text">🚗</text></view>
          <text class="nav-title">{{ role === 'COACH' ? '教练工作台' : '驾校管理' }}</text>
        </view>
        <view class="nav-right" @click="goTo('/pages/message/index')">
          <view class="notify-badge" v-if="unreadCount > 0">
            <text class="badge-text">{{ unreadCount > 99 ? '99+' : unreadCount }}</text>
          </view>
          <text class="nav-icon">🔔</text>
        </view>
      </view>
    </view>

    <scroll-view scroll-y class="scroll-area" :style="{ paddingTop: (statusBarHeight + 44) + 'px' }" refresher-enabled @refresherrefresh="onRefresh" :refresher-triggered="refreshing">

      <!-- ==================== 学员端 ==================== -->
      <view v-if="role === 'STUDENT'">
        <!-- 用户信息 -->
        <view class="header-section">
          <view class="header-bg-decor"></view>
          <view class="user-greeting">
            <view class="avatar-wrap" @click="goTo('/pages/profile/index')">
              <image v-if="userInfo.avatar" :src="userInfo.avatar" class="avatar-img" mode="aspectFill" />
              <view v-else class="avatar-default">
                <text class="avatar-letter">{{ (userInfo.realName || userInfo.username || '?').charAt(0) }}</text>
              </view>
            </view>
            <view class="greeting-text">
              <text class="greeting-hello">{{ greeting }}</text>
              <text class="greeting-name">{{ userInfo.realName || userInfo.username || '学员' }}</text>
            </view>
          </view>

          <!-- 学习进度 -->
          <view class="progress-card" @click="goTo('/pages/student/progress')">
            <view class="progress-header">
              <view class="progress-title-row">
                <text class="progress-emoji">📚</text>
                <text class="progress-label">学习进度</text>
              </view>
              <text class="progress-percent">{{ overallProgress }}%</text>
            </view>
            <view class="progress-bar-bg">
              <view class="progress-bar-fill" :style="{ width: overallProgress + '%' }"></view>
            </view>
            <view class="progress-subjects">
              <view v-for="p in progressList" :key="p.id" class="subject-chip" :class="'status-' + p.status">
                <text class="chip-dot"></text>
                <text class="chip-text">{{ p.subjectName }}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 学员快捷功能 -->
        <view class="section">
          <view class="section-header">
            <text class="section-title">常用功能</text>
            <view class="section-decor"></view>
          </view>
          <view class="func-grid">
            <view class="func-item" @click="goTo('/pages/practice/index')">
              <view class="func-icon-wrap" style="background: linear-gradient(135deg, #FF6B6B, #FF8E8E)">
                <text class="func-emoji">📝</text>
              </view>
              <text class="func-name">模拟考试</text>
            </view>
            <view class="func-item" @click="goTo('/pages/lesson/book')">
              <view class="func-icon-wrap" style="background: linear-gradient(135deg, #4ECDC4, #6EE7DE)">
                <text class="func-emoji">📅</text>
              </view>
              <text class="func-name">预约练车</text>
            </view>
            <view class="func-item" @click="goTo('/pages/exam/list')">
              <view class="func-icon-wrap" style="background: linear-gradient(135deg, #45B7D1, #6DCDE5)">
                <text class="func-emoji">📋</text>
              </view>
              <text class="func-name">考试安排</text>
            </view>
            <view class="func-item" @click="goTo('/pages/lesson/list')">
              <view class="func-icon-wrap" style="background: linear-gradient(135deg, #96CEB4, #B5E8CC)">
                <text class="func-emoji">✅</text>
              </view>
              <text class="func-name">我的课时</text>
            </view>
            <view class="func-item" @click="goTo('/pages/finance/payment')">
              <view class="func-icon-wrap" style="background: linear-gradient(135deg, #FFEAA7, #FFF3B8)">
                <text class="func-emoji">💰</text>
              </view>
              <text class="func-name">我的缴费</text>
            </view>
            <view class="func-item" @click="goTo('/pages/survey/index')">
              <view class="func-icon-wrap" style="background: linear-gradient(135deg, #DDA0DD, #E8B8E8)">
                <text class="func-emoji">⭐</text>
              </view>
              <text class="func-name">评价教练</text>
            </view>
            <view class="func-item" @click="goTo('/pages/reminder/index')">
              <view class="func-icon-wrap" style="background: linear-gradient(135deg, #FD79A8, #FEA3C0)">
                <text class="func-emoji">⏰</text>
              </view>
              <text class="func-name">我的提醒</text>
            </view>
            <view class="func-item" @click="goTo('/pages/video/index')">
              <view class="func-icon-wrap" style="background: linear-gradient(135deg, #00B894, #55E6C1)">
                <text class="func-emoji">📹</text>
              </view>
              <text class="func-name">视频教学</text>
            </view>
            <view class="func-item" @click="goTo('/pages/profile/index')">
              <view class="func-icon-wrap" style="background: linear-gradient(135deg, #74B9FF, #A3D4FF)">
                <text class="func-emoji">👤</text>
              </view>
              <text class="func-name">个人中心</text>
            </view>
          </view>
        </view>

        <!-- Banner -->
        <view class="banner-section">
          <swiper class="banner-swiper" :autoplay="true" :interval="4000" :circular="true" indicator-dots indicator-color="rgba(255,255,255,0.4)" indicator-active-color="#fff">
            <swiper-item>
              <view class="banner-card" style="background: linear-gradient(135deg, #667eea, #764ba2)">
                <view class="banner-content">
                  <text class="banner-title">🎉 新学期优惠</text>
                  <text class="banner-desc">报名立减 500 元</text>
                </view>
                <text class="banner-emoji">🎓</text>
              </view>
            </swiper-item>
            <swiper-item>
              <view class="banner-card" style="background: linear-gradient(135deg, #f093fb, #f5576c)">
                <view class="banner-content">
                  <text class="banner-title">📝 科目一技巧</text>
                  <text class="banner-desc">掌握技巧，一次通过</text>
                </view>
                <text class="banner-emoji">💡</text>
              </view>
            </swiper-item>
          </swiper>
        </view>
      </view>

      <!-- ==================== 教练端 ==================== -->
      <view v-if="role === 'COACH'">
        <!-- 教练信息 -->
        <view class="coach-header">
          <view class="coach-bg-decor"></view>
          <view class="coach-info-row">
            <view class="avatar-wrap" @click="goTo('/pages/profile/index')">
              <image v-if="userInfo.avatar" :src="userInfo.avatar" class="avatar-img" mode="aspectFill" />
              <view v-else class="avatar-default">
                <text class="avatar-letter">{{ (userInfo.realName || userInfo.username || '?').charAt(0) }}</text>
              </view>
            </view>
            <view class="coach-info-text">
              <text class="coach-name">{{ userInfo.realName || userInfo.username }}</text>
              <view class="coach-role-tag"><text class="coach-role-text">教练</text></view>
            </view>
          </view>

          <!-- 教练统计 -->
          <view class="coach-stats">
            <view class="coach-stat-item">
              <text class="coach-stat-num">{{ coachStats.todayLessons || 0 }}</text>
              <text class="coach-stat-label">今日课程</text>
            </view>
            <view class="coach-stat-divider"></view>
            <view class="coach-stat-item">
              <text class="coach-stat-num">{{ coachStats.studentCount || 0 }}</text>
              <text class="coach-stat-label">名下学员</text>
            </view>
            <view class="coach-stat-divider"></view>
            <view class="coach-stat-item">
              <text class="coach-stat-num">{{ coachStats.monthLessons || 0 }}</text>
              <text class="coach-stat-label">本月课时</text>
            </view>
          </view>
        </view>

        <!-- 教练快捷功能 -->
        <view class="section">
          <view class="section-header">
            <text class="section-title">工作台</text>
            <view class="section-decor"></view>
          </view>
          <view class="func-grid">
            <view class="func-item" @click="goTo('/pages/coach/students')">
              <view class="func-icon-wrap" style="background: linear-gradient(135deg, #4A7DFF, #6B9BFF)">
                <text class="func-emoji">👥</text>
              </view>
              <text class="func-name">我的学员</text>
            </view>
            <view class="func-item" @click="goTo('/pages/coach/lessons')">
              <view class="func-icon-wrap" style="background: linear-gradient(135deg, #4ECDC4, #6EE7DE)">
                <text class="func-emoji">📅</text>
              </view>
              <text class="func-name">我的课程</text>
            </view>
            <view class="func-item" @click="goTo('/pages/coach/schedule')">
              <view class="func-icon-wrap" style="background: linear-gradient(135deg, #FF6B6B, #FF8E8E)">
                <text class="func-emoji">📋</text>
              </view>
              <text class="func-name">排班管理</text>
            </view>
            <view class="func-item" @click="goTo('/pages/reminder/index')">
              <view class="func-icon-wrap" style="background: linear-gradient(135deg, #FFD93D, #FFEAA7)">
                <text class="func-emoji">⏰</text>
              </view>
              <text class="func-name">我的提醒</text>
            </view>
            <view class="func-item" @click="goTo('/pages/video/index')">
              <view class="func-icon-wrap" style="background: linear-gradient(135deg, #00B894, #55E6C1)">
                <text class="func-emoji">📹</text>
              </view>
              <text class="func-name">教学视频</text>
            </view>
          </view>
        </view>

        <!-- 今日课程 -->
        <view class="section" v-if="todayLessons.length > 0">
          <view class="section-header">
            <text class="section-title">今日课程</text>
            <text class="section-more" @click="goTo('/pages/coach/lessons')">查看全部 ›</text>
          </view>
          <view class="lesson-list">
            <view v-for="l in todayLessons" :key="l.id" class="lesson-card">
              <view class="lesson-time-badge">
                <text class="lesson-time-text">{{ l.startTime }}</text>
              </view>
              <view class="lesson-info">
                <text class="lesson-student">{{ l.studentName || '学员' }}</text>
                <text class="lesson-course">{{ l.courseName || '培训课程' }}</text>
              </view>
              <view class="lesson-status" :class="'status-' + l.status">
                <text class="status-text">{{ lessonStatusText(l.status) }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 通知（通用） -->
      <view class="section" v-if="recentNotifications.length > 0">
        <view class="section-header">
          <text class="section-title">最新通知</text>
          <text class="section-more" @click="goTo('/pages/message/index')">查看全部 ›</text>
        </view>
        <view class="notify-list">
          <view v-for="n in recentNotifications" :key="n.id" class="notify-card">
            <view class="notify-icon-bg" :class="{ unread: !n.isRead }">
              <text class="notify-emoji">📢</text>
            </view>
            <view class="notify-body">
              <text class="notify-title">{{ n.title }}</text>
              <text class="notify-time">{{ n.createTime }}</text>
            </view>
            <view v-if="!n.isRead" class="notify-dot"></view>
          </view>
        </view>
      </view>

      <!-- 底部 -->
      <view class="footer">
        <text class="footer-text">— 驾校管理系统 —</text>
      </view>

      <view style="height: 120rpx;"></view>
    </scroll-view>
    <FloatingAi />
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../store/user'
import { get } from '../../utils/request'
import FloatingAi from '../../components/FloatingAi.vue'

// ========== 状态管理 ==========

/** @description 用户状态仓库实例 */
const userStore = useUserStore()

/** @description 当前登录用户信息（计算属性，响应式读取 store） */
const userInfo = computed(() => userStore.userInfo)

/** @description 当前用户角色：STUDENT / COACH / ADMIN / STAFF */
const role = computed(() => userStore.userInfo.role || '')

/** @description 未读通知数量 */
const unreadCount = ref(0)

/** @description 学员学习进度列表 */
const progressList = ref([])

/** @description 教练今日课程列表 */
const todayLessons = ref([])

/** @description 下拉刷新状态标识 */
const refreshing = ref(false)

/** @description 最新通知列表 */
const recentNotifications = ref([])

/** @description 教练统计数据（今日课程数、名下学员数、本月课时数） */
const coachStats = ref({})

/** @description 系统状态栏高度（px），用于自定义导航栏定位 */
const statusBarHeight = ref(0)

// ========== 计算属性 ==========

/**
 * @description 根据当前时间生成问候语
 * @returns {string} 带 emoji 的问候文本
 */
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了 🌙'
  if (h < 9) return '早上好 ☀️'
  if (h < 12) return '上午好 🌤'
  if (h < 14) return '中午好 🌞'
  if (h < 18) return '下午好 🌅'
  return '晚上好 🌆'
})

/**
 * @description 计算学员整体学习进度百分比
 * @returns {number} 进度百分比（0-100）
 */
const overallProgress = computed(() => {
  if (!progressList.value.length) return 0
  const total = progressList.value.reduce((s, p) => s + (p.requiredHours || 0), 0)
  const done = progressList.value.reduce((s, p) => s + (p.completedHours || 0), 0)
  return total ? Math.round(done / total * 100) : 0
})

// ========== 工具方法 ==========

/**
 * @description 课程状态码转中文文本
 * @param {number} s - 状态码（0待确认/1已确认/2进行中/3已完成/4已取消）
 * @returns {string} 状态中文文本
 */
const lessonStatusText = (s) => ({ 0: '待确认', 1: '已确认', 2: '进行中', 3: '已完成', 4: '已取消' }[s] || '')

/** @description Tab 栏页面路径列表（switchTab 要求使用 switchTab 跳转） */
const tabPages = ['/pages/index/index', '/pages/learn/index', '/pages/booking/index', '/pages/message/index', '/pages/profile/index']

/**
 * @description 页面跳转：Tab 页使用 switchTab，其他页使用 navigateTo
 * @param {string} url - 目标页面路径
 * @returns {void}
 */
const goTo = (url) => { tabPages.includes(url) ? uni.switchTab({ url }) : uni.navigateTo({ url }) }

// ========== 页面数据加载 ==========

/**
 * @description 下拉刷新回调，重新加载页面所有数据
 * @returns {Promise<void>}
 */
const onRefresh = async () => {
  refreshing.value = true
  await loadData()
  refreshing.value = false
}

/**
 * @description 加载页面所有数据：通知、未读数、学员进度或教练统计
 * @returns {Promise<void>}
 */
const loadData = async () => {
  try { const res = await get('/notifications', { pageNum: 1, pageSize: 3 }); recentNotifications.value = res.data?.records || [] } catch { uni.showToast({ title: '加载通知失败', icon: 'none' }) }
  try { const res = await get('/notifications/unread-count'); unreadCount.value = Number(res.data?.count) || 0 } catch {}
  if (role.value === 'STUDENT') {
    try { const res = await get('/students/me/progress'); progressList.value = res.data || [] } catch { uni.showToast({ title: '加载进度失败', icon: 'none' }) }
  }
  if (role.value === 'COACH') {
    try { const res = await get('/statistics/dashboard'); coachStats.value = res.data || {}; todayLessons.value = res.data?.todayLessonsList || [] } catch { uni.showToast({ title: '加载数据失败', icon: 'none' }) }
  }
}

// ========== 生命周期 ==========

/** @description 页面挂载时获取系统状态栏高度 */
onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
})

/**
 * @description 页面显示时（含 tab 切换回来）刷新数据，未登录则跳转登录页
 * @returns {Promise<void>}
 */
// 每次显示页面时刷新数据（tab切换回来时也会触发）
onShow(async () => {
  if (!userStore.token) { uni.reLaunch({ url: '/pages/login/login' }); return }

  // 获取用户信息
  if (!userStore.userInfo.role) {
    try { await userStore.fetchUserInfo() } catch { uni.reLaunch({ url: '/pages/login/login' }); return }
  }

  // 加载数据
  await loadData()
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f7fa; }
.nav-bar { position: fixed; top: 0; left: 0; right: 0; z-index: 100; background: linear-gradient(135deg, #4A7DFF, #6B9BFF); }
.nav-content { height: 44px; display: flex; align-items: center; justify-content: space-between; padding: 0 30rpx; }
.nav-left { display: flex; align-items: center; gap: 12rpx; }
.logo-icon { width: 48rpx; height: 48rpx; background: rgba(255,255,255,0.2); border-radius: 12rpx; display: flex; align-items: center; justify-content: center; }
.logo-text { font-size: 28rpx; }
.nav-title { font-size: 34rpx; font-weight: 700; color: #fff; }
.nav-right { position: relative; padding: 10rpx; }
.nav-icon { font-size: 40rpx; }
.notify-badge { position: absolute; top: 0; right: 0; background: #FF4757; border-radius: 20rpx; padding: 2rpx 10rpx; min-width: 32rpx; text-align: center; }
.badge-text { font-size: 18rpx; color: #fff; font-weight: 600; }
.scroll-area { height: 100vh; }

/* 学员端头部 */
.header-section { background: linear-gradient(135deg, #4A7DFF, #6B9BFF); padding: 20rpx 30rpx 40rpx; border-radius: 0 0 40rpx 40rpx; position: relative; overflow: hidden; }
.header-bg-decor { position: absolute; top: -40rpx; right: -40rpx; width: 200rpx; height: 200rpx; background: rgba(255,255,255,0.08); border-radius: 50%; }
.user-greeting { display: flex; align-items: center; gap: 20rpx; margin-bottom: 24rpx; position: relative; z-index: 1; }
.avatar-wrap { width: 90rpx; height: 90rpx; border-radius: 50%; overflow: hidden; flex-shrink: 0; border: 4rpx solid rgba(255,255,255,0.3); }
.avatar-img { width: 100%; height: 100%; }
.avatar-default { width: 100%; height: 100%; background: rgba(255,255,255,0.2); display: flex; align-items: center; justify-content: center; }
.avatar-letter { font-size: 40rpx; font-weight: 700; color: #fff; }
.greeting-text { flex: 1; }
.greeting-hello { display: block; font-size: 24rpx; color: rgba(255,255,255,0.8); }
.greeting-name { display: block; font-size: 34rpx; font-weight: 700; color: #fff; margin-top: 4rpx; }

/* 进度卡片 */
.progress-card { background: rgba(255,255,255,0.15); border-radius: 24rpx; padding: 24rpx; position: relative; z-index: 1; }
.progress-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.progress-title-row { display: flex; align-items: center; gap: 8rpx; }
.progress-emoji { font-size: 24rpx; }
.progress-label { font-size: 24rpx; color: rgba(255,255,255,0.9); }
.progress-percent { font-size: 36rpx; font-weight: 800; color: #fff; }
.progress-bar-bg { height: 14rpx; background: rgba(255,255,255,0.2); border-radius: 7rpx; overflow: hidden; margin-bottom: 16rpx; }
.progress-bar-fill { height: 100%; background: linear-gradient(90deg, #FFD93D, #FF6B6B); border-radius: 7rpx; }
.progress-subjects { display: flex; gap: 12rpx; }
.subject-chip { display: flex; align-items: center; gap: 8rpx; padding: 6rpx 14rpx; border-radius: 16rpx; }
.status-0 { background: rgba(255,255,255,0.15); }
.status-0 .chip-dot { background: rgba(255,255,255,0.5); }
.status-0 .chip-text { color: rgba(255,255,255,0.7); }
.status-1 { background: rgba(255,215,0,0.3); }
.status-1 .chip-dot { background: #FFD700; }
.status-1 .chip-text { color: #FFD700; }
.status-2 { background: rgba(46,213,115,0.3); }
.status-2 .chip-dot { background: #2ED573; }
.status-2 .chip-text { color: #2ED573; }
.chip-dot { width: 10rpx; height: 10rpx; border-radius: 50%; }
.chip-text { font-size: 22rpx; font-weight: 600; }

/* 教练端头部 */
.coach-header { background: linear-gradient(135deg, #4A7DFF, #6B9BFF); padding: 20rpx 30rpx 30rpx; border-radius: 0 0 40rpx 40rpx; position: relative; overflow: hidden; }
.coach-bg-decor { position: absolute; top: -40rpx; right: -40rpx; width: 200rpx; height: 200rpx; background: rgba(255,255,255,0.08); border-radius: 50%; }
.coach-info-row { display: flex; align-items: center; gap: 20rpx; margin-bottom: 24rpx; position: relative; z-index: 1; }
.coach-info-text { flex: 1; }
.coach-name { display: block; font-size: 34rpx; font-weight: 700; color: #fff; }
.coach-role-tag { display: inline-block; background: rgba(255,255,255,0.2); padding: 4rpx 16rpx; border-radius: 12rpx; margin-top: 8rpx; }
.coach-role-text { font-size: 22rpx; color: #fff; }
.coach-stats { display: flex; align-items: center; background: rgba(255,255,255,0.15); border-radius: 20rpx; padding: 24rpx; position: relative; z-index: 1; }
.coach-stat-item { flex: 1; text-align: center; }
.coach-stat-num { display: block; font-size: 40rpx; font-weight: 800; color: #fff; }
.coach-stat-label { display: block; font-size: 22rpx; color: rgba(255,255,255,0.8); margin-top: 4rpx; }
.coach-stat-divider { width: 2rpx; height: 50rpx; background: rgba(255,255,255,0.2); }

/* 功能区 */
.section { padding: 30rpx; }
.section-header { display: flex; align-items: center; gap: 12rpx; margin-bottom: 20rpx; }
.section-title { font-size: 30rpx; font-weight: 700; color: #1a1a2e; }
.section-decor { width: 6rpx; height: 28rpx; background: linear-gradient(180deg, #4A7DFF, #6B9BFF); border-radius: 3rpx; }
.section-more { font-size: 24rpx; color: #4A7DFF; margin-left: auto; }
.func-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 24rpx; }
.func-item { display: flex; flex-direction: column; align-items: center; gap: 12rpx; }
.func-icon-wrap { width: 100rpx; height: 100rpx; border-radius: 28rpx; display: flex; align-items: center; justify-content: center; box-shadow: 0 8rpx 24rpx rgba(0,0,0,0.08); }
.func-emoji { font-size: 44rpx; }
.func-name { font-size: 24rpx; font-weight: 600; color: #333; }

/* Banner */
.banner-section { padding: 0 30rpx 30rpx; }
.banner-swiper { height: 200rpx; border-radius: 24rpx; overflow: hidden; }
.banner-card { width: 100%; height: 100%; border-radius: 24rpx; padding: 30rpx; display: flex; justify-content: space-between; align-items: center; }
.banner-content { flex: 1; }
.banner-title { display: block; font-size: 30rpx; font-weight: 700; color: #fff; }
.banner-desc { display: block; font-size: 24rpx; color: rgba(255,255,255,0.8); margin-top: 8rpx; }
.banner-emoji { font-size: 70rpx; opacity: 0.3; }

/* 课程列表 */
.lesson-list { display: flex; flex-direction: column; gap: 16rpx; }
.lesson-card { background: #fff; border-radius: 20rpx; padding: 24rpx; display: flex; align-items: center; gap: 16rpx; box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.04); }
.lesson-time-badge { background: linear-gradient(135deg, #4A7DFF, #6B9BFF); padding: 12rpx 16rpx; border-radius: 12rpx; min-width: 80rpx; text-align: center; }
.lesson-time-text { font-size: 26rpx; font-weight: 700; color: #fff; }
.lesson-info { flex: 1; }
.lesson-student { display: block; font-size: 28rpx; font-weight: 600; color: #1a1a2e; }
.lesson-course { display: block; font-size: 22rpx; color: #999; margin-top: 6rpx; }
.lesson-status { padding: 6rpx 16rpx; border-radius: 16rpx; }
.lesson-status.status-0 { background: #E8F0FE; }
.lesson-status.status-0 .status-text { color: #4A7DFF; }
.lesson-status.status-1 { background: #E8F5E9; }
.lesson-status.status-1 .status-text { color: #4CAF50; }
.lesson-status.status-3 { background: #F5F5F5; }
.lesson-status.status-3 .status-text { color: #999; }
.status-text { font-size: 22rpx; font-weight: 600; }

/* 通知 */
.notify-list { display: flex; flex-direction: column; gap: 12rpx; }
.notify-card { background: #fff; border-radius: 16rpx; padding: 20rpx; display: flex; align-items: center; gap: 16rpx; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.03); }
.notify-icon-bg { width: 60rpx; height: 60rpx; border-radius: 16rpx; background: #F0F4FF; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.notify-icon-bg.unread { background: #FFF0F0; }
.notify-emoji { font-size: 28rpx; }
.notify-body { flex: 1; min-width: 0; }
.notify-title { display: block; font-size: 26rpx; font-weight: 600; color: #1a1a2e; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.notify-time { display: block; font-size: 22rpx; color: #bbb; margin-top: 4rpx; }
.notify-dot { width: 12rpx; height: 12rpx; border-radius: 50%; background: #FF4757; flex-shrink: 0; }

/* 底部 */
.footer { text-align: center; padding: 30rpx 0; }
.footer-text { font-size: 22rpx; color: #ccc; }
</style>
