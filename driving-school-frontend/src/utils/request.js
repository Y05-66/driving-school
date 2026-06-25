/**
 * @description HTTP 请求封装模块
 * 基于 axios 创建统一的请求实例，包含：
 * 1. 请求拦截器：自动在请求头中携带 JWT Token
 * 2. 响应拦截器：统一处理业务错误码、401 鉴权失败自动刷新Token或登出
 * 3. 请求失败时通过 Element Plus 的 ElMessage 展示错误提示
 */

import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import router from '../router'

/** @description 标记是否正在刷新 Token，防止并发重复刷新 */
let isRefreshing = false
/** @description 等待 Token 刷新完成的请求队列 */
let failedQueue = []

/**
 * @description 处理等待队列中的请求
 * Token 刷新成功后，用新 Token 重试队列中的所有请求
 * Token 刷新失败后，reject 队列中的所有请求
 * @param {string|null} error - 刷新失败时的错误信息，成功时为 null
 * @param {string} token - 刷新成功后的新 Token
 */
const processQueue = (error, token = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error)
    } else {
      resolve(token)
    }
  })
  failedQueue = []
}

/**
 * @description 创建 axios 请求实例
 * 所有业务接口统一使用此实例发送请求
 * - baseURL: API 基础路径，开发环境由 Vite 代理转发
 * - timeout: 请求超时时间 15 秒
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

/**
 * @description 请求拦截器
 * 在每个请求发出前，检查用户是否已登录（存在 token）
 * 如果已登录，则在请求头中添加 Authorization 字段，携带 Bearer Token
 */
request.interceptors.request.use(config => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers['Authorization'] = 'Bearer ' + userStore.token
  }
  return config
})

/**
 * @description 响应拦截器 - 成功响应处理
 * 后端返回的数据结构约定为 { code: 200, data: ..., message: ... }
 * - 当 code !== 200 时视为业务错误
 * - 当 code === 401 时，尝试使用 refreshToken 刷新，刷新失败则登出
 */
request.interceptors.response.use(
  response => {
    const res = response.data
    // Blob 响应（文件下载）需要检查是否为错误响应
    if (res instanceof Blob) {
      // 如果 Blob 的 content-type 是 JSON，说明是错误响应而非文件下载
      if (res.type && res.type.includes('application/json')) {
        // 读取 Blob 内容并解析错误信息
        return res.text().then(text => {
          try {
            const json = JSON.parse(text)
            if (json.code !== 200) {
              ElMessage.error(json.message || '下载失败')
              return Promise.reject(new Error(json.message))
            }
          } catch (e) {
            // JSON 解析失败，说明服务器返回了非标准格式的错误
            ElMessage.error('服务器响应异常')
            return Promise.reject(new Error('服务器响应异常'))
          }
          return res
        })
      }
      return res
    }
    // 业务状态码非 200，视为请求失败
    if (res.code !== 200) {
      // 401 状态码表示 Token 过期或无效，尝试刷新
      if (res.code === 401) {
        return handle401(response.config)
      }
      // 其他业务错误，弹出后端返回的错误信息
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    // 业务成功，返回数据体
    return res
  },
  /**
   * @description 响应拦截器 - 网络/HTTP 错误处理
   * HTTP 401 同样尝试刷新 Token
   */
  error => {
    if (error.response?.status === 401) {
      return handle401(error.config)
    }
    const msg = error.response?.data?.message || error.message || '网络错误'
    // 过滤请求取消的情况，避免误提示
    if (msg !== 'cancel') {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

/**
 * @description 处理 401 错误，尝试使用 refreshToken 刷新
 * 如果正在刷新中，将当前请求加入等待队列
 * 如果刷新成功，用新 Token 重试原请求
 * 如果刷新失败或没有 refreshToken，执行登出
 * @param {Object} originalConfig - 原始请求配置
 * @returns {Promise} 重试后的请求结果
 */
function handle401(originalConfig) {
  const userStore = useUserStore()

  // 如果已经重试过仍然 401，直接登出（防止无限循环）
  if (originalConfig._retry) {
    handleLogout()
    return Promise.reject(new Error('Token 刷新失败'))
  }

  // 如果没有 refreshToken，直接登出
  if (!userStore.refreshToken) {
    handleLogout()
    return Promise.reject(new Error('登录已过期'))
  }

  // 如果正在刷新中，将请求加入等待队列
  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      failedQueue.push({ resolve, reject })
    }).then(token => {
      originalConfig.headers['Authorization'] = 'Bearer ' + token
      return request(originalConfig)
    })
  }

  // 开始刷新 Token
  isRefreshing = true
  originalConfig._retry = true

  return new Promise((resolve, reject) => {
    axios.post('/api/auth/refresh', { refreshToken: userStore.refreshToken })
      .then(res => {
        if (res.data?.code === 200) {
          const { accessToken, refreshToken: newRefreshToken } = res.data.data
          // 同步更新 Pinia store 和 localStorage（请求拦截器读的是 store 中的 token）
          userStore.token = accessToken
          userStore.refreshToken = newRefreshToken
          localStorage.setItem('token', accessToken)
          localStorage.setItem('refreshToken', newRefreshToken)
          // 处理等待队列
          processQueue(null, accessToken)
          // 修复：isRefreshing 必须在 .then()/.catch() 中重置，不能放到 .finally() 中
          // 原因：如果放在 .finally()，会在 resolve(request(originalConfig)) 之后才执行，
          // 导致重试请求发出时 isRefreshing 仍为 true，新的 401 响应会再次进入队列而非触发刷新
          isRefreshing = false
          // 重试原始请求
          originalConfig.headers['Authorization'] = 'Bearer ' + accessToken
          resolve(request(originalConfig))
        } else {
          isRefreshing = false
          processQueue(new Error('刷新失败'))
          handleLogout()
          reject(new Error('刷新Token失败'))
        }
      })
      .catch(err => {
        processQueue(err)
        isRefreshing = false
        handleLogout()
        reject(err)
      })
  })
}

/**
 * @description 处理用户登出逻辑
 * 当 Token 失效且刷新失败时调用，清除本地存储的用户信息并跳转到登录页
 */
function handleLogout() {
  const userStore = useUserStore()
  // 仅在用户确实已登录的情况下执行登出，避免重复操作
  if (userStore.token) {
    userStore.logout()
    router.push('/login')
    ElMessage.warning('登录已过期，请重新登录')
  }
}

/** @description 导出请求实例，供 api 模块统一调用 */
export default request

/**
 * @description 导出便捷方法，供页面直接调用
 * 页面可通过 import { get, post, put } from '../../utils/request' 使用
 */
export const get = (url, config) => request.get(url, config)
export const post = (url, data, config) => request.post(url, data, config)
export const put = (url, data, config) => request.put(url, data, config)
