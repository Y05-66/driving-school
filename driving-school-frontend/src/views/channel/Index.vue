<!--
  @description 招生渠道管理页面
  提供招生渠道的完整管理功能，包括：
  1. 顶部统计卡片：渠道总数、本月招生数、平均转化率、最佳渠道
  2. 渠道列表：展示所有招生渠道信息，支持按类型筛选
  3. 创建渠道对话框：填写名称、类型、联系人、电话、费用
  4. 渠道统计区域：各渠道招生数量、转化率图表展示
  5. 类型标签：线上=primary, 线下=success, 转介绍=warning, 其他=info

  API 接口：
  - 列表：GET /channels
  - 创建：POST /channels
  - 绑定：POST /channels/bind
  - 统计：GET /channels/stats

  权限控制：
  - 管理员：可查看所有渠道、创建渠道
  - 前台：可查看渠道、创建渠道
-->

<template>
  <div class="page-container">
    <!-- ==================== 顶部统计卡片 ==================== -->
    <div class="stat-row">
      <!-- 渠道总数统计卡片 -->
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background:#eef2ff;color:#6366f1">
          <el-icon :size="18"><Connection /></el-icon>
        </div>
        <div>
          <div class="mini-stat-value">{{ stats.totalChannels }}</div>
          <div class="mini-stat-label">渠道总数</div>
        </div>
      </div>
      <!-- 本月招生数统计卡片 -->
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background:#ecfdf5;color:#10b981">
          <el-icon :size="18"><UserFilled /></el-icon>
        </div>
        <div>
          <div class="mini-stat-value">{{ stats.monthEnrollment }}</div>
          <div class="mini-stat-label">本月招生</div>
        </div>
      </div>
      <!-- 平均转化率统计卡片 -->
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background:#fffbeb;color:#f59e0b">
          <el-icon :size="18"><TrendCharts /></el-icon>
        </div>
        <div>
          <div class="mini-stat-value">{{ stats.avgConversion }}%</div>
          <div class="mini-stat-label">平均转化率</div>
        </div>
      </div>
      <!-- 最佳渠道统计卡片 -->
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background:#fef2f2;color:#ef4444">
          <el-icon :size="18"><Trophy /></el-icon>
        </div>
        <div>
          <div class="mini-stat-value">{{ stats.bestChannel }}</div>
          <div class="mini-stat-label">最佳渠道</div>
        </div>
      </div>
    </div>

    <!-- ==================== 渠道列表卡片 ==================== -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><List /></el-icon>
            <span>渠道列表</span>
          </div>
          <!-- 创建渠道按钮：管理员/前台可见 -->
          <el-button type="primary" @click="showCreate" v-if="['ADMIN','STAFF'].includes(role)">
            <el-icon><Plus /></el-icon>创建渠道
          </el-button>
        </div>
      </template>

      <!-- 搜索筛选栏 -->
      <div class="search-bar">
        <!-- 渠道名称搜索 -->
        <el-input v-model="query.name" placeholder="搜索渠道名称" clearable prefix-icon="Search" style="width:200px" @keyup.enter="fetchChannels" />
        <!-- 类型筛选下拉框 -->
        <el-select v-model="query.type" placeholder="类型筛选" clearable style="width:130px">
          <el-option label="线上" value="ONLINE" />
          <el-option label="线下" value="OFFLINE" />
          <el-option label="转介绍" value="REFERRAL" />
          <el-option label="其他" value="OTHER" />
        </el-select>
        <!-- 查询按钮 -->
        <el-button type="primary" @click="fetchChannels"><el-icon><Search /></el-icon>查询</el-button>
        <!-- 重置筛选条件按钮 -->
        <el-button @click="resetQuery"><el-icon><Refresh /></el-icon>重置</el-button>
      </div>

      <!-- 渠道数据表格 -->
      <el-table :data="channelList" stripe v-loading="loading" class="data-table">
        <!-- 渠道名称列 -->
        <el-table-column prop="name" label="渠道名称" min-width="150">
          <template #default="{ row }">
            <div class="channel-name-cell">
              <el-icon :size="16" :style="{ color: typeColorMap[row.type] }"><Link /></el-icon>
              <span style="font-weight:500">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <!-- 渠道类型列：使用彩色标签展示 -->
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small" effect="light" round>
              {{ typeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 联系人列 -->
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <!-- 联系电话列 -->
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <!-- 招生数量列 -->
        <el-table-column label="招生数量" width="100" align="center">
          <template #default="{ row }">
            <span style="font-weight:600;color:#6366f1">{{ row.enrollmentCount || 0 }}</span>
          </template>
        </el-table-column>
        <!-- 转化率列 -->
        <el-table-column label="转化率" width="120" align="center">
          <template #default="{ row }">
            <el-progress
              :percentage="Number(row.conversionRate || 0)"
              :stroke-width="8"
              :color="row.conversionRate >= 50 ? '#10b981' : row.conversionRate >= 30 ? '#f59e0b' : '#ef4444'"
              style="width:80px"
            />
          </template>
        </el-table-column>
        <!-- 费用列 -->
        <el-table-column label="费用" width="100" align="right">
          <template #default="{ row }">
            <span style="color:#f59e0b;font-weight:600">¥{{ row.cost || 0 }}</span>
          </template>
        </el-table-column>
        <!-- 单个学员成本列 -->
        <el-table-column label="单人成本" width="100" align="right">
          <template #default="{ row }">
            <span v-if="row.costPerStudent" style="color:#94a3b8">¥{{ row.costPerStudent }}</span>
            <span v-else style="color:#94a3b8">-</span>
          </template>
        </el-table-column>
        <!-- 状态标签列 -->
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status===1?'success':'info'" size="small" effect="light" round>
              {{ row.status===1?'启用':'停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 创建时间列 -->
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <!-- 操作列 -->
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleBind(row)">
              <el-icon><Connection /></el-icon>绑定学员
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页栏 -->
      <div class="pagination-bar">
        <span class="total-text">共 {{ channelTotal }} 条记录</span>
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
          :total="channelTotal" layout="prev, pager, next" @change="fetchChannels" />
      </div>
    </el-card>

    <!-- ==================== 渠道统计卡片 ==================== -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><DataAnalysis /></el-icon>
            <span>渠道招生统计</span>
          </div>
        </div>
      </template>

      <!-- 统计图表区域 -->
      <div class="stats-container" v-loading="statsLoading">
        <!-- 各渠道招生数量统计 -->
        <div class="stats-section">
          <div class="stats-section-title">各渠道招生数量</div>
          <div class="stats-bars">
            <div v-for="item in channelStats" :key="item.channelName" class="stats-bar-item">
              <div class="stats-bar-label">{{ item.channelName }}</div>
              <div class="stats-bar-wrapper">
                <div
                  class="stats-bar-fill"
                  :style="{
                    width: getBarWidth(item.enrollmentCount) + '%',
                    background: getBarColor(item.type)
                  }"
                ></div>
              </div>
              <div class="stats-bar-value">{{ item.enrollmentCount }}</div>
            </div>
          </div>
        </div>

        <!-- 各渠道转化率统计 -->
        <div class="stats-section">
          <div class="stats-section-title">各渠道转化率</div>
          <div class="stats-bars">
            <div v-for="item in channelStats" :key="'rate-'+item.channelName" class="stats-bar-item">
              <div class="stats-bar-label">{{ item.channelName }}</div>
              <div class="stats-bar-wrapper">
                <div
                  class="stats-bar-fill"
                  :style="{
                    width: item.conversionRate + '%',
                    background: item.conversionRate >= 50 ? '#10b981' : item.conversionRate >= 30 ? '#f59e0b' : '#ef4444'
                  }"
                ></div>
              </div>
              <div class="stats-bar-value">{{ item.conversionRate }}%</div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- ==================== 创建渠道对话框 ==================== -->
    <el-dialog v-model="createVisible" title="创建招生渠道" width="520" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <!-- 渠道名称输入 -->
        <el-form-item label="渠道名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入渠道名称" />
        </el-form-item>
        <!-- 渠道类型选择 -->
        <el-form-item label="渠道类型" prop="type">
          <el-select v-model="createForm.type" style="width:100%">
            <el-option label="线上" value="ONLINE" />
            <el-option label="线下" value="OFFLINE" />
            <el-option label="转介绍" value="REFERRAL" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <!-- 联系人和联系电话（同行排列） -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactPerson">
              <el-input v-model="createForm.contactPerson" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="createForm.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 渠道费用 -->
        <el-form-item label="渠道费用" prop="cost">
          <el-input-number v-model="createForm.cost" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <!-- 备注 -->
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible=false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 绑定学员对话框 ==================== -->
    <el-dialog v-model="bindVisible" title="绑定学员到渠道" width="420" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="渠道">
          <el-input :model-value="bindChannelName" disabled />
        </el-form-item>
        <el-form-item label="学员ID">
          <el-input v-model="bindStudentId" placeholder="请输入学员ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindVisible=false">取消</el-button>
        <el-button type="primary" @click="submitBind">确认绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '../../store/user'
// 修复：统一使用 api/index.js 中集中管理的 API 函数，便于维护和避免重复定义请求路径
import { getChannels, createChannel, bindChannelStudent, getChannelStats } from '../../api'
import { ElMessage } from 'element-plus'
import { Connection, UserFilled, TrendCharts, Trophy, List, Plus, Search, Refresh, DataAnalysis, Link } from '@element-plus/icons-vue'

// ========== 用户状态 ==========
/** @description 用户状态 Store */
const userStore = useUserStore()
/** @description 当前用户角色 */
const role = computed(() => userStore.userInfo.role)

// ========== 列表数据状态 ==========
/** @description 表格数据加载状态 */
const loading = ref(false)
/** @description 渠道列表数据 */
const channelList = ref([])
/** @description 总记录数 */
const channelTotal = ref(0)
/** @description 查询参数 */
const query = reactive({ name: '', type: null, pageNum: 1, pageSize: 10 })

// ========== 统计数据状态 ==========
/** @description 统计数据加载状态 */
const statsLoading = ref(false)
/** @description 渠道统计数据 */
const channelStats = ref([])
/** @description 顶部统计汇总数据 */
const stats = reactive({
  totalChannels: 0,
  monthEnrollment: 0,
  avgConversion: 0,
  bestChannel: '-'
})

// ========== 创建渠道表单状态 ==========
/** @description 控制创建渠道对话框的显示/隐藏 */
const createVisible = ref(false)
/** @description 创建渠道表单引用 */
const createFormRef = ref()
/** @description 提交按钮加载状态 */
const submitting = ref(false)
/** @description 创建渠道表单数据 */
const createForm = reactive({
  name: '',
  type: 'ONLINE',
  contactPerson: '',
  contactPhone: '',
  cost: 0,
  remark: ''
})
/** @description 创建渠道表单验证规则 */
const createRules = {
  name: [{ required: true, message: '请输入渠道名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择渠道类型', trigger: 'change' }]
}

// ========== 绑定学员状态 ==========
/** @description 控制绑定学员对话框的显示/隐藏 */
const bindVisible = ref(false)
/** @description 当前绑定的渠道ID */
const bindChannelId = ref('')
/** @description 当前绑定的渠道名称（仅用于显示） */
const bindChannelName = ref('')
/** @description 要绑定的学员ID */
const bindStudentId = ref('')

// ========== 工具函数 ==========
/**
 * @description 将渠道类型代码转换为中文文本
 * @param {string} type - 类型代码（ONLINE/OFFLINE/REFERRAL/OTHER）
 * @returns {string} 中文类型名称
 */
const typeText = (type) => ({
  ONLINE: '线上',
  OFFLINE: '线下',
  REFERRAL: '转介绍',
  OTHER: '其他'
}[type] || '未知')

/**
 * @description 根据渠道类型返回对应的标签颜色
 * @param {string} type - 类型代码
 * @returns {string} Element Plus 标签颜色类型
 */
const typeTagType = (type) => ({
  ONLINE: 'primary',
  OFFLINE: 'success',
  REFERRAL: 'warning',
  OTHER: 'info'
}[type] || 'info')

/**
 * @description 渠道类型对应的颜色映射
 */
const typeColorMap = {
  ONLINE: '#6366f1',
  OFFLINE: '#10b981',
  REFERRAL: '#f59e0b',
  OTHER: '#94a3b8'
}

/**
 * @description 根据渠道类型返回对应的柱状图颜色
 * @param {string} type - 类型代码
 * @returns {string} 颜色值
 */
const getBarColor = (type) => ({
  ONLINE: '#6366f1',
  OFFLINE: '#10b981',
  REFERRAL: '#f59e0b',
  OTHER: '#94a3b8'
}[type] || '#94a3b8')

/**
 * @description 计算柱状图宽度百分比
 * @param {number} count - 招生数量
 * @returns {number} 宽度百分比
 */
const getBarWidth = (count) => {
  const maxCount = Math.max(...channelStats.value.map(s => s.enrollmentCount || 0), 1)
  return Math.round((count / maxCount) * 100)
}

// ========== 查询与数据请求函数 ==========
/**
 * @description 重置所有查询条件并重新加载数据
 */
const resetQuery = () => {
  query.name = ''
  query.type = null
  query.pageNum = 1
  fetchChannels()
}

/**
 * @description 获取渠道列表
 * 根据当前查询参数从后端获取分页数据
 */
const fetchChannels = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      name: query.name || undefined,
      type: query.type || undefined
    }
    const res = await getChannels(params)
    channelList.value = res.data?.records || []
    channelTotal.value = Number(res.data?.total || 0)
  } catch {
    channelList.value = []
    channelTotal.value = 0
  } finally {
    loading.value = false
  }
}

/**
 * @description 获取渠道统计数据
 * 从后端获取各渠道的招生数量和转化率
 */
const fetchStats = async () => {
  statsLoading.value = true
  try {
    const res = await getChannelStats()
    channelStats.value = res.data || []
    // 计算汇总统计
    calculateSummary()
  } catch {
    channelStats.value = []
  } finally {
    statsLoading.value = false
  }
}

/**
 * @description 计算汇总统计数据
 * 根据渠道统计数据计算总渠道数、本月招生、平均转化率、最佳渠道
 */
const calculateSummary = () => {
  const list = channelStats.value
  stats.totalChannels = list.length
  stats.monthEnrollment = list.reduce((sum, item) => sum + (item.enrollmentCount || 0), 0)
  stats.avgConversion = list.length > 0
    ? (list.reduce((sum, item) => sum + Number(item.conversionRate || 0), 0) / list.length).toFixed(1)
    : 0
  // 找出招生数量最多的渠道
  if (list.length > 0) {
    const best = list.reduce((prev, curr) =>
      (prev.enrollmentCount || 0) > (curr.enrollmentCount || 0) ? prev : curr
    )
    stats.bestChannel = best.channelName || '-'
  }
}

// ========== 创建渠道相关函数 ==========
/**
 * @description 打开创建渠道对话框
 */
const showCreate = () => {
  createForm.name = ''
  createForm.type = 'ONLINE'
  createForm.contactPerson = ''
  createForm.contactPhone = ''
  createForm.cost = 0
  createForm.remark = ''
  createVisible.value = true
}

/**
 * @description 提交创建渠道
 * 验证表单 -> 调用 API -> 成功提示 -> 关闭对话框 -> 刷新列表
 */
const submitCreate = async () => {
  try {
    await createFormRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await createChannel(createForm)
    ElMessage.success('渠道创建成功')
    createVisible.value = false
    fetchChannels()
    fetchStats()
  } catch {
    // 错误已由 request 拦截器处理
  } finally {
    submitting.value = false
  }
}

// ========== 绑定学员相关函数 ==========
/**
 * @description 打开绑定学员对话框
 * @param {Object} row - 渠道行数据
 */
const handleBind = (row) => {
  bindChannelId.value = row.id
  bindChannelName.value = row.name
  bindStudentId.value = ''
  bindVisible.value = true
}

/**
 * @description 提交绑定学员
 * 验证学员ID后调用绑定接口
 */
const submitBind = async () => {
  if (!bindStudentId.value) {
    ElMessage.warning('请输入学员ID')
    return
  }
  try {
    await bindChannelStudent({
      channelId: bindChannelId.value,
      studentId: bindStudentId.value
    })
    ElMessage.success('绑定成功')
    bindVisible.value = false
    // 刷新渠道列表和统计数据
    fetchChannels()
    fetchStats()
  } catch {
    // 错误已由 request 拦截器处理
  }
}

// ========== 生命周期 ==========
/** @description 组件挂载后同时加载渠道列表和统计数据 */
onMounted(() => {
  fetchChannels()
  fetchStats()
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

/* 渠道名称单元格 */
.channel-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 分页栏布局 */
.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}

/* 总记录数文字样式 */
.total-text {
  font-size: 13px;
  color: #94a3b8;
}

/* 统计容器 */
.stats-container {
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
}

/* 统计区域 */
.stats-section {
  flex: 1;
  min-width: 300px;
}

/* 统计区域标题 */
.stats-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
}

/* 统计柱状图列表 */
.stats-bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 统计柱状图单项 */
.stats-bar-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 统计柱状图标签 */
.stats-bar-label {
  width: 80px;
  font-size: 13px;
  color: #64748b;
  text-align: right;
  flex-shrink: 0;
}

/* 统计柱状图容器 */
.stats-bar-wrapper {
  flex: 1;
  height: 20px;
  background: #f1f5f9;
  border-radius: 4px;
  overflow: hidden;
}

/* 统计柱状图填充 */
.stats-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s ease;
  min-width: 2px;
}

/* 统计柱状图数值 */
.stats-bar-value {
  width: 50px;
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  text-align: right;
}
</style>
