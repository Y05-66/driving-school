<!--
  在线刷题页面
  功能：随机出题、答题、查看解析、统计正确率
-->
<template>
  <view class="container">
    <!-- 统计卡片 -->
    <view class="stats-row">
      <view class="stats-card">
        <text class="stats-num">{{ stats.totalAnswered || 0 }}</text>
        <text class="stats-label">已答题</text>
      </view>
      <view class="stats-card">
        <text class="stats-num" style="color:#10b981">{{ stats.correctCount || 0 }}</text>
        <text class="stats-label">答对</text>
      </view>
      <view class="stats-card">
        <text class="stats-num" style="color:#ef4444">{{ stats.wrongCount || 0 }}</text>
        <text class="stats-label">答错</text>
      </view>
      <view class="stats-card">
        <text class="stats-num" style="color:#6366f1">{{ stats.accuracy || 0 }}%</text>
        <text class="stats-label">正确率</text>
      </view>
    </view>

    <!-- 科目选择 -->
    <view class="subject-tabs">
      <view class="tab-item" :class="{ active: subject === 1 }" @click="switchSubject(1)">
        <text class="tab-text">科目一</text>
      </view>
      <view class="tab-item" :class="{ active: subject === 4 }" @click="switchSubject(4)">
        <text class="tab-text">科目四</text>
      </view>
    </view>

    <!-- 题目卡片 -->
    <view v-if="currentQuestion" class="question-card">
      <view class="question-header">
        <text class="question-index">第 {{ currentIndex + 1 }} 题</text>
        <text class="question-type">{{ currentQuestion.questionType === 1 ? '判断题' : '单选题' }}</text>
      </view>
      <text class="question-title">{{ currentQuestion.title }}</text>

      <!-- 选项列表 -->
      <view class="options-list">
        <view
          v-for="(opt, idx) in currentQuestion.options"
          :key="idx"
          class="option-item"
          :class="optionClass(idx)"
          @click="selectOption(idx)"
        >
          <view class="option-letter">{{ ['A','B','C','D'][idx] }}</view>
          <text class="option-text">{{ opt }}</text>
          <text v-if="showAnswer && idx === currentQuestion.correctIndex" class="option-icon">✓</text>
          <text v-if="showAnswer && selectedIdx === idx && idx !== currentQuestion.correctIndex" class="option-icon wrong">✗</text>
        </view>
      </view>

      <!-- 解析 -->
      <view v-if="showAnswer && currentQuestion.explanation" class="explanation">
        <text class="explanation-title">💡 解析</text>
        <text class="explanation-text">{{ currentQuestion.explanation }}</text>
      </view>

      <!-- 操作按钮 -->
      <view class="action-bar">
        <button v-if="!showAnswer" class="btn-primary" @click="submitAnswer" :disabled="selectedIdx === -1">提交答案</button>
        <button v-if="showAnswer" class="btn-primary" @click="nextQuestion">下一题</button>
      </view>
    </view>

    <!-- 空状态 -->
    <view v-else class="empty-state">
      <text class="empty-icon">📝</text>
      <text class="empty-text">点击下方按钮开始刷题</text>
      <button class="btn-primary" @click="loadQuestions">开始刷题</button>
    </view>

    <!-- 错题本入口 -->
    <view class="wrong-book" @click="goToWrongBook">
      <text class="wrong-icon">📕</text>
      <text class="wrong-text">错题本</text>
      <text class="wrong-count">{{ stats.wrongCount || 0 }} 题</text>
      <text class="wrong-arrow">▸</text>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '../../store/user'
import { get, post } from '../../utils/request'

const userStore = useUserStore()

/** 当前选择的科目：1-科目一 4-科目四 */
const subject = ref(1)

/** 练习模式：sequential-顺序练习 random-随机练习 mock-模拟考试 */
const mode = ref('random')

/** 题目列表（已转换为前端展示格式） */
const questions = ref([])

/** 当前题目索引 */
const currentIndex = ref(0)

/** 用户选择的选项索引，-1表示未选择 */
const selectedIdx = ref(-1)

/** 是否显示答案和解析 */
const showAnswer = ref(false)

/** 刷题统计数据（已答题数、答对数、答错数、正确率） */
const stats = ref({})

/** 当前正在作答的题目（computed，从列表中按索引取） */
const currentQuestion = computed(() => questions.value[currentIndex.value])

/**
 * 切换科目
 * @param {number} s - 科目编号：1-科目一 4-科目四
 * @description 重置所有答题状态并重新加载题目
 */
const switchSubject = (s) => {
  subject.value = s
  questions.value = []
  currentIndex.value = 0
  selectedIdx.value = -1
  showAnswer.value = false
  loadQuestions()
}

/**
 * 加载随机题目
 * @description 从后端获取10道随机题目，并将后端数据格式转换为前端展示格式：
 * - 将optionA/B/C/D合并为options数组
 * - 将correctAnswer字母转换为correctIndex数字索引
 */
const loadQuestions = async () => {
  // 非学员不允许加载题目
  if (userStore.userInfo.role !== 'STUDENT') return
  try {
    const res = await get('/practice/questions', { subject: subject.value, count: 10 })
    // 数据格式转换：后端的选项字段 → 前端的数组格式
    questions.value = (res.data || []).map(q => {
      const allOptions = [q.optionA, q.optionB, q.optionC, q.optionD]
      const letters = ['A', 'B', 'C', 'D']
      // 保留非空选项，同时记录对应的字母索引
      const filtered = []
      const letterMap = [] // filtered 中每个选项对应的原始字母索引
      allOptions.forEach((opt, i) => {
        if (opt) {
          filtered.push(opt)
          letterMap.push(i)
        }
      })
      const correctLetterIdx = letters.indexOf(q.correctAnswer)
      const correctIndex = letterMap.indexOf(correctLetterIdx)
      return {
        ...q,
        options: filtered,
        correctIndex: correctIndex
      }
    })
    currentIndex.value = 0
    selectedIdx.value = -1
    showAnswer.value = false
  } catch {}
}

/**
 * 选择选项
 * @param {number} idx - 选项索引（0-3对应A-D）
 * @description 答案已提交后不允许修改选择
 */
const selectOption = (idx) => {
  if (showAnswer.value) return
  selectedIdx.value = idx
}

/**
 * 提交答案
 * @description 将用户选择的选项索引转换为字母（A/B/C/D）提交到后端，后端记录答题结果并返回是否正确
 */
const submitAnswer = async () => {
  if (selectedIdx.value === -1 || userStore.userInfo.role !== 'STUDENT') return
  try {
    await post('/practice/answer', {
      questionId: currentQuestion.value.id,
      studentAnswer: ['A','B','C','D'][selectedIdx.value]
    })
    showAnswer.value = true
    // 提交后刷新统计数据
    loadStats()
  } catch {
    uni.showToast({ title: '提交失败，请重试', icon: 'none' })
  }
}

/**
 * 加载下一题
 * @description 如果还有未答题目则切换到下一题，否则重新加载一批新题目
 */
const nextQuestion = () => {
  if (currentIndex.value < questions.value.length - 1) {
    // 切换到下一题，重置选择状态
    currentIndex.value++
    selectedIdx.value = -1
    showAnswer.value = false
  } else {
    // 所有题目已答完，加载新一批题目
    loadQuestions()
  }
}

/**
 * 计算选项的CSS类名
 * @param {number} idx - 选项索引
 * @returns {string} CSS类名：'selected'-已选中 'correct'-正确答案 'wrong'-选择错误 ''-默认
 * @description 状态机逻辑：未提交时高亮已选项；提交后高亮正确答案和错误选择
 */
const optionClass = (idx) => {
  if (!showAnswer.value) return selectedIdx.value === idx ? 'selected' : ''
  if (idx === currentQuestion.value.correctIndex) return 'correct'
  if (selectedIdx.value === idx) return 'wrong'
  return ''
}

/**
 * 加载刷题统计数据
 * @description 获取当前学员的答题统计，包括已答题数、答对数、答错数、正确率
 */
const loadStats = async () => {
  // 只有学员才加载统计
  if (userStore.userInfo.role !== 'STUDENT') return
  try {
    const res = await get('/practice/stats')
    stats.value = res.data || {}
  } catch {}
}

/**
 * 跳转错题本页面
 */
const goToWrongBook = () => {
  uni.navigateTo({ url: '/pages/practice/wrong' })
}

/**
 * 页面加载（读取 URL 参数）
 * @description 从页面路径参数中读取 subject 和 mode，支持从学车页面跳转时传入
 */
onLoad((options) => {
  if (options.subject) subject.value = Number(options.subject)
  if (options.mode) mode.value = options.mode
})

/** 页面挂载
 * @description 校验登录状态和角色，未登录或非学员则返回上一页；否则加载统计数据和题目
 */
onMounted(() => {
  // 未登录或非学员，返回上一页
  if (!userStore.token || userStore.userInfo.role !== 'STUDENT') {
    uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages/index/index' }) })
    return
  }
  loadStats()
  loadQuestions()
})
</script>

<style scoped>
/* 页面容器 */
.container { padding: 24rpx; background: #f5f7fa; min-height: 100vh; }

/* 统计卡片行 */
.stats-row { display: flex; gap: 16rpx; margin-bottom: 24rpx; }
.stats-card {
  flex: 1; background: #fff; border-radius: 16rpx; padding: 20rpx 12rpx;
  text-align: center; box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.04);
}
.stats-num { display: block; font-size: 36rpx; font-weight: 800; color: #1e293b; }
.stats-label { display: block; font-size: 22rpx; color: #94a3b8; margin-top: 4rpx; }

/* 科目选择标签 */
.subject-tabs { display: flex; gap: 16rpx; margin-bottom: 24rpx; }
.tab-item {
  flex: 1; background: #fff; border-radius: 12rpx; padding: 20rpx;
  text-align: center; border: 2rpx solid #e8ecf1;
}
.tab-item.active { background: #6366f1; border-color: #6366f1; }
.tab-item.active .tab-text { color: #fff; }
.tab-text { font-size: 28rpx; font-weight: 600; color: #475569; }

/* 题目卡片 */
.question-card {
  background: #fff; border-radius: 20rpx; padding: 32rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.question-header { display: flex; justify-content: space-between; margin-bottom: 20rpx; }
.question-index { font-size: 24rpx; color: #6366f1; font-weight: 600; }
.question-type { font-size: 22rpx; color: #94a3b8; background: #f1f5f9; padding: 4rpx 12rpx; border-radius: 8rpx; }
.question-title { font-size: 30rpx; font-weight: 600; color: #1e293b; line-height: 1.6; margin-bottom: 28rpx; }

/* 选项列表 */
.options-list { margin-bottom: 24rpx; }
.option-item {
  display: flex; align-items: center; gap: 16rpx;
  padding: 24rpx; margin-bottom: 16rpx;
  background: #f8fafc; border-radius: 12rpx;
  border: 2rpx solid #e8ecf1; transition: all 0.2s;
}
.option-item.selected { border-color: #6366f1; background: #eef2ff; }
.option-item.correct { border-color: #10b981; background: #ecfdf5; }
.option-item.wrong { border-color: #ef4444; background: #fef2f2; }
.option-letter {
  width: 48rpx; height: 48rpx; border-radius: 50%;
  background: #e8ecf1; display: flex; align-items: center; justify-content: center;
  font-size: 24rpx; font-weight: 700; color: #475569; flex-shrink: 0;
}
.option-item.selected .option-letter { background: #6366f1; color: #fff; }
.option-item.correct .option-letter { background: #10b981; color: #fff; }
.option-item.wrong .option-letter { background: #ef4444; color: #fff; }
.option-text { flex: 1; font-size: 28rpx; color: #334155; }
.option-icon { font-size: 28rpx; color: #10b981; font-weight: 700; }
.option-icon.wrong { color: #ef4444; }

/* 解析区域 */
.explanation {
  background: #fffbeb; border-radius: 12rpx; padding: 24rpx;
  margin-bottom: 24rpx; border-left: 6rpx solid #f59e0b;
}
.explanation-title { display: block; font-size: 26rpx; font-weight: 600; color: #92400e; margin-bottom: 8rpx; }
.explanation-text { font-size: 26rpx; color: #78350f; line-height: 1.6; }

/* 操作按钮 */
.action-bar { margin-top: 16rpx; }
.btn-primary {
  width: 100%; height: 88rpx; line-height: 88rpx;
  background: linear-gradient(135deg, #6366f1, #818cf8);
  color: #fff; font-size: 30rpx; font-weight: 600;
  border-radius: 16rpx; border: none;
}
.btn-primary[disabled] { opacity: 0.5; }

/* 空状态 */
.empty-state {
  background: #fff; border-radius: 20rpx; padding: 80rpx 40rpx;
  text-align: center; box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.empty-icon { font-size: 80rpx; display: block; margin-bottom: 20rpx; }
.empty-text { font-size: 28rpx; color: #94a3b8; display: block; margin-bottom: 40rpx; }

/* 错题本入口 */
.wrong-book {
  display: flex; align-items: center; gap: 16rpx;
  background: #fff; border-radius: 16rpx; padding: 28rpx 24rpx;
  margin-top: 24rpx; box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.04);
}
.wrong-icon { font-size: 40rpx; }
.wrong-text { flex: 1; font-size: 28rpx; font-weight: 600; color: #1e293b; }
.wrong-count { font-size: 24rpx; color: #ef4444; }
.wrong-arrow { font-size: 28rpx; color: #94a3b8; }
</style>
