<!--
  在线报名页面（小程序端）
  功能：学员填写报名信息、提交报名申请、查询报名状态
-->
<template>
  <view class="page">
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">在线报名</text>
      </view>
    </view>

    <scroll-view scroll-y class="scroll-area" :style="{ paddingTop: (statusBarHeight + 44) + 'px' }">
      <!-- 已有报名记录 -->
      <view v-if="existingReg && existingReg.status !== -1" class="status-card">
        <view class="status-icon-wrap" :class="'status-' + existingReg.status">
          <text class="status-emoji">{{ existingReg.status === 0 ? '⏳' : existingReg.status === 1 ? '✅' : '❌' }}</text>
        </view>
        <text class="status-title">{{ statusText(existingReg.status) }}</text>
        <text class="status-desc">{{ statusDesc(existingReg.status) }}</text>
        <view v-if="existingReg.reviewComment" class="review-comment">
          <text class="comment-label">审核意见：</text>
          <text class="comment-text">{{ existingReg.reviewComment }}</text>
        </view>
        <view v-if="existingReg.status === 1" class="login-info">
          <text class="login-label">您的登录账号：</text>
          <text class="login-value">{{ existingReg.phone }}</text>
          <text class="login-hint">初始密码为身份证后6位</text>
        </view>
      </view>

      <!-- 报名表单 -->
      <view v-else class="form-card">
        <text class="form-title">📝 填写报名信息</text>

        <view class="form-item">
          <text class="form-label">姓名 <text class="required">*</text></text>
          <input v-model="form.name" class="form-input" placeholder="请输入真实姓名" />
        </view>

        <view class="form-item">
          <text class="form-label">身份证号 <text class="required">*</text></text>
          <input v-model="form.idCard" class="form-input" placeholder="请输入18位身份证号" maxlength="18" />
        </view>

        <view class="form-item">
          <text class="form-label">手机号 <text class="required">*</text></text>
          <input v-model="form.phone" class="form-input" placeholder="请输入手机号" maxlength="11" type="number" />
        </view>

        <view class="form-item">
          <text class="form-label">性别</text>
          <view class="radio-group">
            <view class="radio-item" :class="{ active: form.gender === 1 }" @click="form.gender = 1">
              <text class="radio-text">男</text>
            </view>
            <view class="radio-item" :class="{ active: form.gender === 0 }" @click="form.gender = 0">
              <text class="radio-text">女</text>
            </view>
          </view>
        </view>

        <view class="form-item">
          <text class="form-label">出生日期</text>
          <picker mode="date" @change="onBirthdayChange">
            <view class="picker-box">
              <text class="picker-text">{{ form.birthday || '请选择' }}</text>
              <text class="picker-arrow">▸</text>
            </view>
          </picker>
        </view>

        <view class="form-item">
          <text class="form-label">联系地址</text>
          <input v-model="form.address" class="form-input" placeholder="请输入联系地址" />
        </view>

        <view class="form-item">
          <text class="form-label">报考类型 <text class="required">*</text></text>
          <view class="radio-group">
            <view class="radio-item" :class="{ active: form.applyType === 'C1' }" @click="form.applyType = 'C1'">
              <text class="radio-text">C1 手动挡</text>
            </view>
            <view class="radio-item" :class="{ active: form.applyType === 'C2' }" @click="form.applyType = 'C2'">
              <text class="radio-text">C2 自动挡</text>
            </view>
          </view>
        </view>

        <view class="form-item">
          <text class="form-label">报名班型</text>
          <view class="class-grid">
            <view class="class-item" :class="{ active: form.classType === '普通班' }" @click="form.classType = '普通班'">
              <text class="class-name">普通班</text>
              <text class="class-desc">标准培训</text>
            </view>
            <view class="class-item" :class="{ active: form.classType === 'VIP班' }" @click="form.classType = 'VIP班'">
              <text class="class-name">VIP班</text>
              <text class="class-desc">优先预约</text>
            </view>
            <view class="class-item" :class="{ active: form.classType === '计时班' }" @click="form.classType = '计时班'">
              <text class="class-name">计时班</text>
              <text class="class-desc">灵活学时</text>
            </view>
          </view>
        </view>

        <view class="form-item">
          <text class="form-label">备注</text>
          <textarea v-model="form.remark" class="form-textarea" placeholder="其他说明（选填）" />
        </view>

        <button class="btn-submit" @click="submitForm" :loading="submitting">提交报名</button>
      </view>

      <view style="height: 120rpx;"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { post, get } from '../../utils/request'

// ========== 状态变量 ==========

/** 状态栏高度（用于自定义导航栏适配） */
const statusBarHeight = ref(0)

/** 提交中状态，防止重复提交 */
const submitting = ref(false)

/** 已有的报名记录（用于展示审核状态） */
const existingReg = ref(null)

/** 报名表单数据 */
const form = reactive({
  name: '', idCard: '', phone: '', gender: 1,
  birthday: '', address: '', applyType: 'C1', classType: '普通班', remark: ''
})

// ========== 工具函数 ==========

/**
 * 获取报名状态文本
 * @param {number|string} s - 状态码：-1-未找到 0-审核中 1-审核通过 2-审核未通过
 * @returns {string} 状态对应的中文文本
 */
const statusText = (s) => ({ '-1': '未找到报名', 0: '审核中', 1: '审核通过', 2: '审核未通过' }[s] || '')

/**
 * 获取报名状态描述
 * @param {number|string} s - 状态码
 * @returns {string} 状态对应的详细描述文本
 */
const statusDesc = (s) => ({
  '-1': '该手机号暂无报名记录，请先提交报名申请',
  0: '您的报名信息已提交，请耐心等待工作人员审核',
  1: '恭喜！您的报名已通过，请使用手机号登录小程序',
  2: '很抱歉，您的报名未通过，请根据审核意见修改后重新提交'
}[s] || '')

// ========== 事件处理 ==========

/**
 * 出生日期选择变更
 * @param {Object} e - picker组件的change事件对象
 */
const onBirthdayChange = (e) => { form.birthday = e.detail.value }

// ========== 数据操作 ==========

/**
 * 提交报名表单
 * @description 校验必填项 → 调用接口提交报名 → 成功后保存手机号到本地存储并查询报名状态
 */
const submitForm = async () => {
  if (!form.name) return uni.showToast({ title: '请输入姓名', icon: 'none' })
  if (!form.idCard || form.idCard.length !== 18) return uni.showToast({ title: '请输入正确的身份证号', icon: 'none' })
  if (!form.phone || form.phone.length !== 11) return uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
  if (!form.applyType) return uni.showToast({ title: '请选择报考类型', icon: 'none' })

  submitting.value = true
  try {
    await post('/registrations', form)
    uni.showToast({ title: '报名提交成功', icon: 'success' })
    // 保存手机号，下次打开自动查询
    uni.setStorageSync('registrationPhone', form.phone)
    loadExistingReg()
  } catch {} finally { submitting.value = false }
}

/**
 * 加载已有报名记录
 * @description 根据手机号查询报名状态，用于展示审核结果
 */
const loadExistingReg = async () => {
  if (form.phone) {
    try {
      const res = await get('/registrations/status', { phone: form.phone })
      existingReg.value = res.data
    } catch {}
  }
}

// ========== 生命周期 ==========

/** 页面挂载：获取状态栏高度，尝试从本地存储恢复手机号并查询报名状态 */
onMounted(async () => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  // 尝试从本地存储恢复手机号，查询已有报名
  const savedPhone = uni.getStorageSync('registrationPhone')
  if (savedPhone) {
    form.phone = savedPhone
    loadExistingReg()
  }
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f7fa; }
.nav-bar { position: fixed; top: 0; left: 0; right: 0; z-index: 100; background: linear-gradient(135deg, #4A7DFF, #6B9BFF); }
.nav-content { height: 44px; display: flex; align-items: center; justify-content: center; }
.nav-title { font-size: 34rpx; font-weight: 700; color: #fff; }
.scroll-area { height: 100vh; }

/* 状态卡片 */
.status-card {
  margin: 30rpx; background: #fff; border-radius: 24rpx; padding: 40rpx;
  text-align: center; box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.status-icon-wrap { width: 120rpx; height: 120rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 20rpx; }
.status-0 { background: #FFF8E1; }
.status-1 { background: #E8F5E9; }
.status-2 { background: #FFF0F0; }
.status-emoji { font-size: 60rpx; }
.status-title { display: block; font-size: 36rpx; font-weight: 700; color: #1a1a2e; margin-bottom: 12rpx; }
.status-desc { display: block; font-size: 26rpx; color: #999; margin-bottom: 24rpx; }
.review-comment { background: #f5f7fa; border-radius: 12rpx; padding: 20rpx; text-align: left; margin-bottom: 20rpx; }
.comment-label { font-size: 24rpx; color: #666; }
.comment-text { font-size: 26rpx; color: #333; }
.login-info { background: #E8F5E9; border-radius: 12rpx; padding: 20rpx; }
.login-label { display: block; font-size: 24rpx; color: #666; }
.login-value { display: block; font-size: 32rpx; font-weight: 700; color: #4A7DFF; margin: 8rpx 0; }
.login-hint { display: block; font-size: 22rpx; color: #999; }

/* 表单 */
.form-card { margin: 30rpx; background: #fff; border-radius: 24rpx; padding: 30rpx; box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04); }
.form-title { display: block; font-size: 32rpx; font-weight: 700; color: #1a1a2e; margin-bottom: 30rpx; }
.form-item { margin-bottom: 28rpx; }
.form-label { display: block; font-size: 26rpx; font-weight: 600; color: #475569; margin-bottom: 12rpx; }
.required { color: #FF4757; }
.form-input { width: 100%; height: 80rpx; padding: 0 24rpx; background: #f5f7fa; border-radius: 12rpx; border: 2rpx solid #e8ecf1; font-size: 28rpx; }
.form-textarea { width: 100%; height: 160rpx; padding: 20rpx 24rpx; background: #f5f7fa; border-radius: 12rpx; border: 2rpx solid #e8ecf1; font-size: 28rpx; }
.picker-box { display: flex; justify-content: space-between; align-items: center; height: 80rpx; padding: 0 24rpx; background: #f5f7fa; border-radius: 12rpx; border: 2rpx solid #e8ecf1; }
.picker-text { font-size: 28rpx; color: #333; }
.picker-arrow { font-size: 24rpx; color: #999; }

/* 单选 */
.radio-group { display: flex; gap: 16rpx; }
.radio-item { flex: 1; height: 72rpx; display: flex; align-items: center; justify-content: center; background: #f5f7fa; border-radius: 12rpx; border: 3rpx solid transparent; }
.radio-item.active { border-color: #4A7DFF; background: #F0F4FF; }
.radio-text { font-size: 26rpx; color: #333; }
.radio-item.active .radio-text { color: #4A7DFF; font-weight: 600; }

/* 班型选择 */
.class-grid { display: flex; gap: 16rpx; }
.class-item { flex: 1; padding: 20rpx; background: #f5f7fa; border-radius: 12rpx; border: 3rpx solid transparent; text-align: center; }
.class-item.active { border-color: #4A7DFF; background: #F0F4FF; }
.class-name { display: block; font-size: 26rpx; font-weight: 600; color: #333; }
.class-desc { display: block; font-size: 20rpx; color: #999; margin-top: 4rpx; }
.class-item.active .class-name { color: #4A7DFF; }

.btn-submit {
  width: 100%; height: 96rpx; line-height: 96rpx;
  background: linear-gradient(135deg, #4A7DFF, #6B9BFF);
  color: #fff; font-size: 32rpx; font-weight: 700;
  border-radius: 20rpx; border: none;
  box-shadow: 0 8rpx 24rpx rgba(74,125,255,0.3);
  margin-top: 16rpx;
}
</style>
