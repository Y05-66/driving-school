<!--
  操作日志管理页面
  功能说明：
  - 展示系统操作日志列表，记录用户的操作行为
  - 显示操作人（带头像首字母）、操作类型、调用方法、IP地址、耗时、时间、请求参数
  - 耗时超过 1000ms 以红色显示，正常以绿色显示，便于快速定位性能问题
  - 支持分页浏览日志记录
  - 该页面为只读查看，无新增/编辑/删除操作
-->
<template>
  <div class="page-container">
    <!-- 主内容卡片 -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <!-- 卡片标题 -->
        <div class="card-header">
          <div class="card-title"><el-icon><Document /></el-icon><span>操作日志</span></div>
        </div>
      </template>

      <!-- 操作日志数据表格：小尺寸展示更多列信息 -->
      <el-table :data="tableData" stripe v-loading="loading" class="data-table" size="small">
        <!-- 操作人列：显示头像首字母 + 用户名 -->
        <el-table-column prop="username" label="操作人" width="100">
          <template #default="{ row }">
            <div class="cell-with-avatar">
              <!-- 头像：取用户名首字符，紫色背景 -->
              <el-avatar :size="24" style="background:#6366f1;font-size:11px">{{ (row.username||'?').charAt(0) }}</el-avatar>
              <span>{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <!-- 操作类型列：使用标签展示 -->
        <el-table-column prop="operation" label="操作" width="140">
          <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.operation }}</el-tag></template>
        </el-table-column>
        <!-- 调用方法列：等宽字体显示，溢出省略 -->
        <el-table-column prop="method" label="方法" width="200" show-overflow-tooltip>
          <template #default="{ row }"><span style="font-family:monospace;font-size:12px">{{ row.method }}</span></template>
        </el-table-column>
        <!-- IP 地址列 -->
        <el-table-column prop="ip" label="IP" width="130" />
        <!-- 耗时列：超过 1000ms 红色警告，正常绿色 -->
        <el-table-column label="耗时" width="80" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.duration > 1000 ? '#ef4444' : '#10b981' }">{{ row.duration }}ms</span>
          </template>
        </el-table-column>
        <!-- 操作时间列 -->
        <el-table-column prop="createTime" label="时间" width="170" />
        <!-- 请求参数列：灰色小字，溢出省略 -->
        <el-table-column prop="params" label="参数" min-width="150" show-overflow-tooltip>
          <template #default="{ row }"><span style="font-size:12px;color:#64748b">{{ row.params || '-' }}</span></template>
        </el-table-column>
      </el-table>

      <!-- 分页栏：显示总记录数和分页控件 -->
      <div class="pagination-bar">
        <span class="total-text">共 {{ total }} 条记录</span>
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
          :total="total" layout="prev, pager, next" @change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getLogs } from '../../api'

// ========== 列表数据状态 ==========
/** 表格数据加载状态 */
const loading = ref(false)
/** 操作日志列表数据数组 */
const tableData = ref([])
/** 总记录数，用于分页 */
const total = ref(0)
/** 分页查询参数：当前页码和每页条数（日志较多，默认20条/页） */
const query = reactive({ pageNum: 1, pageSize: 20 })

// ========== 数据请求函数 ==========
/**
 * 获取操作日志列表数据
 * 根据分页参数请求后端接口，更新表格数据和总记录数
 */
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getLogs(query)
    tableData.value = res.data.records
    total.value = Number(res.data.total)
  } catch {} finally { loading.value = false }
}

// ========== 生命周期 ==========
/** 组件挂载后自动加载日志列表数据 */
onMounted(fetchData)
</script>

<style scoped>
/* 页面容器：垂直排列子元素 */
.page-container { display: flex; flex-direction: column; gap: 16px; }
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
/* 操作人单元格布局：头像和用户名水平排列 */
.cell-with-avatar { display: flex; align-items: center; gap: 6px; }
/* 分页栏布局 */
.pagination-bar { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; }
/* 总记录数文字样式 */
.total-text { font-size: 13px; color: #94a3b8; }
</style>
