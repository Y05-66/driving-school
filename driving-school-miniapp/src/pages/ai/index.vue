<!--
  AI 助手页面（小程序端）
  基于阿里云百练平台的智能问答助手
  支持历史对话续聊
-->
<template>
  <view class="page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <text class="back-icon">←</text>
        </view>
        <text class="nav-title">AI 助手</text>
        <view class="nav-right" @click="showHistoryPanel = true">
          <text class="history-text">历史</text>
        </view>
      </view>
    </view>

    <!-- 历史会话面板 -->
    <view v-if="showHistoryPanel" class="history-mask" @click.self="showHistoryPanel = false">
      <view class="history-panel">
        <view class="history-header">
          <text class="history-title">历史对话</text>
          <view class="history-close" @click="showHistoryPanel = false">
            <text class="close-icon">✕</text>
          </view>
        </view>
        <scroll-view scroll-y class="history-scroll">
          <view v-if="conversations.length === 0" class="history-empty">
            <text class="history-empty-text">暂无历史对话</text>
          </view>
          <view
            v-for="conv in conversations"
            :key="conv.conversationId"
            class="history-item"
          >
            <view class="history-item-main" @click="loadConversation(conv.conversationId)">
              <text class="history-item-text">{{ conv.lastMessage || '新对话' }}</text>
              <text class="history-item-time">{{ formatTime(conv.latestTime) }}</text>
            </view>
            <view class="history-item-delete" @click.stop="deleteConversation(conv.conversationId)">
              <text class="delete-icon">✕</text>
            </view>
          </view>
        </scroll-view>
        <view class="history-footer">
          <view class="new-chat-btn" @click="newConversation">
            <text class="new-chat-text">+ 新对话</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 消息列表 -->
    <scroll-view
      scroll-y
      class="chat-scroll"
      :style="{ paddingTop: (statusBarHeight + 44) + 'px' }"
      :scroll-into-view="scrollToId"
      scroll-with-animation
    >
      <!-- 欢迎消息 -->
      <view v-if="messages.length === 0" class="welcome">
        <text class="welcome-icon">🚗</text>
        <text class="welcome-title">驾校智能助手</text>
        <text class="welcome-desc">我可以帮您解答驾校相关问题</text>
        <view class="suggestions">
          <view
            v-for="item in suggestions"
            :key="item"
            class="suggestion-item"
            @click="sendMessage(item)"
          >
            <text class="suggestion-text">{{ item }}</text>
          </view>
        </view>
      </view>

      <!-- 消息列表 -->
      <view
        v-for="(msg, index) in messages"
        :key="index"
        :id="'msg-' + index"
        class="message"
        :class="msg.role"
      >
        <view class="avatar" :class="msg.role">
          <text class="avatar-text">{{ msg.role === 'user' ? '我' : 'AI' }}</text>
        </view>
        <view class="bubble-wrap">
          <view class="bubble" :class="msg.role">
            <text class="bubble-text" :class="msg.role">{{ msg.content }}</text>
          </view>
          <text v-if="msg.time" class="msg-time" :class="msg.role">{{ msg.time }}</text>
        </view>
      </view>

      <!-- 加载中 -->
      <view v-if="loading" class="message assistant">
        <view class="avatar assistant">
          <text class="avatar-text">AI</text>
        </view>
        <view class="bubble assistant loading-bubble">
          <view class="typing-dots">
            <view class="dot"></view>
            <view class="dot"></view>
            <view class="dot"></view>
          </view>
        </view>
      </view>

      <view style="height: 30rpx;"></view>
    </scroll-view>

    <!-- 输入区域 -->
    <view class="input-area">
      <!-- 模型选择 -->
      <scroll-view scroll-x class="model-bar" :show-scrollbar="false">
        <view class="model-tags">
          <view
            v-for="m in models"
            :key="m.id"
            class="model-tag"
            :class="{ active: selectedModel === m.id }"
            @click="selectModel(m.id)"
          >
            <text class="model-tag-text">{{ m.name }}</text>
          </view>
          <view class="model-tag custom" @click="showCustomInput = true">
            <text class="model-tag-text">+自定义</text>
          </view>
        </view>
      </scroll-view>
      <!-- 自定义模型输入弹窗 -->
      <view v-if="showCustomInput" class="custom-mask" @click.self="showCustomInput = false">
        <view class="custom-box">
          <text class="custom-title">输入模型ID</text>
          <input
            v-model="customModelId"
            class="custom-input"
            placeholder="例如 qwen2.5-14b-instruct"
            @confirm="addCustomModel"
          />
          <view class="custom-btns">
            <view class="custom-btn cancel" @click="showCustomInput = false">
              <text>取消</text>
            </view>
            <view class="custom-btn confirm" @click="addCustomModel">
              <text>确定</text>
            </view>
          </view>
        </view>
      </view>
      <view class="input-row">
        <input
          v-model="inputMessage"
          class="input-box"
          placeholder="输入您的问题..."
          :disabled="loading"
          @confirm="sendMessage()"
          confirm-type="send"
        />
        <view
          class="send-btn"
          :class="{ disabled: !inputMessage.trim() || loading }"
          @click="sendMessage()"
        >
          <text class="send-text">发送</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '../../store/user'
import { get, post } from '../../utils/request'

const userStore = useUserStore()

// ========== 状态变量 ==========

/** 状态栏高度，用于自定义导航栏适配 */
const statusBarHeight = ref(0)
/** 消息列表，每条消息包含 { role, content, time } */
const messages = ref([])
/** 输入框内容 */
const inputMessage = ref('')
/** 是否正在等待AI响应 */
const loading = ref(false)
/** 当前会话ID，为空表示新对话 */
const conversationId = ref('')
/** 滚动定位目标元素ID */
const scrollToId = ref('')
/** 历史会话列表 */
const conversations = ref([])
/** 是否显示历史会话面板 */
const showHistoryPanel = ref(false)

/** 可用模型列表，初始为预设值，页面加载后从API合并更新 */
const models = ref([
  { id: 'qwen-plus', name: 'Plus' },
  { id: 'qwen-turbo', name: 'Turbo' },
  { id: 'qwen-max', name: 'Max' },
  { id: 'qwen-long', name: 'Long' },
  { id: 'qwen2.5-72b-instruct', name: '2.5-72B' },
  { id: 'qwen3-32b', name: '3-32B' },
  { id: 'qwen3-235b-a22b', name: '3-235B' },
  { id: 'deepseek-v3', name: 'DS-V3' },
  { id: 'deepseek-r1', name: 'DS-R1' }
])
/** 当前选中的模型ID，从本地存储恢复，默认 qwen-plus */
const selectedModel = ref(uni.getStorageSync('ai-model') || 'qwen-plus')
/** 是否显示自定义模型输入弹窗 */
const showCustomInput = ref(false)
/** 自定义模型输入框内容 */
const customModelId = ref('')

/**
 * @description 添加自定义模型到列表并选中
 * 如果模型ID已存在则不重复添加
 */
const addCustomModel = () => {
  const id = customModelId.value.trim()
  if (!id) return
  if (!models.value.find(m => m.id === id)) {
    models.value.push({ id, name: id })
  }
  selectModel(id)
  customModelId.value = ''
  showCustomInput.value = false
}

/**
 * @description 选择模型并持久化到本地存储
 * @param {string} modelId - 模型ID
 */
const selectModel = (modelId) => {
  selectedModel.value = modelId
  uni.setStorageSync('ai-model', modelId)
}

// ========== 建议问题 ==========

const suggestions = [
  '科目一考试有哪些技巧？',
  '科目二倒车入库怎么看点位？',
  '科目三路考需要注意什么？',
  '学车报名需要什么条件？'
]

// ========== 初始化 ==========

onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
})

onLoad((options) => {
  if (!userStore.token) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  // 如果传入了 conversationId，加载该会话历史
  if (options.conversationId) {
    loadConversation(options.conversationId)
  }
  // 加载会话列表和模型列表
  loadConversations()
  loadModels()
})

// ========== 会话管理 ==========

/** @description 加载用户的会话列表 */
const loadConversations = async () => {
  try {
    const res = await get('/ai/conversations')
    conversations.value = res.data || []
  } catch {}
}

/** @description 从API加载可用模型列表，与本地预设合并（保留用户自定义模型） */
const loadModels = async () => {
  try {
    const res = await get('/ai/models')
    if (res.data && res.data.length) {
      // 以后端列表为基础，保留用户自定义的模型
      const backendIds = new Set(res.data.map(m => m.id))
      const customModels = models.value.filter(m => !backendIds.has(m.id))
      models.value = [...res.data, ...customModels]
    }
  } catch {}
}

/**
 * @description 加载指定会话的聊天历史并切换到该会话
 * @param {string} convId - 会话ID
 */
const loadConversation = async (convId) => {
  showHistoryPanel.value = false
  conversationId.value = convId
  try {
    const res = await get('/ai/conversations/' + convId)
    messages.value = (res.data || []).map(item => ({
      role: item.role,
      content: item.content,
      time: formatMessageTime(item.createTime)
    }))
    scrollToBottom()
  } catch {
    uni.showToast({ title: '加载失败', icon: 'none' })
  }
}

/** @description 新建对话，清空消息列表和会话ID */
const newConversation = () => {
  showHistoryPanel.value = false
  conversationId.value = ''
  messages.value = []
}

/**
 * @description 删除指定历史会话，删除后刷新列表；若删除的是当前会话则清空消息
 * @param {string} convId - 会话ID
 */
const deleteConversation = async (convId) => {
  uni.showModal({
    title: '提示',
    content: '确定删除该对话？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        const token = uni.getStorageSync('token')
        await new Promise((resolve, reject) => {
          uni.request({
            url: require('../../utils/config.js').BASE_URL + '/ai/conversations/' + convId,
            method: 'DELETE',
            header: {
              'Authorization': token ? 'Bearer ' + token : ''
            },
            success: (r) => {
              if (r.statusCode === 200 && r.data.code === 200) {
                resolve()
              } else {
                reject(new Error(r.data?.message || '删除失败'))
              }
            },
            fail: (err) => reject(new Error(err.errMsg || '网络错误'))
          })
        })
        uni.showToast({ title: '已删除', icon: 'success' })
        if (conversationId.value === convId) {
          conversationId.value = ''
          messages.value = []
        }
        loadConversations()
      } catch (e) {
        uni.showToast({ title: '删除失败', icon: 'none' })
      }
    }
  })
}

// ========== 消息操作 ==========

/**
 * @description 发送消息并接收AI回复（同步接口）
 * 流程：添加用户消息 → 调用chat-sync接口 → 添加AI回复 → 处理模型降级提示
 * @param {string} [text] - 消息内容，为空时取输入框内容（用于点击建议问题）
 */
const sendMessage = async (text) => {
  const content = text || inputMessage.value.trim()
  if (!content || loading.value) return

  messages.value.push({
    role: 'user',
    content,
    time: formatMessageTime(new Date().toISOString())
  })
  inputMessage.value = ''
  loading.value = true
  scrollToBottom()

  try {
    // AI聊天使用较长超时（60秒），因为多模型降级可能需要更长时间
    const token = uni.getStorageSync('token')
    const res = await new Promise((resolve, reject) => {
      uni.request({
        url: require('../../utils/config.js').BASE_URL + '/ai/chat-sync',
        method: 'POST',
        header: {
          'Content-Type': 'application/json',
          'Authorization': token ? 'Bearer ' + token : ''
        },
        data: {
          message: content,
          conversationId: conversationId.value || undefined,
          model: selectedModel.value
        },
        timeout: 60000,
        success: (r) => {
          if (r.statusCode === 200 && r.data.code === 200) {
            resolve(r.data)
          } else {
            reject(new Error(r.data?.message || '请求失败'))
          }
        },
        fail: (err) => reject(new Error(err.errMsg || '网络错误'))
      })
    })

    messages.value.push({
      role: 'assistant',
      content: res.data?.reply || '',
      time: formatMessageTime(new Date().toISOString())
    })

    if (res.data?.conversationId) {
      conversationId.value = res.data.conversationId
    }

    // 模型降级提示
    if (res.data?.modelSwitched) {
      const m = models.value.find(item => item.id === res.data.modelSwitched)
      uni.showToast({ title: '额度不足，已切换到' + (m ? m.name : res.data.modelSwitched), icon: 'none', duration: 3000 })
    }

    // 刷新会话列表
    loadConversations()
    scrollToBottom()
  } catch (error) {
    console.error('AI对话失败:', error)
    messages.value.push({
      role: 'assistant',
      content: '抱歉，AI服务暂时不可用，请稍后重试。',
      time: formatMessageTime(new Date().toISOString())
    })
  } finally {
    loading.value = false
  }
}

// ========== 工具函数 ==========

/** @description 返回上一页 */
const goBack = () => {
  uni.navigateBack()
}

/** @description 滚动消息列表到底部，先清空scrollToId再设置以确保重复触发 */
const scrollToBottom = () => {
  nextTick(() => {
    const lastIndex = messages.value.length - 1
    if (lastIndex >= 0) {
      // 先清空再设置，确保 scroll-into-view 能重复触发
      scrollToId.value = ''
      nextTick(() => {
        scrollToId.value = 'msg-' + lastIndex
      })
    }
  })
}

/**
 * @description 格式化时间为相对时间（刚刚/N分钟前/N小时前/M月D日）
 * 兼容 "yyyy-MM-dd HH:mm:ss" 格式（无T分隔符）
 * @param {string} time - 时间字符串
 * @returns {string} 格式化后的相对时间
 */
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time.replace(' ', 'T'))
  if (isNaN(date.getTime())) return ''
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return (date.getMonth() + 1) + '/' + date.getDate()
}

/**
 * @description 格式化ISO时间字符串为 HH:mm 格式
 * @param {string} isoString - ISO 8601 时间字符串
 * @returns {string} 格式化后的时间，如 "14:30"
 */
const formatMessageTime = (isoString) => {
  if (!isoString) return ''
  const date = new Date(isoString)
  if (isNaN(date.getTime())) return ''
  const h = String(date.getHours()).padStart(2, '0')
  const m = String(date.getMinutes()).padStart(2, '0')
  return h + ':' + m
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

/* ===== 导航栏 ===== */
.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: linear-gradient(135deg, #4A7DFF, #6B9BFF);
}

.nav-content {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30rpx;
}

.nav-back, .nav-right {
  width: 80rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-icon {
  font-size: 36rpx;
  color: #fff;
}

.nav-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #fff;
}

.history-text {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
}

/* ===== 历史会话面板 ===== */
.history-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 200;
  display: flex;
  justify-content: flex-end;
}

.history-panel {
  width: 70%;
  height: 100%;
  background: #fff;
  display: flex;
  flex-direction: column;
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 30rpx;
  border-bottom: 1rpx solid #eee;
  padding-top: calc(20rpx + env(safe-area-inset-top));
}

.history-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1a1a2e;
}

.history-close {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-icon {
  font-size: 32rpx;
  color: #999;
}

.history-scroll {
  flex: 1;
}

.history-empty {
  padding: 80rpx 0;
  text-align: center;
}

.history-empty-text {
  font-size: 26rpx;
  color: #999;
}

.history-item {
  display: flex;
  align-items: center;
  border-bottom: 1rpx solid #f5f5f5;
}

.history-item-main {
  flex: 1;
  padding: 24rpx 30rpx;
  overflow: hidden;
}

.history-item-main:active {
  background: #f5f7fa;
}

.history-item-delete {
  padding: 24rpx 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.history-item-delete:active {
  background: #fee2e2;
}

.delete-icon {
  font-size: 28rpx;
  color: #999;
}

.history-item-text {
  font-size: 26rpx;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}

.history-item-time {
  font-size: 22rpx;
  color: #999;
  margin-top: 6rpx;
  display: block;
}

.history-footer {
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #eee;
}

.new-chat-btn {
  height: 72rpx;
  background: #4A7DFF;
  border-radius: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.new-chat-text {
  font-size: 28rpx;
  color: #fff;
  font-weight: 600;
}

/* ===== 消息列表 ===== */
.chat-scroll {
  flex: 1;
}

.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 40rpx 40rpx;
}

.welcome-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.welcome-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 12rpx;
}

.welcome-desc {
  font-size: 26rpx;
  color: #999;
  margin-bottom: 40rpx;
}

.suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  justify-content: center;
}

.suggestion-item {
  padding: 16rpx 24rpx;
  background: #E8F0FE;
  border-radius: 30rpx;
}

.suggestion-text {
  font-size: 24rpx;
  color: #4A7DFF;
}

.message {
  display: flex;
  gap: 16rpx;
  padding: 16rpx 30rpx;
  align-items: flex-start;
}

.message.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar.user {
  background: #4A7DFF;
}

.avatar.assistant {
  background: #10b981;
}

.avatar-text {
  font-size: 24rpx;
  color: #fff;
  font-weight: 600;
}

.bubble-wrap {
  max-width: 70%;
  display: flex;
  flex-direction: column;
}

.bubble {
  padding: 20rpx 28rpx;
  border-radius: 24rpx;
  word-break: break-all;
}

.bubble.user {
  background: #4A7DFF;
  border-top-right-radius: 8rpx;
}

.bubble.assistant {
  background: #fff;
  border-top-left-radius: 8rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.bubble-text {
  font-size: 28rpx;
  line-height: 1.6;
}

.bubble-text.user {
  color: #fff;
}

.bubble-text.assistant {
  color: #1a1a2e;
}

.msg-time {
  font-size: 20rpx;
  color: #bbb;
  margin-top: 6rpx;
}

.msg-time.user {
  text-align: right;
}

/* 加载动画 */
.loading-bubble {
  padding: 24rpx 32rpx;
}

.typing-dots {
  display: flex;
  gap: 10rpx;
}

.dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #999;
  animation: typing 1.4s infinite;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-8rpx); opacity: 1; }
}

/* ===== 输入区域 ===== */
.input-area {
  padding: 16rpx 30rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #eee;
}

.model-bar {
  margin-bottom: 12rpx;
  white-space: nowrap;
}

.model-tags {
  display: inline-flex;
  gap: 12rpx;
  padding: 4rpx 0;
}

.model-tag {
  display: inline-flex;
  padding: 6rpx 18rpx;
  background: #f5f7fa;
  border-radius: 20rpx;
  border: 2rpx solid transparent;
  flex-shrink: 0;
}

.model-tag.active {
  background: #E8F0FE;
  border-color: #4A7DFF;
}

.model-tag-text {
  font-size: 22rpx;
  color: #666;
}

.model-tag.active .model-tag-text {
  color: #4A7DFF;
  font-weight: 600;
}

.model-tag.custom {
  background: #fff;
  border: 2rpx dashed #ccc;
}

.model-tag.custom .model-tag-text {
  color: #999;
}

/* 自定义模型弹窗 */
.custom-mask {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 300;
}

.custom-box {
  width: 600rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx;
}

.custom-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1a1a2e;
  display: block;
  text-align: center;
  margin-bottom: 30rpx;
}

.custom-input {
  width: 100%;
  height: 80rpx;
  background: #f5f7fa;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  border: 2rpx solid #e8ecf1;
}

.custom-btns {
  display: flex;
  gap: 20rpx;
  margin-top: 30rpx;
}

.custom-btn {
  flex: 1;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx;
  font-size: 28rpx;
}

.custom-btn.cancel {
  background: #f1f5f9;
  color: #475569;
}

.custom-btn.confirm {
  background: #4A7DFF;
  color: #fff;
  font-weight: 600;
}

.input-row {
  display: flex;
  gap: 16rpx;
  align-items: center;
}

.input-box {
  flex: 1;
  height: 72rpx;
  background: #f5f7fa;
  border-radius: 36rpx;
  padding: 0 28rpx;
  font-size: 28rpx;
}

.send-btn {
  width: 120rpx;
  height: 72rpx;
  background: #4A7DFF;
  border-radius: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-btn.disabled {
  opacity: 0.5;
}

.send-text {
  font-size: 28rpx;
  color: #fff;
  font-weight: 600;
}
</style>
