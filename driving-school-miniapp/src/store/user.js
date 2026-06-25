/**
 * 用户状态管理模块
 * 功能：管理登录状态、Token、用户信息，提供登录/登出/获取用户信息等方法
 * 使用 Pinia 实现，数据持久化到小程序本地存储
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { get, post } from '../utils/request'

export const useUserStore = defineStore('user', () => {
  // ====== 状态 ======

  /** 访问令牌 */
  const token = ref(uni.getStorageSync('token') || '')

  /** 用户信息（从本地存储恢复，带异常保护） */
  let parsedInfo = {}
  try { parsedInfo = JSON.parse(uni.getStorageSync('userInfo') || '{}') } catch {}
  const userInfo = ref(parsedInfo)

  // ====== 方法 ======

  /**
   * 用户登录
   * @param {string} username - 用户名
   * @param {string} password - 密码
   * @returns {Promise} 登录结果
   * @description 1.调用登录接口获取Token 2.存储Token 3.获取用户信息
   */
  async function login(username, password) {
    // 调用后端登录接口
    const res = await post('/auth/login', { username, password })

    // 检查响应中是否包含 accessToken
    if (!res.data || !res.data.accessToken) {
      throw new Error('登录响应缺少令牌，请检查账号密码')
    }

    // 存储 Token 到本地
    token.value = res.data.accessToken
    uni.setStorageSync('token', res.data.accessToken)
    if (res.data.refreshToken) {
      uni.setStorageSync('refreshToken', res.data.refreshToken)
    }

    // 获取用户信息（失败则清除登录态）
    try {
      await fetchUserInfo()
    } catch {
      logout()
      throw new Error('获取用户信息失败')
    }
    return res
  }

  /**
   * 获取当前用户信息
   * @returns {Promise} 用户信息
   * @description 从后端获取最新用户信息并更新本地存储
   */
  async function fetchUserInfo() {
    const res = await get('/auth/info')
    userInfo.value = res.data
    uni.setStorageSync('userInfo', JSON.stringify(res.data))
    return res.data
  }

  /**
   * 退出登录
   * @description 通知后端登出（忽略结果），清除本地所有登录态
   */
  function logout() {
    // 通知后端（fire-and-forget，不等待结果）
    post('/auth/logout').catch(() => {})

    // 清除本地状态
    token.value = ''
    userInfo.value = {}

    // 清除本地存储
    uni.removeStorageSync('token')
    uni.removeStorageSync('refreshToken')
    uni.removeStorageSync('userInfo')
  }

  /**
   * 更新用户信息（部分更新）
   * @param {object} updates - 需要更新的字段
   * @description 合并传入的字段到现有 userInfo，并同步到本地存储
   */
  function updateUserInfo(updates) {
    userInfo.value = { ...userInfo.value, ...updates }
    uni.setStorageSync('userInfo', JSON.stringify(userInfo.value))
  }

  return { token, userInfo, login, fetchUserInfo, logout, updateUserInfo }
})
