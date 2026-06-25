<!--
  @description 学员档案附件管理页面
  提供学员附件资料的上传和管理功能，包括：
  1. 附件列表：展示学员已上传的所有附件资料
  2. 上传附件：支持上传身份证、体检报告、照片、合同、其他等类型文件
  3. 删除附件：支持删除不需要的附件资料
  4. 文件信息：显示文件名、类型、大小、上传时间等详细信息

  使用场景：
  - 学员上传个人证件和资料
  - 管理员/前台查看和管理学员档案附件
-->

<template>
  <div class="page-container">
    <!-- ==================== 主内容卡片 ==================== -->
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <div class="card-title">
            <el-icon><FolderOpened /></el-icon>
            <span>学员档案</span>
          </div>
          <!-- 上传附件按钮 -->
          <el-button type="primary" @click="showUploadDialog">
            <el-icon><Upload /></el-icon>上传附件
          </el-button>
        </div>
      </template>

      <!-- ==================== 筛选栏 ==================== -->
      <div class="search-bar">
        <!-- 附件类型筛选 -->
        <el-select v-model="filterType" placeholder="类型筛选" clearable style="width: 140px">
          <el-option label="身份证" value="身份证" />
          <el-option label="体检报告" value="体检报告" />
          <el-option label="照片" value="照片" />
          <el-option label="合同" value="合同" />
          <el-option label="其他" value="其他" />
        </el-select>
      </div>

      <!-- ==================== 附件列表 ==================== -->
      <el-table :data="filteredAttachments" stripe v-loading="loading" class="data-table">
        <!-- 文件名 -->
        <el-table-column label="文件名" min-width="220">
          <template #default="{ row }">
            <div class="file-name-cell">
              <div class="file-icon" :style="{ background: getFileIconBg(row.attachmentType) }">
                <el-icon :size="16" :style="{ color: getFileIconColor(row.attachmentType) }">
                  <component :is="getFileIcon(row.attachmentType)" />
                </el-icon>
              </div>
              <div class="file-info">
                <div class="file-name">{{ row.fileName }}</div>
                <div class="file-ext">{{ getFileExt(row.fileName) }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <!-- 附件类型 -->
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="attachmentTypeTag(row.attachmentType)" size="small" effect="light">
              {{ row.attachmentType }}
            </el-tag>
          </template>
        </el-table-column>
        <!-- 文件大小 -->
        <el-table-column label="大小" width="100" align="center">
          <template #default="{ row }">
            <span class="file-size">{{ formatFileSize(row.fileSize) }}</span>
          </template>
        </el-table-column>
        <!-- 上传时间 -->
        <el-table-column prop="uploadTime" label="上传时间" width="170" />
        <!-- 备注 -->
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{ 'no-data': !row.remark }">{{ row.remark || '-' }}</span>
          </template>
        </el-table-column>
        <!-- 操作列 -->
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="previewFile(row)">
              <el-icon><View /></el-icon>预览
            </el-button>
            <el-button link type="danger" size="small" @click="deleteAttachment(row)">
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空数据提示 -->
      <el-empty v-if="!loading && filteredAttachments.length === 0" description="暂无附件数据" />

      <!-- 分页栏 -->
      <div class="pagination-bar" v-if="attachments.length > 0">
        <span class="total-text">共 {{ attachments.length }} 个附件</span>
      </div>
    </el-card>

    <!-- ==================== 上传附件对话框 ==================== -->
    <el-dialog v-model="uploadVisible" title="上传附件" width="520" destroy-on-close>
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="80px">
        <!-- 附件类型选择 -->
        <el-form-item label="附件类型" prop="attachmentType">
          <el-select v-model="uploadForm.attachmentType" placeholder="请选择附件类型" style="width: 100%">
            <el-option label="身份证" value="身份证">
              <el-icon style="margin-right: 8px"><Postcard /></el-icon>身份证
            </el-option>
            <el-option label="体检报告" value="体检报告">
              <el-icon style="margin-right: 8px"><FirstAidKit /></el-icon>体检报告
            </el-option>
            <el-option label="照片" value="照片">
              <el-icon style="margin-right: 8px"><Picture /></el-icon>照片
            </el-option>
            <el-option label="合同" value="合同">
              <el-icon style="margin-right: 8px"><Document /></el-icon>合同
            </el-option>
            <el-option label="其他" value="其他">
              <el-icon style="margin-right: 8px"><More /></el-icon>其他
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 文件上传区域 -->
        <el-form-item label="选择文件" prop="file">
          <el-upload ref="uploadRef" class="upload-area"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
            :on-remove="handleFileRemove"
            drag
          >
            <el-icon class="el-icon--upload"><Upload /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 jpg/png/pdf/doc/docx 格式，单个文件不超过 10MB
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <!-- 备注 -->
        <el-form-item label="备注">
          <el-input v-model="uploadForm.remark" placeholder="添加备注信息（选填）" maxlength="100" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">
          <el-icon><Upload /></el-icon>确认上传
        </el-button>
      </template>
    </el-dialog>

    <!-- ==================== 文件预览对话框 ==================== -->
    <el-dialog v-model="previewVisible" title="文件预览" width="700" destroy-on-close>
      <div class="preview-container">
        <!-- 图片预览 -->
        <el-image v-if="isImageFile(previewFileData?.fileName)"
          :src="previewFileData?.fileUrl"
          fit="contain"
          style="max-width: 100%; max-height: 500px"
          :preview-src-list="[previewFileData?.fileUrl]"
        />
        <!-- PDF 预览 -->
        <iframe v-else-if="isPdfFile(previewFileData?.fileName)"
          :src="previewFileData?.fileUrl"
          style="width: 100%; height: 500px; border: none"
        />
        <!-- 其他文件类型提示 -->
        <div v-else class="preview-other">
          <el-icon :size="48" color="#94a3b8"><Document /></el-icon>
          <p>该文件类型暂不支持在线预览</p>
          <el-button type="primary" @click="downloadFile(previewFileData)">
            <el-icon><Download /></el-icon>下载文件
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import {
  FolderOpened, Upload, View, Delete, Postcard, FirstAidKit,
  Picture, Document, More, Download
} from '@element-plus/icons-vue'

// ========== 路由信息 ==========
/** @description 路由实例，用于获取URL参数中的学员ID */
const route = useRoute()
/** @description 当前学员ID，从路由参数中获取 */
const studentId = computed(() => route.params.id)

// ========== 加载状态 ==========
/** @description 列表加载状态 */
const loading = ref(false)
/** @description 上传按钮加载状态 */
const uploading = ref(false)

// ========== 附件列表状态 ==========
/** @description 附件列表数据 */
const attachments = ref([])
/** @description 附件类型筛选条件 */
const filterType = ref('')

/**
 * @description 过滤后的附件列表
 * 根据选择的类型筛选附件
 */
const filteredAttachments = computed(() => {
  if (!filterType.value) return attachments.value
  return attachments.value.filter(a => a.attachmentType === filterType.value)
})

// ========== 上传对话框状态 ==========
/** @description 上传对话框是否可见 */
const uploadVisible = ref(false)
/** @description 上传表单引用 */
const uploadFormRef = ref()
/** @description 上传组件引用 */
const uploadRef = ref()
/** @description 上传表单数据 */
const uploadForm = reactive({
  attachmentType: '',
  file: null,
  remark: ''
})
/** @description 上传表单验证规则 */
const uploadRules = {
  attachmentType: [{ required: true, message: '请选择附件类型', trigger: 'change' }],
  file: [{ required: true, message: '请选择要上传的文件', trigger: 'change' }]
}

// ========== 预览对话框状态 ==========
/** @description 预览对话框是否可见 */
const previewVisible = ref(false)
/** @description 当前预览的文件数据 */
const previewFileData = ref(null)

// ========== 工具函数 ==========

/**
 * @description 根据附件类型返回文件图标背景色
 * @param {string} type - 附件类型
 * @returns {string} 背景颜色
 */
const getFileIconBg = (type) => {
  const map = {
    '身份证': '#eef2ff',
    '体检报告': '#ecfdf5',
    '照片': '#fef2f2',
    '合同': '#fffbeb',
    '其他': '#f1f5f9'
  }
  return map[type] || '#f1f5f9'
}

/**
 * @description 根据附件类型返回文件图标颜色
 * @param {string} type - 附件类型
 * @returns {string} 图标颜色
 */
const getFileIconColor = (type) => {
  const map = {
    '身份证': '#6366f1',
    '体检报告': '#10b981',
    '照片': '#ef4444',
    '合同': '#f59e0b',
    '其他': '#94a3b8'
  }
  return map[type] || '#94a3b8'
}

/**
 * @description 根据附件类型返回对应的图标组件名
 * @param {string} type - 附件类型
 * @returns {string} 图标组件名
 */
const getFileIcon = (type) => {
  const map = {
    '身份证': 'Postcard',
    '体检报告': 'FirstAidKit',
    '照片': 'Picture',
    '合同': 'Document',
    '其他': 'More'
  }
  return map[type] || 'Document'
}

/**
 * @description 根据附件类型返回标签颜色类型
 * @param {string} type - 附件类型
 * @returns {string} Element Plus 标签类型
 */
const attachmentTypeTag = (type) => {
  const map = {
    '身份证': 'primary',
    '体检报告': 'success',
    '照片': 'danger',
    '合同': 'warning',
    '其他': 'info'
  }
  return map[type] || 'info'
}

/**
 * @description 获取文件扩展名
 * @param {string} fileName - 文件名
 * @returns {string} 扩展名（大写）
 */
const getFileExt = (fileName) => {
  if (!fileName) return ''
  const ext = fileName.split('.').pop()
  return ext ? ext.toUpperCase() : ''
}

/**
 * @description 格式化文件大小
 * 将字节数转换为人类可读的格式
 * @param {number} bytes - 文件大小（字节）
 * @returns {string} 格式化后的文件大小
 */
const formatFileSize = (bytes) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

/**
 * @description 判断文件是否为图片类型
 * @param {string} fileName - 文件名
 * @returns {boolean}
 */
const isImageFile = (fileName) => {
  if (!fileName) return false
  const ext = fileName.split('.').pop().toLowerCase()
  return ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext)
}

/**
 * @description 判断文件是否为PDF类型
 * @param {string} fileName - 文件名
 * @returns {boolean}
 */
const isPdfFile = (fileName) => {
  if (!fileName) return false
  return fileName.split('.').pop().toLowerCase() === 'pdf'
}

// ========== 数据请求函数 ==========

/**
 * @description 获取学员附件列表
 * 从后端获取指定学员的所有附件资料
 */
const fetchAttachments = async () => {
  if (!studentId.value) return
  loading.value = true
  try {
    const res = await request.get(`/students/${studentId.value}/attachments`)
    attachments.value = res.data || []
  } catch {} finally {
    loading.value = false
  }
}

/**
 * @description 打开上传对话框
 */
const showUploadDialog = () => {
  uploadForm.attachmentType = ''
  uploadForm.file = null
  uploadForm.remark = ''
  uploadVisible.value = true
}

/**
 * @description 文件选择变化回调
 * 验证文件类型和大小后保存到表单
 * @param {object} uploadFile - 上传文件对象
 */
const handleFileChange = (uploadFile) => {
  // 验证文件大小（10MB）
  const maxSize = 10 * 1024 * 1024
  if (uploadFile.size > maxSize) {
    ElMessage.warning('文件大小不能超过 10MB')
    uploadRef.value?.clearFiles()
    uploadForm.file = null
    return
  }
  // 验证文件类型
  const allowedTypes = ['image/jpeg', 'image/png', 'application/pdf',
    'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document']
  if (!allowedTypes.includes(uploadFile.raw.type)) {
    ElMessage.warning('仅支持 jpg/png/pdf/doc/docx 格式')
    uploadRef.value?.clearFiles()
    uploadForm.file = null
    return
  }
  uploadForm.file = uploadFile.raw
}

/**
 * @description 文件移除回调
 * 清除表单中的文件数据
 */
const handleFileRemove = () => {
  uploadForm.file = null
}

/**
 * @description 文件数量超出限制回调
 */
const handleExceed = () => {
  ElMessage.warning('只能上传一个文件，请先移除已选文件')
}

/**
 * @description 提交上传附件
 * 使用 FormData 格式上传文件到后端
 */
const submitUpload = async () => {
  // 验证表单
  try {
    await uploadFormRef.value.validate()
  } catch {
    return
  }
  if (!uploadForm.file) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  uploading.value = true
  try {
    // 构建 FormData 对象
    const formData = new FormData()
    formData.append('file', uploadForm.file)
    formData.append('attachmentType', uploadForm.attachmentType)
    if (uploadForm.remark) {
      formData.append('remark', uploadForm.remark)
    }

    await request.post(`/students/${studentId.value}/attachments/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success('上传成功')
    uploadVisible.value = false
    fetchAttachments()
  } catch {} finally {
    uploading.value = false
  }
}

/**
 * @description 预览文件
 * 打开预览对话框展示文件内容
 * @param {object} row - 附件行数据
 */
const previewFile = (row) => {
  previewFileData.value = row
  previewVisible.value = true
}

/**
 * @description 下载文件
 * 在新窗口中打开文件URL进行下载
 * @param {object} file - 文件数据
 */
const downloadFile = (file) => {
  if (file?.fileUrl) {
    window.open(file.fileUrl, '_blank')
  }
}

/**
 * @description 删除附件
 * 弹出确认框后调用后端接口删除附件
 * @param {object} row - 附件行数据
 */
const deleteAttachment = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除附件「${row.fileName}」吗？删除后无法恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
    await request.delete(`/students/${studentId.value}/attachments/${row.id}`)
    ElMessage.success('删除成功')
    fetchAttachments()
  } catch {}
}

// ========== 生命周期 ==========

/** @description 组件挂载时获取附件列表数据 */
onMounted(fetchAttachments)
</script>

<style scoped>
/* 页面容器 */
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ==================== 主卡片样式 ==================== */
.main-card {
  border-radius: 12px;
}
.main-card :deep(.el-card__header) {
  padding: 16px 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

/* ==================== 搜索栏 ==================== */
.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

/* ==================== 数据表格 ==================== */
.data-table {
  border-radius: 8px;
}

/* 文件名单元格 */
.file-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}
.file-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.file-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.file-name {
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
  word-break: break-all;
}
.file-ext {
  font-size: 11px;
  color: #94a3b8;
}

/* 文件大小 */
.file-size {
  font-size: 13px;
  color: #64748b;
}

/* 无数据提示 */
.no-data {
  color: #cbd5e1;
}

/* ==================== 上传区域 ==================== */
.upload-area {
  width: 100%;
}
.upload-area :deep(.el-upload) {
  width: 100%;
}
.upload-area :deep(.el-upload-dragger) {
  width: 100%;
  padding: 30px 0;
}

/* ==================== 分页栏 ==================== */
.pagination-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}
.total-text {
  font-size: 13px;
  color: #94a3b8;
}

/* ==================== 预览区域 ==================== */
.preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}
.preview-other {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px;
}
.preview-other p {
  margin: 0;
  font-size: 14px;
  color: #94a3b8;
}
</style>
