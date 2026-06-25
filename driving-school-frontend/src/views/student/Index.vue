<!--
  @description 学员管理页面
  提供学员信息的完整管理功能，包括：
  1. 顶部统计卡片：按状态（待审核/在学/已毕业/已退学）统计学员数量
  2. 搜索栏：支持按姓名、电话、状态筛选学员
  3. 学员列表：展示学员基本信息，支持查看详情、查看进度、审核通过等操作
  4. 学员报名对话框：管理员/前台可新增学员，填写基本信息并创建登录账号
  5. 学员进度对话框：查看学员各科目的学时完成情况

  权限控制：
  - 管理员/前台：可报名新学员、审核待审核学员
  - 教练：可查看学员列表和进度
  - 学员：不可见此菜单（由 Layout 组件控制）
-->

<template>
  <div class="page-container">
    <!-- ==================== 顶部统计卡片 ==================== -->
    <div class="stat-row">
      <div class="mini-stat" v-for="s in stats" :key="s.label">
        <div class="mini-stat-icon" :style="{ background: s.bg, color: s.color }">
          <el-icon :size="18"><component :is="s.icon" /></el-icon>
        </div>
        <div>
          <div class="mini-stat-value">{{ s.value }}</div>
          <div class="mini-stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <!-- ==================== 主内容卡片 ==================== -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><User /></el-icon>
            <span>学员列表</span>
          </div>
          <!-- 学员报名按钮：仅管理员和前台可见 -->
          <el-button type="primary" @click="showAdd" v-if="['ADMIN','STAFF'].includes(role)">
            <el-icon><Plus /></el-icon>学员报名
          </el-button>
        </div>
      </template>

      <!-- ==================== 搜索栏 ==================== -->
      <div class="search-bar">
        <el-input v-model="query.name" placeholder="搜索姓名" clearable prefix-icon="Search" style="width:180px" @keyup.enter="fetchData" />
        <el-input v-model="query.phone" placeholder="搜索电话" clearable prefix-icon="Phone" style="width:180px" @keyup.enter="fetchData" />
        <el-select v-model="query.status" placeholder="状态筛选" clearable style="width:130px">
          <el-option label="待审核" :value="0" /><el-option label="在学" :value="1" />
          <el-option label="已毕业" :value="2" /><el-option label="已退学" :value="3" />
        </el-select>
        <el-button type="primary" @click="fetchData"><el-icon><Search /></el-icon>查询</el-button>
        <el-button @click="resetQuery"><el-icon><Refresh /></el-icon>重置</el-button>
      </div>

      <!-- ==================== 学员数据表格 ==================== -->
      <el-table :data="tableData" stripe v-loading="loading" class="data-table" row-key="id">
        <!-- 姓名列：带头像显示 -->
        <el-table-column prop="name" label="姓名" min-width="100">
          <template #default="{ row }">
            <div class="cell-with-avatar">
              <el-avatar :size="28" :style="{ background: getAvatarColor(row.name) }">{{ (row.name||'?').charAt(0) }}</el-avatar>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" width="130" />
        <!-- 报名类型：C1/C2 -->
        <el-table-column prop="applyType" label="报名类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.applyType || '-' }}</el-tag>
          </template>
        </el-table-column>
        <!-- 状态标签：不同状态不同颜色 -->
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="light" round>
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="registerDate" label="注册日期" width="120" />
        <!-- 操作列：详情、进度、审核通过 -->
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/students/${row.id}`)">详情</el-button>
            <el-button link type="primary" size="small" @click="showProgress(row)">进度</el-button>
            <!-- 审核通过按钮：仅待审核状态且管理员/前台可见 -->
            <el-button link type="success" size="small" @click="approveStudent(row)" v-if="row.status===0 && ['ADMIN','STAFF'].includes(role)">通过</el-button>
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

    <!-- ==================== 学员报名对话框 ==================== -->
    <el-dialog v-model="addVisible" title="学员报名" width="520" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="80px" label-position="right">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name"><el-input v-model="addForm.name" placeholder="请输入姓名" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证" prop="idCard"><el-input v-model="addForm.idCard" placeholder="请输入身份证号" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="电话"><el-input v-model="addForm.phone" placeholder="请输入电话" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="addForm.gender">
                <el-radio-button :value="1">男</el-radio-button>
                <el-radio-button :value="0">女</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="报名类型">
              <el-select v-model="addForm.applyType" style="width:100%">
                <el-option label="C1 手动挡" value="C1" /><el-option label="C2 自动挡" value="C2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生日"><el-date-picker v-model="addForm.birthday" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" /></el-form-item>
          </el-col>
        </el-row>
        <!-- 登录账号信息 -->
        <el-divider content-position="left">登录账号</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username"><el-input v-model="addForm.username" placeholder="4-20位" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" prop="password"><el-input v-model="addForm.password" type="password" show-password placeholder="至少6位" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="addVisible=false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAdd">确认报名</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 学员进度对话框 ==================== -->
    <el-dialog v-model="progressVisible" title="学员科目进度" width="560" destroy-on-close>
      <!-- 学员信息头部 -->
      <div class="progress-header">
        <el-avatar :size="36" :style="{ background: getAvatarColor(progressStudent.name) }">{{ (progressStudent.name||'?').charAt(0) }}</el-avatar>
        <div>
          <div style="font-weight:600;color:#1e293b">{{ progressStudent.name }}</div>
          <div style="font-size:12px;color:#94a3b8">{{ progressStudent.applyType }} · {{ statusText(progressStudent.status) }}</div>
        </div>
      </div>
      <!-- 各科目进度列表 -->
      <div v-for="p in progressData" :key="p.id" class="progress-item">
        <div class="progress-info">
          <span class="progress-subject">{{ p.subjectName }}</span>
          <span class="progress-hours">{{ p.completedHours }}/{{ p.requiredHours }} 小时</span>
        </div>
        <!-- 进度条：已完成/进行中/未开始 三种状态 -->
        <el-progress :percentage="Math.min(100, Math.round((p.completedHours || 0) / (p.requiredHours || 1) * 100))"
          :status="p.status===2?'success':p.status===1?'':'info'"
          :stroke-width="10" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '../../store/user'
import { getStudents, createStudent, updateStudentStatus, getStudentProgress, getStudentStats } from '../../api'
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
const query = reactive({ name: '', phone: '', status: null, applyType: null, pageNum: 1, pageSize: 10 })

/**
 * @description 顶部统计数据（从后端接口获取）
 */
const studentStats = ref({})

/**
 * @description 加载学员统计数据
 * 调用后端 /students/stats 接口获取各状态学员数量
 */
const loadStats = async () => {
  try {
    const res = await getStudentStats()
    studentStats.value = res.data || {}
  } catch {}
}

/**
 * @description 顶部统计卡片配置
 * 将后端返回的统计数据转换为卡片展示格式
 */
const stats = computed(() => {
  const statuses = ['pending', 'studying', 'graduated', 'withdrawn']
  const labels = ['待审核', '在学', '已毕业', '已退学']
  const icons = ['Clock', 'Loading', 'CircleCheck', 'CircleClose']
  const colors = ['#f59e0b', '#6366f1', '#10b981', '#94a3b8']
  const bgs = ['#fffbeb', '#eef2ff', '#ecfdf5', '#f1f5f9']
  return labels.map((l, i) => ({
    label: l, icon: icons[i], color: colors[i], bg: bgs[i],
    value: studentStats.value[statuses[i]] ?? '-'
  }))
})

/** @description 学员报名对话框是否可见 */
const addVisible = ref(false)
/** @description 报名表单引用 */
const addFormRef = ref()
/** @description 报名表单数据 */
const addForm = reactive({ name: '', idCard: '', phone: '', gender: 1, applyType: 'C1', birthday: '', username: '', password: '' })
/** @description 报名表单验证规则 */
const addRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '至少6位', trigger: 'blur' }]
}

/** @description 进度对话框是否可见 */
const progressVisible = ref(false)
/** @description 进度数据列表 */
const progressData = ref([])
/** @description 当前查看进度的学员信息 */
const progressStudent = ref({})

/**
 * @description 状态文本映射
 * @param {number} s - 状态码
 * @returns {string} 状态中文名称
 */
const statusText = (s) => ({ 0: '待审核', 1: '在学', 2: '已毕业', 3: '已退学' }[s] || '未知')
/**
 * @description 状态标签类型映射（Element Plus Tag 组件的 type 属性）
 * @param {number} s - 状态码
 * @returns {string} 标签类型
 */
const statusType = (s) => ({ 0: 'warning', 1: 'primary', 2: 'success', 3: 'info' }[s] || 'info')
/**
 * @description 根据姓名生成头像背景色
 * 使用简单的哈希算法，确保同一姓名始终生成相同颜色
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
 * @description 重置查询条件并重新获取数据
 */
// 修复：重置时需同时清空 applyType，避免筛选条件残留导致查询结果不完整
const resetQuery = () => { query.name = ''; query.phone = ''; query.status = null; query.applyType = null; query.pageNum = 1; fetchData() }

/**
 * @description 获取学员列表数据
 * 根据当前查询参数从后端获取分页数据
 */
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getStudents(query)
    tableData.value = res.data.records
    total.value = Number(res.data.total)
  } catch {} finally { loading.value = false }
}

/** @description 打开报名对话框，重置表单数据 */
const showAdd = () => {
  addForm.name = ''; addForm.idCard = ''; addForm.phone = ''
  addForm.gender = 1; addForm.applyType = 'C1'; addForm.birthday = ''
  addForm.username = ''; addForm.password = ''
  addVisible.value = true
}
/**
 * @description 提交学员报名
 * 验证表单 -> 调用 API -> 成功提示 -> 关闭对话框 -> 刷新列表
 */
const submitAdd = async () => {
  try { await addFormRef.value.validate() } catch { return }
  submitting.value = true
  try {
    await createStudent(addForm)
    ElMessage.success('报名成功')
    addVisible.value = false
    fetchData()
  } catch {} finally { submitting.value = false }
}

/**
 * @description 审核通过学员
 * 将学员状态从"待审核(0)"更新为"在学(1)"
 * @param {object} row - 学员行数据
 */
const approveStudent = async (row) => {
  try {
    await updateStudentStatus(row.id, 1)
    ElMessage.success('审核通过')
    fetchData()
  } catch {}
}

/**
 * @description 显示学员科目进度
 * @param {object} row - 学员行数据
 */
const showProgress = async (row) => {
  progressStudent.value = row
  try {
    const res = await getStudentProgress(row.id)
    progressData.value = res.data || []
    progressVisible.value = true
  } catch {}
}

/** @description 组件挂载时获取学员列表数据 */
onMounted(() => { fetchData(); loadStats() })
</script>

<style scoped>
/* 页面容器 */
.page-container { display: flex; flex-direction: column; gap: 16px; }

/* 顶部统计卡片行 */
.stat-row {
  display: flex; gap: 12px; flex-wrap: wrap;
}
.mini-stat {
  flex: 1; min-width: 140px; display: flex; align-items: center; gap: 12px;
  background: #fff; border-radius: 10px; padding: 14px 16px; border: 1px solid #f1f5f9;
}
.mini-stat-icon {
  width: 36px; height: 36px; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.mini-stat-value { font-size: 18px; font-weight: 700; color: #1e293b; }
.mini-stat-label { font-size: 12px; color: #94a3b8; }

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

/* 进度对话框样式 */
.progress-header { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid #f1f5f9; }
.progress-item { margin-bottom: 16px; }
.progress-info { display: flex; justify-content: space-between; margin-bottom: 6px; }
.progress-subject { font-size: 14px; font-weight: 600; color: #1e293b; }
.progress-hours { font-size: 13px; color: #94a3b8; }
</style>
