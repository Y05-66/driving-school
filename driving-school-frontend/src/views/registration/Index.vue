<!--
  报名审核页面（PC端）
  功能：查看报名列表、审核报名申请、查看报名详情
  权限：ADMIN、STAFF
-->
<template>
  <div class="page-container">
    <!-- ==================== 统计卡片 ==================== -->
    <div class="stat-row">
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background:#eef2ff;color:#6366f1">
          <el-icon :size="18"><Document /></el-icon>
        </div>
        <div>
          <div class="mini-stat-value">{{ stats.total || 0 }}</div>
          <div class="mini-stat-label">总报名</div>
        </div>
      </div>
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background:#fffbeb;color:#f59e0b">
          <el-icon :size="18"><Clock /></el-icon>
        </div>
        <div>
          <div class="mini-stat-value">{{ stats.pending || 0 }}</div>
          <div class="mini-stat-label">待审核</div>
        </div>
      </div>
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background:#ecfdf5;color:#10b981">
          <el-icon :size="18"><CircleCheck /></el-icon>
        </div>
        <div>
          <div class="mini-stat-value">{{ stats.approved || 0 }}</div>
          <div class="mini-stat-label">已通过</div>
        </div>
      </div>
      <div class="mini-stat">
        <div class="mini-stat-icon" style="background:#fef2f2;color:#ef4444">
          <el-icon :size="18"><CircleClose /></el-icon>
        </div>
        <div>
          <div class="mini-stat-value">{{ stats.rejected || 0 }}</div>
          <div class="mini-stat-label">已拒绝</div>
        </div>
      </div>
    </div>

    <!-- ==================== 主内容 ==================== -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><Document /></el-icon>
            <span>报名列表</span>
          </div>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input v-model="query.keyword" placeholder="搜索姓名/手机号" clearable prefix-icon="Search" style="width:200px" @keyup.enter="fetchData" />
        <el-select v-model="query.status" placeholder="状态筛选" clearable style="width:130px">
          <el-option label="待审核" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已拒绝" :value="2" />
        </el-select>
        <el-button type="primary" @click="fetchData"><el-icon><Search /></el-icon>查询</el-button>
        <el-button @click="resetQuery"><el-icon><Refresh /></el-icon>重置</el-button>
      </div>

      <!-- 数据表格 -->
      <el-table :data="tableData" stripe v-loading="loading" class="data-table">
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="idCard" label="身份证号" width="180" />
        <el-table-column prop="applyType" label="报考类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.applyType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="classType" label="班型" width="100" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="light" round>
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button v-if="row.status === 0" type="success" size="small" @click="handleReview(row, true)">通过</el-button>
              <el-button v-if="row.status === 0" type="danger" size="small" @click="handleReview(row, false)">拒绝</el-button>
              <el-button type="primary" size="small" link @click="showDetail(row)">详情</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <!-- ==================== 详情弹窗 ==================== -->
    <el-dialog v-model="detailVisible" title="报名详情" width="500px">
      <el-descriptions :column="1" border v-if="currentRow">
        <el-descriptions-item label="姓名">{{ currentRow.name }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentRow.phone }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ currentRow.idCard }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ currentRow.gender === 1 ? '男' : '女' }}</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ currentRow.birthday }}</el-descriptions-item>
        <el-descriptions-item label="联系地址">{{ currentRow.address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报考类型">{{ currentRow.applyType }}</el-descriptions-item>
        <el-descriptions-item label="班型">{{ currentRow.classType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(currentRow.status)">{{ statusText(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ currentRow.createTime }}</el-descriptions-item>
        <el-descriptions-item v-if="currentRow.reviewComment" label="审核意见">{{ currentRow.reviewComment }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getRegistrations, reviewRegistration, getRegistrationStats } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

// ========== 列表数据状态 ==========
/** @description 表格加载状态 */
const loading = ref(false)
/** @description 表格数据 */
const tableData = ref([])
/** @description 总记录数 */
const total = ref(0)

// ========== 详情弹窗状态 ==========
/** @description 详情弹窗是否可见 */
const detailVisible = ref(false)
/** @description 当前查看的报名记录 */
const currentRow = ref(null)

// ========== 统计数据 ==========
/** @description 报名统计数据（总数、待审核、已通过、已拒绝） */
const stats = ref({ total: 0, pending: 0, approved: 0, rejected: 0 })

// ========== 查询参数 ==========
/** @description 分页和筛选参数 */
const query = reactive({ pageNum: 1, pageSize: 10, status: null, keyword: '' })

// ========== 工具函数 ==========
/**
 * @description 报名状态码转中文文本
 * @param {number} s - 状态码（0-待审核 1-已通过 2-已拒绝）
 * @returns {string} 状态中文文本
 */
const statusText = (s) => ({ 0: '待审核', 1: '已通过', 2: '已拒绝' }[s] || '未知')

/**
 * @description 报名状态码转标签类型
 * @param {number} s - 状态码
 * @returns {string} Element Plus 标签类型
 */
const statusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[s] || 'info')

// ========== 数据请求函数 ==========
/**
 * @description 重置查询条件并重新加载数据
 */
const resetQuery = () => {
  query.keyword = ''
  query.status = null
  query.pageNum = 1
  fetchData()
}

/**
 * @description 获取报名列表和统计数据
 * 分两步加载：先加载表格数据，再独立加载统计数据（互不影响）
 */
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getRegistrations(query)
    tableData.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } catch (e) {
    // 响应拦截器已处理错误提示
  } finally { loading.value = false }

  // 独立加载统计，不影响表格
  try {
    const statsRes = await getRegistrationStats()
    stats.value = statsRes.data || {}
  } catch {}
}

/**
 * @description 审核报名申请
 * 弹出输入框让审核人填写意见，确认后调用审核接口
 * @param {Object} row - 报名记录
 * @param {boolean} approved - 是否通过
 */
const handleReview = async (row, approved) => {
  const action = approved ? '通过' : '拒绝'
  try {
    const { value: comment } = await ElMessageBox.prompt(`请输入${action}意见（可选）`, `${action}报名`, {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '审核意见...'
    })
    await reviewRegistration(row.id, { approved, comment: comment || '' })
    ElMessage.success(`已${action}`)
    fetchData()
  } catch (e) {
    if (e === 'cancel' || e?.message === 'cancel' || e?.toString?.()?.includes('cancel')) return
    ElMessage.error(`审核失败: ${e?.message || '未知错误'}`)
  }
}

/**
 * @description 显示报名详情弹窗
 * @param {Object} row - 报名记录
 */
const showDetail = (row) => {
  currentRow.value = row
  detailVisible.value = true
}

// ========== 生命周期 ==========
/** @description 页面加载时获取报名列表和统计数据 */
onMounted(() => { fetchData() })
</script>
