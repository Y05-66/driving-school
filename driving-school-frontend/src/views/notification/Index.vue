<!--
  通知公告管理页面
  功能说明：
  - 展示通知公告列表，支持标记已读、全部已读、删除操作
  - 未读通知以加粗高亮样式显示，行背景色区分未读/已读
  - 管理员/前台角色可发布新通知（选择类型和目标角色）
  - 通知类型包括：系统通知、考试通知、课程通知、紧急通知
  - 通知目标可选：全部、学员、教练、前台
  - 与全局通知 Store 联动，操作后自动刷新未读数量
-->
<template>
  <div class="page-container">
    <!-- 主内容卡片 -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <!-- 卡片头部：标题 + 全部已读按钮 + 发布通知按钮（仅管理员/前台可见） -->
        <div class="card-header">
          <div class="card-title"><el-icon><Bell /></el-icon><span>通知公告</span></div>
          <div style="display:flex;gap:8px">
            <!-- 全部已读按钮：无未读通知时禁用 -->
            <el-button @click="handleMarkAllRead" :disabled="notifyStore.unreadCount===0">全部已读</el-button>
            <!-- 发布通知按钮：仅管理员/前台角色可见 -->
            <el-button type="primary" @click="showPublish" v-if="['ADMIN','STAFF'].includes(role)"><el-icon><Plus /></el-icon>发布通知</el-button>
          </div>
        </div>
      </template>

      <!-- 通知列表数据表格：未读行使用特殊样式高亮 -->
      <el-table :data="tableData" stripe v-loading="loading" class="data-table" :row-class-name="rowClassName">
        <!-- 通知类型列：使用彩色标签展示 -->
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="typeColor(row.type)" size="small" effect="light">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <!-- 标题列：未读加粗加色，已读灰色 -->
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="{ row }">
            <span :style="{ fontWeight: row.isRead===0 ? 600 : 400, color: row.isRead===0 ? '#1e293b' : '#64748b' }">{{ row.title }}</span>
          </template>
        </el-table-column>
        <!-- 目标角色列：ALL 显示为"全员" -->
        <el-table-column label="目标" width="80" align="center">
          <template #default="{ row }">{{ row.targetRole === 'ALL' ? '全员' : row.targetRole }}</template>
        </el-table-column>
        <!-- 发布时间列 -->
        <el-table-column prop="createTime" label="时间" width="170" />
        <!-- 阅读状态列：未读红色标签，已读灰色标签 -->
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isRead===0?'danger':'info'" size="small" effect="light" round>{{ row.isRead===0?'未读':'已读' }}</el-tag>
          </template>
        </el-table-column>
        <!-- 操作列：已读和删除按钮 -->
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <!-- 标记已读按钮：仅未读通知可见 -->
            <el-button link type="primary" size="small" @click="handleRead(row)" v-if="row.isRead===0">已读</el-button>
            <!-- 删除按钮 -->
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 发布通知对话框：填写标题、内容、类型和目标角色 -->
    <el-dialog v-model="publishVisible" title="发布通知" width="520" destroy-on-close>
      <el-form ref="publishFormRef" :model="publishForm" :rules="publishRules" label-width="80px">
        <!-- 通知标题输入 -->
        <el-form-item label="标题"><el-input v-model="publishForm.title" placeholder="请输入通知标题" /></el-form-item>
        <!-- 通知内容输入（多行文本域） -->
        <el-form-item label="内容"><el-input v-model="publishForm.content" type="textarea" :rows="4" placeholder="请输入通知内容" /></el-form-item>
        <!-- 通知类型和目标角色（同行排列） -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="类型">
              <el-select v-model="publishForm.type" style="width:100%">
                <el-option label="系统通知" :value="1" /><el-option label="考试通知" :value="2" />
                <el-option label="课程通知" :value="3" /><el-option label="紧急通知" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标">
              <el-select v-model="publishForm.targetRole" style="width:100%">
                <el-option label="全部" value="ALL" /><el-option label="学员" value="STUDENT" />
                <el-option label="教练" value="COACH" /><el-option label="前台" value="STAFF" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible=false">取消</el-button>
        <el-button type="primary" @click="submitPublish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '../../store/user'
import { useNotificationStore } from '../../store/notification'
import { getNotifications, markRead, markAllRead, deleteNotification, publishNotification } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

// ========== 用户状态与权限 ==========
/** 用户状态存储 */
const userStore = useUserStore()
/** 通知 Store，用于管理全局未读通知数量 */
const notifyStore = useNotificationStore()
/** 当前登录用户的角色 */
const role = computed(() => userStore.userInfo.role)

// ========== 列表数据状态 ==========
/** 表格数据加载状态 */
const loading = ref(false)
/** 通知列表数据数组 */
const tableData = ref([])
/** 总记录数，用于分页 */
const total = ref(0)
/** 分页查询参数 */
const query = reactive({ pageNum: 1, pageSize: 10 })

// ========== 发布通知表单状态 ==========
/** 控制发布通知对话框的显示/隐藏 */
const publishVisible = ref(false)
/** 发布通知表单引用 */
const publishFormRef = ref(null)
/** 发布通知表单数据：标题、内容、类型、目标角色 */
const publishForm = reactive({ title: '', content: '', type: 1, targetRole: 'ALL' })
/** 发布通知表单验证规则 */
const publishRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  targetRole: [{ required: true, message: '请选择目标', trigger: 'change' }]
}

// ========== 工具函数 ==========
/**
 * 将通知类型代码转换为中文文本
 * @param {number} t - 通知类型（1=系统, 2=考试, 3=课程, 4=紧急）
 * @returns {string} 中文类型名称
 */
const typeText = (t) => ({ 1: '系统', 2: '考试', 3: '课程', 4: '紧急' }[t] || '其他')

/**
 * 根据通知类型返回对应的标签颜色
 * @param {number} t - 通知类型
 * @returns {string} Element Plus 标签颜色类型
 */
const typeColor = (t) => ({ 1: 'primary', 2: 'success', 3: 'warning', 4: 'danger' }[t] || 'info')

/**
 * 设置表格行的 CSS 类名
 * 未读通知行添加 'unread-row' 类名，用于高亮背景色
 * @param {Object} param0 - 包含 row 数据的对象
 * @returns {string} CSS 类名
 */
const rowClassName = ({ row }) => row.isRead === 0 ? 'unread-row' : ''

// ========== 数据请求函数 ==========
/**
 * 获取通知列表数据
 */
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getNotifications(query)
    tableData.value = res.data.records
    total.value = Number(res.data.total)
  } catch {} finally { loading.value = false }
}

/**
 * 标记单条通知为已读
 * 操作后刷新全局未读数量并重新加载列表
 * @param {Object} row - 当前通知行数据
 */
const handleRead = async (row) => {
  try {
    await markRead(row.id)
    notifyStore.fetchUnread()
    fetchData()
  } catch (e) { /* 已由拦截器处理 */ }
}

/**
 * 标记所有通知为已读
 * 操作后刷新全局未读数量并重新加载列表
 */
const handleMarkAllRead = async () => {
  try {
    await markAllRead()
    notifyStore.fetchUnread()
    ElMessage.success('全部已标记已读')
    fetchData()
  } catch (e) { /* 已由拦截器处理 */ }
}

/**
 * 删除单条通知
 * 弹出确认框，确认后删除并刷新
 * @param {Object} row - 当前通知行数据
 */
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该通知？', '提示', { type: 'warning' })
    await deleteNotification(row.id)
    notifyStore.fetchUnread()
    ElMessage.success('已删除')
    fetchData()
  } catch (e) {
    // 用户点击取消时 ElMessageBox.confirm 会 throw，此处静默处理
  }
}

/**
 * 打开发布通知对话框
 */
const showPublish = () => {
  publishForm.title = ''
  publishForm.content = ''
  publishForm.type = 1
  publishForm.targetRole = 'ALL'
  publishVisible.value = true
}

/**
 * 提交发布通知表单
 * 校验表单必填项，成功后关闭对话框并刷新列表
 */
const submitPublish = async () => {
  if (!publishFormRef.value) return
  try {
    await publishFormRef.value.validate()
    await publishNotification(publishForm)
    ElMessage.success('发布成功')
    publishVisible.value = false
    fetchData()
  } catch (e) { /* 已由拦截器处理 */ }
}

// ========== 生命周期 ==========
/** 组件挂载后自动加载通知列表 */
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
/* 未读通知行的背景色：浅蓝白色高亮 */
.data-table :deep(.unread-row) { background: #fafbff; }
/* 分页栏布局 */
.pagination-bar { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; }
/* 总记录数文字样式 */
.total-text { font-size: 13px; color: #94a3b8; }
</style>
