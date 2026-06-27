<!--
  学车模块
  - 学员端：科目选择、练习模式、学习统计
  - 教练端：教学资料、学员管理入口
  - 管理端：提示使用PC端
-->
<template>
  <view class="page">
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">学车</text>
      </view>
    </view>

    <scroll-view scroll-y class="scroll-area" :style="{ paddingTop: (statusBarHeight + 44) + 'px' }" refresher-enabled @refresherrefresh="onRefresh" :refresher-triggered="refreshing">

      <!-- ========== 学员端 ========== -->
      <view v-if="role === 'STUDENT'">
        <!-- 科目切换 -->
        <view class="subject-switch">
          <view class="switch-item" :class="{ active: subject === 1 }" @click="subject = 1">
            <view class="switch-icon">📖</view>
            <text class="switch-text">科目一</text>
            <text class="switch-desc">道路交通安全法规</text>
          </view>
          <view class="switch-item" :class="{ active: subject === 4 }" @click="subject = 4">
            <view class="switch-icon">📘</view>
            <text class="switch-text">科目四</text>
            <text class="switch-desc">安全文明驾驶</text>
          </view>
        </view>

        <!-- 学习统计 -->
        <view class="stats-card">
          <view class="stats-header">
            <text class="stats-title">📊 学习数据</text>
          </view>
          <view class="stats-row">
            <view class="stats-item">
              <view class="stats-num-wrap" style="background: #F0F4FF">
                <text class="stats-num" style="color: #4A7DFF">{{ stats.totalAnswered || 0 }}</text>
              </view>
              <text class="stats-label">已做题</text>
            </view>
            <view class="stats-item">
              <view class="stats-num-wrap" style="background: #E8F5E9">
                <text class="stats-num" style="color: #2ED573">{{ stats.correctCount || 0 }}</text>
              </view>
              <text class="stats-label">答对</text>
            </view>
            <view class="stats-item">
              <view class="stats-num-wrap" style="background: #FFF0F0">
                <text class="stats-num" style="color: #FF4757">{{ stats.wrongCount || 0 }}</text>
              </view>
              <text class="stats-label">答错</text>
            </view>
            <view class="stats-item">
              <view class="stats-num-wrap" style="background: #FFF8E1">
                <text class="stats-num" style="color: #FFB300">{{ stats.accuracy || 0 }}%</text>
              </view>
              <text class="stats-label">正确率</text>
            </view>
          </view>
        </view>

        <!-- 练习模式 -->
        <view class="section">
          <text class="section-title">🎯 练习模式</text>
          <view class="mode-list">
            <view class="mode-card" @click="startPractice('sequential')">
              <view class="mode-icon" style="background: linear-gradient(135deg, #4A7DFF, #6B9BFF)">
                <text class="mode-emoji">📖</text>
              </view>
              <view class="mode-info">
                <text class="mode-name">顺序练习</text>
                <text class="mode-desc">按题库顺序逐题练习，适合初学</text>
              </view>
              <text class="mode-arrow">›</text>
            </view>

            <view class="mode-card" @click="startPractice('random')">
              <view class="mode-icon" style="background: linear-gradient(135deg, #FF6B6B, #FF8E8E)">
                <text class="mode-emoji">🎲</text>
              </view>
              <view class="mode-info">
                <text class="mode-name">随机练习</text>
                <text class="mode-desc">随机抽题，检验掌握程度</text>
              </view>
              <text class="mode-arrow">›</text>
            </view>

            <view class="mode-card" @click="startPractice('mock')">
              <view class="mode-icon" style="background: linear-gradient(135deg, #FFD93D, #FFEAA7)">
                <text class="mode-emoji">🏆</text>
              </view>
              <view class="mode-info">
                <text class="mode-name">模拟考试</text>
                <text class="mode-desc">100题/45分钟，真实考场模拟</text>
              </view>
              <text class="mode-arrow">›</text>
            </view>
          </view>
        </view>

        <!-- 错题与收藏 -->
        <view class="section">
          <text class="section-title">📚 我的题库</text>
          <view class="my-grid">
            <view class="my-card" @click="goTo('/pages/practice/wrong')">
              <view class="my-icon-wrap" style="background: #FFF0F0">
                <text class="my-icon-emoji">📕</text>
              </view>
              <text class="my-name">错题本</text>
              <text class="my-count">{{ stats.wrongCount || 0 }} 题</text>
            </view>
            <view class="my-card" @click="goTo('/pages/practice/index')">
              <view class="my-icon-wrap" style="background: #F0F4FF">
                <text class="my-icon-emoji">📊</text>
              </view>
              <text class="my-name">练习记录</text>
              <text class="my-count">查看详情</text>
            </view>
          </view>
        </view>
      </view>

      <!-- ========== 教练端 ========== -->
      <view v-if="role === 'COACH'">
        <view class="coach-welcome">
          <view class="coach-icon">👨‍🏫</view>
          <text class="coach-title">教练工作台</text>
          <text class="coach-desc">管理您的学员和课程</text>
        </view>

        <view class="section">
          <text class="section-title">快捷入口</text>
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
            <!-- 排班管理非 Tab 页，goTo 内部自动使用 navigateTo 跳转（而非 switchTab） -->
            <view class="func-item" @click="goTo('/pages/coach/schedule')">
              <view class="func-icon-wrap" style="background: linear-gradient(135deg, #FF6B6B, #FF8E8E)">
                <text class="func-emoji">📋</text>
              </view>
              <text class="func-name">排班管理</text>
            </view>
            <view class="func-item" @click="goTo('/pages/message/index')">
              <view class="func-icon-wrap" style="background: linear-gradient(135deg, #FFD93D, #FFEAA7)">
                <text class="func-emoji">🔔</text>
              </view>
              <text class="func-name">消息通知</text>
            </view>
          </view>
        </view>
      </view>

      <!-- ========== 管理员/前台端 ========== -->
      <view v-if="role === 'ADMIN' || role === 'STAFF'">
        <view class="admin-notice">
          <view class="notice-icon">💻</view>
          <text class="notice-title">请使用电脑端管理</text>
          <text class="notice-desc">管理员和前台工作人员请使用电脑端管理系统，功能更完整</text>
          <view class="notice-info">
            <text class="info-label">电脑端地址：</text>
            <text class="info-value">http://localhost:3000</text>
          </view>
          <view class="notice-features">
            <view class="feature-item">
              <text class="feature-icon">📊</text>
              <text class="feature-text">数据统计与报表</text>
            </view>
            <view class="feature-item">
              <text class="feature-icon">👥</text>
              <text class="feature-text">学员与教练管理</text>
            </view>
            <view class="feature-item">
              <text class="feature-icon">💰</text>
              <text class="feature-text">财务管理与结算</text>
            </view>
            <view class="feature-item">
              <text class="feature-icon">⚙️</text>
              <text class="feature-text">系统设置与权限</text>
            </view>
          </view>
        </view>
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

/** @description 当前用户角色：STUDENT / COACH / ADMIN / STAFF */
const role = computed(() => userStore.userInfo.role || '')

/** @description 当前选中的科目编号（1=科目一，4=科目四） */
const subject = ref(1)

/** @description 学员刷题统计数据（totalAnswered, correctCount, wrongCount, accuracy） */
const stats = ref({})

/** @description 系统状态栏高度（px），用于自定义导航栏定位 */
const statusBarHeight = ref(0)

/** @description 下拉刷新状态标识 */
const refreshing = ref(false)

/** @description Tab 栏页面路径列表 */
const tabPages = ['/pages/index/index', '/pages/learn/index', '/pages/booking/index', '/pages/message/index', '/pages/profile/index']

// ========== 页面跳转 ==========

/**
 * @description 页面跳转：Tab 页使用 switchTab，其他页使用 navigateTo
 * @param {string} url - 目标页面路径
 * @returns {void}
 */
const goTo = (url) => { tabPages.includes(url) ? uni.switchTab({ url }) : uni.navigateTo({ url }) }

/**
 * @description 开始练习，携带科目和模式参数跳转到练习页
 * @param {string} mode - 练习模式：sequential（顺序）、random（随机）、mock（模拟考试）
 * @returns {void}
 */
const startPractice = (mode) => {
  uni.navigateTo({ url: `/pages/practice/index?subject=${subject.value}&mode=${mode}` })
}

// ========== 页面数据加载 ==========

/**
 * @description 下拉刷新回调，重新加载学员刷题统计数据
 * @returns {Promise<void>}
 */
const onRefresh = async () => {
  refreshing.value = true
  if (role.value === 'STUDENT') {
    try { const res = await get('/practice/stats'); stats.value = res.data || {} } catch { uni.showToast({ title: '刷新失败', icon: 'none' }) }
  }
  refreshing.value = false
}

// ========== 生命周期 ==========

/** @description 页面挂载时获取系统状态栏高度 */
onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
})

/**
 * @description 每次显示页面时刷新数据，未登录则跳转登录页；学员角色加载刷题统计
 * @returns {Promise<void>}
 */
onShow(async () => {
  if (!userStore.token) { uni.reLaunch({ url: '/pages/login/login' }); return }

  // 学员加载刷题统计
  if (role.value === 'STUDENT') {
    try { const res = await get('/practice/stats'); stats.value = res.data || {} } catch {}
  }
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f7fa; }
.nav-bar { position: fixed; top: 0; left: 0; right: 0; z-index: 100; background: linear-gradient(135deg, #4A7DFF, #6B9BFF); }
.nav-content { height: 44px; display: flex; align-items: center; justify-content: center; }
.nav-title { font-size: 34rpx; font-weight: 700; color: #fff; }
.scroll-area { height: 100vh; }

/* 学员端 - 科目切换 */
.subject-switch { display: flex; gap: 20rpx; padding: 30rpx; }
.switch-item {
  flex: 1; background: #fff; border-radius: 24rpx; padding: 28rpx;
  text-align: center; border: 4rpx solid transparent;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
  transition: all 0.3s;
}
.switch-item.active { border-color: #4A7DFF; background: #F0F4FF; }
.switch-icon { font-size: 40rpx; margin-bottom: 12rpx; }
.switch-text { display: block; font-size: 32rpx; font-weight: 700; color: #1a1a2e; }
.switch-desc { display: block; font-size: 22rpx; color: #999; margin-top: 8rpx; }
.switch-item.active .switch-text { color: #4A7DFF; }

/* 学员端 - 统计卡片 */
.stats-card { margin: 0 30rpx 30rpx; background: #fff; border-radius: 24rpx; padding: 28rpx; box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04); }
.stats-header { margin-bottom: 20rpx; }
.stats-title { font-size: 28rpx; font-weight: 700; color: #1a1a2e; }
.stats-row { display: flex; }
.stats-item { flex: 1; text-align: center; }
.stats-num-wrap { width: 72rpx; height: 72rpx; border-radius: 20rpx; display: flex; align-items: center; justify-content: center; margin: 0 auto 12rpx; }
.stats-num { font-size: 32rpx; font-weight: 800; }
.stats-label { display: block; font-size: 22rpx; color: #999; }

/* 学员端 - 练习模式 */
.section { padding: 0 30rpx 30rpx; }
.section-title { font-size: 30rpx; font-weight: 700; color: #1a1a2e; margin-bottom: 20rpx; }
.mode-list { display: flex; flex-direction: column; gap: 16rpx; }
.mode-card {
  background: #fff; border-radius: 20rpx; padding: 28rpx;
  display: flex; align-items: center; gap: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.mode-icon { width: 80rpx; height: 80rpx; border-radius: 20rpx; display: flex; align-items: center; justify-content: center; }
.mode-emoji { font-size: 40rpx; }
.mode-info { flex: 1; }
.mode-name { display: block; font-size: 28rpx; font-weight: 600; color: #1a1a2e; }
.mode-desc { display: block; font-size: 22rpx; color: #999; margin-top: 6rpx; }
.mode-arrow { font-size: 36rpx; color: #ccc; }

/* 学员端 - 我的题库 */
.my-grid { display: flex; gap: 20rpx; }
.my-card { flex: 1; background: #fff; border-radius: 20rpx; padding: 28rpx; text-align: center; box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04); }
.my-icon-wrap { width: 80rpx; height: 80rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 16rpx; }
.my-icon-emoji { font-size: 36rpx; }
.my-name { display: block; font-size: 28rpx; font-weight: 600; color: #1a1a2e; }
.my-count { display: block; font-size: 22rpx; color: #999; margin-top: 6rpx; }

/* 教练端 */
.coach-welcome {
  background: linear-gradient(135deg, #4A7DFF, #6B9BFF);
  margin: 30rpx; border-radius: 24rpx; padding: 40rpx; text-align: center;
}
.coach-icon { font-size: 60rpx; margin-bottom: 16rpx; }
.coach-title { display: block; font-size: 36rpx; font-weight: 700; color: #fff; }
.coach-desc { display: block; font-size: 24rpx; color: rgba(255,255,255,0.8); margin-top: 8rpx; }
.func-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 24rpx; }
.func-item { display: flex; flex-direction: column; align-items: center; gap: 12rpx; }
.func-icon-wrap { width: 100rpx; height: 100rpx; border-radius: 28rpx; display: flex; align-items: center; justify-content: center; box-shadow: 0 8rpx 24rpx rgba(0,0,0,0.08); }
.func-emoji { font-size: 44rpx; }
.func-name { font-size: 24rpx; font-weight: 600; color: #333; }

/* 管理员/前台端 */
.admin-notice {
  margin: 60rpx 30rpx; background: #fff; border-radius: 24rpx;
  padding: 60rpx 40rpx; text-align: center;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.notice-icon { font-size: 80rpx; margin-bottom: 20rpx; }
.notice-title { display: block; font-size: 36rpx; font-weight: 700; color: #1a1a2e; margin-bottom: 12rpx; }
.notice-desc { display: block; font-size: 26rpx; color: #999; margin-bottom: 30rpx; }
.notice-info { background: #F0F4FF; border-radius: 16rpx; padding: 20rpx; margin-bottom: 30rpx; }
.info-label { font-size: 24rpx; color: #666; }
.info-value { font-size: 28rpx; font-weight: 600; color: #4A7DFF; }
.notice-features { display: flex; flex-wrap: wrap; gap: 16rpx; justify-content: center; }
.feature-item { display: flex; align-items: center; gap: 8rpx; background: #f5f7fa; padding: 12rpx 20rpx; border-radius: 12rpx; }
.feature-icon { font-size: 24rpx; }
.feature-text { font-size: 22rpx; color: #666; }
</style>
