<!--
  @description 教练详情页面
  展示单个教练的完整信息，采用双栏布局：
  - 左侧：基本信息卡片（姓名、电话、准教车型、从业年限、评分、状态）
  - 右侧：学员评价卡片（展示学员对该教练的评价列表）

  通过路由参数 /coaches/:id 获取教练 ID，页面加载时并行请求教练信息和评价数据
-->

<template>
  <div class="page-container">
    <!-- 页面头部：返回按钮 + 教练头像 + 姓名 + 状态 -->
    <el-page-header @back="$router.back()" style="margin-bottom:16px">
      <template #content>
        <div style="display:flex;align-items:center;gap:10px">
          <el-avatar :size="32" :style="{ background: getAvatarColor(coach.name) }">{{ (coach.name||'?').charAt(0) }}</el-avatar>
          <div>
            <div style="font-weight:600;font-size:16px">{{ coach.name || '加载中' }}</div>
            <div style="font-size:12px;color:#94a3b8">{{ coach.coachType }} · {{ coach.status===1?'在岗':coach.status===0?'休假':'离职' }}</div>
          </div>
        </div>
      </template>
    </el-page-header>

    <el-row :gutter="16">
      <!-- ==================== 左侧：基本信息 ==================== -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="main-card">
          <template #header><div class="card-title"><el-icon><UserFilled /></el-icon><span>基本信息</span></div></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="姓名">{{ coach.name }}</el-descriptions-item>
            <el-descriptions-item label="电话">{{ coach.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="准教车型">{{ coach.coachType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="教练证号">{{ coach.licenseNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="从业年限">{{ coach.experienceYears ? coach.experienceYears + '年' : '-' }}</el-descriptions-item>
            <!-- 评分：使用 el-rate 星级评分组件 -->
            <el-descriptions-item label="评分">
              <el-rate :model-value="Number(coach.rating || 0)" disabled show-score text-color="#f59e0b" />
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="coach.status===1?'success':coach.status===0?'warning':'info'" size="small" effect="light" round>
                {{ coach.status===1?'在岗':coach.status===0?'休假':'离职' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <!-- ==================== 右侧：学员评价 ==================== -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="main-card">
          <template #header><div class="card-title"><el-icon><ChatDotRound /></el-icon><span>学员评价</span></div></template>
          <!-- 无评价时显示空状态 -->
          <el-empty v-if="evaluations.length===0" description="暂无评价" :image-size="80" />
          <!-- 评价列表 -->
          <div v-for="e in evaluations" :key="e.id" class="eval-item">
            <div class="eval-header">
              <el-rate :model-value="e.rating" disabled size="small" />
              <span style="font-size:12px;color:#94a3b8">{{ e.createTime }}</span>
            </div>
            <p class="eval-content">{{ e.content || '该学员未填写评价内容' }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getCoach, getCoachEvaluations } from '../../api'

/** @description 路由实例，用于获取 URL 中的教练 ID */
const route = useRoute()
/** @description 教练详细信息 */
const coach = ref({})
/** @description 学员评价列表 */
const evaluations = ref([])

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
 * 从路由参数获取教练 ID，并行请求教练信息和评价数据
 */
onMounted(async () => {
  const id = route.params.id
  try {
    // 使用 Promise.all 并行请求
    const [c, e] = await Promise.all([getCoach(id), getCoachEvaluations(id)])
    coach.value = c.data || {}
    evaluations.value = e.data?.records || []
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
/* 评价项样式 */
.eval-item { padding: 14px 0; border-bottom: 1px solid #f1f5f9; }
.eval-item:last-child { border-bottom: none; }
.eval-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.eval-content { margin: 0; font-size: 13px; color: #64748b; line-height: 1.6; }
</style>
