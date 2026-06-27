<!--
  我的页面（参考主流APP风格）
  - 渐变头部 + 用户信息
  - 功能菜单分组
  - 设置/退出
-->
<template>
  <view class="page">
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">我的</text>
      </view>
    </view>

    <scroll-view scroll-y class="scroll-area" :style="{ paddingTop: (statusBarHeight + 44) + 'px' }">
      <!-- 用户卡片 -->
      <view class="user-card">
        <view class="user-avatar" @click="chooseAvatar">
          <image v-if="userInfo.avatar" :src="userInfo.avatar" class="avatar-img" mode="aspectFill" />
          <view v-else class="avatar-default">
            <text class="avatar-letter">{{ (userInfo.realName || userInfo.username || '?').charAt(0) }}</text>
          </view>
          <view class="camera-badge">📷</view>
        </view>
        <view class="user-info">
          <text class="user-name">{{ userInfo.realName || userInfo.username }}</text>
          <view class="user-role-tag">
            <text class="role-text">{{ roleName }}</text>
          </view>
        </view>
        <view class="user-id">
          <text class="id-text">ID: {{ userInfo.id }}</text>
        </view>
      </view>

      <!-- 学员专属：学习数据 -->
      <view class="data-card" v-if="userInfo.role === 'STUDENT'">
        <view class="data-item" @click="goTo('/pages/student/progress')">
          <text class="data-num">{{ progressList.length }}</text>
          <text class="data-label">学习科目</text>
        </view>
        <view class="data-divider"></view>
        <view class="data-item" @click="goTo('/pages/lesson/list')">
          <text class="data-num">{{ completedLessons }}</text>
          <text class="data-label">已完成课时</text>
        </view>
        <view class="data-divider"></view>
        <view class="data-item">
          <text class="data-num">{{ overallProgress }}%</text>
          <text class="data-label">总进度</text>
        </view>
      </view>

      <!-- 我的服务 -->
      <view class="menu-group">
        <text class="group-title">我的服务</text>
        <view class="menu-card">
          <view class="menu-item" @click="goTo('/pages/student/progress')" v-if="userInfo.role === 'STUDENT'">
            <view class="menu-icon-wrap" style="background: #E8F0FE"><text class="menu-emoji">📚</text></view>
            <text class="menu-text">学习进度</text>
            <text class="menu-arrow">›</text>
          </view>
          <view class="menu-item" @click="goTo('/pages/lesson/list')">
            <view class="menu-icon-wrap" style="background: #E8F5E9"><text class="menu-emoji">📅</text></view>
            <text class="menu-text">我的课时</text>
            <text class="menu-arrow">›</text>
          </view>
          <view class="menu-item" @click="goTo('/pages/exam/list')">
            <view class="menu-icon-wrap" style="background: #FFF3E0"><text class="menu-emoji">📋</text></view>
            <text class="menu-text">考试安排</text>
            <text class="menu-arrow">›</text>
          </view>
          <view class="menu-item" @click="goTo('/pages/finance/payment')" v-if="userInfo.role === 'STUDENT'">
            <view class="menu-icon-wrap" style="background: #FCE4EC"><text class="menu-emoji">💰</text></view>
            <text class="menu-text">我的缴费</text>
            <text class="menu-arrow">›</text>
          </view>
        </view>
      </view>

      <!-- 更多服务 -->
      <view class="menu-group">
        <text class="group-title">更多服务</text>
        <view class="menu-card">
          <view class="menu-item" @click="goTo('/pages/ai/index')">
            <view class="menu-icon-wrap" style="background: #E8F5E9"><text class="menu-emoji">🤖</text></view>
            <text class="menu-text">AI 助手</text>
            <text class="menu-arrow">›</text>
          </view>
          <view class="menu-item" @click="goTo('/pages/survey/index')" v-if="userInfo.role === 'STUDENT'">
            <view class="menu-icon-wrap" style="background: #F3E5F5"><text class="menu-emoji">⭐</text></view>
            <text class="menu-text">满意度调查</text>
            <text class="menu-arrow">›</text>
          </view>
          <view class="menu-item" @click="goTo('/pages/reminder/index')">
            <view class="menu-icon-wrap" style="background: #E0F7FA"><text class="menu-emoji">⏰</text></view>
            <text class="menu-text">我的提醒</text>
            <text class="menu-arrow">›</text>
          </view>
          <view class="menu-item" @click="goTo('/pages/coach/students')" v-if="userInfo.role === 'COACH'">
            <view class="menu-icon-wrap" style="background: #E8F0FE"><text class="menu-emoji">👥</text></view>
            <text class="menu-text">我的学员</text>
            <text class="menu-arrow">›</text>
          </view>
          <view class="menu-item" @click="showPwdModal = true">
            <view class="menu-icon-wrap" style="background: #FFF8E1"><text class="menu-emoji">🔒</text></view>
            <text class="menu-text">修改密码</text>
            <text class="menu-arrow">›</text>
          </view>
        </view>
      </view>

      <!-- 退出登录 -->
      <view class="logout-section">
        <button class="btn-logout" @click="handleLogout">退出登录</button>
      </view>

      <view style="height: 120rpx;"></view>
    </scroll-view>

    <!-- 修改密码弹窗 -->
    <view v-if="showPwdModal" class="modal-mask" @click.self="showPwdModal = false">
      <view class="modal-box">
        <text class="modal-title">修改密码</text>
        <view class="form-item">
          <text class="form-label">旧密码</text>
          <input v-model="pwdForm.oldPassword" type="text" password class="form-input" placeholder="请输入旧密码" />
        </view>
        <view class="form-item">
          <text class="form-label">新密码</text>
          <input v-model="pwdForm.newPassword" type="text" password class="form-input" placeholder="至少6位" />
        </view>
        <view class="modal-btns">
          <button class="btn-cancel" @click="showPwdModal = false">取消</button>
          <button class="btn-confirm" @click="changePwd">确认</button>
        </view>
      </view>
    </view>
    <FloatingAi />
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../store/user'
import { get } from '../../utils/request'
import { changePassword, uploadAvatar } from '../../utils/api'
import FloatingAi from '../../components/FloatingAi.vue'

const userStore = useUserStore()

/** 当前用户信息（响应式，从store获取） */
const userInfo = computed(() => userStore.userInfo)

/** 角色中文名称映射 */
const roleName = computed(() => ({ ADMIN: '管理员', STAFF: '前台', COACH: '教练', STUDENT: '学员' }[userInfo.value.role] || ''))

/** 状态栏高度，用于自定义导航栏定位 */
const statusBarHeight = ref(0)

/** 学员科目进度列表 */
const progressList = ref([])

/** 已完成课时数（从进度数据中计算已完成的科目数） */
const completedLessons = computed(() => progressList.value.filter(p => p.status === 2).length)

/** 修改密码弹窗显示状态 */
const showPwdModal = ref(false)

/** 修改密码表单 */
const pwdForm = reactive({ oldPassword: '', newPassword: '' })

/**
 * 计算总体学习进度（仅学员角色有效）
 * 遍历所有科目的已学学时和要求学时，计算百分比
 * @returns {number} 进度百分比，0-100
 */
const overallProgress = computed(() => {
  if (!progressList.value.length) return 0
  const total = progressList.value.reduce((s, p) => s + (p.requiredHours || 0), 0)
  const done = progressList.value.reduce((s, p) => s + (p.completedHours || 0), 0)
  return total ? Math.round(done / total * 100) : 0
})

/** TabBar页面路径列表，用于判断跳转方式 */
const tabPages = ['/pages/index/index', '/pages/learn/index', '/pages/booking/index', '/pages/message/index', '/pages/profile/index']

/**
 * 页面跳转
 * @param {string} url - 目标页面路径
 * @description TabBar页面使用switchTab，普通页面使用navigateTo
 */
const goTo = (url) => { tabPages.includes(url) ? uni.switchTab({ url }) : uni.navigateTo({ url }) }

/**
 * 选择并上传头像
 * @description 流程：选择图片 → 校验大小(≤2MB) → 调用上传接口 → 更新本地用户信息
 */
const chooseAvatar = () => {
  uni.chooseImage({
    count: 1, sizeType: ['compressed'], sourceType: ['album', 'camera'],
    success: async (res) => {
      const file = res.tempFiles[0]
      // 校验文件大小，限制2MB
      if (file.size > 2 * 1024 * 1024) return uni.showToast({ title: '图片不能超过2MB', icon: 'none' })
      try {
        const r = await uploadAvatar(file.path)
        // 通过store方法更新用户信息（自动同步到本地存储）
        userStore.updateUserInfo({ avatar: r.data })
        uni.showToast({ title: '头像更新成功', icon: 'success' })
      } catch { uni.showToast({ title: '上传失败', icon: 'none' }) }
    }
  })
}

/**
 * 修改密码
 * @description 校验表单 → 调用修改密码接口 → 成功后退出登录并跳转登录页
 */
const changePwd = async () => {
  // 校验旧密码必填
  if (!pwdForm.oldPassword) return uni.showToast({ title: '请输入旧密码', icon: 'none' })
  // 校验新密码长度
  if (!pwdForm.newPassword || pwdForm.newPassword.length < 6) return uni.showToast({ title: '新密码至少6位', icon: 'none' })
  try {
    await changePassword(pwdForm)
    uni.showToast({ title: '密码已修改，请重新登录', icon: 'success' })
    showPwdModal.value = false
    // 修改成功后退出登录，跳转登录页
    userStore.logout()
    uni.reLaunch({ url: '/pages/login/login' })
  } catch {}
}

/**
 * 退出登录
 * @description 弹出确认弹窗 → 确认后清除登录态并跳转登录页
 */
const handleLogout = () => {
  uni.showModal({
    title: '退出登录', content: '确定退出登录？',
    success: (res) => { if (res.confirm) { userStore.logout(); uni.reLaunch({ url: '/pages/login/login' }) } }
  })
}

/**
 * 页面初始化
 * @description 获取状态栏高度（仅首次）
 */
onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
})

/**
 * 每次显示页面时刷新数据
 * @description 学员角色加载科目进度数据
 */
onShow(async () => {
  if (!userStore.token) { uni.reLaunch({ url: '/pages/login/login' }); return }
  if (userInfo.value.role === 'STUDENT') {
    try { const res = await get('/students/me/progress'); progressList.value = res.data || [] } catch {}
  }
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

/* 用户卡片 */
.user-card {
  background: linear-gradient(135deg, #4A7DFF, #6B9BFF);
  padding: 40rpx 30rpx;
  display: flex; align-items: center; gap: 24rpx;
  border-radius: 0 0 40rpx 40rpx;
}
.user-avatar { position: relative; flex-shrink: 0; }
.avatar-img { width: 100rpx; height: 100rpx; border-radius: 50%; border: 4rpx solid rgba(255,255,255,0.3); }
.avatar-default { width: 100rpx; height: 100rpx; background: rgba(255,255,255,0.2); border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.avatar-letter { font-size: 40rpx; font-weight: 700; color: #fff; }
.camera-badge { position: absolute; bottom: -4rpx; right: -4rpx; width: 36rpx; height: 36rpx; background: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20rpx; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.15); }
.user-info { flex: 1; }
.user-name { display: block; font-size: 36rpx; font-weight: 700; color: #fff; }
.user-role-tag { display: inline-block; background: rgba(255,255,255,0.2); padding: 4rpx 16rpx; border-radius: 12rpx; margin-top: 8rpx; }
.role-text { font-size: 22rpx; color: #fff; }
.user-id { flex-shrink: 0; }
.id-text { font-size: 22rpx; color: rgba(255,255,255,0.6); }

/* 学习数据 */
.data-card {
  margin: -20rpx 30rpx 0; background: #fff; border-radius: 24rpx;
  padding: 30rpx; display: flex; align-items: center;
  box-shadow: 0 8rpx 24rpx rgba(0,0,0,0.06); position: relative; z-index: 1;
}
.data-item { flex: 1; text-align: center; }
.data-num { display: block; font-size: 40rpx; font-weight: 800; color: #4A7DFF; }
.data-label { display: block; font-size: 22rpx; color: #999; margin-top: 4rpx; }
.data-divider { width: 2rpx; height: 60rpx; background: #f0f0f0; }

/* 菜单组 */
.menu-group { padding: 30rpx 30rpx 0; }
.group-title { font-size: 26rpx; font-weight: 600; color: #999; margin-bottom: 16rpx; }
.menu-card { background: #fff; border-radius: 20rpx; overflow: hidden; box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04); }
.menu-item { display: flex; align-items: center; padding: 24rpx; border-bottom: 2rpx solid #f5f5f5; }
.menu-item:last-child { border-bottom: none; }
.menu-icon-wrap {
  width: 60rpx; height: 60rpx; border-radius: 16rpx;
  display: flex; align-items: center; justify-content: center; margin-right: 20rpx;
}
.menu-emoji { font-size: 28rpx; }
.menu-text { flex: 1; font-size: 28rpx; color: #333; }
.menu-arrow { font-size: 28rpx; color: #ccc; }

/* 退出登录 */
.logout-section { padding: 40rpx 30rpx; }
.btn-logout {
  width: 100%; height: 88rpx; line-height: 88rpx;
  background: #fff; color: #FF4757; font-size: 30rpx; font-weight: 600;
  border-radius: 24rpx; border: none;
  box-shadow: 0 2rpx 12rpx rgba(0,0,0,0.04);
}

/* 弹窗 */
.modal-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 999; }
.modal-box { width: 600rpx; background: #fff; border-radius: 24rpx; padding: 40rpx; }
.modal-title { font-size: 32rpx; font-weight: 700; color: #1a1a2e; display: block; margin-bottom: 32rpx; text-align: center; }
.form-item { margin-bottom: 24rpx; }
.form-label { font-size: 26rpx; font-weight: 600; color: #475569; display: block; margin-bottom: 8rpx; }
.form-input { width: 100%; height: 80rpx; padding: 0 20rpx; background: #f8fafc; border-radius: 12rpx; border: 2rpx solid #e8ecf1; font-size: 28rpx; }
.modal-btns { display: flex; gap: 16rpx; margin-top: 32rpx; }
.btn-cancel { flex: 1; height: 80rpx; line-height: 80rpx; background: #f1f5f9; color: #475569; font-size: 28rpx; border-radius: 12rpx; border: none; }
.btn-confirm { flex: 1; height: 80rpx; line-height: 80rpx; background: linear-gradient(135deg, #4A7DFF, #6B9BFF); color: #fff; font-size: 28rpx; font-weight: 600; border-radius: 12rpx; border: none; }
</style>
