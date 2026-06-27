<!--
  @description AI 助手浮动聊天面板
  收起状态：右侧显示一小条触发区（图标 + 文字）
  展开状态：右侧显示完整聊天窗口，支持模型选择
-->

<template>
  <!-- 收起状态：右侧触发条 -->
  <div v-if="!expanded" class="ai-trigger" @click="expanded = true">
    <el-icon size="20"><ChatDotRound /></el-icon>
    <span class="trigger-text">AI 助手</span>
  </div>

  <!-- 展开状态：完整聊天面板 -->
  <div v-else class="ai-panel">
    <!-- 面板头部 -->
    <div class="panel-header">
      <div class="panel-title">
        <el-icon size="18" color="#10b981"><ChatDotRound /></el-icon>
        <span>AI 助手</span>
      </div>
      <div class="panel-actions">
        <el-icon class="action-btn" title="新对话" @click="newConversation"><Plus /></el-icon>
        <el-icon class="action-btn" title="关闭" @click="expanded = false"><Close /></el-icon>
      </div>
    </div>

    <!-- 模型选择 + 历史会话 -->
    <div class="toolbar">
      <el-select
        v-model="selectedModel"
        size="small"
        class="model-select"
        filterable
        allow-create
        placeholder="选择或输入模型"
        @change="onModelChange"
      >
        <el-option-group
          v-for="(group, groupName) in groupedModels"
          :key="groupName"
          :label="groupName"
        >
          <el-option
            v-for="m in group"
            :key="m.id"
            :label="m.name"
            :value="m.id"
          >
            <div class="model-option">
              <span class="model-name">{{ m.name }}</span>
              <span class="model-desc">{{ m.description }}</span>
            </div>
          </el-option>
        </el-option-group>
      </el-select>
      <el-dropdown v-if="conversations.length > 0" trigger="click" @command="handleConvCommand" popper-class="conv-dropdown-popper">
        <el-button size="small" text>
          <el-icon size="14"><ChatDotRound /></el-icon>
          <span style="margin-left:4px;font-size:12px">历史</span>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-for="conv in conversations.slice(0, 10)"
              :key="conv.conversationId"
              :command="'switch:' + conv.conversationId"
              divided
            >
              <div class="conv-item">
                <span class="conv-text">{{ conv.lastMessage || '新对话' }}</span>
                <el-icon class="conv-delete" @click.prevent.stop="handleConvCommand('delete:' + conv.conversationId)"><Delete /></el-icon>
              </div>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 消息列表 -->
    <el-scrollbar class="message-list" ref="scrollbarRef">
      <!-- 欢迎 -->
      <div v-if="messages.length === 0" class="welcome">
        <div class="welcome-icon">🚗</div>
        <div class="welcome-title">驾校智能助手</div>
        <div class="welcome-desc">解答驾校相关问题</div>
        <div class="suggestions">
          <div
            v-for="item in suggestions"
            :key="item"
            class="suggestion-item"
            @click="sendMessage(item)"
          >{{ item }}</div>
        </div>
      </div>

      <!-- 消息 -->
      <div v-for="(msg, index) in messages" :key="index" class="message" :class="msg.role">
        <div class="msg-avatar" :class="msg.role">
          {{ msg.role === 'user' ? avatarText : 'AI' }}
        </div>
        <div class="msg-body">
          <div class="msg-bubble" :class="msg.role">
            <!-- AI 消息：有内容显示 Markdown，无内容显示打字动画 -->
            <div v-if="msg.role === 'assistant' && msg.content" class="md" v-html="renderMd(msg.content)"></div>
            <div v-else-if="msg.role === 'assistant'" class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
            <!-- 用户消息 -->
            <div v-else>{{ msg.content }}</div>
          </div>
          <div class="msg-time" :class="msg.role">
            <span>{{ msg.time || '' }}</span>
            <span v-if="msg.model" class="msg-model">{{ msg.model }}</span>
          </div>
        </div>
      </div>

    </el-scrollbar>

    <!-- 输入区 -->
    <div class="input-area">
      <el-input
        v-model="inputMessage"
        type="textarea"
        :rows="1"
        :autosize="{ minRows: 1, maxRows: 3 }"
        placeholder="输入问题..."
        resize="none"
        @keydown.enter.exact.prevent="handleEnter"
        :disabled="loading"
        ref="inputRef"
      />
      <el-button
        type="primary"
        :icon="Promotion"
        circle
        :loading="loading"
        :disabled="!inputMessage.trim() || loading"
        @click="sendMessage()"
        class="send-btn"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, computed, onUnmounted } from 'vue'
import { useUserStore } from '../store/user'
import { getAiConversations, getAiConversationHistory, getAiModels, deleteAiConversation } from '../api'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Plus, Close, Promotion, Delete } from '@element-plus/icons-vue'

const userStore = useUserStore()

// ====== 状态变量 ======

/** 面板是否展开 */
const expanded = ref(false)
/** 消息列表，每条消息包含 { role, content, time, model } */
const messages = ref([])
/** 输入框内容 */
const inputMessage = ref('')
/** 是否正在等待AI响应 */
const loading = ref(false)
/** 当前会话ID，为空表示新对话 */
const currentConversationId = ref('')
/** 历史会话列表 */
const conversations = ref([])
/** 消息列表滚动条引用 */
const scrollbarRef = ref(null)
/** 输入框引用 */
const inputRef = ref(null)
/** 用于取消进行中的 fetch 请求 */
let abortController = null

/** 可用模型列表，从后端API加载 */
const models = ref([])
/** 当前选中的模型ID，持久化到 localStorage */
const selectedModel = ref(localStorage.getItem('ai-model') || 'qwen-plus')

/** 用户头像显示文字（姓名首字母） */
const avatarText = computed(() => {
  const name = userStore.userInfo.realName || userStore.userInfo.username || '?'
  return name.charAt(0)
})

/** 按 group 字段对模型分组，用于 el-option-group 展示 */
const groupedModels = computed(() => {
  const groups = {}
  for (const m of models.value) {
    const g = m.group || '其他'
    if (!groups[g]) groups[g] = []
    groups[g].push(m)
  }
  return groups
})

/** 预设的建议问题，用户可点击快速发送 */
const suggestions = [
  '科目一考试有哪些技巧？',
  '科目二倒车入库怎么看点位？',
  '学车报名需要什么条件？'
]

/** 展开面板时加载会话列表和模型列表，聚焦输入框 */
watch(expanded, async (val) => {
  if (val) {
    await Promise.all([loadConversations(), loadModels()])
    nextTick(() => inputRef.value?.focus())
  }
})

/** 组件卸载时取消进行中的请求，防止内存泄漏 */
onUnmounted(() => {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
})

/** @description 加载可用模型列表，确保当前选中模型在列表中 */
const loadModels = async () => {
  try {
    const res = await getAiModels()
    models.value = res.data || []
    // 确保 selectedModel 在列表中
    if (models.value.length && !models.value.find(m => m.id === selectedModel.value)) {
      selectedModel.value = models.value[0].id
    }
  } catch {}
}

/** @description 模型选择变更时持久化到 localStorage */
const onModelChange = (val) => {
  localStorage.setItem('ai-model', val)
}

/** @description 加载用户的会话列表 */
const loadConversations = async () => {
  try {
    const res = await getAiConversations()
    conversations.value = res.data || []
  } catch {}
}

/** @description 新建对话，清空消息列表和会话ID */
const newConversation = () => {
  currentConversationId.value = ''
  messages.value = []
  nextTick(() => inputRef.value?.focus())
}

/**
 * @description 处理会话下拉命令（切换 / 删除）
 * @param {string} command - 格式 "switch:<id>" 或 "delete:<id>"
 */
const handleConvCommand = async (command) => {
  const idx = command.indexOf(':')
  const action = command.substring(0, idx)
  const conversationId = command.substring(idx + 1)
  if (action === 'switch') {
    if (loading.value) return
    currentConversationId.value = conversationId
    try {
      const res = await getAiConversationHistory(conversationId)
      messages.value = (res.data || []).map(item => ({
        role: item.role,
        content: item.content,
        time: fmtMsgTime(item.createTime)
      }))
      scrollToBottom()
    } catch {}
  } else if (action === 'delete') {
    try {
      await deleteAiConversation(conversationId)
      ElMessage.success('会话已删除')
      if (currentConversationId.value === conversationId) {
        currentConversationId.value = ''
        messages.value = []
      }
      await loadConversations()
    } catch {}
  }
}

/** @description 键盘事件：Enter发送，Shift+Enter换行 */
const handleEnter = (e) => {
  if (e.shiftKey) return
  sendMessage()
}

/**
 * @description 发送消息并接收SSE流式响应
 * 流程：添加用户消息 → 发起fetch请求 → 逐字接收AI回复 → 处理model-switch/done/error事件
 * @param {string} [text] - 消息内容，为空时取输入框内容（用于点击建议问题）
 */
const sendMessage = async (text) => {
  const content = text || inputMessage.value.trim()
  if (!content || loading.value) return

  messages.value.push({ role: 'user', content, time: fmtMsgTime(new Date().toISOString()) })
  inputMessage.value = ''
  loading.value = true

  const aiIdx = messages.value.length
  messages.value.push({ role: 'assistant', content: '', time: '', model: selectedModel.value })

  try {
    // 取消之前的请求
    if (abortController) abortController.abort()
    abortController = new AbortController()

    const token = userStore.token
    // 使用与 axios 相同的 baseURL，保持一致性
    const response = await fetch('/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        message: content,
        conversationId: currentConversationId.value || undefined,
        model: selectedModel.value
      }),
      signal: abortController.signal
    })

    if (!response.ok) throw new Error(`HTTP ${response.status}`)

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let evtType = 'message'

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const t = line.trim()
        // 空行 = 事件边界，重置事件类型
        if (!t) { evtType = 'message'; continue }
        if (t.startsWith('event:')) { evtType = t.slice(6).trim(); continue }
        if (t.startsWith('data:')) {
          const data = t.slice(5).trim()
          if (evtType === 'message') {
            if (data) { messages.value[aiIdx].content += data; scrollToBottom() }
          } else if (evtType === 'reset') {
            // 模型降级时清除之前模型的残余内容
            messages.value[aiIdx].content = ''
          } else if (evtType === 'model-switch') {
            const switchedModel = models.value.find(m => m.id === data)
            const modelName = switchedModel ? switchedModel.name : data
            ElMessage.warning({ message: `模型额度不足，已自动切换到 ${modelName}`, duration: 3000 })
            messages.value[aiIdx].model = data
          } else if (evtType === 'done') {
            currentConversationId.value = data
            messages.value[aiIdx].time = fmtMsgTime(new Date().toISOString())
          } else if (evtType === 'error') {
            messages.value[aiIdx].content = data || 'AI服务调用失败'
          }
        }
      }
    }
    await loadConversations()
  } catch (e) {
    console.error('AI对话失败:', e)
    messages.value[aiIdx].content = 'AI服务暂时不可用，请稍后重试。'
    messages.value[aiIdx].time = fmtMsgTime(new Date().toISOString())
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

/** @description 滚动消息列表到底部，使用实际滚动高度而非硬编码值 */
const scrollToBottom = () => {
  nextTick(() => {
    if (scrollbarRef.value) {
      const wrap = scrollbarRef.value.wrapRef
      if (wrap) scrollbarRef.value.setScrollTop(wrap.scrollHeight)
    }
  })
}

/**
 * @description 格式化ISO时间字符串为 HH:mm 格式
 * @param {string} iso - ISO 8601 时间字符串
 * @returns {string} 格式化后的时间，如 "14:30"
 */
const fmtMsgTime = (iso) => {
  if (!iso) return ''
  const d = new Date(iso)
  return String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0')
}

/**
 * @description 将Markdown文本转为安全的HTML（先转义防XSS，再做Markdown转换）
 * 支持：**加粗**、`行内代码`、换行
 * @param {string} text - Markdown文本
 * @returns {string} 安全的HTML字符串
 */
const renderMd = (text) => {
  if (!text) return ''
  // 先转义 HTML，防止 XSS
  let s = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
  // Markdown 转换
  s = s
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
  return s
}
</script>

<style scoped>
/* ===== 收起触发条 ===== */
.ai-trigger {
  position: fixed;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  background: linear-gradient(135deg, #6366f1, #818cf8);
  color: #fff;
  border-radius: 12px 0 0 12px;
  padding: 16px 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  box-shadow: -2px 0 12px rgba(99, 102, 241, 0.3);
  transition: width 0.3s ease;
  z-index: 1000;
}

.ai-trigger:hover {
  width: 52px;
}

.trigger-text {
  writing-mode: vertical-rl;
  font-size: 12px;
  letter-spacing: 2px;
  font-weight: 600;
}

/* ===== 展开聊天面板 ===== */
.ai-panel {
  position: fixed;
  right: 0;
  top: 0;
  bottom: 0;
  width: 400px;
  background: #fff;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  z-index: 1000;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

/* 面板头部 */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #e5e7eb;
  background: linear-gradient(135deg, #f8fafc, #f1f5f9);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.panel-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  font-size: 16px;
  color: #64748b;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #e2e8f0;
  color: #1e293b;
}

/* 工具栏：模型选择 + 历史 */
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-bottom: 1px solid #e5e7eb;
}

.model-select {
  flex: 1;
}

.model-select :deep(.el-input__inner) {
  font-size: 12px;
}

.model-option {
  display: flex;
  flex-direction: column;
}

.model-name {
  font-size: 13px;
  font-weight: 500;
}

.model-desc {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

/* 消息列表 */
.message-list {
  flex: 1;
  padding: 16px;
}

/* 欢迎 */
.welcome {
  text-align: center;
  padding: 40px 16px 20px;
}

.welcome-icon {
  font-size: 36px;
  margin-bottom: 12px;
}

.welcome-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
}

.welcome-desc {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 20px;
}

.suggestions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.suggestion-item {
  padding: 10px 14px;
  background: #eef2ff;
  color: #6366f1;
  border-radius: 10px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
}

.suggestion-item:hover {
  background: #6366f1;
  color: #fff;
}

/* 消息 */
.message {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.message.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  flex-shrink: 0;
}

.msg-avatar.user {
  background: #6366f1;
}

.msg-avatar.assistant {
  background: #10b981;
}

.msg-body {
  max-width: 75%;
}

.msg-bubble {
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}

.msg-bubble.user {
  background: #6366f1;
  color: #fff;
  border-top-right-radius: 4px;
}

.msg-bubble.assistant {
  background: #f5f7fa;
  color: #1e293b;
  border-top-left-radius: 4px;
}

.msg-time {
  font-size: 10px;
  color: #94a3b8;
  margin-top: 3px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.msg-time.user {
  justify-content: flex-end;
}

.msg-model {
  background: #e2e8f0;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 9px;
  color: #64748b;
}

.md :deep(strong) {
  font-weight: 600;
}

.md :deep(code) {
  background: rgba(0, 0, 0, 0.06);
  padding: 1px 5px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
}

/* 加载动画 */
.loading-bubble,
.typing-indicator {
  display: flex;
  gap: 5px;
  padding: 12px 16px;
}

.dot {
  width: 7px;
  height: 7px;
  background: #94a3b8;
  border-radius: 50%;
  animation: bounce 1.4s infinite;
}

.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-5px); opacity: 1; }
}

/* 历史会话下拉 — popper 渲染到 body，需用 :global 穿透 scoped */
:global(.conv-dropdown-popper) {
  max-width: 240px;
  min-width: 160px;
}
:global(.conv-dropdown-popper .el-dropdown-menu__item) {
  max-width: 240px;
  overflow: hidden;
}

.conv-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 8px;
}

.conv-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
}

.conv-delete {
  color: #94a3b8;
  cursor: pointer;
  flex-shrink: 0;
  transition: color 0.2s;
}

.conv-delete:hover {
  color: #ef4444;
}

/* 输入区 */
.input-area {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid #e5e7eb;
}

.input-area :deep(.el-textarea__inner) {
  border-radius: 10px;
  padding: 10px 14px;
  resize: none;
  font-size: 13px;
}

.send-btn {
  flex-shrink: 0;
  width: 38px;
  height: 38px;
}
</style>
