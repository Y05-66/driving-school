<!--
  车辆管理页面
  功能说明：
  - 到期提醒：页面顶部显示保险/年检即将到期（30天内）的警告提示
  - 车辆列表：展示所有训练车辆信息（车牌号、品牌型号、状态、保险到期、年检到期）
  - 按车牌号搜索和按状态（可用/维修中/报废）筛选车辆
  - 添加新车辆：填写车牌号、车型、品牌、型号、颜色（仅管理员）
  - 维修记录：查看指定车辆的维修保养历史记录
  - 添加维修记录：为车辆添加保养/维修/年检/保险记录（仅管理员）
  - 保险和年检到期日期已过期时以红色加粗显示
-->
<template>
  <div class="page-container">
    <!-- 到期提醒区域：当有即将到期的保险/年检时显示警告横幅 -->
    <el-alert v-if="expiringList.length > 0" type="warning" :closable="false" show-icon class="expire-alert">
      <template #title>
        <span>有 {{ expiringList.length }} 条保险/年检即将到期</span>
      </template>
      <template #default>
        <!-- 最多显示前3条到期记录 -->
        <div v-for="item in expiringList.slice(0,3)" :key="item.id" style="font-size:13px;margin-top:4px">
          {{ item.type===3?'年检':'保险' }} · {{ item.nextDueDate }} · 车辆ID: {{ item.vehicleId }}
        </div>
      </template>
    </el-alert>

    <!-- 车辆列表卡片 -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <!-- 卡片头部：标题 + 添加车辆按钮（仅管理员可见） -->
        <div class="card-header">
          <div class="card-title"><el-icon><Van /></el-icon><span>车辆列表</span></div>
          <el-button type="primary" @click="showAdd" v-if="role==='ADMIN'"><el-icon><Plus /></el-icon>添加车辆</el-button>
        </div>
      </template>

      <!-- 搜索筛选栏：车牌号搜索、状态筛选、查询和重置按钮 -->
      <div class="search-bar">
        <!-- 车牌号搜索输入框，支持回车查询 -->
        <el-input v-model="query.plateNo" placeholder="搜索车牌号" clearable prefix-icon="Search" style="width:180px" @keyup.enter="fetchData" />
        <!-- 车辆状态下拉筛选 -->
        <el-select v-model="query.status" placeholder="状态筛选" clearable style="width:130px">
          <el-option label="可用" :value="1" /><el-option label="维修中" :value="0" /><el-option label="报废" :value="2" />
        </el-select>
        <!-- 查询按钮 -->
        <el-button type="primary" @click="fetchData"><el-icon><Search /></el-icon>查询</el-button>
        <!-- 重置筛选条件按钮 -->
        <el-button @click="query.plateNo='';query.status=null;fetchData()"><el-icon><Refresh /></el-icon>重置</el-button>
      </div>

      <!-- 车辆数据表格 -->
      <el-table :data="tableData" stripe v-loading="loading" class="data-table">
        <!-- 车牌号列：加粗深色显示 -->
        <el-table-column prop="plateNo" label="车牌号" width="120">
          <template #default="{ row }">
            <span style="font-weight:600;color:#1e293b">{{ row.plateNo }}</span>
          </template>
        </el-table-column>
        <!-- 车型信息列：品牌型号 + 车型颜色副文本 -->
        <el-table-column label="车型信息" min-width="150">
          <template #default="{ row }">
            <div>{{ row.brand }} {{ row.model }}</div>
            <div style="font-size:11px;color:#94a3b8">{{ row.carType }} · {{ row.color }}</div>
          </template>
        </el-table-column>
        <!-- 车辆状态列：可用绿色、维修中黄色、报废红色 -->
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status===1?'success':row.status===0?'warning':'danger'" size="small" effect="light" round>
              {{ row.status===1?'可用':row.status===0?'维修中':'报废' }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 保险到期列：已过期日期以红色加粗显示 -->
        <el-table-column label="保险到期" width="120">
          <template #default="{ row }">
            <span :class="{ 'expire-text': isExpired(row.insuranceExpire) }">{{ row.insuranceExpire || '-' }}</span>
          </template>
        </el-table-column>
        <!-- 年检到期列：已过期日期以红色加粗显示 -->
        <el-table-column label="年检到期" width="120">
          <template #default="{ row }">
            <span :class="{ 'expire-text': isExpired(row.inspectionExpire) }">{{ row.inspectionExpire || '-' }}</span>
          </template>
        </el-table-column>
        <!-- 操作列：维修记录查看 + 添加维修记录（仅管理员） -->
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="showMaintenance(row)">维修记录</el-button>
            <el-button link type="warning" size="small" @click="showAddMaintenance(row)" v-if="role==='ADMIN'">添加记录</el-button>
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

    <!-- 添加车辆对话框：填写车牌号、车型、品牌、型号、颜色 -->
    <el-dialog v-model="addVisible" title="添加车辆" width="520" destroy-on-close>
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="80px">
        <!-- 车牌号和车型（同行排列） -->
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="车牌号"><el-input v-model="addForm.plateNo" placeholder="如: 京A12345" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="车型"><el-select v-model="addForm.carType" style="width:100%"><el-option label="C1" value="C1" /><el-option label="C2" value="C2" /></el-select></el-form-item></el-col>
        </el-row>
        <!-- 品牌和型号（同行排列） -->
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="品牌"><el-input v-model="addForm.brand" placeholder="如: 大众" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="型号"><el-input v-model="addForm.model" placeholder="如: 捷达" /></el-form-item></el-col>
        </el-row>
        <!-- 颜色输入 -->
        <el-form-item label="颜色"><el-input v-model="addForm.color" placeholder="如: 白色" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible=false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- 维修保养记录查看对话框：展示指定车辆的历史维修记录表格 -->
    <el-dialog v-model="maintenanceVisible" title="维修保养记录" width="700" destroy-on-close>
      <el-table :data="maintenanceList" stripe>
        <!-- 记录类型列：保养/维修/年检/保险，使用不同颜色标签 -->
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="['','primary','warning','success','info'][row.type]" size="small">{{ ['','保养','维修','年检','保险'][row.type] }}</el-tag>
          </template>
        </el-table-column>
        <!-- 描述列 -->
        <el-table-column prop="description" label="描述" />
        <!-- 费用列：带人民币符号 -->
        <el-table-column prop="cost" label="费用" width="100"><template #default="{ row }">{{ row.cost ? '¥'+row.cost : '-' }}</template></el-table-column>
        <!-- 维修日期列 -->
        <el-table-column prop="maintenanceDate" label="日期" width="120" />
        <!-- 下次到期日期列 -->
        <el-table-column prop="nextDueDate" label="下次到期" width="120" />
      </el-table>
    </el-dialog>

    <!-- 添加维修记录对话框：填写类型、描述、费用、日期、下次到期 -->
    <el-dialog v-model="addMaintenanceVisible" title="添加维修记录" width="500" destroy-on-close>
      <el-form ref="maintenanceFormRef" :model="maintenanceForm" :rules="maintenanceRules" label-width="80px">
        <!-- 记录类型选择：保养/维修/年检/保险 -->
        <el-form-item label="类型">
          <el-select v-model="maintenanceForm.type" style="width:100%">
            <el-option label="保养" :value="1" /><el-option label="维修" :value="2" />
            <el-option label="年检" :value="3" /><el-option label="保险" :value="4" />
          </el-select>
        </el-form-item>
        <!-- 描述输入（多行文本域） -->
        <el-form-item label="描述"><el-input v-model="maintenanceForm.description" type="textarea" :rows="3" placeholder="请输入描述" /></el-form-item>
        <!-- 费用和日期（同行排列） -->
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="费用"><el-input-number v-model="maintenanceForm.cost" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="日期"><el-date-picker v-model="maintenanceForm.maintenanceDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <!-- 下次到期日期选择 -->
        <el-form-item label="下次到期"><el-date-picker v-model="maintenanceForm.nextDueDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addMaintenanceVisible=false">取消</el-button>
        <el-button type="primary" @click="submitMaintenance">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useUserStore } from '../../store/user'
import { getVehicles, createVehicle, getVehicleMaintenance, createVehicleMaintenance, getExpiringMaintenance } from '../../api'
import { ElMessage } from 'element-plus'

// ========== 用户状态与权限 ==========
/** 用户状态存储，用于判断当前用户角色 */
const userStore = useUserStore()
/** 当前登录用户的角色（仅 ADMIN 可执行添加等管理操作） */
const role = computed(() => userStore.userInfo.role)

// ========== 列表数据状态 ==========
/** 表格数据加载状态 */
const loading = ref(false)
/** 车辆列表数据数组 */
const tableData = ref([])
/** 总记录数，用于分页 */
const total = ref(0)
/** 查询参数：车牌号搜索、状态筛选、页码、每页条数 */
const query = reactive({ plateNo: '', status: null, pageNum: 1, pageSize: 10 })
/** 即将到期的保险/年检记录列表（30天内到期） */
const expiringList = ref([])

// ========== 添加车辆表单状态 ==========
/** 控制添加车辆对话框的显示/隐藏 */
const addVisible = ref(false)
/** 添加车辆表单引用 */
const addFormRef = ref(null)
/** 添加车辆表单数据：车牌号、品牌、型号、车型、颜色 */
const addForm = reactive({ plateNo: '', brand: '', model: '', carType: 'C1', color: '' })
/** 添加车辆表单验证规则 */
const addRules = {
  plateNo: [{ required: true, message: '请输入车牌号', trigger: 'blur' }],
  brand: [{ required: true, message: '请输入品牌', trigger: 'blur' }],
  carType: [{ required: true, message: '请选择车型', trigger: 'change' }]
}

// ========== 维修记录相关状态 ==========
/** 控制维修记录查看对话框的显示/隐藏 */
const maintenanceVisible = ref(false)
/** 当前车辆的维修记录列表 */
const maintenanceList = ref([])
/** 控制添加维修记录对话框的显示/隐藏 */
const addMaintenanceVisible = ref(false)
/** 添加维修记录表单引用 */
const maintenanceFormRef = ref(null)
/** 添加维修记录表单数据：关联车辆ID、类型、描述、费用、日期、下次到期 */
const maintenanceForm = reactive({ vehicleId: '', type: 1, description: '', cost: 0, maintenanceDate: '', nextDueDate: '' })
/** 添加维修记录表单验证规则 */
const maintenanceRules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }],
  maintenanceDate: [{ required: true, message: '请选择日期', trigger: 'change' }]
}

// ========== 工具函数 ==========
/**
 * 判断指定日期是否已过期
 * @param {string} date - 日期字符串（YYYY-MM-DD 格式）
 * @returns {boolean} 如果日期早于当前日期则返回 true
 */
const isExpired = (date) => {
  if (!date) return false
  const d = new Date(date + 'T00:00:00')
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return d < today
}

// ========== 数据请求函数 ==========
/**
 * 获取车辆列表数据
 * 根据分页和筛选参数请求后端接口
 */
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getVehicles(query)
    tableData.value = res.data.records
    total.value = Number(res.data.total)
  } catch {} finally { loading.value = false }
}

/**
 * 获取即将到期的保险/年检记录
 * 查询未来30天内即将到期的记录，用于页面顶部的到期提醒
 */
const fetchExpiring = async () => {
  try {
    const res = await getExpiringMaintenance(30)
    expiringList.value = res.data || []
  } catch {}
}

/**
 * 打开添加车辆对话框
 */
const showAdd = () => {
  addForm.plateNo = ''
  addForm.brand = ''
  addForm.model = ''
  addForm.carType = 'C1'
  addForm.color = ''
  addVisible.value = true
}

/**
 * 提交添加车辆表单
 * 成功后关闭对话框并刷新列表
 */
const submitAdd = async () => {
  if (!addFormRef.value) return
  try {
    await addFormRef.value.validate()
    await createVehicle(addForm)
    ElMessage.success('添加成功')
    addVisible.value = false
    fetchData()
  } catch {}
}

/**
 * 查看指定车辆的维修保养记录
 * 请求后端获取该车辆的维修记录列表，然后打开查看对话框
 * @param {Object} row - 当前车辆行数据，包含车辆 id
 */
const showMaintenance = async (row) => {
  try {
    const res = await getVehicleMaintenance(row.id)
    maintenanceList.value = res.data || []
    maintenanceVisible.value = true
  } catch {}
}

/**
 * 打开添加维修记录对话框
 * 重置表单数据并关联当前车辆ID
 * @param {Object} row - 当前车辆行数据
 */
const showAddMaintenance = (row) => {
  maintenanceForm.vehicleId = row.id
  maintenanceForm.type = 1
  maintenanceForm.description = ''
  maintenanceForm.cost = 0
  maintenanceForm.maintenanceDate = ''
  maintenanceForm.nextDueDate = ''
  addMaintenanceVisible.value = true
}

/**
 * 提交添加维修记录表单
 * 调用接口为指定车辆创建维修保养记录
 */
const submitMaintenance = async () => {
  if (!maintenanceFormRef.value) return
  try {
    await maintenanceFormRef.value.validate()
    await createVehicleMaintenance(maintenanceForm.vehicleId, maintenanceForm)
    ElMessage.success('添加成功')
    addMaintenanceVisible.value = false
    // 刷新到期提醒
    fetchExpiring()
    // 如果维修记录对话框正在显示，刷新列表
    if (maintenanceVisible.value) {
      try {
        const res = await getVehicleMaintenance(maintenanceForm.vehicleId)
        maintenanceList.value = res.data || []
      } catch {}
    }
  } catch {}
}

// ========== 生命周期 ==========
/** 组件挂载后同时加载车辆列表和到期提醒数据 */
onMounted(() => { fetchData(); fetchExpiring() })
</script>

<style scoped>
/* 页面容器：垂直排列子元素 */
.page-container { display: flex; flex-direction: column; gap: 16px; }
/* 到期提醒横幅圆角样式 */
.expire-alert { border-radius: 10px; }
/* 已过期日期文字样式：红色加粗 */
.expire-text { color: #ef4444; font-weight: 600; }
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
/* 分页栏布局 */
.pagination-bar { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; }
/* 总记录数文字样式 */
.total-text { font-size: 13px; color: #94a3b8; }
</style>
