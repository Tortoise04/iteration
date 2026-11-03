<script setup lang="ts">
import { ref, nextTick, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api'
import { ElMessage } from 'element-plus'
import { Delete, Plus, ChatDotRound } from '@element-plus/icons-vue'

interface Message {
  role: string
  content: string
  timestamp?: string
  action?: { type: string; data?: any }
}

interface Session {
  sessionId: string
  title: string
  messageCount: number
  updatedAt: string | null
}

const router = useRouter()
const messages = ref<Message[]>([])
const sessions = ref<Session[]>([])
const currentSessionId = ref<string | null>(null)
const input = ref('')
const loading = ref(false)
const chatContainer = ref<HTMLElement>()

const quickPrompts = [
  '帮我安排明天的日程',
  '今天有什么任务？',
  '调整我的学习计划',
  '给我一些时间管理建议',
]

onMounted(async () => {
  await loadSessions()
  if (sessions.value.length > 0) {
    currentSessionId.value = sessions.value[0].sessionId
    await loadHistory()
  }
})

async function loadSessions() {
  try {
    const { data } = await api.get('/ai/sessions')
    sessions.value = data
  } catch (e) {
    // 静默处理
  }
}

async function loadHistory() {
  if (!currentSessionId.value) {
    messages.value = []
    return
  }
  try {
    const { data } = await api.get('/ai/history', { params: { sessionId: currentSessionId.value } })
    messages.value = data.map((m: any) => ({
      role: m.role,
      content: m.content,
    }))
    await nextTick()
    scrollToBottom()
  } catch (e) {
    messages.value = []
  }
}

async function selectSession(sessionId: string) {
  currentSessionId.value = sessionId
  await loadHistory()
}

async function newChat() {
  currentSessionId.value = null
  messages.value = []
  input.value = ''
}

async function sendMessage(text?: string) {
  const msg = (text || input.value).trim()
  if (!msg) return
  if (loading.value) return

  messages.value.push({ role: 'user', content: msg, timestamp: new Date().toLocaleTimeString() })
  input.value = ''

  await nextTick()
  scrollToBottom()

  loading.value = true
  try {
    const { data } = await api.post('/ai/chat', {
      message: msg,
      sessionId: currentSessionId.value || undefined,
    })

    // 保存返回的 sessionId（新会话时后端会生成）
    if (data.sessionId && !currentSessionId.value) {
      currentSessionId.value = data.sessionId
    }

    const msgObj: Message = {
      role: data.role,
      content: data.content,
      timestamp: new Date().toLocaleTimeString(),
    }
    if (data.action && data.action.type === 'SCHEDULE_GENERATED') {
      msgObj.action = data.action
    }
    messages.value.push(msgObj)

    // 刷新会话列表
    await loadSessions()
  } catch (e: any) {
    const errMsg = e.response?.data?.error || '抱歉，AI 服务暂时不可用。'
    messages.value.push({ role: 'assistant', content: errMsg, timestamp: new Date().toLocaleTimeString() })
    ElMessage.error('AI 调用失败')
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

async function deleteCurrentSession() {
  if (!currentSessionId.value) return
  try {
    await api.delete(`/ai/sessions/${currentSessionId.value}`)
    ElMessage.success('会话已删除')
    currentSessionId.value = null
    messages.value = []
    await loadSessions()
    if (sessions.value.length > 0) {
      currentSessionId.value = sessions.value[0].sessionId
      await loadHistory()
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.error || '删除失败')
  }
}

async function deleteSessionById(sessionId: string) {
  try {
    await api.delete(`/ai/sessions/${sessionId}`)
    ElMessage.success('会话已删除')
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = null
      messages.value = []
    }
    await loadSessions()
    if (!currentSessionId.value && sessions.value.length > 0) {
      currentSessionId.value = sessions.value[0].sessionId
      await loadHistory()
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.error || '删除失败')
  }
}

function scrollToBottom() {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

function goToDashboard(date?: string) {
  if (date) {
    router.push({ path: '/dashboard', query: { date } })
  } else {
    router.push('/dashboard')
  }
}

function formatTime(ts: string | null) {
  if (!ts) return ''
  const d = new Date(ts)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 86400000) return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}
</script>

<template>
  <div style="display: flex; gap: 0; height: calc(100vh - 140px);">
    <!-- 会话列表侧边栏 -->
    <div style="width: 240px; border-right: 1px solid #e2e8f0; display: flex; flex-direction: column; flex-shrink: 0;">
      <div style="padding: 12px;">
        <el-button type="primary" :icon="Plus" size="small" style="width: 100%;" @click="newChat">
          新建对话
        </el-button>
      </div>
      <div style="flex: 1; overflow-y: auto; padding: 0 8px;">
        <div
          v-for="s in sessions"
          :key="s.sessionId"
          @click="selectSession(s.sessionId)"
          :style="{
            padding: '10px 12px',
            marginBottom: '4px',
            borderRadius: '8px',
            cursor: 'pointer',
            background: currentSessionId === s.sessionId ? '#eff6ff' : 'transparent',
            border: currentSessionId === s.sessionId ? '1px solid #bfdbfe' : '1px solid transparent',
          }"
        >
          <div style="display: flex; align-items: flex-start; gap: 6px;">
            <el-icon style="margin-top: 2px; color: #94a3b8;" :size="14"><ChatDotRound /></el-icon>
            <div style="flex: 1; min-width: 0;">
              <div style="font-size: 0.85rem; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                {{ s.title }}
              </div>
              <div style="font-size: 0.7rem; color: #94a3b8; margin-top: 2px;">
                {{ s.messageCount }} 条消息 · {{ formatTime(s.updatedAt) }}
              </div>
            </div>
            <el-button
              text
              size="small"
              type="danger"
              :icon="Delete"
              @click.stop="deleteSessionById(s.sessionId)"
              style="flex-shrink: 0; opacity: 0.5;"
              title="删除此会话"
            />
          </div>
        </div>
        <div v-if="sessions.length === 0" style="text-align: center; color: #94a3b8; padding: 40px 16px; font-size: 0.85rem;">
          暂无对话，点击上方按钮开始
        </div>
      </div>
    </div>

    <!-- 聊天区域 -->
    <div style="flex: 1; display: flex; flex-direction: column; min-width: 0;">
      <div style="display: flex; justify-content: space-between; align-items: center; padding: 0 0 12px 16px;">
        <h2 style="margin: 0;">AI 智能助手</h2>
        <el-button text type="danger" :icon="Delete" @click="deleteCurrentSession" v-if="currentSessionId">
          删除会话
        </el-button>
      </div>

      <!-- 快捷指令 -->
      <div style="margin-bottom: 12px; padding-left: 16px; display: flex; gap: 8px; flex-wrap: wrap;">
        <el-tag
          v-for="p in quickPrompts"
          :key="p"
          type="info"
          style="cursor: pointer;"
          @click="sendMessage(p)"
        >
          {{ p }}
        </el-tag>
      </div>

      <div
        style="flex: 1; display: flex; flex-direction: column; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; min-height: 0;"
      >
        <!-- 聊天消息 -->
        <div
          ref="chatContainer"
          style="flex: 1; overflow-y: auto; padding: 16px; background: #f8fafc;"
        >
          <div v-if="messages.length === 0" style="text-align: center; color: #94a3b8; padding-top: 120px;">
            <div style="font-size: 3rem; margin-bottom: 12px;">🤖</div>
            <p style="font-size: 1rem; font-weight: 500;">你好！我是你的 AI 日程管家</p>
            <p style="font-size: 0.85rem;">试试点击上方标签，或输入你的问题</p>
          </div>

          <div
            v-for="(msg, i) in messages"
            :key="i"
            :style="{
              display: 'flex',
              flexDirection: 'column',
              alignItems: msg.role === 'user' ? 'flex-end' : 'flex-start',
              marginBottom: '12px',
            }"
          >
            <div style="font-size: 0.7rem; color: #94a3b8; margin-bottom: 2px;">
              {{ msg.role === 'user' ? '👤 我' : '🤖 AI 助手' }}
              <span style="margin-left: 8px;">{{ msg.timestamp }}</span>
            </div>
            <div
              :style="{
                maxWidth: '75%',
                padding: '12px 16px',
                borderRadius: msg.role === 'user' ? '16px 4px 16px 16px' : '4px 16px 16px 16px',
                background: msg.role === 'user' ? '#2563eb' : '#ffffff',
                color: msg.role === 'user' ? '#fff' : '#1e293b',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                boxShadow: '0 1px 2px rgba(0,0,0,.06)',
                lineHeight: '1.6',
              }"
            >
              {{ msg.content }}
              <div v-if="msg.action?.type === 'SCHEDULE_GENERATED'" style="margin-top: 10px;">
                <el-button type="success" size="small" @click="goToDashboard(msg.action?.data?.date)">
                  查看日程看板
                </el-button>
              </div>
            </div>
          </div>

          <div v-if="loading" style="display: flex; align-items: center; gap: 8px; padding: 8px 16px;">
            <span style="color: #94a3b8;">AI 正在思考</span>
            <span class="typing-dots">
              <span>.</span><span>.</span><span>.</span>
            </span>
          </div>
        </div>

        <!-- 输入区域 -->
        <div style="display: flex; gap: 12px; padding: 16px; border-top: 1px solid #e2e8f0;">
          <el-input
            v-model="input"
            placeholder="输入你的问题或指令... (Enter 发送)"
            :disabled="loading"
            @keydown="handleKeydown"
            clearable
            size="large"
          />
          <el-button type="primary" :disabled="!input.trim() || loading" @click="sendMessage()" size="large">
            发送
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.typing-dots span {
  animation: blink 1.4s infinite both;
  font-size: 1.5rem;
  line-height: 0;
}
.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes blink {
  0% { opacity: 0.2; }
  20% { opacity: 1; }
  100% { opacity: 0.2; }
}
</style>
