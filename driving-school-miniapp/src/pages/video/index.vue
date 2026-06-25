<!--
  视频教学页面
  功能：按科目和分类浏览教学视频
-->
<template>
  <view class="page">
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">视频教学</text>
      </view>
    </view>

    <scroll-view scroll-y class="scroll-area" :style="{ paddingTop: (statusBarHeight + 44) + 'px' }">
      <!-- 科目切换 -->
      <view class="subject-tabs">
        <view class="tab-item" :class="{ active: subject === '科目二' }" @click="switchSubject('科目二')">
          <text class="tab-text">科目二</text>
        </view>
        <view class="tab-item" :class="{ active: subject === '科目三' }" @click="switchSubject('科目三')">
          <text class="tab-text">科目三</text>
        </view>
      </view>

      <!-- 分类筛选 -->
      <scroll-view scroll-x class="category-scroll">
        <view class="category-list">
          <view class="category-item" :class="{ active: !selectedCategory }" @click="selectedCategory = ''; loadVideos()">
            <text class="category-text">全部</text>
          </view>
          <view v-for="c in categories" :key="c" class="category-item" :class="{ active: selectedCategory === c }" @click="selectedCategory = c; loadVideos()">
            <text class="category-text">{{ c }}</text>
          </view>
        </view>
      </scroll-view>

      <!-- 视频列表 -->
      <view class="video-list">
        <view v-if="videoList.length === 0" class="empty">
          <text class="empty-icon">📹</text>
          <text class="empty-text">暂无视频</text>
        </view>

        <view v-for="v in videoList" :key="v.id" class="video-card" @click="playVideo(v)">
          <view class="video-cover">
            <image v-if="v.coverUrl" :src="v.coverUrl" class="cover-img" mode="aspectFill" />
            <view v-else class="cover-default">
              <text class="cover-emoji">🎬</text>
            </view>
            <view class="play-icon">
              <text class="play-emoji">▶</text>
            </view>
            <view v-if="v.duration" class="duration-badge">
              <text class="duration-text">{{ formatDuration(v.duration) }}</text>
            </view>
          </view>
          <view class="video-info">
            <text class="video-title">{{ v.title }}</text>
            <view class="video-meta">
              <text class="meta-category">{{ v.category }}</text>
              <text class="meta-views">{{ v.viewCount || 0 }} 次播放</text>
            </view>
          </view>
        </view>
      </view>

      <view style="height: 120rpx;"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { get } from '../../utils/request'

// ========== 状态变量 ==========

/** 状态栏高度（用于自定义导航栏适配） */
const statusBarHeight = ref(0)

/** 当前选择的科目：科目二/科目三 */
const subject = ref('科目二')

/** 当前选中的分类筛选，空字符串表示全部 */
const selectedCategory = ref('')

/** 当前科目下的视频分类列表 */
const categories = ref([])

/** 视频列表 */
const videoList = ref([])

// ========== 工具函数 ==========

/**
 * 格式化视频时长
 * @param {number} seconds - 视频时长（秒）
 * @returns {string} 格式化后的时长，如 "3:05"
 */
const formatDuration = (seconds) => {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

// ========== 交互操作 ==========

/**
 * 切换科目
 * @param {string} s - 科目名称：'科目二' 或 '科目三'
 * @description 重置分类筛选，重新加载分类和视频列表
 */
const switchSubject = (s) => {
  subject.value = s
  selectedCategory.value = ''
  loadCategories()
  loadVideos()
}

// ========== 数据操作 ==========

/**
 * 加载视频分类列表
 * @description 根据当前科目从后端获取分类列表
 */
const loadCategories = async () => {
  try {
    const res = await get('/videos/categories', { subject: subject.value })
    categories.value = res.data || []
  } catch {}
}

/**
 * 加载视频列表
 * @description 根据当前科目和分类筛选条件获取视频列表
 */
const loadVideos = async () => {
  try {
    const params = { subject: subject.value, pageNum: 1, pageSize: 50 }
    if (selectedCategory.value) params.category = selectedCategory.value
    const res = await get('/videos', params)
    videoList.value = res.data?.records || []
  } catch {}
}

/**
 * 播放视频
 * @param {Object} v - 视频对象
 * @description 跳转到视频播放页面，视频地址无效时提示用户
 */
const playVideo = (v) => {
  if (v.videoUrl) {
    uni.navigateTo({ url: `/pages/video/play?url=${encodeURIComponent(v.videoUrl)}&title=${encodeURIComponent(v.title || '')}` })
  } else {
    uni.showToast({ title: '视频地址无效', icon: 'none' })
  }
}

// ========== 生命周期 ==========

/** 页面挂载：获取状态栏高度，加载分类和视频列表 */
onMounted(async () => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  loadCategories()
  loadVideos()
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f7fa; }
.nav-bar { position: fixed; top: 0; left: 0; right: 0; z-index: 100; background: linear-gradient(135deg, #4A7DFF, #6B9BFF); }
.nav-content { height: 44px; display: flex; align-items: center; justify-content: center; }
.nav-title { font-size: 34rpx; font-weight: 700; color: #fff; }
.scroll-area { height: 100vh; }

/* 科目切换 */
.subject-tabs { display: flex; gap: 20rpx; padding: 20rpx 30rpx; }
.tab-item { flex: 1; height: 72rpx; line-height: 72rpx; text-align: center; background: #fff; border-radius: 16rpx; border: 3rpx solid transparent; }
.tab-item.active { border-color: #4A7DFF; background: #F0F4FF; }
.tab-text { font-size: 28rpx; font-weight: 600; color: #333; }
.tab-item.active .tab-text { color: #4A7DFF; }

/* 分类筛选 */
.category-scroll { padding: 0 30rpx; white-space: nowrap; margin-bottom: 20rpx; }
.category-list { display: inline-flex; gap: 12rpx; }
.category-item { padding: 10rpx 24rpx; background: #fff; border-radius: 20rpx; }
.category-item.active { background: #4A7DFF; }
.category-text { font-size: 24rpx; color: #666; }
.category-item.active .category-text { color: #fff; }

/* 视频列表 */
.video-list { padding: 0 30rpx; }
.empty { text-align: center; padding: 80rpx 0; }
.empty-icon { font-size: 60rpx; display: block; margin-bottom: 12rpx; }
.empty-text { font-size: 26rpx; color: #999; }

.video-card { background: #fff; border-radius: 20rpx; overflow: hidden; margin-bottom: 20rpx; box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.04); }
.video-cover { position: relative; height: 320rpx; background: #000; }
.cover-img { width: 100%; height: 100%; }
.cover-default { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #1a1a2e, #2d2d44); }
.cover-emoji { font-size: 80rpx; }
.play-icon { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); width: 80rpx; height: 80rpx; background: rgba(255,255,255,0.3); border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.play-emoji { font-size: 36rpx; color: #fff; }
.duration-badge { position: absolute; bottom: 12rpx; right: 12rpx; background: rgba(0,0,0,0.6); padding: 4rpx 12rpx; border-radius: 8rpx; }
.duration-text { font-size: 22rpx; color: #fff; }
.video-info { padding: 20rpx; }
.video-title { display: block; font-size: 28rpx; font-weight: 600; color: #1a1a2e; margin-bottom: 8rpx; }
.video-meta { display: flex; gap: 16rpx; }
.meta-category { font-size: 22rpx; color: #4A7DFF; background: #F0F4FF; padding: 4rpx 12rpx; border-radius: 8rpx; }
.meta-views { font-size: 22rpx; color: #999; }
</style>
