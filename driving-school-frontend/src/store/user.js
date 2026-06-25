/**
 * @description 用户状态管理模块 (Pinia Store)
 * 使用 Vue 3 Composition API 风格定义，管理用户的登录状态信息：
 * 1. token / refreshToken: 用户认证令牌，持久化存储在 localStorage
 * 2. userInfo: 用户基本信息（角色、姓名等），登录后从后端获取
 * 3. login / logout: 登录与登出操作
 * 4. fetchUserInfo: 获取当前登录用户详细信息
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '../utils/request'

/**
 * @description 定义用户状态 Store
 * Store ID 为 'user'，通过 useUserStore() 在任意组件中调用
 */
export const useUserStore = defineStore('user', () => {
  /**
   * @description 用户访问令牌 (Access Token)
   * 登录成功后由后端返回，用于后续请求的身份验证
   * 初始化时从 localStorage 读取，保持页面刷新后仍处于登录态
   */
  const token = ref(localStorage.getItem('token') || '')

  /**
   * @description 刷新令牌 (Refresh Token)
   * 当 Access Token 过期时，可用于获取新的 Access Token
   * 同样持久化存储在 localStorage
   */
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')

  /**
   * @description 用户信息对象
   * 包含 username、realName、role、phone 等字段
   * 初始化时尝试从 localStorage 解析，解析失败则使用空对象
   */
  let parsedInfo = {}
  try {
    parsedInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch { parsedInfo = {} }
  const userInfo = ref(parsedInfo)

  /**
   * @description 用户登录
   * @param {string} username - 用户名
   * @param {string} password - 密码
   * @returns {Promise} 登录结果
   * 流程：发送登录请求 -> 保存 Token -> 获取用户信息 -> 返回结果
   */
  async function login(username, password) {
    const res = await request.post('/auth/login', { username, password })
    // 保存 Access Token 和 Refresh Token 到响应式变量和 localStorage
    token.value = res.data.accessToken
    refreshToken.value = res.data.refreshToken
    localStorage.setItem('token', res.data.accessToken)
    localStorage.setItem('refreshToken', res.data.refreshToken)
    // 登录成功后立即获取用户详细信息（角色、权限等）
    await fetchUserInfo()
    return res
  }

  /**
   * @description 获取当前登录用户信息
   * 登录后或页面刷新时调用，从后端获取用户详细资料
   * @returns {Promise<object>} 用户信息对象
   */
  async function fetchUserInfo() {
    const res = await request.get('/auth/info')
    userInfo.value = res.data
    // 持久化存储用户信息，避免每次刷新都请求后端
    localStorage.setItem('userInfo', JSON.stringify(res.data))
    return res.data
  }

  /**
   * @description 用户登出
   * 清除所有本地存储的认证信息，并通知后端注销会话
   * 后端注销失败不影响前端登出流程（使用 catch 静默处理）
   */
  function logout() {
    // 通知后端注销（允许失败）
    request.post('/auth/logout').catch(() => {})
    // 清除内存中的状态
    token.value = ''
    refreshToken.value = ''
    userInfo.value = {}
    // 清除 localStorage 持久化数据
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
  }

  /**
   * @description 更新用户信息（部分更新）
   * 合并传入的字段到现有 userInfo，并同步到 localStorage
   * @param {object} updates - 需要更新的字段对象
   */
  function updateUserInfo(updates) {
    userInfo.value = { ...userInfo.value, ...updates }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  // 导出状态和方法，供组件使用
  return { token, refreshToken, userInfo, login, fetchUserInfo, logout, updateUserInfo }
})
