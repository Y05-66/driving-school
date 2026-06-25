/**
 * API 接口封装模块
 * 提供需要特殊处理的接口（如文件上传）
 */
import { post } from './request'
import { BASE_URL } from './config'

/** 修改密码 */
export const changePassword = (data) => post('/auth/change-password', data)

/** 上传头像（文件上传需要特殊处理） */
export const uploadAvatar = (filePath) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    uni.uploadFile({
      url: BASE_URL + '/auth/avatar',
      filePath: filePath,
      name: 'file',
      // uni.uploadFile 不经过 request.js 拦截器，需手动携带 Authorization 头；token 为空时传空对象避免发送无效头
      header: token ? { 'Authorization': 'Bearer ' + token } : {},
      success: (res) => {
        if (res.statusCode === 200) {
          try {
            const data = JSON.parse(res.data)
            if (data.code === 200) {
              resolve(data)
            } else {
              uni.showToast({ title: data.message || '上传失败', icon: 'none' })
              reject(new Error(data.message))
            }
          } catch (e) {
            uni.showToast({ title: '服务器响应异常', icon: 'none' })
            reject(new Error('JSON解析失败'))
          }
        } else {
          uni.showToast({ title: '上传失败', icon: 'none' })
          reject(new Error('上传失败'))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}
