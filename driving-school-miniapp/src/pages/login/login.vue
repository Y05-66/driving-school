<!--
  登录页面
  - 仅允许学员和教练登录
  - 管理员/前台提示使用PC端
  - 演示账号快捷填充
-->
<template>
  <view class="page">
    <!-- 背景装饰 -->
    <view class="bg-top"></view>
    <view class="bg-circle circle-1"></view>
    <view class="bg-circle circle-2"></view>

    <view class="content">
      <!-- Logo区域 -->
      <view class="logo-section">
        <view class="logo-icon">
          <text class="logo-emoji">🚗</text>
        </view>
        <text class="logo-title">驾校管理</text>
        <text class="logo-subtitle">学车无忧，轻松拿证</text>
      </view>

      <!-- 登录表单 -->
      <view class="form-card">
        <text class="form-title">欢迎登录</text>

        <view class="input-group">
          <view class="input-wrap" :class="{ focus: focusUser }">
            <text class="input-icon">👤</text>
            <input v-model="form.username" class="input-field" placeholder="请输入用户名"
              @focus="focusUser = true" @blur="focusUser = false" />
          </view>
        </view>

        <view class="input-group">
          <view class="input-wrap" :class="{ focus: focusPass }">
            <text class="input-icon">🔒</text>
            <input v-model="form.password" class="input-field" type="password" placeholder="请输入密码"
              @focus="focusPass = true" @blur="focusPass = false" />
          </view>
        </view>

        <button class="btn-login" :loading="loading" @click="handleLogin">
          {{ loading ? '登录中...' : '登 录' }}
        </button>

        <!-- 提示 -->
        <view class="tip-section">
          <text class="tip-text">💡 本小程序仅支持学员和教练登录</text>
          <text class="tip-text">管理员请使用电脑端管理系统</text>
        </view>

        <!-- 报名入口 -->
        <view class="register-link" @click="goToRegister">
          <text class="register-text">还没有账号？</text>
          <text class="register-btn">立即报名</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useUserStore } from '../../store/user'

// ========== 状态管理 ==========

/** @description 用户状态仓库实例 */
const userStore = useUserStore()

/** @description 登录按钮加载状态标识 */
const loading = ref(false)

/** @description 用户名输入框是否获得焦点 */
const focusUser = ref(false)

/** @description 密码输入框是否获得焦点 */
const focusPass = ref(false)

/** @description 登录表单数据 */
const form = reactive({ username: '', password: '' })

// ========== 页面跳转 ==========

/**
 * @description 跳转到报名注册页面
 * @returns {void}
 */
const goToRegister = () => {
  uni.navigateTo({ url: '/pages/registration/index' })
}

// ========== 登录操作 ==========

/**
 * @description 处理登录逻辑：校验表单、调用登录接口、角色校验（仅允许学员和教练）、跳转首页
 * @returns {Promise<void>}
 */
const handleLogin = async () => {
  if (!form.username || !form.password) {
    uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
    return
  }

  loading.value = true
  try {
    await userStore.login(form.username, form.password)

    // 检查角色，只允许学员和教练
    const role = userStore.userInfo.role
    if (role !== 'STUDENT' && role !== 'COACH') {
      // 清除登录态
      userStore.logout()
      uni.showModal({
        title: '温馨提示',
        content: '管理员和前台工作人员请使用电脑端管理系统\n\n小程序仅支持学员和教练登录',
        showCancel: false,
        confirmText: '我知道了'
      })
      return
    }

    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 500)
  } catch (e) {
    uni.showToast({ title: e.message || '登录失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f7fa; position: relative; overflow: hidden; }

/* 背景装饰 */
.bg-top { position: absolute; top: 0; left: 0; right: 0; height: 500rpx; background: linear-gradient(135deg, #4A7DFF, #6B9BFF); border-radius: 0 0 60rpx 60rpx; }
.bg-circle { position: absolute; border-radius: 50%; opacity: 0.1; background: #fff; }
.circle-1 { width: 300rpx; height: 300rpx; top: -60rpx; right: -60rpx; }
.circle-2 { width: 200rpx; height: 200rpx; top: 200rpx; left: -60rpx; }

.content { position: relative; z-index: 1; padding: 0 40rpx; }

/* Logo */
.logo-section { text-align: center; padding: 120rpx 0 60rpx; }
.logo-icon { width: 120rpx; height: 120rpx; background: rgba(255,255,255,0.2); border-radius: 30rpx; display: flex; align-items: center; justify-content: center; margin: 0 auto 20rpx; }
.logo-emoji { font-size: 60rpx; }
.logo-title { display: block; font-size: 44rpx; font-weight: 800; color: #fff; }
.logo-subtitle { display: block; font-size: 26rpx; color: rgba(255,255,255,0.8); margin-top: 8rpx; }

/* 表单卡片 */
.form-card { background: #fff; border-radius: 32rpx; padding: 50rpx 40rpx; box-shadow: 0 20rpx 60rpx rgba(0,0,0,0.08); }
.form-title { display: block; font-size: 36rpx; font-weight: 700; color: #1a1a2e; margin-bottom: 40rpx; }

.input-group { margin-bottom: 28rpx; }
.input-wrap {
  display: flex; align-items: center; gap: 16rpx;
  background: #f5f7fa; border-radius: 16rpx; padding: 24rpx;
  border: 3rpx solid transparent; transition: all 0.3s;
}
.input-wrap.focus { border-color: #4A7DFF; background: #F0F4FF; }
.input-icon { font-size: 32rpx; }
.input-field { flex: 1; font-size: 28rpx; color: #1a1a2e; }

.btn-login {
  width: 100%; height: 96rpx; line-height: 96rpx;
  background: linear-gradient(135deg, #4A7DFF, #6B9BFF);
  color: #fff; font-size: 32rpx; font-weight: 700;
  border-radius: 20rpx; border: none;
  box-shadow: 0 8rpx 24rpx rgba(74,125,255,0.3);
  margin-top: 16rpx;
}
.btn-login:active { transform: scale(0.98); }

.tip-section { margin-top: 24rpx; text-align: center; }
.tip-text { display: block; font-size: 22rpx; color: #999; line-height: 1.6; }

/* 报名入口 */
.register-link { margin-top: 30rpx; display: flex; align-items: center; justify-content: center; gap: 8rpx; }
.register-text { font-size: 26rpx; color: #999; }
.register-btn { font-size: 26rpx; color: #4A7DFF; font-weight: 600; }

</style>
