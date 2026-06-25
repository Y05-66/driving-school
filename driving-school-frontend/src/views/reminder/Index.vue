<!--
  提醒管理页面
  功能说明：
  - 展示提醒列表：管理员查看所有提醒，学员/教练查看自己的提醒
  - 创建提醒：管理员可创建各类提醒（课前提醒、考试提醒、合同到期、保险到期、自定义）
  - 取消提醒：对待发送状态的提醒进行取消操作
  - 类型标签：课前提醒、考试提醒、合同到期、保险到期、自定义
  - 状态标签：待发送（warning）、已发送（success）、已取消（info）

  API 调用：
  - 我的提醒：GET /reminders/my
  - 创建提醒：POST /reminders
  - 取消提醒：PUT /reminders/{id}/cancel
-->
<template>
  <div class="page-container">
    <!-- ==================== 主内容卡片 ==================== -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <!-- 卡片头部：标题 + 创建提醒按钮（仅管理员可见） -->
        <div class="card-header">
          <div class="card-title">
            <el-icon><Bell /></el-icon>
            <span>提醒管理</span>
          </div>
          <!-- 创建提醒按钮：仅管理员角色可见 -->
          <el-button type="primary" @click="showAdd" v-if="['ADMIN'].includes(role)">
            <el-icon><Plus /></el-icon>创建提醒
          </el-button>
        </div>
      </template>

      <!-- ==================== 搜索筛选栏 ==================== -->
      <div class="search-bar">
        <!-- 提醒类型下拉筛选 -->
        <el-select v-model="query.type" placeholder="提醒类型" clearable style="width: 130px">
          <el-option label="课前提醒" :value="1" />
          <el-option label="考试提醒" :value="2" />
          <el-option label="合同到期" :value="3" />
          <el-option label="保险到期" :value="4" />
          <el-option label="自定义" :value="5" />
        </el-select>
        <!-- 提醒状态下拉筛选 -->
        <el-select v-model="query.status" placeholder="提醒状态" clearable style="width: 130px">
          <el-option label="待发送" :value="0" />
          <el-option label="已发送" :value="1" />
          <el-option label="已取消" :value="2" />
        </el-select>
        <!-- 查询按钮 -->
        <el-button type="primary" @click="fetchData">
          <el-icon><Search /></el-icon>查询
        </el-button>
        <!-- 重置筛选条件按钮 -->
        <el-button @click="resetQuery">
          <el-icon><Refresh /></el-icon>重置
        </el-button>
      </div>

      <!-- ==================== 提醒数据表格 ==================== -->
      <el-table :data="tableData" stripe v-loading="loading" class="data-table">
        <!-- 提醒类型列：使用彩色标签展示不同类型 -->
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="typeColor(row.type)" size="small" effect="light">
              {{ typeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 提醒标题列 -->
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <!-- 提醒内容列 -->
        <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="content-text">{{ row.content || '-' }}</span>
          </template>
        </el-table-column>
        <!-- 目标用户列 -->
        <el-table-column label="目标用户" width="160">
          <template #default="{ row }">
            <div class="target-cell">
              <span class="target-user">{{ row.targetUserName || row.targetUserId || '-' }}</span>
              <span class="target-role" v-if="row.targetRole">{{ row.targetRole }}</span>
            </div>
          </template>
        </el-table-column>
        <!-- 提醒时间列 -->
        <el-table-column prop="remindTime" label="提醒时间" width="170">
          <template #default="{ row }">
            {{ row.remindTime || '-' }}
          </template>
        </el-table-column>
        <!-- 提醒状态列：使用彩色标签展示不同状态 -->
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusColor(row.status)" size="small" effect="light" round>
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 操作列：取消提醒按钮 -->
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <!-- 取消按钮：仅待发送状态可见 -->
            <el-button
              link
              type="danger"
              size="small"
              @click="handleCancel(row)"
              v-if="row.status === 0"
            >取消</el-button>
            <!-- 已发送/已取消状态提示 -->
            <el-button
              link
              type="info"
              size="small"
              disabled
              v-if="row.status === 1"
            >已发送</el-button>
            <el-button
              link
              type="info"
              size="small"
              disabled
              v-if="row.status === 2"
            >已取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页栏：显示总记录数和分页控件 -->
      <div class="pagination-bar">
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

    <!-- ==================== 创建提醒对话框 ==================== -->
    <el-dialog v-model="addVisible" title="创建提醒" width="550" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="90px">
        <!-- 提醒类型选择 -->
        <el-form-item label="提醒类型" prop="type">
          <el-select v-model="addForm.type" style="width: 100%">
            <el-option label="课前提醒" :value="1" />
            <el-option label="考试提醒" :value="2" />
            <el-option label="合同到期" :value="3" />
            <el-option label="保险到期" :value="4" />
            <el-option label="自定义" :value="5" />
          </el-select>
        </el-form-item>
        <!-- 提醒标题输入 -->
        <el-form-item label="提醒标题" prop="title">
          <el-input v-model="addForm.title" placeholder="请输入提醒标题" />
        </el-form-item>
        <!-- 提醒内容输入（多行文本域） -->
        <el-form-item label="提醒内容">
          <el-input
            v-model="addForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入提醒内容"
          />
        </el-form-item>
        <!-- 目标用户ID和提醒时间（同行排列） -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="目标用户">
              <el-input v-model="addForm.targetUserId" placeholder="用户ID" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="提醒时间">
              <el-date-picker
                v-model="addForm.remindTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="选择提醒时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 目标角色选择 -->
        <el-form-item label="目标角色">
          <el-select v-model="addForm.targetRole" style="width: 100%" clearable>
            <el-option label="学员" value="STUDENT" />
            <el-option label="教练" value="COACH" />
            <el-option label="前台" value="STAFF" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确认创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '../../store/user'
import { get, post, put } from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

// ========== 用户状态与权限 ==========
/** 用户状态存储，用于获取当前用户信息和角色 */
const userStore = useUserStore()
/** 当前登录用户的角色（ADMIN/STAFF/COACH/STUDENT） */
const role = computed(() => userStore.userInfo.role)

// ========== 列表数据状态 ==========
/** 表格数据加载状态，控制加载动画的显示/隐藏 */
const loading = ref(false)
/** 提醒列表数据数组 */
const tableData = ref([])
/** 总记录数，用于分页组件 */
const total = ref(0)
/** 查询参数：类型筛选、状态筛选、页码、每页条数 */
const query = reactive({ type: null, status: null, pageNum: 1, pageSize: 10 })

// ========== 创建提醒表单状态 ==========
/** 控制创建提醒对话框的显示/隐藏 */
const addVisible = ref(false)
/** 创建提醒表单引用 */
const addFormRef = ref(null)
/** 创建提醒的表单数据 */
const addForm = reactive({
  type: 1,
  title: '',
  content: '',
  targetUserId: '',
  targetRole: '',
  remindTime: ''
})
/** 创建提醒表单验证规则 */
const addRules = {
  // 修复：提醒类型为必填项，决定提醒的业务分类
  type: [{ required: true, message: '请选择提醒类型', trigger: 'change' }],
  // 修复：提醒标题为必填项，用于展示提醒摘要
  title: [{ required: true, message: '请输入提醒标题', trigger: 'blur' }],
  // 修复：提醒时间为必填项，决定提醒何时发送
  remindTime: [{ required: true, message: '请选择提醒时间', trigger: 'change' }]
}

// ========== 工具函数 ==========
/**
 * 将提醒类型代码转换为中文文本
 * @param {number} t - 提醒类型（1=课前提醒, 2=考试提醒, 3=合同到期, 4=保险到期, 5=自定义）
 * @returns {string} 中文类型名称
 */
const typeText = (t) => ({
  1: '课前提醒',
  2: '考试提醒',
  3: '合同到期',
  4: '保险到期',
  5: '自定义'
}[t] || '未知')

/**
 * 根据提醒类型返回对应的标签颜色类型
 * @param {number} t - 提醒类型
 * @returns {string} Element Plus 标签颜色类型
 */
const typeColor = (t) => ({
  1: 'primary',
  2: 'success',
  3: 'warning',
  4: 'danger',
  5: 'info'
}[t] || 'info')

/**
 * 将提醒状态代码转换为中文文本
 * @param {number} s - 状态码（0=待发送, 1=已发送, 2=已取消）
 * @returns {string} 中文状态名称
 */
const statusText = (s) => ({ 0: '待发送', 1: '已发送', 2: '已取消' }[s] || '未知')

/**
 * 根据提醒状态返回对应的标签颜色类型
 * @param {number} s - 状态码
 * @returns {string} Element Plus 标签颜色类型（warning/success/info）
 */
const statusColor = (s) => ({ 0: 'warning', 1: 'success', 2: 'info' }[s] || 'info')

// ========== 数据请求函数 ==========
/**
 * 获取我的提醒列表数据
 * 根据分页和筛选参数请求后端接口
 */
const fetchData = async () => {
  loading.value = true
  try {
    // 构建查询参数
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.type !== null && query.type !== '') params.type = query.type
    if (query.status !== null && query.status !== '') params.status = query.status

    const res = await get('/reminders/my', { params })
    // 兼容分页和列表两种返回格式
    const data = res.data
    if (Array.isArray(data)) {
      tableData.value = data
      total.value = data.length
    } else {
      tableData.value = data.records || []
      total.value = Number(data.total || 0)
    }
  } catch {
    // 请求失败时不处理，由拦截器统一提示
  } finally {
    loading.value = false
  }
}

/**
 * 重置所有查询条件并重新加载数据
 * 清空类型、状态筛选，重置到第一页
 */
const resetQuery = () => {
  query.type = null
  query.status = null
  query.pageNum = 1
  fetchData()
}

/**
 * 打开创建提醒对话框
 */
const showAdd = () => {
  addForm.type = 1
  addForm.title = ''
  addForm.content = ''
  addForm.targetUserId = ''
  addForm.targetRole = ''
  addForm.remindTime = ''
  addVisible.value = true
}

/**
 * 提交创建提醒表单
 * 校验必填字段，调用接口创建提醒，成功后关闭对话框并刷新列表
 */
const submitAdd = async () => {
  if (!addFormRef.value) return
  try {
    await addFormRef.value.validate()
  } catch {
    return
  }
  try {
    await post('/reminders', addForm)
    ElMessage.success('提醒创建成功')
    addVisible.value = false
    // 重置表单
    addForm.type = 1
    addForm.title = ''
    addForm.content = ''
    addForm.targetUserId = ''
    addForm.targetRole = ''
    addForm.remindTime = ''
    fetchData()
  } catch {
    // 请求失败由拦截器处理
  }
}

/**
 * 取消提醒操作
 * 弹出确认框，确认后调用取消接口
 * @param {Object} row - 当前提醒行数据，包含提醒id等信息
 */
const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定取消提醒「${row.title}」？`,
      '取消确认',
      { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '返回' }
    )
    await put(`/reminders/${row.id}/cancel`)
    ElMessage.success('提醒已取消')
    fetchData()
  } catch {
    // 用户取消或请求失败
  }
}

// ========== 生命周期 ==========
/** 组件挂载后自动加载提醒列表数据 */
onMounted(fetchData)
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

/* 提醒内容文字样式：灰色，限制最大宽度 */
.content-text {
  color: #64748b;
  font-size: 13px;
}

/* 目标用户单元格布局 */
.target-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* 目标用户名文字样式 */
.target-user {
  font-size: 13px;
  color: #1e293b;
}

/* 目标角色文字样式 */
.target-role {
  font-size: 12px;
  color: #94a3b8;
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
