<!--
  学时签到管理页面
  功能说明：
  - 展示指定课时的签到记录列表
  - 支持签到操作：学员到达训练场地后进行签到
  - 支持签退操作：训练结束后进行签退
  - 显示签到时间、签退时间、签到方式、签到状态
  - 签到方式：GPS定位签到、手动签到
  - 签到状态：正常签到、迟到、早退、缺勤

  API 调用：
  - 签到：POST /check-in/{lessonId}/check-in
  - 签退：PUT /check-in/{lessonId}/check-out
  - 记录：GET /check-in/{lessonId}
-->
<template>
  <div class="page-container">
    <!-- ==================== 主内容卡片 ==================== -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <!-- 卡片头部：标题 + 课时ID输入 + 签到/签退按钮 -->
        <div class="card-header">
          <div class="card-title">
            <el-icon><Clock /></el-icon>
            <span>学时签到</span>
          </div>
          <!-- 操作按钮区域 -->
          <div class="header-actions">
            <el-button type="success" @click="handleCheckIn" :disabled="!lessonId">
              <el-icon><Check /></el-icon>签到
            </el-button>
            <el-button type="warning" @click="handleCheckOut" :disabled="!lessonId">
              <el-icon><SwitchButton /></el-icon>签退
            </el-button>
          </div>
        </div>
      </template>

      <!-- ==================== 课时ID搜索栏 ==================== -->
      <div class="search-bar">
        <!-- 课时ID输入框：输入课时ID后查询签到记录 -->
        <el-input
          v-model="lessonId"
          placeholder="请输入课时ID"
          clearable
          prefix-icon="Search"
          style="width: 220px"
          @keyup.enter="fetchData"
        />
        <!-- 查询按钮 -->
        <el-button type="primary" @click="fetchData">
          <el-icon><Search /></el-icon>查询
        </el-button>
      </div>

      <!-- ==================== 签到状态提示 ==================== -->
      <!-- 未输入课时ID时显示提示信息 -->
      <el-empty v-if="!lessonId" description="请输入课时ID查询签到记录" />

      <!-- ==================== 签到记录数据表格 ==================== -->
      <el-table v-else :data="tableData" stripe v-loading="loading" class="data-table">
        <!-- 学员信息列：显示学员ID -->
        <el-table-column prop="studentId" label="学员ID" width="180" show-overflow-tooltip />
        <!-- 签到时间列 -->
        <el-table-column label="签到时间" width="180">
          <template #default="{ row }">
            <div v-if="row.checkInTime" class="time-cell">
              <el-icon class="time-icon success"><Clock /></el-icon>
              <span>{{ row.checkInTime }}</span>
            </div>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <!-- 签退时间列 -->
        <el-table-column label="签退时间" width="180">
          <template #default="{ row }">
            <div v-if="row.checkOutTime" class="time-cell">
              <el-icon class="time-icon warning"><Clock /></el-icon>
              <span>{{ row.checkOutTime }}</span>
            </div>
            <span v-else class="empty-text">-</span>
          </template>
        </el-table-column>
        <!-- 签到方式列 -->
        <el-table-column label="签到方式" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="checkMethodColor(row.checkMethod)">
              {{ checkMethodText(row.checkMethod) }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 签到状态列：使用彩色标签展示不同状态 -->
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="checkStatusColor(row.status)" size="small" effect="light" round>
              {{ checkStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 备注列 -->
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.remark || '-' }}
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页栏：显示总记录数和分页控件 -->
      <div class="pagination-bar" v-if="lessonId">
        <span class="total-text">共 {{ total }} 条记录</span>
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          layout="prev, pager, next"
          @change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getCheckInRecords, checkIn, checkOut } from '../../api/index.js'
import { useUserStore } from '../../store/user'
import { ElMessage, ElMessageBox } from 'element-plus'

/** @description 用户状态 Store */
const userStore = useUserStore()

// ========== 列表数据状态 ==========
/** 课时ID输入框的值，作为查询签到记录的关键参数 */
const lessonId = ref('')
/** 表格数据加载状态，控制加载动画的显示/隐藏 */
const loading = ref(false)
/** 签到记录列表数据数组 */
const tableData = ref([])
/** 总记录数，用于分页组件 */
const total = ref(0)
/** 分页查询参数：当前页码和每页条数 */
const query = reactive({ pageNum: 1, pageSize: 10 })

// ========== 工具函数 ==========
/**
 * 将签到方式代码转换为中文文本
 * @param {number} m - 签到方式（1=GPS定位签到, 2=手动签到）
 * @returns {string} 中文签到方式名称
 */
const checkMethodText = (m) => ({ 1: 'GPS定位', 2: '手动签到' }[m] || '未知')

/**
 * 根据签到方式返回对应的标签颜色类型
 * @param {number} m - 签到方式
 * @returns {string} Element Plus 标签颜色类型
 */
const checkMethodColor = (m) => ({ 1: 'primary', 2: 'info' }[m] || 'info')

/**
 * 将签到状态代码转换为中文文本
 * @param {number} s - 签到状态（1=正常, 2=迟到, 3=早退, 4=缺勤）
 * @returns {string} 中文签到状态名称
 */
const checkStatusText = (s) => ({ 1: '正常', 2: '迟到', 3: '早退', 4: '缺勤' }[s] || '未知')

/**
 * 根据签到状态返回对应的标签颜色类型
 * @param {number} s - 签到状态
 * @returns {string} Element Plus 标签颜色类型
 */
const checkStatusColor = (s) => ({ 1: 'success', 2: 'warning', 3: 'warning', 4: 'danger' }[s] || 'info')

// ========== 数据请求函数 ==========
/**
 * 获取签到记录列表数据
 * 根据课时ID和分页参数请求后端接口
 */
const fetchData = async () => {
  // 未输入课时ID时不发送请求
  if (!lessonId.value) return

  loading.value = true
  try {
    const res = await getCheckInRecords(lessonId.value)
    tableData.value = res.data.records || []
    total.value = Number(res.data.total)
  } catch {
    // 请求失败时不处理，由拦截器统一提示
  } finally {
    loading.value = false
  }
}

/**
 * 执行签到操作
 * 弹出确认框确认后，调用签到接口
 */
const handleCheckIn = async () => {
  if (!lessonId.value) {
    ElMessage.warning('请先输入课时ID')
    return
  }
  try {
    await ElMessageBox.confirm(
      '确认进行签到操作？',
      '签到确认',
      { type: 'info', confirmButtonText: '确认签到', cancelButtonText: '取消' }
    )
    // 修复：请求体中携带 studentId，后端需要通过该字段关联学员与签到记录
    await checkIn(lessonId.value, { studentId: userStore.userInfo.id })
    ElMessage.success('签到成功')
    fetchData()
  } catch {
    // 用户取消或请求失败
  }
}

/**
 * 执行签退操作
 * 弹出确认框确认后，调用签退接口
 */
const handleCheckOut = async () => {
  if (!lessonId.value) {
    ElMessage.warning('请先输入课时ID')
    return
  }
  try {
    await ElMessageBox.confirm(
      '确认进行签退操作？',
      '签退确认',
      { type: 'warning', confirmButtonText: '确认签退', cancelButtonText: '取消' }
    )
    // 修复：请求体中携带 studentId，后端需要通过该字段关联学员与签退记录
    await checkOut(lessonId.value, { studentId: userStore.userInfo.id })
    ElMessage.success('签退成功')
    fetchData()
  } catch {
    // 用户取消或请求失败
  }
}
</script>

<style scoped>
/* 页面容器：垂直排列子元素，设置间距 */
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 主卡片圆角样式 */
.main-card {
  border-radius: 12px;
}

/* 覆盖卡片头部内边距 */
.main-card :deep(.el-card__header) {
  padding: 16px 20px;
}

/* 卡片头部布局：标题和按钮左右对齐 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 卡片标题样式：图标+文字水平排列，加粗显示 */
.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

/* 头部操作按钮区域布局 */
.header-actions {
  display: flex;
  gap: 8px;
}

/* 搜索栏布局：水平排列筛选项，支持换行 */
.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

/* 数据表格圆角样式 */
.data-table {
  border-radius: 8px;
}

/* 时间单元格布局：图标+文字水平排列 */
.time-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 时间图标样式 */
.time-icon {
  font-size: 14px;
}

/* 签到时间图标：绿色 */
.time-icon.success {
  color: #10b981;
}

/* 签退时间图标：橙色 */
.time-icon.warning {
  color: #f59e0b;
}

/* 空数据占位文字样式 */
.empty-text {
  color: #c0c4cc;
}

/* 分页栏布局：总数和分页按钮左右对齐 */
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
</style>
