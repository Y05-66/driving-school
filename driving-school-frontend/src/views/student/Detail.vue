<!--
  @description 学员详情页面
  展示单个学员的完整信息，采用双栏布局：
  - 左侧（2/3 宽度）：基本信息卡片，使用 el-descriptions 展示学员详细资料
  - 右侧（1/3 宽度）：科目进度卡片，展示各科目学时完成情况

  通过路由参数 /students/:id 获取学员 ID，页面加载时并行请求学员信息和进度数据
-->

<template>
  <div class="page-container">
    <!-- 页面头部：返回按钮 + 学员头像 + 姓名 + 状态 -->
    <el-page-header @back="$router.back()" style="margin-bottom:16px">
      <template #content>
        <div style="display:flex;align-items:center;gap:10px">
          <el-avatar :size="32" :style="{ background: getAvatarColor(student.name) }">{{ (student.name||'?').charAt(0) }}</el-avatar>
          <div>
            <div style="font-weight:600;font-size:16px">{{ student.name || '加载中' }}</div>
            <div style="font-size:12px;color:#94a3b8">{{ student.applyType }} · {{ statusText(student.status) }}</div>
          </div>
        </div>
      </template>
    </el-page-header>

    <el-row :gutter="16">
      <!-- ==================== 左侧：基本信息 ==================== -->
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="main-card">
          <template #header><div class="card-title"><el-icon><User /></el-icon><span>基本信息</span></div></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="姓名">{{ student.name }}</el-descriptions-item>
            <el-descriptions-item label="身份证号">{{ student.idCard }}</el-descriptions-item>
            <el-descriptions-item label="电话">{{ student.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ student.gender === 1 ? '男' : student.gender === 0 ? '女' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="生日">{{ student.birthday || '-' }}</el-descriptions-item>
            <el-descriptions-item label="报名类型">{{ student.applyType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusType(student.status)" size="small" effect="light" round>{{ statusText(student.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="注册日期">{{ student.registerDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ student.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <!-- ==================== 右侧：科目进度 ==================== -->
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="main-card">
          <template #header><div class="card-title"><el-icon><TrendCharts /></el-icon><span>科目进度</span></div></template>
          <!-- 无进度数据时显示空状态 -->
          <div v-if="progressList.length === 0" style="text-align:center;padding:20px;color:#94a3b8">暂无进度数据</div>
          <!-- 各科目进度列表 -->
          <div v-for="p in progressList" :key="p.id" class="progress-item">
            <div class="progress-info">
              <span class="progress-subject">{{ p.subjectName }}</span>
              <span class="progress-hours">{{ p.completedHours || 0 }}/{{ p.requiredHours || 0 }}h</span>
            </div>
            <el-progress :percentage="Math.min(100, Math.round((p.completedHours || 0) / (p.requiredHours || 1) * 100))"
              :status="p.status===2?'success':p.status===1?'':'info'" :stroke-width="8" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getStudent, getStudentProgress } from '../../api'

/** @description 路由实例，用于获取 URL 中的学员 ID */
const route = useRoute()
/** @description 学员详细信息 */
const student = ref({})
/** @description 学员科目进度列表 */
const progressList = ref([])

/**
 * @description 状态文本映射
 * @param {number} s - 状态码（0:待审核, 1:在学, 2:已毕业, 3:已退学）
 * @returns {string} 状态中文名称
 */
const statusText = (s) => ({ 0: '待审核', 1: '在学', 2: '已毕业', 3: '已退学' }[s] || '未知')
/**
 * @description 状态标签类型映射
 * @param {number} s - 状态码
 * @returns {string} Element Plus Tag 组件的 type
 */
const statusType = (s) => ({ 0: 'warning', 1: 'primary', 2: 'success', 3: 'info' }[s] || 'info')
/**
 * @description 根据姓名生成头像背景色
 * @param {string} name - 姓名
 * @returns {string} 颜色值
 */
const getAvatarColor = (name) => {
  const colors = ['#6366f1', '#10b981', '#f59e0b', '#3b82f6', '#ef4444', '#8b5cf6']
  let hash = 0
  for (const c of (name || '')) hash = ((hash << 5) - hash) + c.charCodeAt(0)
  return colors[Math.abs(hash) % colors.length]
}

/**
 * @description 组件挂载时初始化
 * 从路由参数获取学员 ID，并行请求学员信息和科目进度
 */
onMounted(async () => {
  const id = route.params.id
  try {
    // 使用 Promise.all 并行请求，提升加载速度
    const [s, p] = await Promise.all([getStudent(id), getStudentProgress(id)])
    student.value = s.data || {}
    progressList.value = p.data || []
  } catch {}
})
</script>

<style scoped>
/* 页面容器 */
.page-container { display: flex; flex-direction: column; gap: 16px; }
/* 主内容卡片 */
.main-card { border-radius: 12px; }
.main-card :deep(.el-card__header) { padding: 16px 20px; }
.card-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: #1e293b; }
/* 进度项样式 */
.progress-item { margin-bottom: 16px; }
.progress-item:last-child { margin-bottom: 0; }
.progress-info { display: flex; justify-content: space-between; margin-bottom: 6px; }
.progress-subject { font-size: 14px; font-weight: 600; color: #1e293b; }
.progress-hours { font-size: 13px; color: #94a3b8; }
</style>
