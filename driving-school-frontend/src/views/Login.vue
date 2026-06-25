<!--
  @description 登录页面
  系统入口页面，采用左右分栏布局：
  - 左侧品牌区：展示系统 Logo、名称、英文标题和功能亮点
  - 右侧登录表单：用户名/密码输入框 + 登录按钮 + 演示账号快捷填充

  页面视觉特点：
  - 深色背景 + 模糊光晕装饰，营造科技感
  - 左侧渐变品牌区 + 右侧白色表单区的经典登录页布局
  - 4个演示账号（管理员/前台/教练/学员）可一键填充，方便测试
-->

<template>
  <div class="login-container">
    <!-- 背景装饰：3个模糊圆形光晕，增加视觉层次感 -->
    <div class="bg-shape shape-1"></div>
    <div class="bg-shape shape-2"></div>
    <div class="bg-shape shape-3"></div>

    <div class="login-wrapper">
      <!-- ==================== 左侧品牌展示区 ==================== -->
      <div class="brand-side">
        <div class="brand-content">
          <!-- 系统 Logo（SVG 图标） -->
          <div class="brand-icon">
            <svg viewBox="0 0 48 48" width="56" height="56" fill="none" stroke="#fff" stroke-width="1.5">
              <circle cx="24" cy="24" r="20" opacity="0.2" fill="#fff"/>
              <path d="M16 32h-2a2 2 0 01-2-2V18a2 2 0 012-2h2m16 16h2a2 2 0 002-2V18a2 2 0 00-2-2h-2" stroke-linecap="round"/>
              <circle cx="18" cy="20" r="2"/><circle cx="30" cy="20" r="2"/>
              <path d="M14 28h20M14 24h20" stroke-linecap="round" opacity="0.6"/>
              <path d="M20 32v4M28 32v4" stroke-linecap="round"/>
            </svg>
          </div>
          <!-- 系统标题 -->
          <h1>驾校管理系统</h1>
          <p class="brand-desc">Driving School Management System</p>
          <!-- 功能亮点列表 -->
          <div class="brand-features">
            <div class="feature-item"><el-icon><CircleCheck /></el-icon> 学员全流程管理</div>
            <div class="feature-item"><el-icon><CircleCheck /></el-icon> 智能排课预约</div>
            <div class="feature-item"><el-icon><CircleCheck /></el-icon> 财务数据统计</div>
          </div>
        </div>
      </div>

      <!-- ==================== 右侧登录表单区 ==================== -->
      <div class="form-side">
        <div class="form-content">
          <h2>欢迎回来</h2>
          <p class="form-subtitle">请登录您的账号</p>

          <!-- 登录表单：支持回车键提交 -->
          <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin" size="large">
            <!-- 用户名输入框 -->
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
            </el-form-item>
            <!-- 密码输入框：支持显示/隐藏密码 -->
            <el-form-item prop="password">
              <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
            </el-form-item>
            <!-- 登录按钮：点击后显示加载状态 -->
            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleLogin" class="login-btn">
                {{ loading ? '登录中...' : '登 录' }}
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { ElMessage } from 'element-plus'

/** @description 路由实例 */
const router = useRouter()
/** @description 用户状态 Store */
const userStore = useUserStore()
/** @description 表单引用，用于调用 validate 方法 */
const formRef = ref()
/** @description 登录按钮加载状态 */
const loading = ref(false)

/** @description 登录表单数据 */
const form = reactive({ username: '', password: '' })
/** @description 表单验证规则 */
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

/**
 * @description 处理登录操作
 * 流程：表单验证 -> 调用 Store 登录方法 -> 成功提示 -> 跳转仪表盘
 */
const handleLogin = async () => {
  // 触发表单验证，验证不通过则抛出异常
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch {
    // 拦截器已通过 ElMessage 展示错误，此处无需额外处理
  } finally { loading.value = false }
}
</script>

<style scoped>
/* 登录页容器：全屏居中，深色背景 */
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0f172a;
  position: relative;
  overflow: hidden;
}

/* 背景装饰圆形光晕：使用模糊滤镜营造光效 */
.bg-shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
}
.shape-1 { width: 400px; height: 400px; background: #6366f1; top: -100px; left: -100px; }
.shape-2 { width: 300px; height: 300px; background: #3b82f6; bottom: -80px; right: -80px; }
.shape-3 { width: 200px; height: 200px; background: #8b5cf6; top: 50%; left: 50%; transform: translate(-50%,-50%); opacity: 0.2; }

/* 登录卡片容器：白色卡片 + 阴影 */
.login-wrapper {
  display: flex;
  width: 900px;
  min-height: 520px;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 25px 80px rgba(0,0,0,0.5), 0 0 0 1px rgba(255,255,255,0.05);
  position: relative;
  z-index: 1;
  animation: cardEnter 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes cardEnter {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 左侧品牌区：渐变紫色背景 + 装饰圆形 */
.brand-side {
  width: 380px;
  background: linear-gradient(135deg, #6366f1 0%, #818cf8 50%, #a78bfa 100%);
  padding: 48px 36px;
  display: flex;
  align-items: center;
  position: relative;
  overflow: hidden;
}
/* 品牌区右上角装饰圆 */
.brand-side::before {
  content: '';
  position: absolute;
  width: 200px; height: 200px;
  background: rgba(255,255,255,0.08);
  border-radius: 50%;
  top: -60px; right: -60px;
}
/* 品牌区左下角装饰圆 */
.brand-side::after {
  content: '';
  position: absolute;
  width: 150px; height: 150px;
  background: rgba(255,255,255,0.05);
  border-radius: 50%;
  bottom: -40px; left: -40px;
}

/* 品牌内容：相对定位，确保在装饰元素之上 */
.brand-content { position: relative; z-index: 1; color: #fff; }
.brand-icon { margin-bottom: 24px; }
.brand-content h1 { font-size: 28px; font-weight: 700; margin: 0 0 8px; letter-spacing: 1px; }
.brand-desc { font-size: 14px; opacity: 0.8; margin: 0 0 32px; }

/* 功能亮点列表 */
.brand-features { display: flex; flex-direction: column; gap: 14px; }
.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  opacity: 0.9;
}

/* 右侧表单区：白色背景 */
.form-side {
  flex: 1;
  background: #fff;
  padding: 48px 40px;
  display: flex;
  align-items: center;
}
.form-content { width: 100%; }
.form-content h2 { font-size: 24px; font-weight: 700; color: #1e293b; margin: 0 0 4px; }
.form-subtitle { font-size: 14px; color: #94a3b8; margin: 0 0 32px; }

/* 登录按钮：渐变紫色 + 全宽 */
.login-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1 0%, #818cf8 100%);
  border: none;
  letter-spacing: 2px;
  box-shadow: 0 4px 14px rgba(99,102,241,0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.login-btn:hover {
  opacity: 0.92;
  box-shadow: 0 6px 20px rgba(99,102,241,0.4);
  transform: translateY(-1px);
}
.login-btn:active {
  transform: translateY(0);
}
</style>
