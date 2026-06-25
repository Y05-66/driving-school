<!--
  @description 仪表盘页面
  系统首页，展示核心业务数据概览，根据用户角色显示不同内容：
  - 管理员/前台：总学员数、在学学员、本月收入、通过率 + 收入趋势柱状图 + 车辆使用率饼图
  - 教练/学员：今日课程、待上课时、已完成、未读通知

  数据来源：/statistics/dashboard 接口（根据角色返回不同数据）
  图表使用 ECharts 渲染，包含响应式自适应
-->

<template>
  <div class="dashboard" v-loading="loading">
    <!-- 欢迎语区域：根据当前时间显示不同问候语 -->
    <div class="welcome-bar">
      <div>
        <h2>你好，{{ userInfo.realName || userInfo.username }} 👋</h2>
        <p>{{ greeting }}，欢迎使用驾校管理系统</p>
      </div>
    </div>

    <!-- ==================== 统计卡片区域 ==================== -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :xs="12" :sm="6" v-for="card in statCards" :key="card.title">
        <div class="stat-card">
          <div class="stat-info">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-title">{{ card.title }}</div>
          </div>
          <!-- 图标容器：不同统计项使用不同背景色和图标色 -->
          <div class="stat-icon" :style="{ background: card.bg, color: card.color }">
            <el-icon :size="22"><component :is="card.icon" /></el-icon>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- ==================== 图表区域 ==================== -->
    <el-row :gutter="16" style="margin-top:16px">
      <!-- 收入趋势图（柱状图） -->
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="card-title">收入趋势</span></template>
          <div ref="revenueChart" style="height:320px"></div>
        </el-card>
      </el-col>
      <!-- 车辆使用率图（环形饼图） -->
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="card-title">车辆使用率</span></template>
          <div ref="vehicleChart" style="height:320px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useUserStore } from '../../store/user'
import { getDashboard, getRevenueTrend, getVehicleUsage } from '../../api'
import * as echarts from 'echarts'

/** @description 用户状态 Store */
const userStore = useUserStore()
/** @description 页面加载状态 */
const loading = ref(true)
/** @description 仪表盘数据（从后端获取） */
const dashboard = ref({})
/** @description 收入趋势图表 DOM 引用 */
const revenueChart = ref()
/** @description 车辆使用率图表 DOM 引用 */
const vehicleChart = ref()
/** @description ECharts 图表实例（用于销毁） */
let revenueChartInstance = null
let vehicleChartInstance = null
/** @description 窗口 resize 事件处理函数（用于移除监听） */
let resizeHandler = null

/** @description 当前用户信息 */
const userInfo = computed(() => userStore.userInfo)
/** @description 当前用户角色 */
const role = computed(() => userStore.userInfo.role)

/**
 * @description 根据当前时间生成问候语
 * 凌晨提醒休息，白天分时段问候，深夜提醒休息
 */
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨了，注意休息'
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 17) return '下午好'
  if (h < 22) return '晚上好'
  return '夜深了，注意休息'
})

/**
 * @description 统计卡片数据（计算属性）
 * 根据角色返回不同的统计数据：
 * - 管理员/前台：总学员数、在学学员、本月收入、通过率
 * - 其他角色（教练/学员）：今日课程、待上课时、已完成、未读通知
 */
const statCards = computed(() => {
  const d = dashboard.value
  if (role.value === 'ADMIN' || role.value === 'STAFF') {
    return [
      { title: '总学员数', value: d.overview?.totalStudents || d.totalStudents || 0, icon: 'User', color: '#6366f1', bg: '#eef2ff' },
      { title: '在学学员', value: d.overview?.studyingStudents || 0, icon: 'UserFilled', color: '#10b981', bg: '#ecfdf5' },
      { title: '本月收入', value: '¥' + (d.overview?.monthRevenue || 0), icon: 'Money', color: '#f59e0b', bg: '#fffbeb' },
      { title: '通过率', value: (d.overview?.passRate || 0) + '%', icon: 'TrendCharts', color: '#ef4444', bg: '#fef2f2' }
    ]
  }
  if (role.value === 'COACH') {
    return [
      { title: '今日课程', value: d.todayLessons || 0, icon: 'Calendar', color: '#6366f1', bg: '#eef2ff' },
      { title: '名下学员', value: d.studentCount || 0, icon: 'User', color: '#10b981', bg: '#ecfdf5' },
      { title: '评分', value: d.rating || '-', icon: 'Star', color: '#f59e0b', bg: '#fffbeb' },
      { title: '教练姓名', value: d.coachName || '-', icon: 'UserFilled', color: '#ef4444', bg: '#fef2f2' }
    ]
  }
  // STUDENT
  return [
    { title: '待上课时', value: d.pendingLessons || 0, icon: 'Clock', color: '#6366f1', bg: '#eef2ff' },
    { title: '已完成', value: d.completedLessons || 0, icon: 'CircleCheck', color: '#10b981', bg: '#ecfdf5' },
    { title: '报名类型', value: d.applyType || '-', icon: 'Document', color: '#f59e0b', bg: '#fffbeb' },
    { title: '状态', value: ['待审核','在学','已毕业','已退学'][d.status] || '-', icon: 'InfoFilled', color: '#ef4444', bg: '#fef2f2' }
  ]
})

/**
 * @description 组件挂载时初始化
 * 1. 获取仪表盘数据
 * 2. 初始化收入趋势图表
 * 3. 初始化车辆使用率图表
 */
onMounted(async () => {
  loading.value = true
  try {
    // 并行请求仪表盘数据和图表数据
    const [dashRes] = await Promise.all([
      getDashboard().catch(() => ({ data: {} }))
    ])
    dashboard.value = dashRes.data || {}
  } catch {} finally { loading.value = false }
  // 等待 DOM 更新后再初始化图表
  await nextTick()
  initRevenueChart()
  initVehicleChart()
})

/**
 * @description 组件卸载时清理资源
 * 销毁 ECharts 实例，移除窗口 resize 监听，避免内存泄漏
 */
onUnmounted(() => {
  if (revenueChartInstance) {
    revenueChartInstance.dispose()
    revenueChartInstance = null
  }
  if (vehicleChartInstance) {
    vehicleChartInstance.dispose()
    vehicleChartInstance = null
  }
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
    resizeHandler = null
  }
})

/**
 * @description 初始化收入趋势柱状图
 * 从后端获取月度收入数据，使用 ECharts 渲染渐变色柱状图
 */
const initRevenueChart = async () => {
  if (!revenueChart.value) return
  const chart = echarts.init(revenueChart.value)
  revenueChartInstance = chart
  try {
    const res = await getRevenueTrend('month')
    const data = res.data || []
    chart.setOption({
      // 提示框：鼠标悬停时显示详细数据
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(255,255,255,0.95)',
        borderColor: '#e2e8f0',
        textStyle: { color: '#334155' }
      },
      grid: { left: 50, right: 20, top: 20, bottom: 30 },
      xAxis: {
        type: 'category',
        data: data.map(d => d.period),
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { color: '#94a3b8' }
      },
      yAxis: {
        type: 'value',
        splitLine: { lineStyle: { color: '#f1f5f9' } },
        axisLabel: { color: '#94a3b8' }
      },
      series: [{
        data: data.map(d => d.amount),
        type: 'bar',
        barWidth: 28,
        itemStyle: {
          borderRadius: [6, 6, 0, 0], // 柱状图顶部圆角
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#818cf8' },
            { offset: 1, color: '#6366f1' }
          ])
        }
      }]
    })
  } catch {}
  // 监听窗口 resize 事件，自动调整图表大小
  resizeHandler = () => {
    revenueChartInstance?.resize()
    vehicleChartInstance?.resize()
  }
  window.addEventListener('resize', resizeHandler)
}

/**
 * @description 初始化车辆使用率环形饼图
 * 从后端获取车辆状态数据（可用/维修中），使用 ECharts 渲染环形饼图
 */
const initVehicleChart = async () => {
  if (!vehicleChart.value) return
  const chart = echarts.init(vehicleChart.value)
  vehicleChartInstance = chart
  try {
    const res = await getVehicleUsage()
    const d = res.data || {}
    chart.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['45%', '72%'], // 内外半径，形成环形效果
        center: ['50%', '50%'],
        data: [
          { value: d.available || 0, name: '可用', itemStyle: { color: '#10b981' } },
          { value: d.maintenance || 0, name: '维修中', itemStyle: { color: '#f59e0b' } }
        ],
        label: { formatter: '{b}\n{c} 辆', fontSize: 12, color: '#64748b' },
        emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.1)' } }
      }]
    })
  } catch {}
}
</script>

<style scoped>
/* 欢迎语区域 */
.welcome-bar {
  margin-bottom: 20px;
  animation: fadeInUp 0.4s ease;
}
.welcome-bar h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-color, #1e293b);
  margin: 0 0 6px;
  letter-spacing: -0.01em;
}
.welcome-bar p {
  font-size: 13px;
  color: var(--text-secondary, #94a3b8);
  margin: 0;
}

/* 统计卡片：白色背景 + 圆角 + hover 上浮效果 */
.stat-card {
  background: var(--card-bg, #fff);
  border-radius: 14px;
  padding: 22px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid var(--border-color, #e8ecf1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  animation: fadeInUp 0.4s ease;
}
.stat-card:nth-child(2) { animation-delay: 0.05s; }
.stat-card:nth-child(3) { animation-delay: 0.1s; }
.stat-card:nth-child(4) { animation-delay: 0.15s; }
.stat-card:hover {
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
  transform: translateY(-3px);
}

/* 统计数值和标题 */
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-color, #1e293b);
  letter-spacing: -0.02em;
  line-height: 1.2;
}
.stat-title {
  font-size: 13px;
  color: var(--text-secondary, #94a3b8);
  margin-top: 6px;
  font-weight: 500;
}

/* 统计图标容器 */
.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s ease;
}
.stat-card:hover .stat-icon {
  transform: scale(1.08);
}

/* 图表卡片样式 */
.chart-card {
  border: 1px solid var(--border-color, #e8ecf1);
  border-radius: 14px;
  animation: fadeInUp 0.5s ease;
  animation-delay: 0.2s;
  animation-fill-mode: both;
}
.chart-card :deep(.el-card__header) {
  border-bottom: 1px solid var(--border-color, #e8ecf1);
  padding: 16px 24px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-color, #1e293b);
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
