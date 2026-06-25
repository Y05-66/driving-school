<!--
  课时预约管理页面
  功能说明：
  - 展示课时预约列表，显示学员/教练信息、日期时间、状态、实际学时
  - 支持按预约状态和日期筛选预约记录
  - 新建课时预约：填写学员ID、教练ID、课程ID、日期和时间段
  - 预约状态流转：待确认 -> 已确认 -> 已完成 / 已取消
  - 操作权限：确认（待确认状态）、完成（已确认状态，需输入实际学时）、取消（待确认或已确认状态）
-->
<template>
  <div class="page-container">
    <!-- 主内容卡片 -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <!-- 卡片头部：标题 + 新建预约按钮 -->
        <div class="card-header">
          <div class="card-title"><el-icon><Calendar /></el-icon><span>课时预约</span></div>
          <el-button type="primary" @click="showAdd"><el-icon><Plus /></el-icon>新建预约</el-button>
        </div>
      </template>

      <!-- 搜索筛选栏：状态筛选、日期选择、查询和重置按钮 -->
      <div class="search-bar">
        <!-- 预约状态下拉筛选 -->
        <el-select v-model="query.status" placeholder="状态筛选" clearable style="width:130px">
          <el-option label="待确认" :value="0" /><el-option label="已确认" :value="1" />
          <el-option label="已完成" :value="3" /><el-option label="已取消" :value="4" />
        </el-select>
        <!-- 预约日期选择器 -->
        <el-date-picker v-model="query.lessonDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" clearable style="width:160px" />
        <!-- 查询按钮 -->
        <el-button type="primary" @click="fetchData"><el-icon><Search /></el-icon>查询</el-button>
        <!-- 重置筛选条件按钮 -->
        <el-button @click="query.status=null;query.lessonDate=null;fetchData()"><el-icon><Refresh /></el-icon>重置</el-button>
      </div>

      <!-- 课时预约数据表格 -->
      <el-table :data="tableData" stripe v-loading="loading" class="data-table">
        <!-- 学员/教练信息列：上下两行分别显示学员ID和教练ID -->
        <el-table-column label="学员/教练" min-width="180">
          <template #default="{ row }">
            <div style="display:flex;flex-direction:column;gap:2px">
              <span style="font-size:13px">学员ID: {{ row.studentId }}</span>
              <span style="font-size:12px;color:#94a3b8">教练ID: {{ row.coachId }}</span>
            </div>
          </template>
        </el-table-column>
        <!-- 日期时间列：上行显示日期，下行显示起止时间段 -->
        <el-table-column label="日期时间" width="180">
          <template #default="{ row }">
            <div>{{ row.lessonDate }}</div>
            <div style="font-size:12px;color:#94a3b8">{{ row.startTime }} - {{ row.endTime }}</div>
          </template>
        </el-table-column>
        <!-- 状态列：使用彩色圆角标签展示不同状态 -->
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="light" round>
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 实际学时列：已完成时显示学时数，否则显示"-" -->
        <el-table-column prop="actualHours" label="实际学时" width="90" align="center">
          <template #default="{ row }">{{ row.actualHours ? row.actualHours + 'h' : '-' }}</template>
        </el-table-column>
        <!-- 操作列：根据状态显示不同的操作按钮 -->
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <!-- 确认按钮：仅待确认状态可见 -->
            <el-button link type="success" size="small" @click="handleConfirm(row)" v-if="row.status===0">确认</el-button>
            <!-- 完成按钮：仅已确认状态可见 -->
            <el-button link type="primary" size="small" @click="handleComplete(row)" v-if="row.status===1">完成</el-button>
            <!-- 取消按钮：待确认或已确认状态可见 -->
            <el-button link type="danger" size="small" @click="handleCancel(row)" v-if="row.status<=1">取消</el-button>
            <!-- 已完成/已取消状态显示禁用标签 -->
            <el-button link type="info" size="small" disabled v-if="row.status>=3">{{ statusText(row.status) }}</el-button>
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

    <!-- 新建预约对话框：填写学员、教练、课程、日期和时间段 -->
    <el-dialog v-model="addVisible" title="新建预约" width="500px" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="80px">
        <!-- 学员ID和教练ID（同行排列） -->
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="学员ID" prop="studentId"><el-input v-model="addForm.studentId" placeholder="学员ID" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="教练ID" prop="coachId"><el-input v-model="addForm.coachId" placeholder="教练ID" /></el-form-item></el-col>
        </el-row>
        <!-- 课程ID输入 -->
        <el-form-item label="课程ID" prop="courseId"><el-input v-model="addForm.courseId" placeholder="课程ID" /></el-form-item>
        <!-- 预约日期选择 -->
        <el-form-item label="预约日期" prop="lessonDate"><el-date-picker v-model="addForm.lessonDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" /></el-form-item>
        <!-- 开始时间和结束时间（同行排列） -->
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="开始时间" prop="startTime"><el-time-picker v-model="addForm.startTime" format="HH:mm" value-format="HH:mm" placeholder="开始" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="结束时间" prop="endTime"><el-time-picker v-model="addForm.endTime" format="HH:mm" value-format="HH:mm" placeholder="结束" style="width:100%" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="addVisible=false">取消</el-button>
        <el-button type="primary" @click="submitAdd" :loading="submitting">确认预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getLessons, createLesson, confirmLesson, completeLesson, cancelLesson } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

// ========== 列表数据状态 ==========
/** 表格数据加载状态 */
const loading = ref(false)
/** 课时预约列表数据数组 */
const tableData = ref([])
/** 总记录数，用于分页 */
const total = ref(0)
/** 查询参数：状态筛选、日期筛选、页码、每页条数 */
const query = reactive({ status: null, lessonDate: null, pageNum: 1, pageSize: 10 })

// ========== 新建预约表单状态 ==========
/** 控制新建预约对话框的显示/隐藏 */
const addVisible = ref(false)
/** 新建预约表单数据：学员ID、教练ID、课程ID、预约日期、开始时间、结束时间 */
const addForm = reactive({ studentId: '', coachId: '', courseId: '', lessonDate: '', startTime: '', endTime: '' })
/** 表单引用 */
const addFormRef = ref()
/** 提交状态 */
const submitting = ref(false)
/** 表单验证规则 */
const addRules = {
  studentId: [{ required: true, message: '请输入学员ID', trigger: 'blur' }],
  coachId: [{ required: true, message: '请输入教练ID', trigger: 'blur' }],
  lessonDate: [{ required: true, message: '请选择预约日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

// ========== 工具函数 ==========
/**
 * 将预约状态码转换为中文文本
 * @param {number} s - 状态码（0=待确认, 1=已确认, 2=进行中, 3=已完成, 4=已取消）
 * @returns {string} 中文状态名称
 */
const statusText = (s) => ({ 0: '待确认', 1: '已确认', 2: '进行中', 3: '已完成', 4: '已取消' }[s] || '未知')

/**
 * 根据预约状态返回对应的标签颜色类型
 * @param {number} s - 状态码
 * @returns {string} Element Plus 标签颜色类型
 */
const statusType = (s) => ({ 0: 'warning', 1: 'primary', 2: '', 3: 'success', 4: 'info' }[s] || 'info')

// ========== 数据请求函数 ==========
/**
 * 获取课时预约列表数据
 * 根据分页和筛选参数请求后端接口
 */
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getLessons(query)
    tableData.value = res.data.records
    total.value = Number(res.data.total)
  } catch {} finally { loading.value = false }
}

/**
 * 打开新建预约对话框
 */
const showAdd = () => {
  addForm.studentId = ''
  addForm.coachId = ''
  addForm.courseId = ''
  addForm.lessonDate = ''
  addForm.startTime = ''
  addForm.endTime = ''
  addVisible.value = true
}

/**
 * 提交新建预约表单
 * 成功后关闭对话框并刷新列表
 */
const submitAdd = async () => {
  try { await addFormRef.value.validate() } catch { return }
  submitting.value = true
  try {
    await createLesson(addForm)
    ElMessage.success('预约成功')
    addVisible.value = false
    fetchData()
  } catch {} finally { submitting.value = false }
}

/**
 * 确认预约操作
 * 将预约状态从"待确认"变更为"已确认"
 * @param {Object} row - 当前预约行数据
 */
const handleConfirm = async (row) => {
  try {
    await confirmLesson(row.id)
    ElMessage.success('已确认')
    fetchData()
  } catch {}
}

/**
 * 完成课时操作
 * 弹出输入框让用户输入实际学时数，验证为有效数字后提交
 * @param {Object} row - 当前预约行数据
 */
const handleComplete = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入实际学时', '完成课时', {
      inputPattern: /^\d+(\.\d+)?$/, inputErrorMessage: '请输入有效数字'
    })
    await completeLesson(row.id, parseFloat(value))
    ElMessage.success('已完成')
    fetchData()
  } catch {}
}

/**
 * 取消预约操作
 * 弹出确认框，确认后取消该预约
 * @param {Object} row - 当前预约行数据
 */
const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm('确定取消该预约？', '提示', { type: 'warning' })
    await cancelLesson(row.id)
    ElMessage.success('已取消')
    fetchData()
  } catch {}
}

// ========== 生命周期 ==========
/** 组件挂载后自动加载预约列表数据 */
onMounted(fetchData)
</script>

<style scoped>
/* 页面容器：垂直排列子元素 */
.page-container { display: flex; flex-direction: column; gap: 16px; }
/* 主卡片圆角样式 */
.main-card { border-radius: 12px; }
/* 覆盖卡片头部内边距 */
.main-card :deep(.el-card__header) { padding: 16px 20px; }
/* 卡片头部布局：标题和按钮左右对齐 */
.card-header { display: flex; justify-content: space-between; align-items: center; }
/* 卡片标题样式 */
.card-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: #1e293b; }
/* 搜索栏布局：水平排列筛选项，支持换行 */
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; align-items: center; }
/* 数据表格圆角样式 */
.data-table { border-radius: 8px; }
/* 分页栏布局 */
.pagination-bar { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; }
/* 总记录数文字样式 */
.total-text { font-size: 13px; color: #94a3b8; }
</style>
