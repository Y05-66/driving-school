<!--
  数据统计与导出页面
  功能说明：
  - 报名趋势图表：使用 ECharts 折线图展示各时间段的报名人数趋势
  - 通过率统计图表：使用 ECharts 柱状图展示各科目的考试通过率
  - 教练排行榜：以表格形式展示教练评分、学员数、从业年限排名
  - 数据导出功能：支持导出学员列表 Excel 和按日期范围导出财务报表 Excel
  - 页面加载时并行请求三个接口，使用 Promise.all 提高加载效率
-->
<template>
  <div>
    <!-- 第一行图表区域：报名趋势 + 通过率统计 -->
    <el-row :gutter="20">
      <!-- 报名趋势折线图卡片 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header><span>报名趋势</span></template>
          <!-- ECharts 图表容器，通过 ref 绑定 DOM 元素 -->
          <div ref="enrollmentChart" style="height:350px"></div>
        </el-card>
      </el-col>
      <!-- 通过率统计柱状图卡片 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header><span>通过率统计</span></template>
          <!-- ECharts 图表容器 -->
          <div ref="passRateChart" style="height:350px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行：教练排行 + 数据导出 -->
    <el-row :gutter="20" style="margin-top:20px">
      <!-- 教练排行榜卡片 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header><span>教练排行</span></template>
          <!-- 教练排名数据表格 -->
          <el-table :data="coachRanking" stripe>
            <el-table-column type="index" label="排名" width="70" />
            <el-table-column prop="name" label="教练姓名" />
            <el-table-column prop="rating" label="评分" width="80" />
            <el-table-column prop="studentCount" label="学员数" width="80" />
            <el-table-column prop="experienceYears" label="从业年限" width="90" />
          </el-table>
        </el-card>
      </el-col>
      <!-- 数据导出卡片 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>数据导出</span>
            </div>
          </template>
          <!-- 导出功能描述列表 -->
          <el-descriptions :column="1" border>
            <!-- 导出学员列表：直接下载 Excel -->
            <el-descriptions-item label="导出学员列表">
              <el-button type="primary" @click="exportStudents">导出 Excel</el-button>
            </el-descriptions-item>
            <!-- 导出财务报表：需选择日期范围后下载 -->
            <el-descriptions-item label="导出财务报表">
              <el-date-picker v-model="exportRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="margin-right:10px" />
              <el-button type="primary" @click="exportFinance">导出 Excel</el-button>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { getEnrollmentTrend, getPassRate, getCoachRanking } from '../../api'
import request from '../../utils/request'
import * as echarts from 'echarts'

// ========== 图表 DOM 引用 ==========
/** 报名趋势图表的 DOM 元素引用，用于 ECharts 初始化 */
const enrollmentChart = ref()
/** 通过率统计图表的 DOM 元素引用 */
const passRateChart = ref()

// ========== 数据状态 ==========
/** 教练排行榜数据数组 */
const coachRanking = ref([])
/** ECharts 图表实例引用，用于销毁 */
let enrollmentChartInstance = null
let passRateChartInstance = null
let resizeHandler = null
/** 财务报表导出的日期范围 [开始日期, 结束日期] */
const exportRange = ref([])

// ========== 生命周期 ==========
/**
 * 组件挂载后并行请求三个接口数据
 * 使用 Promise.all 同时请求报名趋势、通过率、教练排行
 * 每个接口都有 catch 兜底，避免单个接口失败影响整体
 * 请求完成后等待 nextTick 确保 DOM 已渲染，再初始化 ECharts 图表
 */
onMounted(async () => {
  // 并行请求三个数据接口，每个接口失败时返回空数据兜底
  const [e, p, c] = await Promise.all([
    getEnrollmentTrend().catch(() => ({ data: [] })),
    getPassRate('subject').catch(() => ({ data: {} })),
    getCoachRanking().catch(() => ({ data: [] }))
  ])

  // 等待 DOM 更新完成后再初始化图表
  await nextTick()

  // ========== 初始化报名趋势折线图 ==========
  if (enrollmentChart.value) {
    const chart = echarts.init(enrollmentChart.value)
    const data = e.data || []
    chart.setOption({
      tooltip: { trigger: 'axis' },  // 提示框：鼠标悬停时显示
      xAxis: { type: 'category', data: data.map(d => d.period) },  // X轴：时间段标签
      yAxis: { type: 'value' },  // Y轴：数值轴
      series: [{ data: data.map(d => d.count), type: 'line', smooth: true, areaStyle: { opacity: 0.3 }, itemStyle: { color: '#409eff' } }]  // 平滑折线图，带半透明面积填充
    })
    enrollmentChartInstance = chart
  }

  // ========== 初始化通过率统计柱状图 ==========
  if (passRateChart.value) {
    const chart = echarts.init(passRateChart.value)
    const d = p.data || {}
    const keys = Object.keys(d)
    chart.setOption({
      tooltip: { trigger: 'axis' },  // 提示框
      xAxis: { type: 'category', data: keys },  // X轴：科目名称
      yAxis: { type: 'value', max: 100 },  // Y轴：百分比，最大值100
      series: [{ data: keys.map(k => d[k]), type: 'bar', itemStyle: { color: '#67c23a', borderRadius: [4, 4, 0, 0] } }]  // 柱状图，圆角顶部
    })
    passRateChartInstance = chart
  }

  // 统一监听窗口大小变化，自动调整所有图表尺寸
  resizeHandler = () => {
    enrollmentChartInstance?.resize()
    passRateChartInstance?.resize()
  }
  window.addEventListener('resize', resizeHandler)

  // 赋值教练排行数据
  coachRanking.value = c.data || []
})

/**
 * 组件卸载时清理资源
 * 销毁 ECharts 实例，移除窗口 resize 监听，避免内存泄漏
 */
onUnmounted(() => {
  enrollmentChartInstance?.dispose()
  enrollmentChartInstance = null
  passRateChartInstance?.dispose()
  passRateChartInstance = null
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
    resizeHandler = null
  }
})

// ========== 导出功能函数 ==========
/**
 * 导出学员列表
 * 使用axios下载文件（带Authorization header），创建临时链接触发下载
 */
const exportStudents = async () => {
  try {
    const res = await request.get('/statistics/export/students', { responseType: 'blob' })
    const url = URL.createObjectURL(res)
    const a = document.createElement('a')
    a.href = url
    a.download = '学员列表.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    console.error('导出学员列表失败:', e)
  }
}

/**
 * 导出财务报表
 * 将选择的日期范围作为查询参数，使用axios下载文件
 */
const exportFinance = async () => {
  try {
    const [start, end] = exportRange.value || []
    const res = await request.get('/statistics/export/finance', {
      params: { startDate: start || '', endDate: end || '' },
      responseType: 'blob'
    })
    const url = URL.createObjectURL(res)
    const a = document.createElement('a')
    a.href = url
    a.download = '财务报表.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    console.error('导出财务报表失败:', e)
  }
}
</script>
