<!--
  @description 教练管理页面
  提供教练信息的完整管理功能，包括：
  1. 搜索栏：支持按姓名、状态筛选教练
  2. 教练列表：展示教练基本信息、评分、状态，支持查看详情、分配学员、状态切换
  3. 添加教练对话框：管理员可新增教练
  4. 分配学员对话框：管理员/前台可将学员分配给教练并指定负责科目

  权限控制：
  - 管理员：可添加教练、分配学员、切换教练状态
  - 前台：可分配学员
  - 教练/学员：仅可查看列表和详情
-->

<template>
  <div class="page-container">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><UserFilled /></el-icon>
            <span>教练列表</span>
          </div>
          <!-- 添加教练按钮：仅管理员可见 -->
          <el-button type="primary" @click="showAdd" v-if="role==='ADMIN'">
            <el-icon><Plus /></el-icon>添加教练
          </el-button>
        </div>
      </template>

      <!-- ==================== 搜索栏 ==================== -->
      <div class="search-bar">
        <el-input v-model="query.name" placeholder="搜索姓名" clearable prefix-icon="Search" style="width:180px" @keyup.enter="fetchData" />
        <el-select v-model="query.status" placeholder="状态筛选" clearable style="width:130px">
          <el-option label="在岗" :value="1" /><el-option label="休假" :value="0" /><el-option label="离职" :value="2" />
        </el-select>
        <el-button type="primary" @click="fetchData"><el-icon><Search /></el-icon>查询</el-button>
        <el-button @click="query.name='';query.status=null;fetchData()"><el-icon><Refresh /></el-icon>重置</el-button>
      </div>

      <!-- ==================== 教练数据表格 ==================== -->
      <el-table :data="tableData" stripe v-loading="loading" class="data-table">
        <!-- 教练姓名列：带头像和准教车型 -->
        <el-table-column prop="name" label="教练姓名" min-width="120">
          <template #default="{ row }">
            <div class="cell-with-avatar">
              <el-avatar :size="28" :style="{ background: getAvatarColor(row.name) }">{{ (row.name||'?').charAt(0) }}</el-avatar>
              <div>
                <div style="font-weight:500">{{ row.name }}</div>
                <div style="font-size:11px;color:#94a3b8">{{ row.coachType || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" width="130" />
        <!-- 从业年限 -->
        <el-table-column prop="experienceYears" label="从业年限" width="90" align="center">
          <template #default="{ row }">{{ row.experienceYears ? row.experienceYears + '年' : '-' }}</template>
        </el-table-column>
        <!-- 评分：使用 el-rate 星级评分组件 -->
        <el-table-column label="评分" width="120" align="center">
          <template #default="{ row }">
            <div class="rating-cell" v-if="row.rating">
              <el-rate :model-value="Number(row.rating)" disabled show-score text-color="#f59e0b" score-template="{value}" size="small" />
            </div>
            <span v-else style="color:#94a3b8;font-size:13px">暂无评分</span>
          </template>
        </el-table-column>
        <!-- 状态标签 -->
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status===1?'success':row.status===0?'warning':'info'" size="small" effect="light" round>
              {{ row.status===1?'在岗':row.status===0?'休假':'离职' }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 操作列 -->
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/coaches/${row.id}`)">详情</el-button>
            <!-- 分配学员：管理员/前台可见 -->
            <el-button link type="primary" size="small" @click="showAssign(row)" v-if="['ADMIN','STAFF'].includes(role)">分配学员</el-button>
            <!-- 切换状态：仅管理员可见 -->
            <el-button link type="danger" size="small" @click="toggleStatus(row)" v-if="role==='ADMIN'">
              {{ row.status===1?'设为休假':'设为在岗' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页栏 -->
      <div class="pagination-bar">
        <span class="total-text">共 {{ total }} 条记录</span>
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
          :total="total" layout="prev, pager, next" @change="fetchData" />
      </div>
    </el-card>

    <!-- ==================== 添加教练对话框 ==================== -->
    <el-dialog v-model="addVisible" title="添加教练" width="520" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name"><el-input v-model="addForm.name" placeholder="请输入姓名" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电话" prop="phone"><el-input v-model="addForm.phone" placeholder="请输入电话" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="身份证"><el-input v-model="addForm.idCard" placeholder="请输入身份证号" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="准教车型">
              <el-select v-model="addForm.coachType" style="width:100%">
                <el-option label="C1 手动挡" value="C1" /><el-option label="C2 自动挡" value="C2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="从业年限"><el-input-number v-model="addForm.experienceYears" :min="0" :max="50" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible=false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAdd">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 分配学员对话框 ==================== -->
    <el-dialog v-model="assignVisible" title="分配学员" width="420" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="教练">
          <el-input :model-value="assignCoachName" disabled />
        </el-form-item>
        <el-form-item label="学员ID">
          <el-input v-model="assignStudentId" placeholder="请输入学员ID" />
        </el-form-item>
        <el-form-item label="负责科目">
          <el-select v-model="assignSubject" placeholder="选择科目" style="width:100%">
            <el-option label="科目一" value="科目一" /><el-option label="科目二" value="科目二" />
            <el-option label="科目三" value="科目三" /><el-option label="科目四" value="科目四" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible=false">取消</el-button>
        <el-button type="primary" @click="submitAssign">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '../../store/user'
import { getCoaches, createCoach, updateCoachStatus, assignCoach } from '../../api'
import { ElMessage } from 'element-plus'

/** @description 用户状态 Store */
const userStore = useUserStore()
/** @description 当前用户角色 */
const role = computed(() => userStore.userInfo.role)
/** @description 表格加载状态 */
const loading = ref(false)
/** @description 提交按钮加载状态 */
const submitting = ref(false)
/** @description 表格数据 */
const tableData = ref([])
/** @description 总记录数 */
const total = ref(0)
/** @description 查询参数 */
const query = reactive({ name: '', status: null, pageNum: 1, pageSize: 10 })

/** @description 添加教练对话框可见性 */
const addVisible = ref(false)
/** @description 添加教练表单引用 */
const addFormRef = ref()
/** @description 添加教练表单数据 */
const addForm = reactive({ name: '', phone: '', idCard: '', coachType: 'C1', experienceYears: 0 })
/** @description 添加教练表单验证规则 */
const addRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入电话', trigger: 'blur' }]
}

/** @description 分配学员对话框可见性 */
const assignVisible = ref(false)
/** @description 当前分配的教练 ID */
const assignCoachId = ref('')
/** @description 当前分配的教练姓名（仅用于显示） */
const assignCoachName = ref('')
/** @description 要分配的学员 ID */
const assignStudentId = ref('')
/** @description 负责科目 */
const assignSubject = ref('')

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

/**
 * @description 获取教练列表数据
 */
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getCoaches(query)
    tableData.value = res.data.records
    total.value = Number(res.data.total)
  } catch {} finally { loading.value = false }
}

/** @description 打开添加教练对话框 */
const showAdd = () => {
  addForm.name = ''
  addForm.phone = ''
  addForm.idCard = ''
  addForm.coachType = 'C1'
  addForm.experienceYears = 0
  addVisible.value = true
}
/**
 * @description 提交添加教练
 */
const submitAdd = async () => {
  try { await addFormRef.value.validate() } catch { return }
  submitting.value = true
  try {
    await createCoach(addForm)
    ElMessage.success('添加成功')
    addVisible.value = false
    fetchData()
  } catch {} finally { submitting.value = false }
}

/**
 * @description 切换教练状态（在岗 <-> 休假）
 * @param {object} row - 教练行数据
 */
const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await updateCoachStatus(row.id, newStatus)
    ElMessage.success('状态已更新')
    fetchData()
  } catch (e) { /* 已由拦截器处理 */ }
}

/**
 * @description 打开分配学员对话框
 * @param {object} row - 教练行数据
 */
const showAssign = (row) => {
  assignCoachId.value = row.id
  assignCoachName.value = row.name
  assignStudentId.value = ''
  assignSubject.value = ''
  assignVisible.value = true
}
/**
 * @description 提交分配学员
 */
const submitAssign = async () => {
  if (!assignStudentId.value) { ElMessage.warning('请输入学员ID'); return }
  if (!assignSubject.value) { ElMessage.warning('请选择负责科目'); return }
  try {
    await assignCoach(assignCoachId.value, assignStudentId.value, assignSubject.value)
    ElMessage.success('分配成功')
    assignVisible.value = false
  } catch {}
}

/** @description 组件挂载时获取教练列表 */
onMounted(fetchData)
</script>

<style scoped>
/* 页面容器 */
.page-container { display: flex; flex-direction: column; gap: 16px; }
/* 主内容卡片 */
.main-card { border-radius: 12px; }
.main-card :deep(.el-card__header) { padding: 16px 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: #1e293b; }
/* 搜索栏 */
.search-bar { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; align-items: center; }
/* 数据表格 */
.data-table { border-radius: 8px; }
.cell-with-avatar { display: flex; align-items: center; gap: 8px; }
/* 分页栏 */
.pagination-bar { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; }
.total-text { font-size: 13px; color: #94a3b8; }
</style>
