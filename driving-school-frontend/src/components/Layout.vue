<!--
  @description 系统主布局组件
  作为所有业务页面的外壳容器，采用经典的后台管理系统布局结构：
  1. 左侧侧边栏（el-aside）：Logo + 可折叠导航菜单，根据用户角色动态显示菜单项
  2. 右侧内容区（el-container）：
     - 顶部栏（el-header）：面包屑导航、主题切换、通知角标、用户下拉菜单
     - 主内容区（el-main）：路由出口，带有页面切换过渡动画

  菜单项的可见性控制逻辑：
  - 仪表盘、教练管理、课时预约、考试管理、数据统计、通知公告：所有角色可见
  - 学员管理：ADMIN、STAFF、COACH 可见
  - 车辆管理、财务管理：ADMIN、STAFF 可见
  - 系统管理（用户管理、操作日志）：仅 ADMIN 可见

  未读通知通过定时器每 60 秒从后端拉取一次，保持角标实时更新
-->

<template>
  <el-container class="layout-container">
    <!-- ==================== 左侧侧边栏 ==================== -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <!-- Logo 区域：点击可跳转到仪表盘，折叠时仅显示图标 -->
      <div class="logo" @click="$router.push('/dashboard')">
        <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M9 17H5a2 2 0 01-2-2V5a2 2 0 012-2h4m6 14h4a2 2 0 002-2V5a2 2 0 00-2-2h-4M12 3v18" stroke-linecap="round" stroke-linejoin="round"/>
          <circle cx="8" cy="8" r="1" fill="currentColor"/>
          <circle cx="16" cy="16" r="1" fill="currentColor"/>
          <path d="M7 12h10" stroke-linecap="round" opacity="0.5"/>
        </svg>
        <transition name="fade">
          <!-- 侧边栏展开时显示系统名称，折叠时隐藏 -->
          <span v-show="!isCollapse" class="logo-text">驾校管理系统</span>
        </transition>
      </div>

      <!-- 导航菜单区域，使用 el-scrollbar 支持内容溢出时滚动 -->
      <el-scrollbar class="menu-scrollbar">
        <el-menu
          :default-active="$route.path"
          :collapse="isCollapse"
          router
          :collapse-transition="false"
          class="side-menu"
        >
          <!-- 仪表盘 - 所有角色可见 -->
          <el-menu-item index="/dashboard">
            <el-icon><Odometer /></el-icon>
            <template #title>仪表盘</template>
          </el-menu-item>

          <!-- 学员管理 - 管理员、前台、教练可见 -->
          <el-menu-item index="/students" v-if="['ADMIN','STAFF','COACH'].includes(role)">
            <el-icon><User /></el-icon>
            <template #title>学员管理</template>
          </el-menu-item>

          <!-- 教练管理 - 所有角色可见 -->
          <el-menu-item index="/coaches">
            <el-icon><UserFilled /></el-icon>
            <template #title>教练管理</template>
          </el-menu-item>

          <!-- 车辆管理 - 管理员、前台可见 -->
          <el-menu-item index="/vehicles" v-if="['ADMIN','STAFF'].includes(role)">
            <el-icon><Van /></el-icon>
            <template #title>车辆管理</template>
          </el-menu-item>

          <!-- 课时预约 - 所有角色可见 -->
          <el-menu-item index="/lessons">
            <el-icon><Calendar /></el-icon>
            <template #title>课时预约</template>
          </el-menu-item>

          <!-- 考试管理 - 所有角色可见 -->
          <el-menu-item index="/exams">
            <el-icon><EditPen /></el-icon>
            <template #title>考试管理</template>
          </el-menu-item>

          <!-- 财务管理（子菜单）- 管理员、前台可见 -->
          <el-sub-menu index="finance-menu" v-if="['ADMIN','STAFF'].includes(role)">
            <template #title>
              <el-icon><Money /></el-icon>
              <span>财务管理</span>
            </template>
            <el-menu-item index="/finance">收费记录</el-menu-item>
            <el-menu-item index="/finance/installment">分期付款</el-menu-item>
          </el-sub-menu>

          <!-- 电子合同 - 管理员、前台、学员可见 -->
          <el-menu-item index="/contracts" v-if="['ADMIN','STAFF','STUDENT'].includes(role)">
            <el-icon><Document /></el-icon>
            <template #title>电子合同</template>
          </el-menu-item>

          <!-- 学时签到 - 教练、管理员可见 -->
          <el-menu-item index="/checkin" v-if="['ADMIN','STAFF','COACH'].includes(role)">
            <el-icon><Location /></el-icon>
            <template #title>学时签到</template>
          </el-menu-item>

          <!-- 报名审核 - 管理员、前台可见 -->
          <el-menu-item index="/registrations" v-if="['ADMIN','STAFF'].includes(role)">
            <el-icon><Document /></el-icon>
            <template #title>报名审核</template>
          </el-menu-item>

          <!-- 在线刷题 - 学员可见 -->
          <el-menu-item index="/practice" v-if="role === 'STUDENT'">
            <el-icon><Notebook /></el-icon>
            <template #title>在线刷题</template>
          </el-menu-item>

          <!-- 满意度调查 - 学员可见 -->
          <el-menu-item index="/survey" v-if="role === 'STUDENT'">
            <el-icon><Star /></el-icon>
            <template #title>满意度调查</template>
          </el-menu-item>

          <!-- 招生渠道 - 管理员、前台可见 -->
          <el-menu-item index="/channels" v-if="['ADMIN','STAFF'].includes(role)">
            <el-icon><Share /></el-icon>
            <template #title>招生渠道</template>
          </el-menu-item>

          <!-- 教练绩效 - 管理员、前台、教练可见 -->
          <el-menu-item index="/coaches/performance" v-if="['ADMIN','STAFF','COACH'].includes(role)">
            <el-icon><TrendCharts /></el-icon>
            <template #title>教练绩效</template>
          </el-menu-item>

          <!-- 数据统计 - 管理员、前台可见 -->
          <el-menu-item index="/statistics" v-if="['ADMIN','STAFF'].includes(role)">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>数据统计</template>
          </el-menu-item>

          <!-- 提醒管理 - 所有角色可见 -->
          <el-menu-item index="/reminders">
            <el-icon><AlarmClock /></el-icon>
            <template #title>提醒管理</template>
          </el-menu-item>

          <!-- 通知公告 - 所有角色可见，显示未读通知数量角标 -->
          <el-menu-item index="/notifications">
            <el-icon><Bell /></el-icon>
            <template #title>
              通知公告
              <span v-if="notifyStore.unreadCount > 0" class="menu-badge">{{ notifyStore.unreadCount }}</span>
            </template>
          </el-menu-item>

          <!-- 系统管理（子菜单）- 仅管理员可见 -->
          <el-sub-menu index="system" v-if="role === 'ADMIN'">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/users">用户管理</el-menu-item>
            <el-menu-item index="/logs">操作日志</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <!-- ==================== 右侧内容区 ==================== -->
    <el-container>
      <!-- 顶部导航栏 -->
      <el-header class="header">
        <div class="header-left">
          <!-- 侧边栏折叠/展开按钮 -->
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse" size="18">
            <Expand v-if="isCollapse" />
            <Fold v-else />
          </el-icon>
          <!-- 面包屑导航：显示首页和当前页面标题 -->
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="$route.meta.title">{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- 主题切换器组件 -->
          <ThemePicker />
          <!-- 通知图标按钮，点击跳转到通知页面 -->
          <div class="header-action" @click="$router.push('/notifications')">
            <el-badge :value="notifyStore.unreadCount" :hidden="notifyStore.unreadCount === 0" :max="99">
              <el-icon size="18"><Bell /></el-icon>
            </el-badge>
          </div>
          <el-divider direction="vertical" />
          <!-- 用户信息下拉菜单 -->
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-info">
              <!-- 用户头像：有上传图片则显示图片，否则显示姓名首字母 -->
              <el-avatar :size="32" :style="{ background: avatarColor }" :src="userInfo.avatar">
                {{ avatarText }}
              </el-avatar>
              <div class="user-meta">
                <div class="user-name">{{ userInfo.realName || userInfo.username }}</div>
                <div class="user-role">{{ roleName }}</div>
              </div>
              <el-icon size="12"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人信息
                </el-dropdown-item>
                <el-dropdown-item command="password">
                  <el-icon><Lock /></el-icon>修改密码
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区：路由出口，带页面切换过渡动画 -->
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>

  <!-- AI 助手浮动面板 -->
  <AiChatWidget />
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { useNotificationStore } from '../store/notification'
import { ElMessageBox } from 'element-plus'
import ThemePicker from './ThemePicker.vue'
import AiChatWidget from './AiChatWidget.vue'

/** @description 路由实例，用于编程式导航 */
const router = useRouter()
/** @description 用户状态 Store */
const userStore = useUserStore()
/** @description 通知状态 Store */
const notifyStore = useNotificationStore()
/** @description 侧边栏是否折叠状态 */
const isCollapse = ref(false)

/** @description 当前登录用户信息（计算属性，响应式） */
const userInfo = computed(() => userStore.userInfo)
/** @description 当前用户角色标识（ADMIN/STAFF/COACH/STUDENT） */
const role = computed(() => userStore.userInfo.role || '')

/**
 * @description 角色名称映射
 * 将英文角色标识转换为中文显示名称
 */
const roleName = computed(() => {
  const map = { ADMIN: '管理员', STAFF: '前台', COACH: '教练', STUDENT: '学员' }
  return map[role.value] || ''
})

/**
 * @description 头像显示文字
 * 取用户真实姓名或用户名的第一个字符作为头像文字
 */
const avatarText = computed(() => (userInfo.value.realName || userInfo.value.username || '?').charAt(0))

/**
 * @description 头像背景颜色
 * 根据用户角色设置不同颜色，增强角色识别度
 * - 管理员：靛蓝紫
 * - 前台：翠绿
 * - 教练：暖橙
 * - 学员：海蓝
 */
const avatarColor = computed(() => {
  const colors = { ADMIN: '#6366f1', STAFF: '#10b981', COACH: '#f59e0b', STUDENT: '#3b82f6' }
  return colors[role.value] || '#6366f1'
})

/**
 * @description 未读通知定时轮询器
 * 组件挂载时立即获取一次未读数，之后每 60 秒轮询一次
 * 组件卸载时清除定时器，避免内存泄漏
 */
let timer = null
onMounted(() => {
  // 立即获取一次未读通知数
  notifyStore.fetchUnread()
  // 每 60 秒轮询一次
  timer = setInterval(() => notifyStore.fetchUnread(), 60000)
})
onUnmounted(() => {
  // 组件销毁时清除定时器
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})

/**
 * @description 处理用户下拉菜单命令
 * @param {string} cmd - 菜单命令标识
 * - profile: 跳转到个人信息页
 * - password: 跳转到个人信息页（修改密码功能在该页面）
 * - logout: 执行登出操作并跳转到登录页
 */
const handleCommand = async (cmd) => {
  if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'password') router.push('/profile')
  else if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      userStore.logout()
      router.push('/login')
    } catch {}
  }
}
</script>

<style scoped>
/* ===== 布局容器 ===== */
.layout-container { height: 100vh; }

/* ===== 侧边栏 ===== */
.aside {
  background: var(--sidebar-bg, linear-gradient(180deg, #0f172a 0%, #1e293b 100%));
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 12px rgba(0,0,0,0.08);
}

/* Logo 区域样式 */
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #e2e8f0;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(255,255,255,0.06);
  transition: background 0.2s ease;
}
.logo:hover { background: rgba(255,255,255,0.04); }
.logo-text {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #e2e8f0, #a5b4fc);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* 菜单滚动容器，占满剩余空间 */
.menu-scrollbar { flex: 1; overflow: hidden; }

/* 侧边栏菜单基础样式 */
.side-menu {
  border-right: none;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: var(--sidebar-text, #94a3b8);
  --el-menu-active-color: #fff;
  --el-menu-hover-bg-color: rgba(255,255,255,0.06);
}
.side-menu:not(.el-menu--collapse) { padding: 8px 10px; }

/* 菜单项样式：统一高度、圆角、过渡动画 */
.side-menu .el-menu-item,
.side-menu :deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
  margin-bottom: 3px;
  border-radius: 10px;
  font-size: 13.5px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  letter-spacing: 0.02em;
}

/* 激活菜单项样式：渐变背景 + 白色文字 + 阴影 */
.side-menu .el-menu-item.is-active {
  background: var(--sidebar-active, linear-gradient(135deg, #6366f1 0%, #818cf8 100%)) !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(99,102,241,0.35);
  font-weight: 500;
}

/* 菜单项 hover 效果 */
.side-menu .el-menu-item:hover:not(.is-active) {
  background: rgba(255,255,255,0.08) !important;
  color: #e2e8f0;
}

/* 子菜单标题 hover 效果 */
.side-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(255,255,255,0.08) !important;
  color: #e2e8f0;
}

/* 未读通知角标样式：红色圆角小标签 */
.menu-badge {
  display: inline-block;
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  text-align: center;
  border-radius: 9px;
  padding: 0 5px;
  margin-left: 6px;
  font-weight: 600;
}

/* ===== 顶部导航栏 ===== */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--header-bg, #fff);
  padding: 0 24px;
  height: 60px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.04);
  z-index: 10;
  border-bottom: 1px solid rgba(0,0,0,0.04);
}

/* 顶栏左侧区域：折叠按钮 + 面包屑 */
.header-left { display: flex; align-items: center; gap: 16px; }
.collapse-btn {
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.2s ease;
  color: var(--text-secondary, #64748b);
}
.collapse-btn:hover {
  background: var(--primary-bg, #eef2ff);
  color: var(--primary, #6366f1);
}

/* 顶栏右侧区域：主题切换、通知、用户菜单 */
.header-right { display: flex; align-items: center; gap: 6px; }

/* 顶栏操作按钮通用样式（通知图标等） */
.header-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--text-secondary, #64748b);
}
.header-action:hover {
  background: var(--primary-bg, #eef2ff);
  color: var(--primary, #6366f1);
}

/* 用户信息区域样式 */
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 10px;
  transition: all 0.2s ease;
}
.user-info:hover {
  background: var(--primary-bg, #eef2ff);
}

/* 用户姓名和角色文字 */
.user-meta { line-height: 1.2; }
.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-color, #1e293b);
}
.user-role {
  font-size: 11px;
  color: var(--text-secondary, #94a3b8);
  margin-top: 1px;
}

/* ===== 主内容区 ===== */
.main {
  background: var(--main-bg, #f5f7fa);
  padding: 24px;
  overflow-y: auto;
}

/* ===== 过渡动画 ===== */
/* Logo 文字淡入淡出动画 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* 页面切换动画：进入时从下方滑入 + 淡入，离开时淡出 */
.page-fade-enter-active {
  transition: opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1), transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.page-fade-leave-active {
  transition: opacity 0.15s ease;
}
.page-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.page-fade-leave-to { opacity: 0; }
</style>
