<!--
  考试安排管理页面
  功能说明：
  - 展示所有考试安排列表（考试类型、日期、地点、最大人数、适用车型）
  - 管理员/前台角色可创建新考试安排
  - 所有角色可为学员报名参加考试（输入学员ID）
  - 支持分页浏览考试列表
-->
<template>
  <div class="page-container">
    <!-- 主内容卡片 -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <!-- 卡片头部：标题 + 创建考试按钮（仅管理员/前台可见） -->
        <div class="card-header">
          <div class="card-title"><el-icon><EditPen /></el-icon><span>考试安排</span></div>
          <el-button type="primary" @click="showAdd" v-if="['ADMIN','STAFF'].includes(role)"><el-icon><Plus /></el-icon>创建考试</el-button>
        </div>
      </template>

      <!-- 考试数据表格：展示考试类型、日期、地点、人数、车型等信息 -->
      <el-table :data="tableData" stripe v-loading="loading" class="data-table">
        <!-- 考试类型列：使用标签展示不同科目类型及对应颜色 -->
        <el-table-column label="考试类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="examTypeColor(row.examType)" size="small" effect="light">{{ examTypeText(row.examType) }}</el-tag>
          </template>
        </el-table-column>
        <!-- 考试日期列 -->
        <el-table-column prop="examDate" label="考试日期" width="120" />
        <!-- 考试地点列 -->
        <el-table-column prop="examLocation" label="考试地点" min-width="150" />
        <!-- 最大报名人数列 -->
        <el-table-column prop="maxCandidates" label="最大人数" width="90" align="center" />
        <!-- 适用车型列：使用标签展示 -->
        <el-table-column prop="applyType" label="适用车型" width="90" align="center">
          <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.applyType || '-' }}</el-tag></template>
        </el-table-column>
        <!-- 操作列：报名按钮 -->
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEnroll(row)">报名</el-button>
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

    <!-- 创建考试对话框：填写考试类型、日期、最大人数、地点、车型等信息 -->
    <el-dialog v-model="addVisible" title="创建考试" width="500" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="80px">
        <!-- 考试类型选择：科目一至科目四 -->
        <el-form-item label="考试类型">
          <el-select v-model="addForm.examType" style="width:100%">
            <el-option label="科目一" value="SUBJECT_1" /><el-option label="科目二" value="SUBJECT_2" />
            <el-option label="科目三" value="SUBJECT_3" /><el-option label="科目四" value="SUBJECT_4" />
          </el-select>
        </el-form-item>
        <!-- 考试日期和最大人数（同行排列） -->
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="考试日期"><el-date-picker v-model="addForm.examDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="最大人数"><el-input-number v-model="addForm.maxCandidates" :min="1" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <!-- 考试地点输入 -->
        <el-form-item label="考试地点"><el-input v-model="addForm.examLocation" placeholder="请输入考试地点" /></el-form-item>
        <!-- 适用车型选择：C1/C2 -->
        <el-form-item label="适用车型">
          <el-select v-model="addForm.applyType" style="width:100%"><el-option label="C1" value="C1" /><el-option label="C2" value="C2" /></el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible=false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确认创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '../../store/user'
import { getExams, createExam, enrollExam } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

// ========== 用户状态与权限 ==========
/** 用户状态存储，用于获取当前用户信息和角色 */
const userStore = useUserStore()
/** 当前登录用户的角色（ADMIN/STAFF/COACH/STUDENT） */
const role = computed(() => userStore.userInfo.role)

// ========== 列表数据状态 ==========
/** 表格数据加载状态，控制加载动画的显示/隐藏 */
const loading = ref(false)
/** 考试列表数据数组 */
const tableData = ref([])
/** 总记录数，用于分页组件 */
const total = ref(0)
/** 分页查询参数：当前页码和每页条数 */
const query = reactive({ pageNum: 1, pageSize: 20 })

// ========== 创建考试表单状态 ==========
/** 控制创建考试对话框的显示/隐藏 */
const addVisible = ref(false)
/** 创建考试表单引用 */
const addFormRef = ref(null)
/** 创建考试的表单数据：考试类型、日期、地点、最大人数、适用车型 */
const addForm = reactive({ examType: 'SUBJECT_1', examDate: '', examLocation: '', maxCandidates: 50, applyType: 'C1' })
/** 创建考试表单验证规则 */
const addRules = {
  examType: [{ required: true, message: '请选择考试类型', trigger: 'change' }],
  examDate: [{ required: true, message: '请选择考试日期', trigger: 'change' }],
  examLocation: [{ required: true, message: '请输入考试地点', trigger: 'blur' }],
  applyType: [{ required: true, message: '请选择适用车型', trigger: 'change' }]
}

// ========== 工具函数 ==========
/**
 * 将考试类型代码转换为中文文本
 * @param {string} t - 考试类型代码（SUBJECT_1/SUBJECT_2/SUBJECT_3/SUBJECT_4）
 * @returns {string} 中文考试类型名称
 */
const examTypeText = (t) => ({ SUBJECT_1: '科目一', SUBJECT_2: '科目二', SUBJECT_3: '科目三', SUBJECT_4: '科目四' }[t] || t)

/**
 * 根据考试类型返回对应的标签颜色类型
 * @param {string} t - 考试类型代码
 * @returns {string} Element Plus 标签颜色类型（primary/success/warning/danger）
 */
const examTypeColor = (t) => ({ SUBJECT_1: 'primary', SUBJECT_2: 'success', SUBJECT_3: 'warning', SUBJECT_4: 'danger' }[t] || 'info')

// ========== 数据请求函数 ==========
/**
 * 获取考试列表数据
 * 根据分页参数请求后端接口，更新表格数据和总记录数
 */
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getExams(query)
    tableData.value = res.data.records || []
    total.value = Number(res.data.total)
  } catch {} finally { loading.value = false }
}

/**
 * 打开创建考试对话框
 */
const showAdd = () => {
  addForm.examType = 'SUBJECT_1'
  addForm.examDate = ''
  addForm.examLocation = ''
  addForm.maxCandidates = 50
  addForm.applyType = 'C1'
  addVisible.value = true
}

/**
 * 提交创建考试表单
 * 调用接口创建考试，成功后关闭对话框并刷新列表
 */
const submitAdd = async () => {
  if (!addFormRef.value) return
  try {
    await addFormRef.value.validate()
    await createExam(addForm)
    ElMessage.success('创建成功')
    addVisible.value = false
    fetchData()
  } catch {}
}

/**
 * 处理考试报名操作
 * 弹出输入框让用户输入学员ID，然后调用报名接口
 * @param {Object} row - 当前考试行数据，包含考试id等信息
 */
const handleEnroll = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入学员ID', '考试报名', {
      inputPlaceholder: '学员ID',
      inputValidator: (val) => val && val.trim() ? true : '请输入学员ID'
    })
    await enrollExam(row.id, value.trim())
    ElMessage.success('报名成功')
  } catch {}
}

// ========== 生命周期 ==========
/** 组件挂载后自动加载考试列表数据 */
onMounted(fetchData)
</script>

<style scoped>
/* 页面容器：垂直排列子元素，设置间距 */
.page-container { display: flex; flex-direction: column; gap: 16px; }
/* 主卡片圆角样式 */
.main-card { border-radius: 12px; }
/* 覆盖卡片头部内边距 */
.main-card :deep(.el-card__header) { padding: 16px 20px; }
/* 卡片头部布局：标题和按钮左右对齐 */
.card-header { display: flex; justify-content: space-between; align-items: center; }
/* 卡片标题样式：图标+文字水平排列，加粗显示 */
.card-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: #1e293b; }
/* 数据表格圆角样式 */
.data-table { border-radius: 8px; }
/* 分页栏布局：总数和分页按钮左右对齐 */
.pagination-bar { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; }
/* 总记录数文字样式 */
.total-text { font-size: 13px; color: #94a3b8; }
</style>
