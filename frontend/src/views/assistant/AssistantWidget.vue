<template>
  <div v-if="available" class="assistant-widget">
    <el-card v-if="open" class="assistant-panel" shadow="always">
      <template #header>
        <div class="assistant-header">
          <strong>员工智能助理</strong>
          <el-button text aria-label="关闭助理" @click="open = false">关闭</el-button>
        </div>
      </template>
      <div class="assistant-actions">
        <el-button size="small" @click="newConversation">新对话</el-button>
        <el-button size="small" @click="showHistory = !showHistory">历史会话</el-button>
      </div>
      <div v-if="showHistory" class="assistant-history">
        <el-button v-for="item in conversations" :key="item.id" text class="history-item" @click="selectConversation(item.id)">
          {{ item.title }}
        </el-button>
        <p v-if="conversations.length === 0">暂无历史会话</p>
      </div>
      <div ref="messageList" class="assistant-messages" aria-live="polite">
        <el-empty v-if="messages.length === 0" description="可问通用问题及已授权知识；实时业务数据尚未接入" :image-size="70" />
        <div v-for="(item, index) in messages" :key="index" class="assistant-message">
          <strong>{{ item.role === 'user' ? '我' : '助理' }}</strong>
          <p>{{ item.content }}</p>
        </div>
      </div>
      <el-input v-model="question" type="textarea" :rows="2" maxlength="1000" show-word-limit
        placeholder="输入问题，Ctrl + Enter 发送" @keydown.ctrl.enter="send" />
      <el-button class="send-button" type="primary" :loading="loading" :disabled="!question.trim()" @click="send">发送</el-button>
    </el-card>
    <el-button class="assistant-launcher" type="primary" round @click="open = !open">
      {{ open ? '收起助理' : '智能助理' }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ask, getAvailability, getConversations, getMessages, type AssistantConversation, type AssistantMessage } from '@/api/assistant'

const route = useRoute()
const available = ref(false)
const open = ref(false)
const loading = ref(false)
const showHistory = ref(false)
const question = ref('')
const conversationId = ref<number | undefined>()
const conversations = ref<AssistantConversation[]>([])
const messages = ref<Array<Pick<AssistantMessage, 'role' | 'content'>>>([])
const messageList = ref<HTMLElement>()
let refreshTimer: ReturnType<typeof setInterval> | undefined
let typingTimer: ReturnType<typeof setInterval> | undefined
let typingTarget: { index: number; answer: string } | undefined
let chatVersion = 0

const stopTyping = (complete = false) => {
  if (typingTimer) clearInterval(typingTimer)
  typingTimer = undefined
  if (complete && typingTarget) messages.value[typingTarget.index].content = typingTarget.answer
  typingTarget = undefined
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageList.value) messageList.value.scrollTop = messageList.value.scrollHeight
}

const refreshAvailability = async () => {
  try {
    available.value = await getAvailability()
  } catch {
    available.value = false
  }
  if (!available.value) {
    chatVersion++
    stopTyping()
    loading.value = false
    open.value = false
    conversationId.value = undefined
    messages.value = []
  }
}

onMounted(() => {
  refreshAvailability()
  refreshTimer = setInterval(refreshAvailability, 30000)
})
onBeforeUnmount(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  stopTyping()
})
watch(() => route.fullPath, refreshAvailability)
watch(open, (isOpen) => {
  if (!isOpen && typingTimer) {
    stopTyping(true)
    loading.value = false
  }
})

const newConversation = () => {
  chatVersion++
  stopTyping()
  loading.value = false
  conversationId.value = undefined
  messages.value = []
  showHistory.value = false
}

const selectConversation = async (id: number) => {
  chatVersion++
  stopTyping()
  loading.value = false
  const version = chatVersion
  const history = await getMessages(id)
  if (version !== chatVersion) return
  messages.value = history
  conversationId.value = id
  showHistory.value = false
}

watch(showHistory, async (show) => {
  if (show) conversations.value = await getConversations()
})

const send = async () => {
  const value = question.value.trim()
  if (!value || loading.value || !available.value) return
  question.value = ''
  messages.value.push({ role: 'user', content: value })
  scrollToBottom()
  loading.value = true
  const version = chatVersion
  try {
    const result = await ask(value, conversationId.value)
    if (version !== chatVersion) return
    conversationId.value = result.conversationId
    if (!open.value) {
      messages.value.push({ role: 'assistant', content: result.answer })
      loading.value = false
      return
    }
    const replyIndex = messages.value.push({ role: 'assistant', content: '' }) - 1
    typingTarget = { index: replyIndex, answer: result.answer }
    const characters = Array.from(result.answer)
    let position = 0
    typingTimer = setInterval(() => {
      messages.value[replyIndex].content += characters.slice(position, position + 3).join('')
      position += 3
      scrollToBottom()
      if (position >= characters.length) {
        stopTyping()
        loading.value = false
      }
    }, 20)
  } catch {
    if (version !== chatVersion) return
    messages.value.push({ role: 'assistant', content: '助理暂时无法回答，请检查模型连接或稍后重试。' })
    await refreshAvailability()
    loading.value = false
  }
}
</script>

<style scoped>
.assistant-widget { position: fixed; right: 24px; bottom: 24px; z-index: 2500; }
.assistant-panel { width: min(430px, calc(100vw - 32px)); margin-bottom: 12px; }
.assistant-header, .assistant-actions { display: flex; align-items: center; justify-content: space-between; }
.assistant-actions { justify-content: flex-start; margin-bottom: 10px; }
.assistant-history { max-height: 150px; overflow: auto; border-bottom: 1px solid var(--el-border-color); margin-bottom: 8px; }
.history-item { display: block; width: 100%; overflow: hidden; text-align: left; text-overflow: ellipsis; white-space: nowrap; }
.assistant-messages { height: min(340px, 42vh); overflow: auto; margin-bottom: 12px; }
.assistant-message { margin-bottom: 15px; }
.assistant-message p { margin: 4px 0; white-space: pre-wrap; }
.assistant-launcher { float: right; }
.send-button { margin-top: 10px; }
@media (max-width: 600px) { .assistant-widget { right: 16px; bottom: 16px; } }
</style>
