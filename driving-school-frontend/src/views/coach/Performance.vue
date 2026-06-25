<!--
  @description 教练绩效管理页面
  提供教练绩效的完整管理功能，包括：
  1. 顶部统计卡片：平均通过率、平均评分、总课时、综合得分最高
  2. 绩效排名列表：展示教练绩效数据，支持按月份筛选
  3. 计算月度绩效按钮：选择月份后计算该月绩效数据
  4. 绩效详情对话框：查看单个教练的绩效详情

  API 接口：
  - 计算绩效：POST /coaches/performance/calculate?month=YYYY-MM
  - 绩效排名：GET /coaches/performance/ranking?month=YYYY-MM
  - 教练绩效：GET /coaches/performance/coach/{id}

  权限控制：
  - 管理员：可查看所有绩效、计算绩效
  - 教练：仅可查看自己的绩效
-->

<template>
  <div class="page-container">
    <!-- ==================== 顶部统计卡片 ==================== -->
    <div class="stat-row">
      <!-- 平均通过率统计卡片 -->
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background:#ecfdf5;color:#10b981">
          <el-icon :size="18"><TrendCharts /></el-icon>
        </div>
        <div>
          <div class="mini-stat-value">{{ stats.avgPassRate }}%</div>
          <div class="mini-stat-label">平均通过率</div>
        </div>
      </div>
      <!-- 平均评分统计卡片 -->
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background:#fffbeb;color:#f59e0b">
          <el-icon :size="18"><Star /></el-icon>
        </div>
        <div>
          <div class="mini-stat-value">{{ stats.avgRating }}</div>
          <div class="mini-stat-label">平均评分</div>
        </div>
      </div>
      <!-- 总课时统计卡片 -->
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background:#eef2ff;color:#6366f1">
          <el-icon :size="18"><Clock /></el-icon>
        </div>
        <div>
          <div class="mini-stat-value">{{ stats.totalHours }}</div>
          <div class="mini-stat-label">总课时</div>
        </div>
      </div>
      <!-- 综合得分最高统计卡片 -->
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background:#fef2f2;color:#ef4444">
          <el-icon :size="18"><Trophy /></el-icon>
        </div>
        <div>
          <div class="mini-stat-value">{{ stats.topScore }}</div>
          <div class="mini-stat-label">最高综合得分</div>
        </div>
      </div>
    </div>

    <!-- ==================== 绩效排名列表卡片 ==================== -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><DataAnalysis /></el-icon>
            <span>教练绩效排名</span>
          </div>
          <!-- 操作按钮组 -->
          <div class="header-actions">
            <!-- 月份选择器 -->
            <el-date-picker
              v-model="selectedMonth"
              type="month"
              placeholder="选择月份"
              value-format="YYYY-MM"
              style="width:160px"
              @change="fetchRanking"
            />
            <!-- 计算绩效按钮：仅管理员可见 -->
            <el-button
              type="primary"
              @click="handleCalculate"
              :loading="calculating"
              v-if="role==='ADMIN'"
            >
              <el-icon><Cpu /></el-icon>计算月度绩效
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选栏 -->
      <div class="search-bar">
        <el-tag effect="plain" type="info" size="large">
          <el-icon><Calendar /></el-icon>
          当前查看：{{ selectedMonth || '本月' }}
        </el-tag>
        <el-button @click="resetMonth"><el-icon><Refresh /></el-icon>重置为本月</el-button>
      </div>

      <!-- 绩效排名数据表格 -->
      <el-table :data="rankingList" stripe v-loading="loading" class="data-table">
        <!-- 排名列：前三名特殊样式 -->
        <el-table-column label="排名" width="80" align="center">
          <template #default="{ row, $index }">
            <div class="rank-cell">
              <!-- 前三名显示奖牌图标 -->
              <el-icon v-if="$index === 0" :size="20" style="color:#f59e0b"><Medal /></el-icon>
              <el-icon v-else-if="$index === 1" :size="20" style="color:#94a3b8"><Medal /></el-icon>
              <el-icon v-else-if="$index === 2" :size="20" style="color:#cd7f32"><Medal /></el-icon>
              <span v-else class="rank-number">{{ $index + 1 }}</span>
            </div>
          </template>
        </el-table-column>
        <!-- 教练姓名列：带头像显示 -->
        <el-table-column prop="coachName" label="教练姓名" min-width="120">
          <template #default="{ row }">
            <div class="cell-with-avatar">
              <el-avatar :size="28" :style="{ background: getAvatarColor(row.coachName) }">
                {{ (row.coachName||'?').charAt(0) }}
              </el-avatar>
              <div>
                <div style="font-weight:500">{{ row.coachName }}</div>
                <div style="font-size:11px;color:#94a3b8">{{ row.coachType || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <!-- 总课时列 -->
        <el-table-column label="总课时" width="90" align="center">
          <template #default="{ row }">
            <span style="font-weight:600">{{ row.totalLessons || 0 }}</span>
          </template>
        </el-table-column>
        <!-- 完成课时列 -->
        <el-table-column label="完成课时" width="90" align="center">
          <template #default="{ row }">
            <span style="color:#10b981;font-weight:600">{{ row.completedLessons || 0 }}</span>
          </template>
        </el-table-column>
        <!-- 学员数列 -->
        <el-table-column label="学员数" width="80" align="center">
          <template #default="{ row }">{{ row.studentCount || 0 }}</template>
        </el-table-column>
        <!-- 通过率列 -->
        <el-table-column label="通过率" width="100" align="center">
          <template #default="{ row }">
            <el-progress
              :percentage="Number(row.passRate || 0)"
              :stroke-width="8"
              :color="row.passRate >= 80 ? '#10b981' : row.passRate >= 60 ? '#f59e0b' : '#ef4444'"
              style="width:80px"
            />
          </template>
        </el-table-column>
        <!-- 平均评分列 -->
        <el-table-column label="平均评分" width="140" align="center">
          <template #default="{ row }">
            <div class="rating-cell" v-if="row.avgRating">
              <el-rate
                :model-value="Number(row.avgRating)"
                disabled
                show-score
                text-color="#f59e0b"
                score-template="{value}"
                size="small"
              />
            </div>
            <span v-else style="color:#94a3b8;font-size:13px">暂无评分</span>
          </template>
        </el-table-column>
        <!-- 综合得分列 -->
        <el-table-column label="综合得分" width="120" align="center">
          <template #default="{ row }">
            <div class="score-cell">
              <span :class="['score-value', getScoreClass(row.totalScore)]">
                {{ row.totalScore || 0 }}
              </span>
              <span class="score-unit">分</span>
            </div>
          </template>
        </el-table-column>
        <!-- 操作列 -->
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="showCoachDetail(row)">
              <el-icon><View /></el-icon>详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空数据提示 -->
      <el-empty v-if="!loading && rankingList.length === 0" description="暂无绩效数据，请选择月份或计算月度绩效" />
    </el-card>

    <!-- ==================== 教练绩效详情对话框 ==================== -->
    <el-dialog v-model="detailVisible" title="教练绩效详情" width="600" destroy-on-close>
      <!-- 教练信息头部 -->
      <div class="coach-detail-header">
        <el-avatar :size="48" :style="{ background: getAvatarColor(detailData.coachName) }">
          {{ (detailData.coachName||'?').charAt(0) }}
        </el-avatar>
        <div class="coach-detail-info">
          <div class="coach-detail-name">{{ detailData.coachName }}</div>
          <div class="coach-detail-meta">{{ detailData.coachType }} · {{ detailData.month }}</div>
        </div>
        <div class="coach-detail-score">
          <span class="detail-score-value" :class="getScoreClass(detailData.totalScore)">
            {{ detailData.totalScore || 0 }}
          </span>
          <span class="detail-score-label">综合得分</span>
        </div>
      </div>

      <!-- 绩效指标详情 -->
      <el-descriptions :column="2" border style="margin-top:20px">
        <el-descriptions-item label="总课时">
          <span style="font-weight:600">{{ detailData.totalLessons || 0 }}</span> 课时
        </el-descriptions-item>
        <el-descriptions-item label="完成课时">
          <span style="font-weight:600;color:#10b981">{{ detailData.completedLessons || 0 }}</span> 课时
        </el-descriptions-item>
        <el-descriptions-item label="学员数量">
          <span style="font-weight:600">{{ detailData.studentCount || 0 }}</span> 人
        </el-descriptions-item>
        <el-descriptions-item label="考试通过率">
          <span :style="{ fontWeight: 600, color: detailData.passRate >= 80 ? '#10b981' : '#f59e0b' }">
            {{ detailData.passRate || 0 }}%
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="平均评分">
          <el-rate :model-value="Number(detailData.avgRating || 0)" disabled show-score text-color="#f59e0b" />
        </el-descriptions-item>
        <el-descriptions-item label="课时完成率">
          <el-progress
            :percentage="detailData.totalLessons ? Math.round((detailData.completedLessons / detailData.totalLessons) * 100) : 0"
            :stroke-width="10"
          />
        </el-descriptions-item>
      </el-descriptions>

      <!-- 绩效趋势说明 -->
      <div class="detail-footer">
        <el-alert
          title="绩效说明"
          description="综合得分由课时完成率(30%)、考试通过率(30%)、学员评分(20%)、学员数量(20%)加权计算得出"
          type="info"
          :closable="false"
          show-icon
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '../../store/user'
// 修复：统一使用 api/index.js 中集中管理的 API 函数，便于维护和避免重复定义请求路径
import { getPerformanceRanking, calculatePerformance, getCoachPerformance } from '../../api'
import { ElMessage } from 'element-plus'
import { Star, Clock, Trophy, Cpu, Calendar, Refresh, Medal, View } from '@element-plus/icons-vue'

// ========== 用户状态 ==========
/** @description 用户状态 Store */
const userStore = useUserStore()
/** @description 当前用户角色 */
const role = computed(() => userStore.userInfo.role)

// ========== 列表数据状态 ==========
/** @description 表格数据加载状态 */
const loading = ref(false)
/** @description 绩效排名列表数据 */
const rankingList = ref([])
/** @description 当前选择的月份（YYYY-MM 格式） */
const selectedMonth = ref('')
/** @description 计算绩效按钮加载状态 */
const calculating = ref(false)

// ========== 统计数据 ==========
/** @description 统计数据：平均通过率、平均评分、总课时、最高综合得分 */
const stats = reactive({
  avgPassRate: 0,
  avgRating: 0,
  totalHours: 0,
  topScore: 0
})

// ========== 绩效详情状态 ==========
/** @description 控制详情对话框的显示/隐藏 */
const detailVisible = ref(false)
/** @description 当前查看的教练绩效详情数据 */
const detailData = ref({})

// ========== 工具函数 ==========
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
 * @description 根据综合得分返回对应的CSS类名
 * @param {number} score - 综合得分
 * @returns {string} CSS类名
 */
const getScoreClass = (score) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  if (score >= 60) return 'score-average'
  return 'score-poor'
}

/**
 * @description 获取当前月份（YYYY-MM 格式）
 * @returns {string} 当前月份
 */
const getCurrentMonth = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}`
}

// ========== 查询与数据请求函数 ==========
/**
 * @description 重置月份为当前月并重新加载数据
 */
const resetMonth = () => {
  selectedMonth.value = getCurrentMonth()
  fetchRanking()
}

/**
 * @description 获取绩效排名列表
 * 根据当前选择的月份从后端获取排名数据
 */
const fetchRanking = async () => {
  loading.value = true
  try {
    const month = selectedMonth.value || getCurrentMonth()
    const res = await getPerformanceRanking(month)
    rankingList.value = res.data || []
    // 计算统计数据
    calculateStats()
  } catch {
    rankingList.value = []
  } finally {
    loading.value = false
  }
}

/**
 * @description 计算统计数据
 * 根据当前排名列表数据计算平均值
 */
const calculateStats = () => {
  const list = rankingList.value
  if (list.length === 0) {
    stats.avgPassRate = 0
    stats.avgRating = 0
    stats.totalHours = 0
    stats.topScore = 0
    return
  }
  // 计算平均通过率
  stats.avgPassRate = (list.reduce((sum, item) => sum + Number(item.passRate || 0), 0) / list.length).toFixed(1)
  // 计算平均评分
  stats.avgRating = (list.reduce((sum, item) => sum + Number(item.avgRating || 0), 0) / list.length).toFixed(1)
  // 计算总课时
  stats.totalHours = list.reduce((sum, item) => sum + Number(item.totalLessons || 0), 0)
  // 获取最高综合得分
  stats.topScore = Math.max(...list.map(item => Number(item.totalScore || 0)))
}

/**
 * @description 计算月度绩效
 * 管理员点击后，调用后端接口计算指定月份的绩效数据
 */
const handleCalculate = async () => {
  const month = selectedMonth.value || getCurrentMonth()
  calculating.value = true
  try {
    await calculatePerformance(month)
    ElMessage.success(`${month} 月绩效计算完成`)
    // 重新获取排名数据
    fetchRanking()
  } catch {
    // 错误已由 request 拦截器处理
  } finally {
    calculating.value = false
  }
}

/**
 * @description 显示教练绩效详情对话框
 * @param {Object} row - 教练绩效行数据
 */
const showCoachDetail = async (row) => {
  try {
    const res = await getCoachPerformance(row.coachId)
    detailData.value = res.data || row
    detailVisible.value = true
  } catch {
    // 如果接口失败，使用列表中的数据
    detailData.value = row
    detailVisible.value = true
  }
}

// ========== 生命周期 ==========
/** @description 组件挂载后初始化月份并获取排名数据 */
onMounted(() => {
  selectedMonth.value = getCurrentMonth()
  fetchRanking()
})
</script>

<style scoped>
/* 页面容器：垂直排列子元素 */
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 统计卡片行布局：水平排列，支持换行 */
.stat-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

/* 单个统计卡片：白色背景、圆角、弹性布局 */
.mini-stat {
  flex: 1;
  min-width: 160px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: 10px;
  padding: 16px;
  border: 1px solid #f1f5f9;
}

/* 统计图标容器：固定尺寸圆角方块 */
.mini-stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 统计数值样式：大号加粗字体 */
.mini-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
}

/* 统计标签文字：小号灰色字体 */
.mini-stat-label {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

/* 主卡片圆角样式 */
.main-card {
  border-radius: 12px;
}

/* 覆盖卡片头部内边距 */
.main-card :deep(.el-card__header) {
  padding: 16px 20px;
}

/* 卡片头部布局 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

/* 卡片标题样式 */
.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

/* 头部操作按钮组 */
.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

/* 搜索栏布局 */
.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

/* 数据表格圆角 */
.data-table {
  border-radius: 8px;
}

/* 排名单元格 */
.rank-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 排名数字样式 */
.rank-number {
  font-size: 16px;
  font-weight: 700;
  color: #64748b;
}

/* 带头像的单元格布局 */
.cell-with-avatar {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 评分单元格 */
.rating-cell {
  display: flex;
  justify-content: center;
}

/* 得分单元格 */
.score-cell {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 2px;
}

/* 得分数值 */
.score-value {
  font-size: 20px;
  font-weight: 700;
}

/* 得分单位 */
.score-unit {
  font-size: 12px;
  color: #94a3b8;
}

/* 得分等级样式 */
.score-excellent {
  color: #10b981;
}

.score-good {
  color: #6366f1;
}

.score-average {
  color: #f59e0b;
}

.score-poor {
  color: #ef4444;
}

/* 教练详情头部 */
.coach-detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
}

/* 教练详情信息 */
.coach-detail-info {
  flex: 1;
}

/* 教练详情姓名 */
.coach-detail-name {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

/* 教练详情元信息 */
.coach-detail-meta {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 4px;
}

/* 教练详情得分 */
.coach-detail-score {
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 详情得分数值 */
.detail-score-value {
  font-size: 32px;
  font-weight: 700;
}

/* 详情得分标签 */
.detail-score-label {
  font-size: 12px;
  color: #94a3b8;
}

/* 详情底部说明 */
.detail-footer {
  margin-top: 20px;
}
</style>
