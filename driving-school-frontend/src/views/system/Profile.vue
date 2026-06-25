<!--
  个人中心页面
  功能说明：
  - 左侧展示当前登录用户的个人信息（头像、姓名、用户名、电话、角色）
  - 头像支持点击上传，上传后实时更新
  - 右侧提供修改密码功能，需输入旧密码和新密码（至少6位）
  - 密码修改成功后自动登出并跳转到登录页
  - 采用双栏布局（响应式：大屏左右排列，小屏上下排列）
-->
<template>
  <div class="page-container">
    <!-- 双栏布局：左侧个人信息 + 右侧修改密码 -->
    <el-row :gutter="16">
      <!-- 左栏：个人信息卡片 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="main-card">
          <template #header>
            <div class="card-title"><el-icon><User /></el-icon><span>个人信息</span></div>
          </template>
          <!-- 用户头像和姓名区域 -->
          <div class="profile-header">
            <!-- 头像：支持点击上传 -->
            <div class="avatar-wrapper" @click="triggerUpload">
              <el-avatar :size="72" :style="{ background: roleColor(userInfo.role) }" :src="userInfo.avatar">
                {{ (userInfo.realName || userInfo.username || '?').charAt(0) }}
              </el-avatar>
              <div class="avatar-overlay">
                <el-icon :size="20"><Camera /></el-icon>
                <span>更换头像</span>
              </div>
              <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="handleAvatarChange" />
            </div>
            <div class="profile-info">
              <!-- 用户姓名 -->
              <div class="profile-name">{{ userInfo.realName || userInfo.username }}</div>
              <!-- 角色标签 -->
              <el-tag :type="roleTagType(userInfo.role)" size="small">{{ roleName(userInfo.role) }}</el-tag>
            </div>
          </div>
          <!-- 详细信息描述列表 -->
          <el-descriptions :column="1" border class="profile-desc">
            <el-descriptions-item label="用户名">{{ userInfo.username }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ userInfo.realName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="电话">{{ userInfo.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="角色">
              <el-tag :type="roleTagType(userInfo.role)" size="small">{{ roleName(userInfo.role) }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 右栏：修改密码卡片 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="main-card">
          <template #header>
            <div class="card-title"><el-icon><Lock /></el-icon><span>修改密码</span></div>
          </template>
          <!-- 修改密码表单：包含旧密码、新密码输入和提交按钮 -->
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px" style="max-width:400px">
            <!-- 旧密码输入框 -->
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
            </el-form-item>
            <!-- 新密码输入框（至少6位） -->
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少6位" />
            </el-form-item>
            <!-- 提交按钮，带加载状态 -->
            <el-form-item>
              <el-button type="primary" @click="handleChangePwd" :loading="submitting">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import { changePassword, uploadAvatar } from '../../api'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'

// ========== 路由与状态 ==========
/** 路由实例，用于页面跳转 */
const router = useRouter()
/** 用户状态存储 */
const userStore = useUserStore()
/** 当前登录用户的个人信息（计算属性，响应式） */
const userInfo = computed(() => userStore.userInfo)
/** 提交按钮的加载状态 */
const submitting = ref(false)
/** 文件输入框引用 */
const fileInput = ref()

// ========== 密码表单相关 ==========
/** 密码表单的 DOM 引用，用于表单验证 */
const pwdFormRef = ref()
/** 密码表单数据：旧密码和新密码 */
const pwdForm = reactive({ oldPassword: '', newPassword: '' })
/**
 * 密码表单验证规则
 * - 旧密码：必填
 * - 新密码：必填，且至少6位
 */
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '至少6位', trigger: 'blur' }]
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
 * @returns {string} Element Plus 标签颜色类型
 */
const roleTagType = (r) => ({ ADMIN: 'danger', STAFF: 'warning', COACH: 'success', STUDENT: 'primary' }[r] || 'info')

/**
 * 根据角色返回对应的头像背景色
 * @param {string} r - 角色代码
 * @returns {string} 十六进制颜色值
 */
const roleColor = (r) => ({ ADMIN: '#ef4444', STAFF: '#f59e0b', COACH: '#10b981', STUDENT: '#6366f1' }[r] || '#94a3b8')

// ========== 头像上传相关 ==========
/**
 * 触发文件选择框
 */
const triggerUpload = () => {
  fileInput.value.click()
}

/**
 * 处理头像文件选择
 * 校验文件大小和格式后上传到服务器
 */
const handleAvatarChange = async (e) => {
  const file = e.target.files[0]
  if (!file) return

  // 校验文件大小（2MB）
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 2MB')
    return
  }

  // 校验文件格式
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }

  // 上传头像
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await uploadAvatar(formData)
    // 更新本地用户信息中的头像URL
    userStore.updateUserInfo({ avatar: res.data })
    ElMessage.success('头像更新成功')
  } catch {
    ElMessage.error('头像上传失败')
  }

  // 清空 input 值，允许重复上传同一文件
  e.target.value = ''
}

// ========== 业务函数 ==========
/**
 * 处理修改密码操作
 * 流程：1.表单验证 -> 2.调用修改密码接口 -> 3.提示成功 -> 4.登出 -> 5.跳转登录页
 */
const handleChangePwd = async () => {
  // 表单验证，不通过则直接返回
  try { await pwdFormRef.value.validate() } catch { return }
  submitting.value = true
  try {
    await changePassword(pwdForm)
    ElMessage.success('密码修改成功，请重新登录')
    // 修改成功后登出用户并跳转到登录页
    userStore.logout()
    router.push('/login')
  } catch {} finally { submitting.value = false }
}
</script>

<style scoped>
/* 页面容器：垂直排列子元素 */
.page-container { display: flex; flex-direction: column; gap: 16px; }
/* 主卡片圆角样式 */
.main-card { border-radius: 12px; }
/* 覆盖卡片头部内边距 */
.main-card :deep(.el-card__header) { padding: 16px 20px; }
/* 卡片标题样式 */
.card-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: #1e293b; }
/* 用户头像和信息区域：水平排列 */
.profile-header { display: flex; align-items: center; gap: 20px; margin-bottom: 24px; }

/* 头像上传区域 */
.avatar-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}
.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: #fff;
  font-size: 11px;
  opacity: 0;
  transition: opacity 0.25s ease;
  border-radius: 50%;
}
.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

/* 用户信息文字区域：垂直排列 */
.profile-info { display: flex; flex-direction: column; gap: 6px; }
/* 用户姓名样式：大号加粗 */
.profile-name { font-size: 20px; font-weight: 700; color: #1e293b; }
/* 描述列表标签列宽度 */
.profile-desc :deep(.el-descriptions__label) { width: 80px; }
</style>
