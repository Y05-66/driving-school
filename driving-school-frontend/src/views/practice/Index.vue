<!--
  @description 在线刷题页面
  提供驾考理论题在线练习功能，包括：
  1. 科目选择：支持科目一和科目四理论考试题库
  2. 练习模式：每次随机抽取10道题目进行作答
  3. 答题界面：展示题目内容、选项列表（单选/多选/判断题）
  4. 即时反馈：提交后显示正确/错误结果及题目解析
  5. 刷题统计：展示总答题数、正确率等数据
  6. 错题本：记录答错的题目，支持回顾复习

  使用场景：
  - 学员角色登录后可进行在线刷题练习
  - 系统自动记录答题数据和错题记录
-->

<template>
  <div class="page-container">
    <!-- ==================== 刷题统计卡片 ==================== -->
    <div class="stat-row">
      <div class="mini-stat" v-for="s in statCards" :key="s.label">
        <div class="mini-stat-icon" :style="{ background: s.bg, color: s.color }">
          <el-icon :size="18"><component :is="s.icon" /></el-icon>
        </div>
        <div>
          <div class="mini-stat-value">{{ s.value }}</div>
          <div class="mini-stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <!-- ==================== 主内容卡片 ==================== -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><EditPen /></el-icon>
            <span>在线刷题</span>
          </div>
          <!-- 顶部操作按钮组 -->
          <div class="header-actions">
            <el-button @click="wrongDrawerVisible = true" :icon="Warning">错题本</el-button>
          </div>
        </div>
      </template>

      <!-- ==================== 科目选择与开始练习 ==================== -->
      <div v-if="!practiceStarted" class="start-section">
        <div class="subject-cards">
          <!-- 科目一卡片 -->
          <div class="subject-card" :class="{ active: selectedSubject === 1 }" @click="selectedSubject = 1">
            <div class="subject-icon subject1">
              <el-icon :size="32"><Document /></el-icon>
            </div>
            <div class="subject-info">
              <h3>科目一</h3>
              <p>道路交通安全法律、法规和相关知识</p>
            </div>
            <el-icon v-if="selectedSubject === 1" class="check-icon"><CircleCheck /></el-icon>
          </div>
          <!-- 科目四卡片 -->
          <div class="subject-card" :class="{ active: selectedSubject === 4 }" @click="selectedSubject = 4">
            <div class="subject-icon subject4">
              <el-icon :size="32"><Stamp /></el-icon>
            </div>
            <div class="subject-info">
              <h3>科目四</h3>
              <p>安全文明驾驶常识</p>
            </div>
            <el-icon v-if="selectedSubject === 4" class="check-icon"><CircleCheck /></el-icon>
          </div>
        </div>
        <!-- 开始练习按钮 -->
        <el-button type="primary" size="large" class="start-btn" @click="startPractice" :loading="loading">
          <el-icon><VideoPlay /></el-icon>开始练习（10题）
        </el-button>
      </div>

      <!-- ==================== 答题界面 ==================== -->
      <div v-else class="practice-section">
        <!-- 进度条和题目计数 -->
        <div class="progress-header">
          <div class="progress-info">
            <span class="progress-label">
              <el-tag :type="selectedSubject === 1 ? 'primary' : 'warning'" effect="dark" size="small">
                科目{{ selectedSubject === 1 ? '一' : '四' }}
              </el-tag>
            </span>
            <span class="progress-count">第 {{ currentIndex + 1 }} / {{ questions.length }} 题</span>
          </div>
          <el-progress :percentage="Math.round(((currentIndex + 1) / questions.length) * 100)" :stroke-width="8" />
        </div>

        <!-- 当前题目 -->
        <div v-if="currentQuestion" class="question-card">
          <!-- 题目类型标签 -->
          <div class="question-type-tag">
            <el-tag size="small" :type="questionTypeTag.type" effect="plain">
              {{ questionTypeTag.text }}
            </el-tag>
          </div>
          <!-- 题目内容 -->
          <div class="question-content">
            <span class="question-index">Q{{ currentIndex + 1 }}.</span>
            {{ currentQuestion.content }}
          </div>
          <!-- 题目图片（如有） -->
          <div v-if="currentQuestion.imageUrl" class="question-image">
            <el-image :src="currentQuestion.imageUrl" fit="contain" style="max-width: 400px; max-height: 250px" />
          </div>

          <!-- 选项列表 -->
          <div class="options-list">
            <!-- 单选题/判断题选项 -->
            <template v-if="currentQuestion.questionType !== 2">
              <div v-for="(opt, idx) in currentQuestion.options" :key="idx"
                class="option-item"
                :class="{
                  selected: userAnswer === opt.label,
                  correct: showResult && opt.label === currentQuestion.correctAnswer,
                  wrong: showResult && userAnswer === opt.label && opt.label !== currentQuestion.correctAnswer
                }"
                @click="!showResult && selectOption(opt.label)"
              >
                <div class="option-label">{{ opt.label }}</div>
                <div class="option-text">{{ opt.content }}</div>
                <!-- 结果图标 -->
                <div v-if="showResult && opt.label === currentQuestion.correctAnswer" class="option-icon correct">
                  <el-icon><CircleCheck /></el-icon>
                </div>
                <div v-if="showResult && userAnswer === opt.label && opt.label !== currentQuestion.correctAnswer" class="option-icon wrong">
                  <el-icon><CircleClose /></el-icon>
                </div>
              </div>
            </template>
            <!-- 多选题选项 -->
            <template v-else>
              <div v-for="(opt, idx) in currentQuestion.options" :key="idx"
                class="option-item"
                :class="{
                  selected: Array.isArray(userAnswer) && userAnswer.includes(opt.label),
                  correct: showResult && currentQuestion.correctAnswer.includes(opt.label),
                  wrong: showResult && Array.isArray(userAnswer) && userAnswer.includes(opt.label) && !currentQuestion.correctAnswer.includes(opt.label)
                }"
                @click="!showResult && toggleMultiOption(opt.label)"
              >
                <div class="option-label">{{ opt.label }}</div>
                <div class="option-text">{{ opt.content }}</div>
                <!-- 结果图标 -->
                <div v-if="showResult && currentQuestion.correctAnswer.includes(opt.label)" class="option-icon correct">
                  <el-icon><CircleCheck /></el-icon>
                </div>
                <div v-if="showResult && Array.isArray(userAnswer) && userAnswer.includes(opt.label) && !currentQuestion.correctAnswer.includes(opt.label)" class="option-icon wrong">
                  <el-icon><CircleClose /></el-icon>
                </div>
              </div>
            </template>
          </div>

          <!-- 答题结果与解析 -->
          <div v-if="showResult" class="result-section">
            <div class="result-banner" :class="isAnswerCorrect ? 'correct' : 'wrong'">
              <el-icon :size="20"><component :is="isAnswerCorrect ? 'CircleCheck' : 'CircleClose'" /></el-icon>
              <span>{{ isAnswerCorrect ? '回答正确！' : '回答错误' }}</span>
              <span v-if="!isAnswerCorrect" class="correct-answer">
                正确答案：{{ Array.isArray(currentQuestion.correctAnswer) ? currentQuestion.correctAnswer.join('') : currentQuestion.correctAnswer }}
              </span>
            </div>
            <!-- 题目解析 -->
            <div v-if="currentQuestion.explanation" class="explanation-box">
              <div class="explanation-title">
                <el-icon><InfoFilled /></el-icon>解析
              </div>
              <div class="explanation-text">{{ currentQuestion.explanation }}</div>
            </div>
          </div>

          <!-- 底部操作按钮 -->
          <div class="question-actions">
            <el-button v-if="!showResult" type="primary" size="large" @click="submitAnswer" :disabled="!canSubmit" :loading="submitting">
              提交答案
            </el-button>
            <el-button v-else type="primary" size="large" @click="nextQuestion">
              {{ currentIndex < questions.length - 1 ? '下一题' : '查看成绩' }}
            </el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- ==================== 成绩单卡片 ==================== -->
    <el-card v-if="showSummary" shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><Trophy /></el-icon>
            <span>本次练习成绩</span>
          </div>
        </div>
      </template>
      <div class="summary-section">
        <div class="summary-score" :class="summaryScoreClass">
          <div class="score-number">{{ correctCount }}</div>
          <div class="score-label">/ {{ questions.length }} 题正确</div>
        </div>
        <div class="summary-details">
          <div class="summary-item">
            <span class="summary-item-label">正确题数</span>
            <span class="summary-item-value correct">{{ correctCount }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-item-label">错误题数</span>
            <span class="summary-item-value wrong">{{ questions.length - correctCount }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-item-label">正确率</span>
            <span class="summary-item-value" :class="summaryScoreClass">{{ summaryAccuracy }}%</span>
          </div>
        </div>
        <div class="summary-actions">
          <el-button type="primary" @click="restartPractice">
            <el-icon><RefreshRight /></el-icon>再来一轮
          </el-button>
          <el-button @click="resetPractice">
            <el-icon><Back /></el-icon>返回选择
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- ==================== 错题本抽屉 ==================== -->
    <el-drawer v-model="wrongDrawerVisible" title="错题本" size="600" destroy-on-close>
      <div v-loading="wrongLoading">
        <div v-if="wrongQuestions.length === 0" class="empty-wrong">
          <el-empty description="暂无错题记录" />
        </div>
        <div v-else>
          <!-- 错题统计 -->
          <div class="wrong-summary">
            <el-tag type="danger" effect="dark">共 {{ wrongQuestions.length }} 道错题</el-tag>
          </div>
          <!-- 错题列表 -->
          <div v-for="(item, idx) in wrongQuestions" :key="idx" class="wrong-item">
            <div class="wrong-item-header">
              <el-tag size="small" :type="item.subject === 1 ? 'primary' : 'warning'">
                科目{{ item.subject === 1 ? '一' : '四' }}
              </el-tag>
              <el-tag size="small" type="info" effect="plain">
                {{ questionTypeText(item.questionType) }}
              </el-tag>
            </div>
            <div class="wrong-item-content">{{ item.content }}</div>
            <div class="wrong-item-options">
              <div v-for="(opt, oi) in item.options" :key="oi" class="wrong-option"
                :class="{ 'is-answer': item.correctAnswer.includes(opt.label) }">
                <span class="opt-label">{{ opt.label }}.</span> {{ opt.content }}
              </div>
            </div>
            <div class="wrong-item-answer">
              <span class="label">正确答案：</span>
              <span class="value correct">{{ Array.isArray(item.correctAnswer) ? item.correctAnswer.join('') : item.correctAnswer }}</span>
            </div>
            <div v-if="item.explanation" class="wrong-item-explanation">
              <span class="label">解析：</span>{{ item.explanation }}
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useUserStore } from '../../store/user'
import { ElMessage } from 'element-plus'
// 修复：统一使用 api/index.js 中集中管理的 API 函数，便于维护和避免重复定义请求路径
import { getPracticeStats, getWrongQuestions, getPracticeQuestions, submitPracticeAnswer } from '../../api'
import {
  EditPen, Document, Stamp, VideoPlay, CircleCheck, CircleClose,
  InfoFilled, Trophy, RefreshRight, Back, Warning, Search
} from '@element-plus/icons-vue'

// ========== 用户状态 ==========
/** @description 用户状态存储 */
const userStore = useUserStore()

// ========== 加载状态 ==========
/** @description 页面主加载状态 */
const loading = ref(false)
/** @description 答案提交加载状态 */
const submitting = ref(false)
/** @description 错题本加载状态 */
const wrongLoading = ref(false)

// ========== 科目选择状态 ==========
/** @description 当前选择的科目（1=科目一, 4=科目四） */
const selectedSubject = ref(1)
/** @description 练习是否已开始 */
const practiceStarted = ref(false)

// ========== 题目数据状态 ==========
/** @description 题目列表 */
const questions = ref([])
/** @description 当前题目索引 */
const currentIndex = ref(0)
/** @description 用户选择的答案 */
const userAnswer = ref(null)
/** @description 是否显示答题结果 */
const showResult = ref(false)
/** @description 用户答题记录（索引 -> 是否正确） */
const answerRecords = ref([])

// ========== 成绩单状态 ==========
/** @description 是否显示成绩单 */
const showSummary = ref(false)

// ========== 错题本状态 ==========
/** @description 错题本抽屉是否可见 */
const wrongDrawerVisible = ref(false)
/** @description 错题列表 */
const wrongQuestions = ref([])

// 打开错题抽屉时自动加载数据
watch(wrongDrawerVisible, (val) => {
  if (val) fetchWrongQuestions()
})

// ========== 刷题统计数据 ==========
/** @description 刷题统计信息 */
const stats = reactive({
  totalAnswered: 0,
  correctCount: 0,
  wrongCount: 0,
  accuracy: 0
})

// ========== 计算属性 ==========

/**
 * @description 顶部统计卡片数据
 * 根据刷题统计数据生成展示卡片
 */
const statCards = computed(() => [
  {
    label: '总答题数', value: stats.totalAnswered || '-',
    icon: 'Document', color: '#6366f1', bg: '#eef2ff'
  },
  {
    label: '答对题数', value: stats.correctCount || '-',
    icon: 'CircleCheck', color: '#10b981', bg: '#ecfdf5'
  },
  {
    label: '答错题数', value: stats.wrongCount || '-',
    icon: 'CircleClose', color: '#ef4444', bg: '#fef2f2'
  },
  {
    label: '正确率', value: stats.accuracy ? stats.accuracy + '%' : '-',
    icon: 'Trophy', color: '#f59e0b', bg: '#fffbeb'
  }
])

/**
 * @description 当前题目对象
 */
const currentQuestion = computed(() => questions.value[currentIndex.value] || null)

/**
 * @description 题目类型标签信息
 * 根据题目类型返回对应的标签文本和颜色
 */
const questionTypeTag = computed(() => {
  const type = currentQuestion.value?.questionType
  if (type === 0) return { text: '判断题', type: 'info' }
  if (type === 1) return { text: '单选题', type: 'primary' }
  if (type === 2) return { text: '多选题', type: 'warning' }
  return { text: '未知', type: 'info' }
})

/**
 * @description 是否可以提交答案
 * 需要用户已选择答案才能提交
 */
const canSubmit = computed(() => {
  if (!userAnswer.value) return false
  if (Array.isArray(userAnswer.value)) return userAnswer.value.length > 0
  return true
})

/**
 * @description 当前题目是否回答正确
 */
const isAnswerCorrect = computed(() => {
  if (!currentQuestion.value || !showResult.value) return false
  const correct = currentQuestion.value.correctAnswer
  if (Array.isArray(correct)) {
    return Array.isArray(userAnswer.value) &&
      correct.length === userAnswer.value.length &&
      correct.every(a => userAnswer.value.includes(a))
  }
  return userAnswer.value === correct
})

/**
 * @description 本次练习正确题目数
 */
const correctCount = computed(() => answerRecords.value.filter(r => r).length)

/**
 * @description 本次练习正确率百分比
 */
const summaryAccuracy = computed(() => {
  if (questions.value.length === 0) return 0
  return Math.round((correctCount.value / questions.value.length) * 100)
})

/**
 * @description 成绩单分数样式类
 * 根据正确率返回不同的颜色样式
 */
const summaryScoreClass = computed(() => {
  const acc = summaryAccuracy.value
  if (acc >= 90) return 'excellent'
  if (acc >= 70) return 'good'
  return 'poor'
})

// ========== 工具函数 ==========

/**
 * @description 题目类型代码转中文文本
 * @param {number} type - 题目类型（0=判断, 1=单选, 2=多选）
 * @returns {string} 中文类型名称
 */
const questionTypeText = (type) => {
  if (type === 0) return '判断题'
  if (type === 1) return '单选题'
  if (type === 2) return '多选题'
  return '未知'
}

// ========== 核心业务函数 ==========

/**
 * @description 获取刷题统计数据
 * 从后端获取用户的答题统计信息
 */
const fetchStats = async () => {
  try {
    const res = await getPracticeStats()
    Object.assign(stats, res.data)
  } catch {}
}

/**
 * @description 获取错题本数据
 * 从后端获取用户答错的题目列表
 */
const fetchWrongQuestions = async () => {
  wrongLoading.value = true
  try {
    const res = await getWrongQuestions()
    wrongQuestions.value = res.data || []
  } catch {} finally {
    wrongLoading.value = false
  }
}

/**
 * @description 开始练习
 * 根据选择的科目获取10道随机题目
 */
const startPractice = async () => {
  loading.value = true
  try {
    const res = await getPracticeQuestions({ subject: selectedSubject.value, count: 10 })
    questions.value = res.data || []
    if (questions.value.length === 0) {
      ElMessage.warning('暂无题目数据')
      return
    }
    // 初始化答题状态
    currentIndex.value = 0
    userAnswer.value = null
    showResult.value = false
    answerRecords.value = []
    showSummary.value = false
    practiceStarted.value = true
  } catch {} finally {
    loading.value = false
  }
}

/**
 * @description 选择选项（单选/判断题）
 * 设置用户选择的答案
 * @param {string} label - 选项标签（如 'A', 'B', '正确', '错误'）
 */
const selectOption = (label) => {
  userAnswer.value = label
}

/**
 * @description 切换多选题选项
 * 在多选模式下，点击选项可选中或取消选中
 * @param {string} label - 选项标签
 */
const toggleMultiOption = (label) => {
  if (!Array.isArray(userAnswer.value)) {
    userAnswer.value = [label]
    return
  }
  const idx = userAnswer.value.indexOf(label)
  if (idx >= 0) {
    userAnswer.value.splice(idx, 1)
  } else {
    userAnswer.value.push(label)
  }
}

/**
 * @description 提交答案
 * 将用户选择的答案提交到后端进行判分
 */
const submitAnswer = async () => {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    const answer = Array.isArray(userAnswer.value)
      ? userAnswer.value.sort().join('')
      : userAnswer.value
    await submitPracticeAnswer({
      questionId: currentQuestion.value.id,
      studentAnswer: answer
    })
    // 显示答题结果
    showResult.value = true
    // 记录答题结果
    answerRecords.value[currentIndex.value] = isAnswerCorrect.value
    // 刷新统计数据
    fetchStats()
  } catch {} finally {
    submitting.value = false
  }
}

/**
 * @description 下一题
 * 如果还有未答题目则跳转下一题，否则显示成绩单
 */
const nextQuestion = () => {
  if (currentIndex.value < questions.value.length - 1) {
    // 进入下一题
    currentIndex.value++
    userAnswer.value = null
    showResult.value = false
  } else {
    // 所有题目答完，显示成绩单
    showSummary.value = true
    practiceStarted.value = false
  }
}

/**
 * @description 再来一轮
 * 使用相同科目重新开始练习
 */
const restartPractice = () => {
  showSummary.value = false
  startPractice()
}

/**
 * @description 返回选择
 * 重置所有状态，返回科目选择界面
 */
const resetPractice = () => {
  practiceStarted.value = false
  showSummary.value = false
  questions.value = []
  currentIndex.value = 0
  userAnswer.value = null
  showResult.value = false
  answerRecords.value = []
}

// ========== 生命周期 ==========

/** @description 组件挂载时获取刷题统计数据 */
onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
/* 页面容器 */
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ==================== 顶部统计卡片 ==================== */
.stat-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.mini-stat {
  flex: 1;
  min-width: 140px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: 10px;
  padding: 14px 16px;
  border: 1px solid #f1f5f9;
}
.mini-stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.mini-stat-value {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}
.mini-stat-label {
  font-size: 12px;
  color: #94a3b8;
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
.header-actions {
  display: flex;
  gap: 8px;
}

/* ==================== 科目选择区域 ==================== */
.start-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
  padding: 20px 0;
}
.subject-cards {
  display: flex;
  gap: 24px;
  width: 100%;
  max-width: 700px;
}
.subject-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  background: #fff;
}
.subject-card:hover {
  border-color: #6366f1;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.1);
}
.subject-card.active {
  border-color: #6366f1;
  background: #eef2ff;
}
.subject-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
}
.subject-icon.subject1 {
  background: linear-gradient(135deg, #6366f1, #818cf8);
}
.subject-icon.subject4 {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
}
.subject-info h3 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}
.subject-info p {
  margin: 0;
  font-size: 13px;
  color: #94a3b8;
}
.check-icon {
  position: absolute;
  top: 12px;
  right: 12px;
  color: #6366f1;
  font-size: 20px;
}
.start-btn {
  min-width: 200px;
  height: 48px;
  font-size: 16px;
  border-radius: 10px;
}

/* ==================== 答题区域 ==================== */
.practice-section {
  padding: 0;
}
.progress-header {
  margin-bottom: 24px;
}
.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.progress-label {
  display: flex;
  align-items: center;
  gap: 8px;
}
.progress-count {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

/* ==================== 题目卡片 ==================== */
.question-card {
  background: #f8fafc;
  border-radius: 12px;
  padding: 24px;
}
.question-type-tag {
  margin-bottom: 16px;
}
.question-content {
  font-size: 16px;
  font-weight: 500;
  color: #1e293b;
  line-height: 1.8;
  margin-bottom: 20px;
}
.question-index {
  font-weight: 700;
  color: #6366f1;
  margin-right: 4px;
}
.question-image {
  margin-bottom: 20px;
  text-align: center;
}

/* ==================== 选项列表 ==================== */
.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}
.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: #fff;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}
.option-item:hover:not(.correct):not(.wrong) {
  border-color: #6366f1;
  background: #eef2ff;
}
.option-item.selected {
  border-color: #6366f1;
  background: #eef2ff;
}
.option-item.correct {
  border-color: #10b981;
  background: #ecfdf5;
  cursor: default;
}
.option-item.wrong {
  border-color: #ef4444;
  background: #fef2f2;
  cursor: default;
}
.option-label {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  color: #475569;
  flex-shrink: 0;
}
.option-item.selected .option-label {
  background: #6366f1;
  color: #fff;
}
.option-item.correct .option-label {
  background: #10b981;
  color: #fff;
}
.option-item.wrong .option-label {
  background: #ef4444;
  color: #fff;
}
.option-text {
  flex: 1;
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
}
.option-icon {
  font-size: 18px;
  flex-shrink: 0;
}
.option-icon.correct {
  color: #10b981;
}
.option-icon.wrong {
  color: #ef4444;
}

/* ==================== 答题结果 ==================== */
.result-section {
  margin-bottom: 24px;
}
.result-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 18px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 16px;
}
.result-banner.correct {
  background: #ecfdf5;
  color: #10b981;
  border: 1px solid #a7f3d0;
}
.result-banner.wrong {
  background: #fef2f2;
  color: #ef4444;
  border: 1px solid #fecaca;
}
.correct-answer {
  margin-left: 8px;
  font-weight: 400;
  font-size: 14px;
}
.explanation-box {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px;
}
.explanation-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 14px;
  color: #6366f1;
  margin-bottom: 8px;
}
.explanation-text {
  font-size: 14px;
  color: #475569;
  line-height: 1.8;
}

/* ==================== 操作按钮 ==================== */
.question-actions {
  display: flex;
  justify-content: center;
  padding-top: 8px;
}
.question-actions .el-button {
  min-width: 160px;
  height: 44px;
  font-size: 15px;
  border-radius: 10px;
}

/* ==================== 成绩单 ==================== */
.summary-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  padding: 20px 0;
}
.summary-score {
  text-align: center;
}
.score-number {
  font-size: 64px;
  font-weight: 800;
  line-height: 1;
}
.summary-score.excellent .score-number {
  color: #10b981;
}
.summary-score.good .score-number {
  color: #f59e0b;
}
.summary-score.poor .score-number {
  color: #ef4444;
}
.score-label {
  font-size: 16px;
  color: #94a3b8;
  margin-top: 8px;
}
.summary-details {
  display: flex;
  gap: 40px;
}
.summary-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.summary-item-label {
  font-size: 13px;
  color: #94a3b8;
}
.summary-item-value {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}
.summary-item-value.correct {
  color: #10b981;
}
.summary-item-value.wrong {
  color: #ef4444;
}
.summary-item-value.excellent {
  color: #10b981;
}
.summary-item-value.good {
  color: #f59e0b;
}
.summary-item-value.poor {
  color: #ef4444;
}
.summary-actions {
  display: flex;
  gap: 12px;
}

/* ==================== 错题本样式 ==================== */
.empty-wrong {
  padding: 40px 0;
}
.wrong-summary {
  margin-bottom: 16px;
}
.wrong-item {
  background: #f8fafc;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 16px;
}
.wrong-item-header {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}
.wrong-item-content {
  font-size: 15px;
  font-weight: 500;
  color: #1e293b;
  line-height: 1.7;
  margin-bottom: 12px;
}
.wrong-item-options {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}
.wrong-option {
  font-size: 13px;
  color: #475569;
  padding: 4px 8px;
  border-radius: 6px;
}
.wrong-option.is-answer {
  background: #ecfdf5;
  color: #10b981;
  font-weight: 600;
}
.wrong-option .opt-label {
  font-weight: 600;
  margin-right: 4px;
}
.wrong-item-answer {
  font-size: 13px;
  margin-bottom: 8px;
}
.wrong-item-answer .label {
  color: #94a3b8;
}
.wrong-item-answer .value.correct {
  color: #10b981;
  font-weight: 600;
}
.wrong-item-explanation {
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
  padding-top: 8px;
  border-top: 1px dashed #e2e8f0;
}
.wrong-item-explanation .label {
  color: #6366f1;
  font-weight: 600;
}
</style>
