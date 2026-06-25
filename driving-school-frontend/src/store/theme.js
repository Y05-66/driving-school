/**
 * @description 主题状态管理模块 (Pinia Store)
 * 管理系统的主题颜色风格切换功能
 * 预设了 6 种主题风格：靛蓝紫(默认)、海蓝、翠绿、暖橙、玫红、深邃黑
 * 每种主题定义了完整的设计 Token（主色、侧边栏、背景色等）
 * 切换主题时通过 CSS 自定义属性（CSS Variables）动态更新全站样式
 */

import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

/**
 * @description 预设主题配置表
 * 每个主题包含以下属性：
 * - name: 主题显示名称
 * - primary / primaryLight: 主色及浅色变体（按钮、链接等强调色）
 * - sidebar: 侧边栏渐变背景
 * - sidebarText: 侧边栏菜单文字颜色
 * - sidebarActive: 侧边栏激活菜单项的渐变背景
 * - headerBg: 顶部导航栏背景色
 * - mainBg: 主内容区背景色
 * - cardBg: 卡片组件背景色
 * - textColor: 主文字颜色
 * - textSecondary: 次要文字颜色
 * - borderColor: 边框颜色
 * - isDark: 是否为暗色主题（影响 data-theme 属性，供其他组件判断）
 */
const themes = {
  // 靛蓝紫主题 - 默认主题，深蓝紫色系
  default: {
    name: '靛蓝紫',
    primary: '#6366f1',
    primaryLight: '#818cf8',
    sidebar: 'linear-gradient(180deg, #0f172a 0%, #1e293b 100%)',
    sidebarText: '#94a3b8',
    sidebarActive: 'linear-gradient(135deg, #6366f1 0%, #818cf8 100%)',
    headerBg: '#fff',
    mainBg: '#f8fafc',
    cardBg: '#fff',
    textColor: '#1e293b',
    textSecondary: '#64748b',
    borderColor: '#f1f5f9',
    isDark: false
  },
  // 海蓝主题 - 清新蓝色系
  blue: {
    name: '海蓝',
    primary: '#3b82f6',
    primaryLight: '#60a5fa',
    sidebar: 'linear-gradient(180deg, #0c1929 0%, #1a365d 100%)',
    sidebarText: '#93c5fd',
    sidebarActive: 'linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%)',
    headerBg: '#fff',
    mainBg: '#f0f9ff',
    cardBg: '#fff',
    textColor: '#1e293b',
    textSecondary: '#64748b',
    borderColor: '#e0f2fe',
    isDark: false
  },
  // 翠绿主题 - 自然绿色系
  green: {
    name: '翠绿',
    primary: '#10b981',
    primaryLight: '#34d399',
    sidebar: 'linear-gradient(180deg, #052e16 0%, #14532d 100%)',
    sidebarText: '#86efac',
    sidebarActive: 'linear-gradient(135deg, #10b981 0%, #34d399 100%)',
    headerBg: '#fff',
    mainBg: '#f0fdf4',
    cardBg: '#fff',
    textColor: '#1e293b',
    textSecondary: '#64748b',
    borderColor: '#dcfce7',
    isDark: false
  },
  // 暖橙主题 - 活力橙色系
  orange: {
    name: '暖橙',
    primary: '#f59e0b',
    primaryLight: '#fbbf24',
    sidebar: 'linear-gradient(180deg, #451a03 0%, #78350f 100%)',
    sidebarText: '#fcd34d',
    sidebarActive: 'linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%)',
    headerBg: '#fff',
    mainBg: '#fffbeb',
    cardBg: '#fff',
    textColor: '#1e293b',
    textSecondary: '#64748b',
    borderColor: '#fef3c7',
    isDark: false
  },
  // 玫红主题 - 时尚玫红色系
  rose: {
    name: '玫红',
    primary: '#f43f5e',
    primaryLight: '#fb7185',
    sidebar: 'linear-gradient(180deg, #4c0519 0%, #881337 100%)',
    sidebarText: '#fda4af',
    sidebarActive: 'linear-gradient(135deg, #f43f5e 0%, #fb7185 100%)',
    headerBg: '#fff',
    mainBg: '#fff1f2',
    cardBg: '#fff',
    textColor: '#1e293b',
    textSecondary: '#64748b',
    borderColor: '#ffe4e6',
    isDark: false
  },
  // 深邃黑主题 - 暗色护眼模式
  dark: {
    name: '深邃黑',
    primary: '#818cf8',
    primaryLight: '#a5b4fc',
    sidebar: 'linear-gradient(180deg, #0f0f1a 0%, #1a1a2e 100%)',
    sidebarText: '#a5b4fc',
    sidebarActive: 'linear-gradient(135deg, #6366f1 0%, #818cf8 100%)',
    headerBg: '#1e1e2e',
    mainBg: '#16161e',
    cardBg: '#1e1e2e',
    textColor: '#e2e8f0',
    textSecondary: '#94a3b8',
    borderColor: '#2d2d3f',
    isDark: true
  }
}

/**
 * @description 定义主题状态 Store
 * Store ID 为 'theme'，通过 useThemeStore() 调用
 */
export const useThemeStore = defineStore('theme', () => {
  /**
   * @description 当前主题名称
   * 初始化时从 localStorage 读取用户上次选择的主题
   * 默认为 'default'（靛蓝紫主题）
   */
  const currentTheme = ref(localStorage.getItem('theme') || 'default')

  /**
   * @description 切换主题
   * @param {string} name - 主题名称（对应 themes 对象的 key）
   * 切换流程：验证主题有效性 -> 更新当前主题 -> 持久化存储 -> 应用主题样式
   */
  function setTheme(name) {
    // 如果主题名称无效，直接返回
    if (!themes[name]) return
    // 更新响应式状态
    currentTheme.value = name
    // 持久化到 localStorage，下次打开页面自动应用
    localStorage.setItem('theme', name)
    // 将主题配置应用到 DOM 的 CSS 自定义属性
    applyTheme(themes[name])
  }

  /**
   * @description 将主题配置应用到页面
   * 通过修改 document.documentElement（即 <html> 标签）的 CSS 自定义属性
   * 使全局样式能够通过 var(--xxx) 引用当前主题的颜色值
   * @param {object} t - 主题配置对象
   */
  function applyTheme(t) {
    const root = document.documentElement
    // 设置各设计 Token 对应的 CSS 变量
    root.style.setProperty('--primary', t.primary)
    root.style.setProperty('--primary-light', t.primaryLight)
    root.style.setProperty('--primary-bg', t.primary + '18')
    root.style.setProperty('--sidebar-bg', t.sidebar)
    root.style.setProperty('--sidebar-text', t.sidebarText)
    root.style.setProperty('--sidebar-active', t.sidebarActive)
    root.style.setProperty('--header-bg', t.headerBg)
    root.style.setProperty('--main-bg', t.mainBg)
    root.style.setProperty('--card-bg', t.cardBg)
    root.style.setProperty('--text-color', t.textColor)
    root.style.setProperty('--text-secondary', t.textSecondary)
    root.style.setProperty('--border-color', t.borderColor)
    // 设置 data-theme 属性，供 CSS 选择器判断是否为暗色模式
    root.setAttribute('data-theme', t.isDark ? 'dark' : 'light')
  }

  // 模块加载时立即应用当前主题，确保页面首次渲染即为正确主题
  applyTheme(themes[currentTheme.value] || themes.default)

  return { currentTheme, themes, setTheme }
})
