/**
 * @description 路由配置模块
 * 使用 Vue Router 4 的 createWebHistory 模式（HTML5 History 模式）
 * 定义了驾校管理系统的所有页面路由，包含：
 * 1. 登录页路由（公开页面，无需鉴权）
 * 2. 主布局下的子路由（需要登录才能访问）
 * 3. 路由守卫：页面鉴权、动态标题、角色权限控制
 */

import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

/**
 * @description 路由配置表
 * 采用嵌套路由结构：
 * - /login: 登录页，独立布局，meta.public = true 表示无需鉴权
 * - /: 根路由使用 Layout 组件作为布局容器，所有业务页面作为其子路由
 *   - /dashboard: 仪表盘 - 数据概览与图表展示
 *   - /students: 学员管理 - 学员列表、报名、审核
 *   - /students/:id: 学员详情 - 单个学员的详细信息与科目进度
 *   - /coaches: 教练管理 - 教练列表、添加、分配学员
 *   - /coaches/:id: 教练详情 - 教练信息与学员评价
 *   - /vehicles: 车辆管理 - 车辆列表、维修保养记录
 *   - /lessons: 课时预约 - 预约管理、确认、完成
 *   - /exams: 考试管理 - 考试安排、报名
 *   - /finance: 财务管理 - 收费记录、退费
 *   - /statistics: 数据统计 - 图表、排行、数据导出
 *   - /notifications: 通知公告 - 通知列表、发布、已读标记
 *   - /users: 用户管理 - 仅 ADMIN 角色可访问
 *   - /logs: 操作日志 - 仅 ADMIN 角色可访问
 *   - /profile: 个人信息 - 查看与修改密码
 * - /:pathMatch(.*)*: 兜底路由，未匹配的路径重定向到仪表盘
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录', public: true } // public: true 标记为公开页面，路由守卫中跳过鉴权
  },
  {
    path: '/',
    component: () => import('../components/Layout.vue'), // 主布局容器（侧边栏 + 顶栏 + 内容区）
    redirect: '/dashboard', // 根路径默认重定向到仪表盘
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/dashboard/Index.vue'), meta: { title: '仪表盘' } },
      { path: 'students', name: 'Students', component: () => import('../views/student/Index.vue'), meta: { title: '学员管理', roles: ['ADMIN', 'STAFF', 'COACH'] } },
      { path: 'students/:id', name: 'StudentDetail', component: () => import('../views/student/Detail.vue'), meta: { title: '学员详情' } },
      { path: 'coaches', name: 'Coaches', component: () => import('../views/coach/Index.vue'), meta: { title: '教练管理' } },
      { path: 'coaches/performance', name: 'CoachPerformance', component: () => import('../views/coach/Performance.vue'), meta: { title: '教练绩效', roles: ['ADMIN', 'STAFF', 'COACH'] } },
      { path: 'coaches/:id', name: 'CoachDetail', component: () => import('../views/coach/Detail.vue'), meta: { title: '教练详情' } },
      { path: 'vehicles', name: 'Vehicles', component: () => import('../views/vehicle/Index.vue'), meta: { title: '车辆管理', roles: ['ADMIN', 'STAFF'] } },
      { path: 'lessons', name: 'Lessons', component: () => import('../views/lesson/Index.vue'), meta: { title: '课时预约' } },
      { path: 'exams', name: 'Exams', component: () => import('../views/exam/Index.vue'), meta: { title: '考试管理' } },
      { path: 'finance', name: 'Finance', component: () => import('../views/finance/Index.vue'), meta: { title: '财务管理', roles: ['ADMIN', 'STAFF'] } },
      { path: 'finance/installment', name: 'Installment', component: () => import('../views/finance/Installment.vue'), meta: { title: '分期付款', roles: ['ADMIN', 'STAFF'] } },
      { path: 'statistics', name: 'Statistics', component: () => import('../views/statistics/Index.vue'), meta: { title: '数据统计', roles: ['ADMIN', 'STAFF'] } },
      { path: 'channels', name: 'Channels', component: () => import('../views/channel/Index.vue'), meta: { title: '招生渠道', roles: ['ADMIN', 'STAFF'] } },
      { path: 'contracts', name: 'Contracts', component: () => import('../views/contract/Index.vue'), meta: { title: '电子合同', roles: ['ADMIN', 'STAFF', 'STUDENT'] } },
      { path: 'checkin', name: 'CheckIn', component: () => import('../views/checkin/Index.vue'), meta: { title: '学时签到', roles: ['ADMIN', 'STAFF', 'COACH'] } },
      { path: 'registrations', name: 'Registrations', component: () => import('../views/registration/Index.vue'), meta: { title: '报名审核', roles: ['ADMIN', 'STAFF'] } },
      { path: 'reminders', name: 'Reminders', component: () => import('../views/reminder/Index.vue'), meta: { title: '提醒管理' } },
      { path: 'notifications', name: 'Notifications', component: () => import('../views/notification/Index.vue'), meta: { title: '通知公告' } },
      { path: 'practice', name: 'Practice', component: () => import('../views/practice/Index.vue'), meta: { title: '在线刷题', roles: ['STUDENT'] } },
      { path: 'survey', name: 'Survey', component: () => import('../views/survey/Index.vue'), meta: { title: '满意度调查', roles: ['STUDENT'] } },
      { path: 'students/:id/attachments', name: 'StudentAttachments', component: () => import('../views/student/Attachment.vue'), meta: { title: '学员档案', roles: ['ADMIN', 'STAFF', 'COACH'] } },
      // 以下两个页面仅限管理员角色访问，通过 meta.roles 进行权限控制
      { path: 'users', name: 'Users', component: () => import('../views/system/UserManage.vue'), meta: { title: '用户管理', roles: ['ADMIN'] } },
      { path: 'logs', name: 'Logs', component: () => import('../views/system/LogManage.vue'), meta: { title: '操作日志', roles: ['ADMIN'] } },
      { path: 'profile', name: 'Profile', component: () => import('../views/system/Profile.vue'), meta: { title: '个人信息' } }
    ]
  },
  {
    // 兜底路由：匹配所有未定义的路径，重定向到仪表盘页
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

/**
 * @description 创建路由实例
 * 使用 HTML5 History 模式，URL 不带 # 号
 */
const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * @description 全局前置路由守卫
 * 在每次路由跳转前执行，负责以下功能：
 * 1. 动态设置页面标题（浏览器标签页标题）
 * 2. 公开页面（如登录页）直接放行
 * 3. 未登录用户重定向到登录页
 * 4. 刷新页面后 userInfo 可能为空，需重新从后端获取
 * 5. 角色权限校验：如果路由要求特定角色而当前用户无权限，重定向到仪表盘
 */
router.beforeEach(async (to, from, next) => {
  // 设置页面标题格式：「页面名称 - 驾校管理系统」
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + '驾校管理系统'
  // 公开页面直接放行（如登录页）
  if (to.meta.public) return next()

  const userStore = useUserStore()
  // 未登录（无 Token），重定向到登录页
  if (!userStore.token) return next('/login')

  /**
   * 刷新页面后，Pinia Store 中的状态会丢失（仅 localStorage 持久化的数据保留）
   * 此时 userInfo 可能为空对象，需要重新从后端获取用户信息
   * 如果获取失败（如 Token 已过期），则清除登录态并跳转登录页
   */
  if (!userStore.userInfo.role) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      userStore.logout()
      return next('/login')
    }
  }

  /**
   * 角色权限校验
   * 如果路由 meta 中指定了 roles 数组，则检查当前用户角色是否在允许列表中
   * 例如 /users 路由要求 roles: ['ADMIN']，非管理员用户将被重定向到仪表盘
   */
  if (to.meta.roles && !to.meta.roles.includes(userStore.userInfo.role)) {
    return next('/dashboard')
  }

  // 所有校验通过，放行
  next()
})

export default router
