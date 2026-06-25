/**
 * HTTP Request Module
 * Handles request/response interceptors, JWT auth, error display, 401 redirect
 */

import { BASE_URL, TIMEOUT } from './config'

let isRedirecting = false

/**
 * Unified request function
 * @param {Object} options - Request config
 * @returns {Promise} Response data
 */
const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      timeout: TIMEOUT,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': 'Bearer ' + token } : {})
      },
      success: (res) => {
        if (res.statusCode === 200) {
          if (!res.data || typeof res.data !== 'object') {
            reject(new Error('服务器响应格式错误'))
            return
          }
          if (res.data.code === 200) {
            resolve(res.data)
          } else if (res.data.code === 401) {
            handleUnauthorized()
            reject(new Error(res.data.message))
          } else {
            uni.showToast({ title: res.data.message || '请求失败', icon: 'none' })
            reject(new Error(res.data.message))
          }
        } else if (res.statusCode === 401) {
          handleUnauthorized()
          reject(new Error('登录已过期'))
        } else if (res.statusCode === 403) {
          uni.showToast({ title: '无权限访问', icon: 'none' })
          reject(new Error('无权限访问'))
        } else if (res.statusCode === 404) {
          uni.showToast({ title: '请求的资源不存在', icon: 'none' })
          reject(new Error('请求的资源不存在'))
        } else if (res.statusCode === 500) {
          uni.showToast({ title: '服务器错误，请稍后重试', icon: 'none' })
          reject(new Error('服务器错误'))
        } else {
          uni.showToast({ title: '请求失败（' + res.statusCode + '）', icon: 'none' })
          reject(new Error('网络错误'))
        }
      },
      // fail 回调：网络异常或超时时触发（success 回调不会执行），需手动 reject 以终止 Promise
      fail: (err) => {
        uni.showToast({ title: '网络连接失败，请检查网络', icon: 'none' })
        reject(err)
      }
    })
  })
}

/**
 * Handle 401 unauthorized
 */
function handleUnauthorized() {
  if (isRedirecting) return
  isRedirecting = true
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
  uni.reLaunch({
    url: '/pages/login/login',
    complete: () => { isRedirecting = false },
    fail: () => { isRedirecting = false }
  })
  // 安全超时：防止 complete/fail 回调在极端情况下不触发，导致 isRedirecting 永远为 true
  setTimeout(() => { isRedirecting = false }, 5000)
}

// Shortcuts
export const get = (url, data) => request({ url, method: 'GET', data })
export const post = (url, data) => request({ url, method: 'POST', data })
export const put = (url, data) => request({ url, method: 'PUT', data })
export const del = (url, data) => request({ url, method: 'DELETE', data })

export default request
