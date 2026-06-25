<!--
  视频播放页面
-->
<template>
  <view class="page">
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">{{ title || '视频播放' }}</text>
      </view>
    </view>
    <view class="player-area" :style="{ paddingTop: (statusBarHeight + 44) + 'px' }">
      <video v-if="videoUrl" :src="videoUrl" class="video-player" controls autoplay show-fullscreen-btn show-play-btn />
      <view v-else class="empty">
        <text class="empty-text">视频地址无效</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

// ========== 响应式状态 ==========

/** @description 系统状态栏高度（px），用于自定义导航栏定位 */
const statusBarHeight = ref(0)

/** @description 视频播放地址 */
const videoUrl = ref('')

/** @description 视频标题，显示在导航栏 */
const title = ref('')

// ========== 生命周期 ==========

/**
 * @description 页面加载时读取路由参数，解码并赋值视频地址和标题
 * @description 使用 onLoad 而非 onMounted 读取页面参数：uni-app 中 onLoad 才能正确接收页面跳转传递的 options
 * @param {Object} options - 路由参数对象
 * @param {string} options.url - 视频地址（URL 编码）
 * @param {string} options.title - 视频标题（URL 编码）
 * @returns {void}
 */
onLoad((options) => {
  videoUrl.value = decodeURIComponent(options.url || '')
  title.value = decodeURIComponent(options.title || '')
})

/** @description 页面挂载时获取系统状态栏高度 */
onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
})
</script>

<style scoped>
.page { min-height: 100vh; background: #000; }
.nav-bar { position: fixed; top: 0; left: 0; right: 0; z-index: 100; background: #000; }
.nav-content { height: 44px; display: flex; align-items: center; justify-content: center; }
.nav-title { font-size: 30rpx; font-weight: 600; color: #fff; }
.player-area { width: 100%; display: flex; align-items: center; justify-content: center; min-height: 400rpx; }
.video-player { width: 100%; }
.empty { text-align: center; padding: 100rpx 0; }
.empty-text { font-size: 28rpx; color: #999; }
</style>
