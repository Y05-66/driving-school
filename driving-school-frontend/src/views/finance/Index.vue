<!--
  财务收费管理页面
  功能说明：
  - 展示今日收入和本月收入的汇总统计卡片
  - 展示收费记录列表，支持按类型、支付方式、日期范围筛选
  - 管理员/前台可新增收费记录（报名费、课时费、补考费等）
  - 支持退费操作，退费后状态更新为"已退费"
  - 收入金额以绿色显示，退费/负数金额以红色显示
-->
<template>
  <div class="page-container">
    <!-- 汇总统计卡片区域：今日收入、本月收入、新增收费按钮 -->
    <div class="stat-row">
      <!-- 今日收入统计卡片 -->
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background:#eef2ff;color:#6366f1"><el-icon :size="18"><Money /></el-icon></div>
        <div><div class="mini-stat-value">¥{{ summary.todayRevenue || 0 }}</div><div class="mini-stat-label">今日收入</div></div>
      </div>
      <!-- 本月收入统计卡片 -->
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background:#ecfdf5;color:#10b981"><el-icon :size="18"><TrendCharts /></el-icon></div>
        <div><div class="mini-stat-value">¥{{ summary.monthRevenue || 0 }}</div><div class="mini-stat-label">本月收入</div></div>
      </div>
      <!-- 新增收费操作按钮 -->
      <div class="mini-stat">
        <el-button type="primary" @click="showAdd"><el-icon><Plus /></el-icon>新增收费</el-button>
      </div>
    </div>

    <!-- 收费记录列表卡片 -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <!-- 卡片标题 -->
        <div class="card-header">
          <div class="card-title"><el-icon><Document /></el-icon><span>收费记录</span></div>
        </div>
      </template>

      <!-- 搜索筛选栏：类型筛选、支付方式筛选、日期范围选择、查询和重置按钮 -->
      <div class="search-bar">
        <!-- 收费类型筛选下拉框 -->
        <el-select v-model="query.type" placeholder="类型筛选" clearable style="width:130px">
          <el-option label="报名费" :value="1" /><el-option label="课时费" :value="2" />
          <el-option label="补考费" :value="3" /><el-option label="其他" :value="4" />
        </el-select>
        <!-- 支付方式筛选下拉框 -->
        <el-select v-model="query.payMethod" placeholder="支付方式" clearable style="width:130px">
          <el-option label="微信" :value="2" /><el-option label="支付宝" :value="3" />
          <el-option label="现金" :value="1" /><el-option label="银行卡" :value="4" />
        </el-select>
        <!-- 日期范围选择器 -->
        <el-date-picker v-model="query.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width:260px" />
        <!-- 查询按钮 -->
        <el-button type="primary" @click="fetchData"><el-icon><Search /></el-icon>查询</el-button>
        <!-- 重置筛选条件按钮 -->
        <el-button @click="resetQuery"><el-icon><Refresh /></el-icon>重置</el-button>
      </div>

      <!-- 收费记录数据表格 -->
      <el-table :data="tableData" stripe v-loading="loading" class="data-table">
        <!-- 收据单号列：等宽字体显示 -->
        <el-table-column prop="receiptNo" label="单号" width="180">
          <template #default="{ row }"><span style="font-family:monospace;font-size:12px;color:#64748b">{{ row.receiptNo }}</span></template>
        </el-table-column>
        <!-- 学员ID列 -->
        <el-table-column prop="studentId" label="学员ID" width="180" show-overflow-tooltip />
        <!-- 收费类型列：使用彩色标签展示 -->
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="paymentTypeColor(row.type)" size="small" effect="plain">{{ paymentTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <!-- 金额列：正值绿色、负值红色，加粗显示 -->
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">
            <span :style="{ color: row.amount < 0 ? '#ef4444' : '#10b981', fontWeight: 600 }">
              {{ row.amount < 0 ? '-' : '+' }}¥{{ Math.abs(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <!-- 支付方式列 -->
        <el-table-column label="支付方式" width="100" align="center">
          <template #default="{ row }">{{ payMethodText(row.payMethod) }}</template>
        </el-table-column>
        <!-- 支付时间列 -->
        <el-table-column prop="payTime" label="支付时间" width="180" />
        <!-- 退费状态列：已退费用红色标签，正常用绿色标签 -->
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.refundStatus===1?'danger':'success'" size="small" effect="light" round>{{ row.refundStatus===1?'已退费':'正常' }}</el-tag>
          </template>
        </el-table-column>
        <!-- 操作列：退费按钮（仅未退费且金额大于0时显示） -->
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" size="small" @click="handleRefund(row)" v-if="row.refundStatus!==1 && row.amount>0">退费</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页栏：显示总记录数和分页控件 -->
      <div class="pagination-bar">
        <span class="total-text">共 {{ total }} 条记录</span>
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
          :total="total" layout="prev, pager, next" @change="fetchData" />
      </div>
    </el-card>

    <!-- 新增收费对话框：填写学员ID、收费类型、金额、支付方式、备注 -->
    <el-dialog v-model="addVisible" title="新增收费" width="500" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="80px">
        <!-- 学员ID输入 -->
        <el-form-item label="学员ID"><el-input v-model="addForm.studentId" placeholder="请输入学员ID" /></el-form-item>
        <!-- 收费类型和金额（同行排列） -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类型">
              <el-select v-model="addForm.type" style="width:100%">
                <el-option label="报名费" :value="1" /><el-option label="课时费" :value="2" />
                <el-option label="补考费" :value="3" /><el-option label="其他" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="金额"><el-input-number v-model="addForm.amount" :min="0.01" :precision="2" style="width:100%" /></el-form-item>
          </el-col>
        </el-row>
        <!-- 支付方式选择 -->
        <el-form-item label="支付方式">
          <el-select v-model="addForm.payMethod" style="width:100%">
            <el-option label="微信" :value="2" /><el-option label="支付宝" :value="3" />
            <el-option label="现金" :value="1" /><el-option label="银行卡" :value="4" />
          </el-select>
        </el-form-item>
        <!-- 备注输入 -->
        <el-form-item label="备注"><el-input v-model="addForm.remark" placeholder="可选" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible=false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确认收费</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getPayments, createPayment, refundPayment, getFinanceSummary } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

// ========== 列表数据状态 ==========
/** 表格数据加载状态 */
const loading = ref(false)
/** 收费记录列表数据数组 */
const tableData = ref([])
/** 总记录数，用于分页 */
const total = ref(0)
/** 财务汇总数据（今日收入、本月收入等） */
const summary = ref({})
/** 查询参数：页码、每页条数、收费类型、支付方式、日期范围 */
const query = reactive({ pageNum: 1, pageSize: 10, type: null, payMethod: null, dateRange: null })

// ========== 新增收费表单状态 ==========
/** 控制新增收费对话框的显示/隐藏 */
const addVisible = ref(false)
/** 新增收费表单引用 */
const addFormRef = ref(null)
/** 新增收费表单数据：学员ID、类型、金额、支付方式、备注 */
const addForm = reactive({ studentId: '', type: 1, amount: 0.01, payMethod: 2, remark: '' })
/** 新增收费表单验证规则 */
const addRules = {
  studentId: [{ required: true, message: '请输入学员ID', trigger: 'blur' }],
  type: [{ required: true, message: '请选择收费类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  payMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }]
}

// ========== 工具函数 ==========
/**
 * 将收费类型代码转换为中文文本
 * @param {number} t - 收费类型（1=报名费, 2=课时费, 3=补考费, 4=其他）
 * @returns {string} 中文类型名称
 */
const paymentTypeText = (t) => ({ 1: '报名费', 2: '课时费', 3: '补考费', 4: '其他' }[t] || '未知')

/**
 * 根据收费类型返回对应的标签颜色
 * @param {number} t - 收费类型
 * @returns {string} Element Plus 标签颜色类型
 */
const paymentTypeColor = (t) => ({ 1: 'primary', 2: 'success', 3: 'warning', 4: 'info' }[t] || 'info')

/**
 * 将支付方式代码转换为中文文本
 * @param {number} m - 支付方式（1=现金, 2=微信, 3=支付宝, 4=银行卡）
 * @returns {string} 中文支付方式名称
 */
const payMethodText = (m) => ({ 1: '现金', 2: '微信', 3: '支付宝', 4: '银行卡' }[m] || '未知')

// ========== 查询与数据请求函数 ==========
/**
 * 重置所有查询条件并重新加载数据
 * 清空类型、支付方式、日期范围筛选，重置到第一页
 */
const resetQuery = () => {
  query.type = null
  query.payMethod = null
  query.dateRange = null
  query.pageNum = 1
  fetchData()
}

/**
 * 获取收费记录列表
 * 将日期范围拆分为 startTime 和 endTime 参数发送给后端
 */
const fetchData = async () => {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize, type: query.type, payMethod: query.payMethod }
    // 将日期范围数组转换为开始时间和结束时间字符串
    if (query.dateRange && query.dateRange.length === 2) {
      params.startTime = query.dateRange[0] + ' 00:00:00'
      params.endTime = query.dateRange[1] + ' 23:59:59'
    }
    const res = await getPayments(params)
    tableData.value = res.data.records
    total.value = Number(res.data.total)
  } catch {} finally { loading.value = false }
}

/**
 * 获取财务汇总数据（今日收入、本月收入）
 */
const fetchSummary = async () => {
  try { const res = await getFinanceSummary(); summary.value = res.data || {} } catch {}
}

/**
 * 打开新增收费对话框
 */
const showAdd = () => {
  addForm.studentId = ''
  addForm.type = 1
  addForm.amount = 0.01
  addForm.payMethod = 2
  addForm.remark = ''
  addVisible.value = true
}

/**
 * 提交新增收费表单
 * 成功后关闭对话框，刷新列表和汇总数据
 */
const submitAdd = async () => {
  if (!addFormRef.value) return
  try {
    await addFormRef.value.validate()
    await createPayment(addForm)
    ElMessage.success('收费成功')
    addVisible.value = false
    fetchData()
    fetchSummary()
  } catch {}
}

/**
 * 处理退费操作
 * 弹出确认框显示退费金额，确认后调用退费接口
 * @param {Object} row - 当前收费记录行数据
 */
const handleRefund = async (row) => {
  try {
    await ElMessageBox.confirm(`确定退费 ¥${row.amount}？`, '退费确认', { type: 'warning' })
    await refundPayment(row.id)
    ElMessage.success('退费成功')
    fetchData()
    fetchSummary()
  } catch {}
}

// ========== 生命周期 ==========
/** 组件挂载后同时加载收费列表和汇总数据 */
onMounted(() => { fetchData(); fetchSummary() })
</script>

<style scoped>
/* 页面容器：垂直排列子元素 */
.page-container { display: flex; flex-direction: column; gap: 16px; }
/* 统计卡片行布局：水平排列，支持换行 */
.stat-row { display: flex; gap: 12px; flex-wrap: wrap; }
/* 单个统计卡片：白色背景、圆角、弹性布局 */
.mini-stat {
  flex: 1; min-width: 180px; display: flex; align-items: center; gap: 12px;
  background: #fff; border-radius: 10px; padding: 16px; border: 1px solid #f1f5f9;
}
/* 统计图标容器：固定尺寸圆角方块 */
.mini-stat-icon {
  width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
/* 统计数值样式：大号加粗字体 */
.mini-stat-value { font-size: 22px; font-weight: 700; color: #1e293b; }
/* 统计标签文字：小号灰色字体 */
.mini-stat-label { font-size: 12px; color: #94a3b8; margin-top: 2px; }
/* 主卡片圆角样式 */
.main-card { border-radius: 12px; }
/* 覆盖卡片头部内边距 */
.main-card :deep(.el-card__header) { padding: 16px 20px; }
/* 卡片头部布局 */
.card-header { display: flex; justify-content: space-between; align-items: center; }
/* 卡片标题样式 */
.card-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: #1e293b; }
/* 数据表格圆角 */
.data-table { border-radius: 8px; }
/* 分页栏布局 */
.pagination-bar { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; }
/* 总记录数文字样式 */
.total-text { font-size: 13px; color: #94a3b8; }
</style>
