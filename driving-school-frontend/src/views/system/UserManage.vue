<!--
  用户管理页面（管理员功能）
  功能说明：
  - 展示系统用户列表，支持按用户名和角色筛选
  - 添加新用户：填写用户名、密码、姓名、电话、角色（管理员/前台/教练/学员）
  - 重置密码：通过弹窗输入新密码重置指定用户的密码
  - 启用/禁用用户：切换用户账号的启用状态
  - 用户头像使用姓名首字符，背景色根据角色区分
-->
<template>
  <div class="page-container">
    <!-- 主内容卡片 -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <!-- 卡片头部：标题 + 添加用户按钮 -->
        <div class="card-header">
          <div class="card-title"><el-icon><Setting /></el-icon><span>用户管理</span></div>
          <el-button type="primary" @click="showAdd"><el-icon><Plus /></el-icon>添加用户</el-button>
        </div>
      </template>

      <!-- 搜索筛选栏：用户名搜索、角色筛选、查询和重置按钮 -->
      <div class="search-bar">
        <!-- 用户名搜索输入框，支持回车查询 -->
        <el-input v-model="query.username" placeholder="搜索用户名" clearable prefix-icon="Search" style="width:180px" @keyup.enter="fetchData" />
        <!-- 角色筛选下拉框 -->
        <el-select v-model="query.role" placeholder="角色筛选" clearable style="width:130px">
          <el-option label="管理员" value="ADMIN" /><el-option label="前台" value="STAFF" />
          <el-option label="教练" value="COACH" /><el-option label="学员" value="STUDENT" />
        </el-select>
        <!-- 查询按钮 -->
        <el-button type="primary" @click="fetchData"><el-icon><Search /></el-icon>查询</el-button>
        <!-- 重置筛选条件按钮 -->
        <el-button @click="query.username='';query.role='';fetchData()"><el-icon><Refresh /></el-icon>重置</el-button>
      </div>

      <!-- 用户数据表格 -->
      <el-table :data="tableData" stripe v-loading="loading" class="data-table">
        <!-- 用户名列：带头像首字母和加粗用户名 -->
        <el-table-column prop="username" label="用户名" width="120">
          <template #default="{ row }">
            <div class="cell-with-avatar">
              <!-- 头像：取姓名或用户名首字符，背景色根据角色变化 -->
              <el-avatar :size="28" :style="{ background: roleColor(row.role) }">{{ (row.realName||row.username||'?').charAt(0) }}</el-avatar>
              <span style="font-weight:500">{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <!-- 真实姓名列 -->
        <el-table-column prop="realName" label="姓名" width="100" />
        <!-- 电话列 -->
        <el-table-column prop="phone" label="电话" width="130" />
        <!-- 角色列：使用彩色标签展示 -->
        <el-table-column label="角色" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small" effect="light">{{ roleName(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <!-- 状态列：启用绿色标签，禁用红色标签 -->
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status===1?'success':'danger'" size="small" effect="light" round>{{ row.status===1?'启用':'禁用' }}</el-tag>
          </template>
        </el-table-column>
        <!-- 创建时间列 -->
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <!-- 操作列：重置密码、启用/禁用切换 -->
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <!-- 重置密码按钮 -->
            <el-button link type="primary" size="small" @click="handleResetPwd(row)">重置密码</el-button>
            <!-- 启用/禁用切换按钮：当前启用则显示"禁用"，当前禁用则显示"启用" -->
            <el-button link :type="row.status===1?'danger':'success'" size="small" @click="toggleStatus(row)">
              {{ row.status===1?'禁用':'启用' }}
            </el-button>
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

    <!-- 添加用户对话框：填写用户名、密码、姓名、电话、角色 -->
    <el-dialog v-model="addVisible" title="添加用户" width="500" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="80px">
        <!-- 用户名和密码（同行排列） -->
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="用户名"><el-input v-model="addForm.username" placeholder="4-20位" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="密码"><el-input v-model="addForm.password" type="password" show-password placeholder="至少6位" /></el-form-item></el-col>
        </el-row>
        <!-- 姓名和电话（同行排列） -->
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="姓名"><el-input v-model="addForm.realName" placeholder="真实姓名" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="电话"><el-input v-model="addForm.phone" placeholder="手机号" /></el-form-item></el-col>
        </el-row>
        <!-- 角色选择 -->
        <el-form-item label="角色">
          <el-select v-model="addForm.role" style="width:100%">
            <el-option label="管理员" value="ADMIN" /><el-option label="前台" value="STAFF" />
            <el-option label="教练" value="COACH" /><el-option label="学员" value="STUDENT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible=false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getUsers, createUser, resetPassword, updateUserStatus } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

// ========== 列表数据状态 ==========
/** 表格数据加载状态 */
const loading = ref(false)
/** 用户列表数据数组 */
const tableData = ref([])
/** 总记录数，用于分页 */
const total = ref(0)
/** 查询参数：用户名搜索、角色筛选、页码、每页条数 */
const query = reactive({ username: '', role: '', pageNum: 1, pageSize: 10 })

// ========== 添加用户表单状态 ==========
/** 控制添加用户对话框的显示/隐藏 */
const addVisible = ref(false)
/** 添加用户表单引用 */
const addFormRef = ref(null)
/** 添加用户表单数据：用户名、密码、姓名、电话、角色（默认学员） */
const addForm = reactive({ username: '', password: '', realName: '', phone: '', role: 'STUDENT' })
/** 添加用户表单验证规则 */
const addRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度为4-20位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

// ========== 工具函数 ==========
/**
 * 将角色代码转换为中文名称
 * @param {string} r - 角色代码（ADMIN/STAFF/COACH/STUDENT）
 * @returns {string} 中文角色名称
 */
const roleName = (r) => ({ ADMIN: '管理员', STAFF: '前台', COACH: '教练', STUDENT: '学员' }[r] || r)

/**
 * 根据角色返回对应的标签颜色类型
 * @param {string} r - 角色代码
 * @returns {string} Element Plus 标签颜色类型（danger/warning/success/primary）
 */
const roleTagType = (r) => ({ ADMIN: 'danger', STAFF: 'warning', COACH: 'success', STUDENT: 'primary' }[r] || 'info')

/**
 * 根据角色返回对应的头像背景色
 * @param {string} r - 角色代码
 * @returns {string} 十六进制颜色值
 */
const roleColor = (r) => ({ ADMIN: '#ef4444', STAFF: '#f59e0b', COACH: '#10b981', STUDENT: '#6366f1' }[r] || '#94a3b8')

// ========== 数据请求函数 ==========
/**
 * 获取用户列表数据
 * 根据分页和筛选参数请求后端接口
 */
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getUsers(query)
    tableData.value = res.data.records
    total.value = Number(res.data.total)
  } catch {} finally { loading.value = false }
}

/**
 * 打开添加用户对话框
 * 修复：手动重置表单字段，避免复用对话框时残留上次输入的数据
 */
const showAdd = () => {
  addForm.username = ''
  addForm.password = ''
  addForm.realName = ''
  addForm.phone = ''
  addForm.role = 'STUDENT'
  addVisible.value = true
}

/**
 * 提交添加用户表单
 * 成功后关闭对话框并刷新列表
 */
const submitAdd = async () => {
  if (!addFormRef.value) return
  try {
    await addFormRef.value.validate()
    await createUser(addForm)
    ElMessage.success('添加成功')
    addVisible.value = false
    fetchData()
  } catch {}
}

/**
 * 重置用户密码
 * 弹出输入框让用户输入新密码，然后调用重置密码接口
 * @param {Object} row - 当前用户行数据
 */
const handleResetPwd = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新密码', '重置密码', {
      inputType: 'password', inputPlaceholder: '至少6位',
      inputValidator: (val) => val && val.length >= 6 ? true : '密码至少6位'
    })
    await resetPassword(row.id, value)
    ElMessage.success('密码已重置')
  } catch {}
}

/**
 * 切换用户启用/禁用状态
 * 将当前状态取反后提交更新
 * @param {Object} row - 当前用户行数据
 */
const toggleStatus = async (row) => {
  try {
    await updateUserStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success('状态已更新')
    fetchData()
  } catch {}
}

// ========== 生命周期 ==========
/** 组件挂载后自动加载用户列表数据 */
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
/* 数据表格圆角 */
.data-table { border-radius: 8px; }
/* 用户名单元格布局：头像和用户名水平排列 */
.cell-with-avatar { display: flex; align-items: center; gap: 8px; }
/* 分页栏布局 */
.pagination-bar { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; }
/* 总记录数文字样式 */
.total-text { font-size: 13px; color: #94a3b8; }
</style>
