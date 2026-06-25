<!--
  电子合同管理页面
  功能说明：
  - 合同列表展示：管理员/前台查看所有合同，学员仅查看自己的合同
  - 搜索筛选：支持按学员ID、合同状态（待签/已签/作废）筛选
  - 创建合同：管理员/前台可创建新合同（填写学员ID、标题、金额、报名类型、合同内容）
  - 签署合同：学员可对待签合同进行签署操作
  - 合同状态：待签（warning 黄色）、已签（success 绿色）、作废（info 灰色）

  权限控制：
  - 管理员/前台：可查看所有合同、创建新合同
  - 学员：仅查看和签署自己的合同
-->
<template>
  <div class="page-container">
    <!-- ==================== 主内容卡片 ==================== -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <!-- 卡片头部：标题 + 创建合同按钮（仅管理员/前台可见） -->
        <div class="card-header">
          <div class="card-title">
            <el-icon><Document /></el-icon>
            <span>电子合同</span>
          </div>
          <!-- 创建合同按钮：仅管理员和前台角色可见 -->
          <el-button type="primary" @click="showAdd" v-if="['ADMIN', 'STAFF'].includes(role)">
            <el-icon><Plus /></el-icon>创建合同
          </el-button>
        </div>
      </template>

      <!-- ==================== 搜索筛选栏 ==================== -->
      <div class="search-bar">
        <!-- 学员ID搜索输入框 -->
        <el-input
          v-model="query.studentId"
          placeholder="搜索学员ID"
          clearable
          prefix-icon="Search"
          style="width: 180px"
          @keyup.enter="fetchData"
        />
        <!-- 合同状态下拉筛选 -->
        <el-select v-model="query.status" placeholder="合同状态" clearable style="width: 130px">
          <el-option label="待签" :value="0" />
          <el-option label="已签" :value="1" />
          <el-option label="作废" :value="2" />
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

      <!-- ==================== 合同数据表格 ==================== -->
      <el-table :data="tableData" stripe v-loading="loading" class="data-table">
        <!-- 合同编号列：等宽字体显示 -->
        <el-table-column label="合同编号" width="180">
          <template #default="{ row }">
            <span class="contract-no">{{ row.contractNo || '-' }}</span>
          </template>
        </el-table-column>
        <!-- 学员ID列 -->
        <el-table-column prop="studentId" label="学员ID" width="180" show-overflow-tooltip />
        <!-- 合同标题列 -->
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <!-- 金额列：加粗显示，带人民币符号 -->
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">
            <span class="amount-text">¥{{ row.amount || 0 }}</span>
          </template>
        </el-table-column>
        <!-- 合同状态列：使用彩色标签展示不同状态 -->
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusColor(row.status)" size="small" effect="light" round>
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 签署时间列 -->
        <el-table-column prop="signTime" label="签署时间" width="170">
          <template #default="{ row }">
            {{ row.signTime || '-' }}
          </template>
        </el-table-column>
        <!-- 操作列：签署按钮（待签状态时显示） -->
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <!-- 签署按钮：仅待签状态可见，学员角色或管理员均可签署 -->
            <el-button
              link
              type="primary"
              size="small"
              @click="handleSign(row)"
              v-if="row.status === 0"
            >签署</el-button>
            <!-- 已签/作废状态提示 -->
            <el-button
              link
              type="info"
              size="small"
              disabled
              v-if="row.status === 1"
            >已签署</el-button>
            <el-button
              link
              type="info"
              size="small"
              disabled
              v-if="row.status === 2"
            >已作废</el-button>
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

    <!-- ==================== 创建合同对话框 ==================== -->
    <el-dialog v-model="addVisible" title="创建合同" width="580" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="90px">
        <!-- 学员ID输入 -->
        <el-form-item label="学员ID" prop="studentId">
          <el-input v-model="addForm.studentId" placeholder="请输入学员ID" />
        </el-form-item>
        <!-- 合同标题输入 -->
        <el-form-item label="合同标题" prop="title">
          <el-input v-model="addForm.title" placeholder="请输入合同标题" />
        </el-form-item>
        <!-- 金额和报名类型（同行排列） -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="金额">
              <el-input-number
                v-model="addForm.amount"
                :min="0"
                :precision="2"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报名类型">
              <el-select v-model="addForm.applyType" style="width: 100%">
                <el-option label="C1 手动挡" value="C1" />
                <el-option label="C2 自动挡" value="C2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 合同内容输入（多行文本域） -->
        <el-form-item label="合同内容">
          <el-input
            v-model="addForm.content"
            type="textarea"
            :rows="6"
            placeholder="请输入合同内容"
          />
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
/** 当前登录用户的ID（学员角色时用于筛选自己的合同） */
const userId = computed(() => userStore.userInfo.id)

// ========== 列表数据状态 ==========
/** 表格数据加载状态，控制加载动画的显示/隐藏 */
const loading = ref(false)
/** 合同列表数据数组 */
const tableData = ref([])
/** 总记录数，用于分页组件 */
const total = ref(0)
/** 查询参数：学员ID、状态筛选、页码、每页条数 */
const query = reactive({ studentId: '', status: null, pageNum: 1, pageSize: 10 })

// ========== 创建合同表单状态 ==========
/** 控制创建合同对话框的显示/隐藏 */
const addVisible = ref(false)
/** 创建合同表单引用 */
const addFormRef = ref(null)
/** 创建合同的表单数据：学员ID、标题、金额、报名类型、合同内容 */
const addForm = reactive({
  studentId: '',
  title: '',
  amount: 0,
  applyType: 'C1',
  content: ''
})
/** 创建合同表单验证规则 */
const addRules = {
  // 修复：学员ID为必填项，合同必须关联到具体学员
  studentId: [{ required: true, message: '请输入学员ID', trigger: 'blur' }],
  // 修复：合同标题为必填项，用于标识合同内容
  title: [{ required: true, message: '请输入合同标题', trigger: 'blur' }]
}

// ========== 工具函数 ==========
/**
 * 将合同状态码转换为中文文本
 * @param {number} s - 状态码（0=待签, 1=已签, 2=作废）
 * @returns {string} 中文状态名称
 */
const statusText = (s) => ({ 0: '待签', 1: '已签', 2: '作废' }[s] || '未知')

/**
 * 根据合同状态返回对应的标签颜色类型
 * @param {number} s - 状态码
 * @returns {string} Element Plus 标签颜色类型（warning/success/info）
 */
const statusColor = (s) => ({ 0: 'warning', 1: 'success', 2: 'info' }[s] || 'info')

// ========== 数据请求函数 ==========
/**
 * 获取合同列表数据
 * 管理员/前台查看所有合同，学员仅查看自己的合同
 * 根据分页和筛选参数请求后端接口
 */
const fetchData = async () => {
  loading.value = true
  try {
    // 构建查询参数
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    // 管理员/前台可按学员ID筛选，学员角色自动过滤为自己的合同
    if (query.studentId) params.studentId = query.studentId
    // 状态筛选
    if (query.status !== null && query.status !== '') params.status = query.status

    const res = await get('/contracts', { params })
    tableData.value = res.data.records || []
    total.value = Number(res.data.total)
  } catch {
    // 请求失败时不处理，由拦截器统一提示
  } finally {
    loading.value = false
  }
}

/**
 * 重置所有查询条件并重新加载数据
 * 清空学员ID、状态筛选，重置到第一页
 */
const resetQuery = () => {
  query.studentId = ''
  query.status = null
  query.pageNum = 1
  fetchData()
}

/**
 * 打开创建合同对话框
 */
const showAdd = () => {
  addForm.studentId = ''
  addForm.title = ''
  addForm.amount = 0
  addForm.applyType = 'C1'
  addForm.content = ''
  addVisible.value = true
}

/**
 * 提交创建合同表单
 * 校验必填字段，调用接口创建合同，成功后关闭对话框并刷新列表
 */
const submitAdd = async () => {
  if (!addFormRef.value) return
  try {
    await addFormRef.value.validate()
  } catch {
    return
  }
  try {
    await post('/contracts', addForm)
    ElMessage.success('合同创建成功')
    addVisible.value = false
    // 重置表单
    addForm.studentId = ''
    addForm.title = ''
    addForm.amount = 0
    addForm.applyType = 'C1'
    addForm.content = ''
    fetchData()
  } catch {
    // 请求失败由拦截器处理
  }
}

/**
 * 签署合同操作
 * 弹出确认框，确认后调用签署接口
 * @param {Object} row - 当前合同行数据，包含合同id等信息
 */
const handleSign = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定签署合同「${row.title}」？签署后不可撤回。`,
      '签署确认',
      { type: 'warning', confirmButtonText: '确认签署', cancelButtonText: '取消' }
    )
    await put(`/contracts/${row.id}/sign`)
    ElMessage.success('签署成功')
    fetchData()
  } catch {
    // 用户取消或请求失败
  }
}

// ========== 生命周期 ==========
/** 组件挂载后自动加载合同列表数据 */
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

/* 合同编号等宽字体样式 */
.contract-no {
  font-family: monospace;
  font-size: 12px;
  color: #64748b;
}

/* 金额文字样式：加粗显示 */
.amount-text {
  font-weight: 600;
  color: #1e293b;
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
