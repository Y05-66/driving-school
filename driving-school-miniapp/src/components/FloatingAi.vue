<!--
  AI 助手浮动按钮（可拖拽）
  放在任意页面中，显示一个悬浮的 AI 入口按钮，支持自由拖拽，点击跳转 AI 助手页面
-->
<template>
  <view
    class="floating-ai"
    :style="{ left: x + 'px', top: y + 'px' }"
    @touchstart="onTouchStart"
    @touchmove.prevent="onTouchMove"
    @touchend="onTouchEnd"
    @click="goAi"
  >
    <text class="floating-ai-icon">🤖</text>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const x = ref(0)
const y = ref(0)
let startX = 0
let startY = 0
let moveX = 0
let moveY = 0
let moved = false

onMounted(() => {
  const info = uni.getSystemInfoSync()
  // 初始位置：右下角，留出安全区域
  x.value = info.windowWidth - 68
  y.value = info.windowHeight - 220
})

const onTouchStart = (e) => {
  const touch = e.touches[0]
  startX = touch.clientX
  startY = touch.clientY
  moveX = x.value
  moveY = y.value
  moved = false
}

const onTouchMove = (e) => {
  const touch = e.touches[0]
  const dx = touch.clientX - startX
  const dy = touch.clientY - startY
  // 移动超过 5px 才算拖拽
  if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
    moved = true
  }
  x.value = moveX + dx
  y.value = moveY + dy
}

const onTouchEnd = () => {
  // 吸附到最近的左/右边缘
  const info = uni.getSystemInfoSync()
  const mid = info.windowWidth / 2
  if (x.value + 34 > mid) {
    // 吸附右边
    x.value = info.windowWidth - 68
  } else {
    // 吸附左边
    x.value = 12
  }
  // 限制上下边界
  if (y.value < info.statusBarHeight + 10) {
    y.value = info.statusBarHeight + 10
  }
  if (y.value > info.windowHeight - 80) {
    y.value = info.windowHeight - 80
  }
}

const goAi = () => {
  // 拖拽时不跳转
  if (moved) return
  uni.navigateTo({ url: '/pages/ai/index' })
}
</script>

<style scoped>
.floating-ai {
  position: fixed;
  width: 68rpx;
  height: 68rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #4A7DFF, #6B9BFF);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(74, 125, 255, 0.4);
  z-index: 999;
}

.floating-ai-icon {
  font-size: 36rpx;
}
</style>
