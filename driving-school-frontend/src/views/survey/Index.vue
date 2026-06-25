<!--
  @description 满意度调查页面
  提供学员对驾校教学服务的满意度评价功能，包括：
  1. 评价表单：教学质量、服务态度、教学环境、总体评价四项星级评分
  2. 文字评价：支持输入文字评价内容和改进建议
  3. 教练评价列表：展示指定教练收到的所有评价
  4. 平均评分展示：展示教练的综合平均评分

  使用场景：
  - 学员完成培训后可提交满意度调查
  - 管理员可查看教练的评价列表和平均评分
-->

<template>
  <div class="page-container">
    <!-- ==================== 提交评价卡片 ==================== -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><Star /></el-icon>
            <span>满意度调查</span>
          </div>
        </div>
      </template>

      <el-form ref="surveyFormRef" :model="surveyForm" :rules="surveyRules" label-width="100px" label-position="top">
        <!-- 教练选择 -->
        <el-form-item label="评价教练" prop="coachId">
          <el-select v-model="surveyForm.coachId" placeholder="请选择要评价的教练" style="width: 100%" filterable>
            <el-option v-for="coach in coachList" :key="coach.id" :label="coach.name" :value="coach.id" />
          </el-select>
        </el-form-item>

        <!-- 评分区域 -->
        <div class="rating-section">
          <!-- 教学质量评分 -->
          <el-form-item label="教学质量" prop="teachingQuality">
            <el-rate v-model="surveyForm.teachingQuality" :colors="rateColors" show-text :texts="rateTexts" />
          </el-form-item>

          <!-- 服务态度评分 -->
          <el-form-item label="服务态度" prop="serviceAttitude">
            <el-rate v-model="surveyForm.serviceAttitude" :colors="rateColors" show-text :texts="rateTexts" />
          </el-form-item>

          <!-- 教学环境评分 -->
          <el-form-item label="教学环境" prop="teachingEnvironment">
            <el-rate v-model="surveyForm.teachingEnvironment" :colors="rateColors" show-text :texts="rateTexts" />
          </el-form-item>

          <!-- 总体评价评分 -->
          <el-form-item label="总体评价" prop="overallRating">
            <el-rate v-model="surveyForm.overallRating" :colors="rateColors" show-text :texts="rateTexts" />
          </el-form-item>
        </div>

        <!-- 文字评价 -->
        <el-form-item label="文字评价">
          <el-input v-model="surveyForm.comment" type="textarea" :rows="4"
            placeholder="请分享您的学车体验，如教练的教学方式、课程安排等..." maxlength="500" show-word-limit />
        </el-form-item>

        <!-- 改进建议 -->
        <el-form-item label="改进建议">
          <el-input v-model="surveyForm.suggestion" type="textarea" :rows="3"
            placeholder="您对驾校有哪些改进建议？（选填）" maxlength="300" show-word-limit />
        </el-form-item>

        <!-- 提交按钮 -->
        <el-form-item>
          <el-button type="primary" size="large" @click="submitSurvey" :loading="submitting" style="min-width: 160px">
            <el-icon><Promotion /></el-icon>提交评价
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ==================== 教练评价查询卡片 ==================== -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><ChatDotRound /></el-icon>
            <span>教练评价</span>
          </div>
        </div>
      </template>

      <!-- 教练选择查询 -->
      <div class="search-bar">
        <el-select v-model="queryCoachId" placeholder="选择教练查看评价" clearable filterable style="width: 220px">
          <el-option v-for="coach in coachList" :key="coach.id" :label="coach.name" :value="coach.id" />
        </el-select>
        <el-button type="primary" @click="fetchCoachSurveys" :icon="Search">查询</el-button>
      </div>

      <!-- 平均评分展示 -->
      <div v-if="avgRating" class="avg-rating-section">
        <div class="avg-score">
          <div class="avg-number">{{ avgRating.avgOverall?.toFixed(1) || '-' }}</div>
          <!-- 修复：使用 :model-value 单向绑定而非 v-model，因为此处为只读展示，避免触发 Vue 的 v-model 警告 -->
          <el-rate :model-value="avgRating.avgOverall" disabled show-score text-color="#ff9900" score-template="" />
          <div class="avg-count">共 {{ avgRating.totalCount || 0 }} 条评价</div>
        </div>
        <div class="avg-details">
          <div class="avg-item">
            <span class="avg-label">教学质量</span>
            <el-progress :percentage="(avgRating.avgTeachingQuality || 0) * 20" :stroke-width="8"
              :format="(p) => (p / 20).toFixed(1)" />
          </div>
          <div class="avg-item">
            <span class="avg-label">服务态度</span>
            <el-progress :percentage="(avgRating.avgServiceAttitude || 0) * 20" :stroke-width="8"
              :format="(p) => (p / 20).toFixed(1)" />
          </div>
          <div class="avg-item">
            <span class="avg-label">教学环境</span>
            <el-progress :percentage="(avgRating.avgTeachingEnvironment || 0) * 20" :stroke-width="8"
              :format="(p) => (p / 20).toFixed(1)" />
          </div>
        </div>
      </div>

      <!-- 评价列表 -->
      <el-table :data="surveyList" stripe v-loading="surveyLoading" class="data-table">
        <!-- 评价学员 -->
        <el-table-column label="评价学员" width="120">
          <template #default="{ row }">
            <div class="cell-with-avatar">
              <el-avatar :size="28" :style="{ background: getAvatarColor(row.studentName) }">
                {{ (row.studentName || '?').charAt(0) }}
              </el-avatar>
              <span>{{ row.studentName || '匿名' }}</span>
            </div>
          </template>
        </el-table-column>
        <!-- 教学质量 -->
        <el-table-column label="教学质量" width="140" align="center">
          <template #default="{ row }">
            <!-- 修复：使用 :model-value 单向绑定，表格中为只读展示，不需要 v-model 双向绑定 -->
            <el-rate :model-value="row.teachingQuality" disabled size="small" />
          </template>
        </el-table-column>
        <!-- 服务态度 -->
        <el-table-column label="服务态度" width="140" align="center">
          <template #default="{ row }">
            <el-rate :model-value="row.serviceAttitude" disabled size="small" />
          </template>
        </el-table-column>
        <!-- 教学环境 -->
        <el-table-column label="教学环境" width="140" align="center">
          <template #default="{ row }">
            <el-rate :model-value="row.teachingEnvironment" disabled size="small" />
          </template>
        </el-table-column>
        <!-- 总体评价 -->
        <el-table-column label="总体评价" width="140" align="center">
          <template #default="{ row }">
            <el-rate :model-value="row.overallRating" disabled size="small" />
          </template>
        </el-table-column>
        <!-- 评价内容 -->
        <el-table-column label="评价内容" min-width="200">
          <template #default="{ row }">
            <div class="comment-cell">
              <div v-if="row.comment" class="comment-text">{{ row.comment }}</div>
              <div v-if="row.suggestion" class="suggestion-text">
                <el-icon><InfoFilled /></el-icon> {{ row.suggestion }}
              </div>
              <span v-if="!row.comment && !row.suggestion" class="no-comment">-</span>
            </div>
          </template>
        </el-table-column>
        <!-- 评价时间 -->
        <el-table-column prop="createTime" label="评价时间" width="170" />
      </el-table>

      <!-- 分页栏 -->
      <div class="pagination-bar">
        <span class="total-text">共 {{ surveyTotal }} 条记录</span>
        <el-pagination v-model:current-page="surveyQuery.pageNum" v-model:page-size="surveyQuery.pageSize"
          :total="surveyTotal" layout="prev, pager, next" @change="fetchCoachSurveys" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { submitSurvey as apiSubmitSurvey, getCoachSurveys, getCoachAvgRating, getCoaches } from '../../api'
import {
  Star, ChatDotRound, Search, Promotion, InfoFilled
} from '@element-plus/icons-vue'

// ========== 加载状态 ==========
/** @description 提交评价按钮加载状态 */
const submitting = ref(false)
/** @description 评价列表加载状态 */
const surveyLoading = ref(false)

// ========== 教练列表数据 ==========
/** @description 教练下拉列表数据 */
const coachList = ref([])

// ========== 评价表单状态 ==========
/** @description 评价表单引用（用于表单验证） */
const surveyFormRef = ref()
/** @description 评价表单数据 */
const surveyForm = reactive({
  coachId: null,
  teachingQuality: null,
  serviceAttitude: null,
  teachingEnvironment: null,
  overallRating: null,
  comment: '',
  suggestion: ''
})
/** @description 评价表单验证规则 */
const surveyRules = {
  coachId: [{ required: true, message: '请选择评价教练', trigger: 'change' }],
  teachingQuality: [{ required: true, message: '请评分教学质量', trigger: 'change' }],
  serviceAttitude: [{ required: true, message: '请评分服务态度', trigger: 'change' }],
  teachingEnvironment: [{ required: true, message: '请评分教学环境', trigger: 'change' }],
  overallRating: [{ required: true, message: '请评分总体评价', trigger: 'change' }]
}
/** @description 评分组件颜色配置 */
const rateColors = ['#99A9BF', '#F7BA2A', '#FF9900']
/** @description 评分组件文字提示 */
const rateTexts = ['很差', '较差', '一般', '满意', '非常满意']

// ========== 教练评价查询状态 ==========
/** @description 当前查询的教练ID */
const queryCoachId = ref(null)
/** @description 评价列表数据 */
const surveyList = ref([])
/** @description 评价列表总记录数 */
const surveyTotal = ref(0)
/** @description 评价列表分页参数 */
const surveyQuery = reactive({ pageNum: 1, pageSize: 10 })
/** @description 教练平均评分数据 */
const avgRating = ref(null)

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

// ========== 数据请求函数 ==========

/**
 * @description 获取教练列表
 * 用于评价表单和查询的教练下拉选择
 */
const fetchCoachList = async () => {
  try {
    const res = await getCoaches({ pageNum: 1, pageSize: 100 })
    coachList.value = res.data?.records || []
  } catch {}
}

/**
 * @description 提交满意度调查
 * 验证表单后提交到后端
 */
const submitSurvey = async () => {
  try {
    await surveyFormRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await apiSubmitSurvey(surveyForm)
    ElMessage.success('评价提交成功，感谢您的反馈！')
    // 保存教练ID（重置表单前），用于判断是否刷新评价列表
    const submittedCoachId = surveyForm.coachId
    // 重置表单
    surveyFormRef.value.resetFields()
    // 如果当前查询的教练与提交的教练一致，刷新评价列表
    if (queryCoachId.value === submittedCoachId) {
      fetchCoachSurveys()
    }
  } catch {} finally {
    submitting.value = false
  }
}

/**
 * @description 获取教练评价列表
 * 根据选择的教练ID查询评价记录和平均分
 */
const fetchCoachSurveys = async () => {
  if (!queryCoachId.value) {
    ElMessage.warning('请先选择教练')
    return
  }
  surveyLoading.value = true
  try {
    // 并行请求评价列表和平均分
    const [listRes, avgRes] = await Promise.all([
      getCoachSurveys(queryCoachId.value, surveyQuery),
      getCoachAvgRating(queryCoachId.value)
    ])
    surveyList.value = listRes.data?.records || []
    surveyTotal.value = Number(listRes.data?.total || 0)
    avgRating.value = avgRes.data || null
  } catch {} finally {
    surveyLoading.value = false
  }
}

// ========== 监听器 ==========

/**
 * @description 监听教练选择变化
 * 当用户在查询区域切换教练时自动加载评价数据
 */
watch(queryCoachId, (newVal) => {
  if (newVal) {
    surveyQuery.pageNum = 1
    fetchCoachSurveys()
  } else {
    surveyList.value = []
    surveyTotal.value = 0
    avgRating.value = null
  }
})

// ========== 生命周期 ==========

/** @description 组件挂载时获取教练列表 */
onMounted(fetchCoachList)
</script>

<style scoped>
/* 页面容器 */
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ==================== 主卡片样式 ==================== */
.main-card {
  border-radius: 12px;
}
.main-card :deep(.el-card__header) {
  padding: 16px 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

/* ==================== 评分区域 ==================== */
.rating-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px 32px;
  padding: 16px 0;
  margin-bottom: 16px;
  border-top: 1px solid #f1f5f9;
  border-bottom: 1px solid #f1f5f9;
}
.rating-section :deep(.el-form-item) {
  margin-bottom: 0;
}
.rating-section :deep(.el-rate__text) {
  font-size: 13px;
  margin-left: 8px;
}

/* ==================== 搜索栏 ==================== */
.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

/* ==================== 平均评分区域 ==================== */
.avg-rating-section {
  display: flex;
  gap: 40px;
  padding: 24px;
  background: linear-gradient(135deg, #eef2ff, #faf5ff);
  border-radius: 12px;
  margin-bottom: 24px;
}
.avg-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 140px;
}
.avg-number {
  font-size: 48px;
  font-weight: 800;
  color: #f59e0b;
  line-height: 1;
}
.avg-score :deep(.el-rate) {
  margin-top: 8px;
}
.avg-count {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 4px;
}
.avg-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 16px;
}
.avg-item {
  display: flex;
  align-items: center;
  gap: 12px;
}
.avg-label {
  width: 70px;
  font-size: 13px;
  color: #64748b;
  flex-shrink: 0;
}
.avg-item :deep(.el-progress) {
  flex: 1;
}

/* ==================== 数据表格 ==================== */
.data-table {
  border-radius: 8px;
}
.cell-with-avatar {
  display: flex;
  align-items: center;
  gap: 8px;
}
.comment-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.comment-text {
  font-size: 13px;
  color: #334155;
  line-height: 1.6;
}
.suggestion-text {
  font-size: 12px;
  color: #6366f1;
  display: flex;
  align-items: flex-start;
  gap: 4px;
  line-height: 1.5;
}
.suggestion-text .el-icon {
  margin-top: 2px;
  flex-shrink: 0;
}
.no-comment {
  color: #cbd5e1;
}

/* ==================== 分页栏 ==================== */
.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}
.total-text {
  font-size: 13px;
  color: #94a3b8;
}
</style>
