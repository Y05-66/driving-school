<!--
  应用根组件
  功能：全局生命周期管理 + 全局样式定义
  优化：统一设计语言、圆角、阴影、动画
-->
<script setup>
import { onLaunch } from '@dcloudio/uni-app'
import { useUserStore } from './store/user'

onLaunch(() => {
  const userStore = useUserStore()
  if (userStore.token) {
    userStore.fetchUserInfo().catch(() => {
      // handleUnauthorized 已在请求拦截器中处理清除存储和重定向
    })
  }
})
</script>

<style>
/* ====== 全局基础样式 ====== */
page {
  background-color: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
  font-size: 28rpx;
  color: #1a1a2e;
  line-height: 1.6;
}

/* 页面容器 */
.container {
  padding: 24rpx;
  min-height: 100vh;
  box-sizing: border-box;
}

/* ====== 卡片系统 ====== */
.card {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
  border: 1rpx solid rgba(0,0,0,0.02);
}

.card-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 24rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

/* ====== 按钮系统 ====== */
.btn-primary {
  background: linear-gradient(135deg, #6366f1 0%, #818cf8 100%);
  color: #fff;
  border: none;
  border-radius: 16rpx;
  font-size: 28rpx;
  font-weight: 600;
  height: 88rpx;
  line-height: 88rpx;
  letter-spacing: 2rpx;
  box-shadow: 0 8rpx 24rpx rgba(99,102,241,0.3);
}
.btn-primary::after { border: none; }
.btn-primary:active {
  transform: scale(0.98);
  box-shadow: 0 4rpx 12rpx rgba(99,102,241,0.2);
}

/* ====== 标签系统 ====== */
.tag {
  display: inline-block;
  padding: 6rpx 20rpx;
  border-radius: 24rpx;
  font-size: 22rpx;
  font-weight: 500;
}
.tag-primary { background: #eef2ff; color: #6366f1; }
.tag-success { background: #ecfdf5; color: #10b981; }
.tag-warning { background: #fffbeb; color: #f59e0b; }
.tag-danger { background: #fef2f2; color: #ef4444; }
.tag-info { background: #f1f5f9; color: #64748b; }

/* ====== 空状态 ====== */
.empty {
  text-align: center;
  padding: 120rpx 60rpx;
}
.empty-icon {
  display: block;
  font-size: 100rpx;
  margin-bottom: 24rpx;
  opacity: 0.6;
}
.empty-text {
  font-size: 28rpx;
  color: #94a3b8;
  line-height: 1.5;
}

/* ====== 加载动画 ====== */
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
.loading-pulse {
  animation: pulse 1.5s ease-in-out infinite;
}
</style>
