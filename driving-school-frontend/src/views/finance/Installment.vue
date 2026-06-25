<!--
  @description 分期付款管理页面
  提供分期付款的完整管理功能，包括：
  1. 顶部统计卡片：进行中计划数、已结清计划数、逾期计划数、本月收款
  2. 分期计划列表：管理员查看所有分期计划，支持按状态筛选
  3. 创建分期计划对话框：填写学员ID、合同ID、总金额、期数
  4. 分期明细对话框：展示每期金额、应还日期、状态，支持支付某期
  5. 状态标签：进行中=primary, 已结清=success, 逾期=danger

  API 接口：
  - 创建计划：POST /finance/installments/plan
  - 学员分期：GET /finance/installments/plan/student/{id}
  - 分期明细：GET /finance/installments/detail/{planId}
  - 支付：POST /finance/installments/detail/{id}/pay

  权限控制：
  - 管理员：可查看所有分期计划、创建计划
  - 前台：可查看分期计划、创建计划
  - 学员：仅可查看自己的分期计划
-->

<template>
  <div class="page-container">
    <!-- ==================== 顶部统计卡片 ==================== -->
    <div class="stat-row">
      <!-- 进行中计划数统计卡片 -->
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background:#eef2ff;color:#6366f1">
          <el-icon :size="18"><Loading /></el-icon>
        </div>
        <div>
          <div class="mini-stat-value">{{ stats.inProgress }}</div>
          <div class="mini-stat-label">进行中</div>
        </div>
      </div>
      <!-- 已结清计划数统计卡片 -->
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background:#ecfdf5;color:#10b981">
          <el-icon :size="18"><CircleCheck /></el-icon>
        </div>
        <div>
          <div class="mini-stat-value">{{ stats.completed }}</div>
          <div class="mini-stat-label">已结清</div>
        </div>
      </div>
      <!-- 逾期计划数统计卡片 -->
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background:#fef2f2;color:#ef4444">
          <el-icon :size="18"><WarningFilled /></el-icon>
        </div>
        <div>
          <div class="mini-stat-value">{{ stats.overdue }}</div>
          <div class="mini-stat-label">逾期</div>
        </div>
      </div>
      <!-- 本月收款统计卡片 -->
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background:#fffbeb;color:#f59e0b">
          <el-icon :size="18"><Money /></el-icon>
        </div>
        <div>
          <div class="mini-stat-value">¥{{ stats.monthAmount }}</div>
          <div class="mini-stat-label">本月收款</div>
        </div>
      </div>
    </div>

    <!-- ==================== 分期计划列表卡片 ==================== -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><List /></el-icon>
            <span>分期计划列表</span>
          </div>
          <!-- 创建分期计划按钮：管理员/前台可见 -->
          <el-button type="primary" @click="showCreatePlan" v-if="['ADMIN','STAFF'].includes(role)">
            <el-icon><Plus /></el-icon>创建分期计划
          </el-button>
        </div>
      </template>

      <!-- 搜索筛选栏 -->
      <div class="search-bar">
        <!-- 学员ID搜索 -->
        <el-input v-model="query.studentId" placeholder="学员ID" clearable prefix-icon="Search" style="width:160px" @keyup.enter="fetchPlans" />
        <!-- 状态筛选下拉框 -->
        <el-select v-model="query.status" placeholder="状态筛选" clearable style="width:130px">
          <el-option label="进行中" :value="1" />
          <el-option label="已结清" :value="2" />
          <el-option label="逾期" :value="3" />
        </el-select>
        <!-- 查询按钮 -->
        <el-button type="primary" @click="fetchPlans"><el-icon><Search /></el-icon>查询</el-button>
        <!-- 重置筛选条件按钮 -->
        <el-button @click="resetQuery"><el-icon><Refresh /></el-icon>重置</el-button>
      </div>

      <!-- 分期计划数据表格 -->
      <el-table :data="planList" stripe v-loading="loading" class="data-table">
        <!-- 计划编号列 -->
        <el-table-column prop="planNo" label="计划编号" width="160">
          <template #default="{ row }">
            <span style="font-family:monospace;font-size:12px;color:#64748b">{{ row.planNo }}</span>
          </template>
        </el-table-column>
        <!-- 学员姓名列 -->
        <el-table-column prop="studentName" label="学员姓名" width="120">
          <template #default="{ row }">
            <div class="cell-with-avatar">
              <el-avatar :size="28" :style="{ background: getAvatarColor(row.studentName) }">
                {{ (row.studentName||'?').charAt(0) }}
              </el-avatar>
              <span>{{ row.studentName }}</span>
            </div>
          </template>
        </el-table-column>
        <!-- 合同编号列 -->
        <el-table-column prop="contractNo" label="合同编号" width="150" show-overflow-tooltip />
        <!-- 总金额列 -->
        <el-table-column label="总金额" width="120" align="right">
          <template #default="{ row }">
            <span style="font-weight:600;color:#1e293b">¥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>
        <!-- 已还金额列 -->
        <el-table-column label="已还金额" width="120" align="right">
          <template #default="{ row }">
            <span style="color:#10b981;font-weight:600">¥{{ row.paidAmount || 0 }}</span>
          </template>
        </el-table-column>
        <!-- 期数列 -->
        <el-table-column label="期数" width="80" align="center">
          <template #default="{ row }">{{ row.paidPeriods || 0 }}/{{ row.totalPeriods }}</template>
        </el-table-column>
        <!-- 进度条列 -->
        <el-table-column label="还款进度" width="150">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.min(100, Math.round(((row.paidPeriods || 0) / (row.totalPeriods || 1)) * 100))"
              :status="row.status===2?'success':row.status===3?'exception':''"
              :stroke-width="8"
            />
          </template>
        </el-table-column>
        <!-- 状态标签列 -->
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="light" round>
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 创建时间列 -->
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <!-- 操作列 -->
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="showDetail(row)">
              <el-icon><View /></el-icon>明细
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页栏 -->
      <div class="pagination-bar">
        <span class="total-text">共 {{ planTotal }} 条记录</span>
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
          :total="planTotal" layout="prev, pager, next" @change="fetchPlans" />
      </div>
    </el-card>

    <!-- ==================== 创建分期计划对话框 ==================== -->
    <el-dialog v-model="createVisible" title="创建分期计划" width="520" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <!-- 学员ID输入 -->
        <el-form-item label="学员ID" prop="studentId">
          <el-input v-model="createForm.studentId" placeholder="请输入学员ID" />
        </el-form-item>
        <!-- 合同ID输入 -->
        <el-form-item label="合同ID" prop="contractId">
          <el-input v-model="createForm.contractId" placeholder="请输入合同ID" />
        </el-form-item>
        <!-- 总金额和期数（同行排列） -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="总金额" prop="totalAmount">
              <el-input-number v-model="createForm.totalAmount" :min="0.01" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分期期数" prop="totalPeriods">
              <el-select v-model="createForm.totalPeriods" style="width:100%">
                <el-option label="3期" :value="3" />
                <el-option label="6期" :value="6" />
                <el-option label="9期" :value="9" />
                <el-option label="12期" :value="12" />
                <el-option label="24期" :value="24" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 预览每期金额 -->
        <el-form-item label="每期金额">
          <el-tag type="info" effect="plain" size="large">
            ¥{{ createForm.totalAmount && createForm.totalPeriods ? (createForm.totalAmount / createForm.totalPeriods).toFixed(2) : '0.00' }}
          </el-tag>
          <span style="font-size:12px;color:#94a3b8;margin-left:8px">系统将自动计算每期还款金额</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible=false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreatePlan">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 分期明细对话框 ==================== -->
    <el-dialog v-model="detailVisible" title="分期还款明细" width="680" destroy-on-close>
      <!-- 计划信息头部 -->
      <div class="detail-header">
        <div class="detail-header-item">
          <span class="detail-header-label">计划编号</span>
          <span class="detail-header-value">{{ currentPlan.planNo }}</span>
        </div>
        <div class="detail-header-item">
          <span class="detail-header-label">学员姓名</span>
          <span class="detail-header-value">{{ currentPlan.studentName }}</span>
        </div>
        <div class="detail-header-item">
          <span class="detail-header-label">总金额</span>
          <span class="detail-header-value" style="color:#6366f1">¥{{ currentPlan.totalAmount }}</span>
        </div>
        <div class="detail-header-item">
          <span class="detail-header-label">状态</span>
          <el-tag :type="statusType(currentPlan.status)" size="small" effect="light" round>
            {{ statusText(currentPlan.status) }}
          </el-tag>
        </div>
      </div>

      <!-- 分期明细列表 -->
      <el-table :data="detailList" stripe v-loading="detailLoading" class="data-table" style="margin-top:16px">
        <!-- 期数列 -->
        <el-table-column label="期数" width="80" align="center">
          <template #default="{ row }">
            <span style="font-weight:600">第{{ row.periodNo }}期</span>
          </template>
        </el-table-column>
        <!-- 应还金额列 -->
        <el-table-column label="应还金额" width="120" align="right">
          <template #default="{ row }">
            <span style="font-weight:600;color:#1e293b">¥{{ row.amount }}</span>
          </template>
        </el-table-column>
        <!-- 应还日期列 -->
        <el-table-column prop="dueDate" label="应还日期" width="130" />
        <!-- 实际还款日期列 -->
        <el-table-column prop="payDate" label="还款日期" width="130">
          <template #default="{ row }">
            <span v-if="row.payDate">{{ row.payDate }}</span>
            <span v-else style="color:#94a3b8">-</span>
          </template>
        </el-table-column>
        <!-- 状态标签列 -->
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="detailStatusType(row.status)" size="small" effect="light" round>
              {{ detailStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 操作列：支付按钮 -->
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0 || row.status === 3"
              type="primary"
              size="small"
              @click="handlePay(row)"
            >
              立即支付
            </el-button>
            <span v-else-if="row.status === 1" style="color:#10b981;font-size:12px">
              <el-icon><CircleCheck /></el-icon> 已支付
            </span>
            <span v-else style="color:#94a3b8;font-size:12px">-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '../../store/user'
import request from '../../utils/request'
import { createInstallmentPlan, getStudentInstallments, getInstallmentDetails, payInstallment } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

// ========== 用户状态 ==========
/** @description 用户状态 Store */
const userStore = useUserStore()
/** @description 当前用户角色 */
const role = computed(() => userStore.userInfo.role)

// ========== 列表数据状态 ==========
/** @description 表格数据加载状态 */
const loading = ref(false)
/** @description 分期计划列表数据 */
const planList = ref([])
/** @description 总记录数 */
const planTotal = ref(0)
/** @description 查询参数 */
const query = reactive({ studentId: '', status: null, pageNum: 1, pageSize: 10 })

// ========== 统计数据 ==========
/** @description 统计数据：进行中、已结清、逾期数量、本月收款 */
const stats = reactive({
  inProgress: 0,
  completed: 0,
  overdue: 0,
  monthAmount: 0
})

// ========== 创建计划表单状态 ==========
/** @description 控制创建计划对话框的显示/隐藏 */
const createVisible = ref(false)
/** @description 创建计划表单引用 */
const createFormRef = ref()
/** @description 提交按钮加载状态 */
const submitting = ref(false)
/** @description 创建计划表单数据 */
const createForm = reactive({
  studentId: '',
  contractId: '',
  totalAmount: null,
  totalPeriods: 6
})
/** @description 创建计划表单验证规则 */
const createRules = {
  studentId: [{ required: true, message: '请输入学员ID', trigger: 'blur' }],
  contractId: [{ required: true, message: '请输入合同ID', trigger: 'blur' }],
  totalAmount: [{ required: true, message: '请输入总金额', trigger: 'blur' }],
  totalPeriods: [{ required: true, message: '请选择分期期数', trigger: 'change' }]
}

// ========== 分期明细状态 ==========
/** @description 控制明细对话框的显示/隐藏 */
const detailVisible = ref(false)
/** @description 明细数据加载状态 */
const detailLoading = ref(false)
/** @description 当前查看的分期计划信息 */
const currentPlan = ref({})
/** @description 分期明细列表 */
const detailList = ref([])

// ========== 工具函数 ==========
/**
 * @description 将分期状态代码转换为中文文本
 * @param {number} s - 状态码（1=进行中, 2=已结清, 3=逾期）
 * @returns {string} 中文状态名称
 */
const statusText = (s) => ({ 1: '进行中', 2: '已结清', 3: '逾期' }[s] || '未知')

/**
 * @description 根据分期状态返回对应的标签颜色
 * @param {number} s - 状态码
 * @returns {string} Element Plus 标签颜色类型
 */
const statusType = (s) => ({ 1: 'primary', 2: 'success', 3: 'danger' }[s] || 'info')

/**
 * @description 将分期明细状态代码转换为中文文本
 * @param {number} s - 状态码（0=待还款, 1=已还款, 2=已逾期, 3=逾期已还）
 * @returns {string} 中文状态名称
 */
const detailStatusText = (s) => ({ 0: '待还款', 1: '已还款', 2: '已逾期', 3: '逾期已还' }[s] || '未知')

/**
 * @description 根据分期明细状态返回对应的标签颜色
 * @param {number} s - 状态码
 * @returns {string} Element Plus 标签颜色类型
 */
const detailStatusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }[s] || 'info')

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

// ========== 查询与数据请求函数 ==========
/**
 * @description 重置所有查询条件并重新加载数据
 */
const resetQuery = () => {
  query.studentId = ''
  query.status = null
  query.pageNum = 1
  fetchPlans()
}

/**
 * @description 获取分期计划列表
 * 根据当前查询参数从后端获取分页数据
 */
const fetchPlans = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      status: query.status
    }
    // 如果指定了学员ID，调用学员分期接口
    if (query.studentId) {
      const res = await getStudentInstallments(query.studentId)
      planList.value = res.data || []
      planTotal.value = planList.value.length
    } else {
      // 否则获取所有分期计划（分页）
      // TODO: No centralized API function exists for this endpoint in api/index.js
      const res = await request.get('/finance/installments/plan', { params })
      planList.value = res.data?.records || []
      planTotal.value = Number(res.data?.total || 0)
    }
    // 加载统计数据
    loadStats()
  } catch {
    // 请求失败时静默处理
  } finally {
    loading.value = false
  }
}

/**
 * @description 从后端获取统计数据
 * 调用 /finance/installments/stats 接口获取各状态数量和本月收款
 */
const loadStats = async () => {
  try {
    // TODO: No centralized API function exists for this endpoint in api/index.js
    const res = await request.get('/finance/installments/stats')
    const data = res.data || {}
    stats.inProgress = data.inProgress ?? 0
    stats.completed = data.completed ?? 0
    stats.overdue = data.overdue ?? 0
    stats.monthAmount = Number(data.monthAmount ?? 0).toFixed(2)
  } catch {}
}

// ========== 创建计划相关函数 ==========
/**
 * @description 打开创建分期计划对话框
 */
const showCreatePlan = () => {
  createForm.studentId = ''
  createForm.contractId = ''
  createForm.totalAmount = null
  createForm.totalPeriods = 6
  createVisible.value = true
}

/**
 * @description 提交创建分期计划
 * 验证表单 -> 调用 API -> 成功提示 -> 关闭对话框 -> 刷新列表
 */
const submitCreatePlan = async () => {
  try {
    await createFormRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await createInstallmentPlan(createForm)
    ElMessage.success('分期计划创建成功')
    createVisible.value = false
    fetchPlans()
  } catch {
    // 错误已由 request 拦截器处理
  } finally {
    submitting.value = false
  }
}

// ========== 分期明细相关函数 ==========
/**
 * @description 显示分期明细对话框
 * @param {Object} plan - 分期计划数据
 */
const showDetail = async (plan) => {
  currentPlan.value = plan
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getInstallmentDetails(plan.id)
    detailList.value = res.data || []
  } catch {
    detailList.value = []
  } finally {
    detailLoading.value = false
  }
}

/**
 * @description 支付某期分期
 * 弹出确认框，确认后调用支付接口
 * @param {Object} row - 分期明细行数据
 */
const handlePay = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认支付第${row.periodNo}期 ¥${row.amount}？`,
      '支付确认',
      { type: 'info', confirmButtonText: '确认支付', cancelButtonText: '取消' }
    )
    await payInstallment(row.id)
    ElMessage.success('支付成功')
    // 刷新明细列表
    showDetail(currentPlan.value)
    // 刷新计划列表
    fetchPlans()
  } catch {
    // 用户取消或请求失败，静默处理
  }
}

// ========== 生命周期 ==========
/** @description 组件挂载后获取分期计划列表 */
onMounted(fetchPlans)
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

/* 带头像的单元格布局 */
.cell-with-avatar {
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

/* 分期明细头部信息 */
.detail-header {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 8px;
}

/* 明细头部单项 */
.detail-header-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* 明细头部标签 */
.detail-header-label {
  font-size: 12px;
  color: #94a3b8;
}

/* 明细头部数值 */
.detail-header-value {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}
</style>
