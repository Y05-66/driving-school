<!--
  我的缴费页面
  功能：查看缴费记录、缴费详情
-->
<template>
  <view class="container">
    <!-- 统计 -->
    <view class="summary-card">
      <view class="summary-item">
        <text class="summary-num">¥{{ totalPaid }}</text>
        <text class="summary-label">已缴金额</text>
      </view>
      <view class="summary-divider"></view>
      <view class="summary-item">
        <text class="summary-num">{{ paymentList.length }}</text>
        <text class="summary-label">缴费次数</text>
      </view>
    </view>

    <!-- 列表 -->
    <view v-if="paymentList.length === 0" class="empty">
      <text class="empty-text">暂无缴费记录</text>
    </view>
    <view v-for="p in paymentList" :key="p.id" class="pay-card">
      <view class="pay-top">
        <view class="pay-type-tag" :class="'type-' + p.type">
          <text class="pay-type-text">{{ payTypeText(p.type) }}</text>
        </view>
        <text class="pay-amount" :class="{ 'text-red': p.type === 2 }">{{ p.type === 2 ? '-' : '+' }}¥{{ p.amount }}</text>
      </view>
      <view class="pay-info">
        <text class="pay-info-item">📅 {{ p.createTime }}</text>
        <text class="pay-info-item">💳 {{ p.payMethod || '-' }}</text>
      </view>
      <view v-if="p.receiptNo" class="pay-receipt">
        <text class="receipt-text">票据号：{{ p.receiptNo }}</text>
      </view>
    </view>
  </view>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import { get } from '../../utils/request'

// ========== 状态变量 ==========

/** 缴费记录列表 */
const paymentList = ref([])

/** 已缴金额合计（排除退费类型，保留两位小数） */
const totalPaid = computed(() => paymentList.value.filter(p => p.type !== 2).reduce((s, p) => s + Number(p.amount || 0), 0).toFixed(2))

// ========== 工具函数 ==========

/**
 * 获取缴费类型文本
 * @param {number} t - 缴费类型：1-报名费 2-退费 3-补考费 4-其他
 * @returns {string} 类型对应的中文文本
 */
const payTypeText = (t) => ({ 1: '报名费', 2: '退费', 3: '补考费', 4: '其他' }[t] || '其他')

// ========== 生命周期 ==========

/** 页面挂载：加载当前学员的缴费记录 */
onMounted(async () => {
  try { const res = await get('/finance/payments/my'); paymentList.value = res.data || [] } catch {}
})
</script>
<style scoped>
.container { padding: 24rpx; min-height: 100vh; background: #f5f7fa; }
.summary-card {
  background: linear-gradient(135deg, #6366f1, #818cf8); border-radius: 20rpx;
  padding: 36rpx; display: flex; align-items: center; margin-bottom: 24rpx;
}
.summary-item { flex: 1; text-align: center; }
.summary-num { display: block; font-size: 40rpx; font-weight: 800; color: #fff; }
.summary-label { display: block; font-size: 24rpx; color: rgba(255,255,255,0.8); margin-top: 4rpx; }
.summary-divider { width: 2rpx; height: 60rpx; background: rgba(255,255,255,0.3); }
.empty { text-align: center; padding: 80rpx 0; }
.empty-text { font-size: 28rpx; color: #94a3b8; }
.pay-card { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 16rpx; box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.04); }
.pay-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.pay-type-tag { padding: 4rpx 14rpx; border-radius: 8rpx; }
.type-1 { background: #eef2ff; } .type-1 .pay-type-text { color: #6366f1; }
.type-2 { background: #fef2f2; } .type-2 .pay-type-text { color: #ef4444; }
.type-3 { background: #fffbeb; } .type-3 .pay-type-text { color: #f59e0b; }
.type-4 { background: #f1f5f9; } .type-4 .pay-type-text { color: #64748b; }
.pay-type-text { font-size: 22rpx; font-weight: 600; }
.pay-amount { font-size: 32rpx; font-weight: 800; color: #10b981; }
.text-red { color: #ef4444; }
.pay-info { display: flex; gap: 24rpx; }
.pay-info-item { font-size: 24rpx; color: #94a3b8; }
.pay-receipt { margin-top: 8rpx; }
.receipt-text { font-size: 22rpx; color: #cbd5e1; }
</style>
