<template>
  <div class="panel-inner chat-panel-inner">
    <div class="panel-header">
      <div>
        <div class="panel-title">AI 问答</div>
        <div class="panel-subtitle">基于知识库的 RAG 对话</div>
      </div>
      <div class="header-actions">
        <button :class="['tab-btn', mode === 'chat' ? 'active' : '']" @click="mode = 'chat'">对话</button>
        <button :class="['tab-btn', mode === 'search' ? 'active' : '']" @click="mode = 'search'">搜索</button>
      </div>
    </div>

    <!-- 对话模式 -->
    <template v-if="mode === 'chat'">
      <div class="chat-body" ref="chatBodyRef">
        <div v-if="messages.length === 0" class="empty-state">
          <div class="empty-icon">🤖</div>
          <p class="empty-title">向 AI 提问</p>
          <p class="empty-desc">AI 会从你的视频知识库中检索相关内容并回答</p>
          <div class="prompt-grid">
            <button v-for="p in suggestions" :key="p" class="prompt-chip" @click="sendMessage(p)">
              {{ p }}
            </button>
          </div>
        </div>

        <template v-for="(msg, idx) in messages" :key="idx">
          <div class="message" :class="msg.role">
            <div class="avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
            <div class="message-bubble">
              <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
              <div v-if="msg.sources && msg.sources.length > 0" class="source-list">
                <div class="source-label">📎 参考来源</div>
                <a v-for="src in msg.sources" :key="src.bvid"
                  :href="src.url" target="_blank" class="source-link">
                  📺 {{ src.title }}
                </a>
              </div>
            </div>
          </div>
        </template>

        <div v-if="loading" class="message assistant">
          <div class="avatar">🤖</div>
          <div class="message-bubble">
            <div class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <div class="chat-footer">
        <div v-if="errorMsg" class="error-banner">⚠️ {{ errorMsg }}</div>
        <div class="input-row">
          <textarea
            v-model="input"
            class="chat-input"
            placeholder="输入问题... (Enter 发送，Shift+Enter 换行)"
            rows="2"
            @keydown.enter.exact.prevent="sendMessage()"
            :disabled="loading"
          ></textarea>
          <button class="btn btn-primary send-btn" @click="sendMessage()"
            :disabled="loading || !input.trim()">
            {{ loading ? '...' : '发送' }}
          </button>
        </div>
        <div class="footer-bar">
          <span class="msg-count">{{ messages.length / 2 | 0 }} 轮对话</span>
          <button class="btn-link" @click="clearMessages" :disabled="messages.length === 0">清空</button>
        </div>
      </div>
    </template>

    <!-- 搜索模式 -->
    <template v-else>
      <div class="search-body">
        <div class="search-bar">
          <input
            v-model="searchQuery"
            class="input search-input"
            placeholder="输入关键词语义搜索知识库..."
            @keyup.enter="doSearch"
          />
          <button class="btn btn-primary" @click="doSearch" :disabled="searching">
            {{ searching ? '搜索中...' : '搜索' }}
          </button>
        </div>

        <div v-if="searchResults.length === 0 && !searching && searchQuery" class="empty-state">
          <p>未找到相关内容</p>
        </div>

        <div v-if="searching" class="loading"><div class="loading-spinner"></div></div>

        <div v-else class="search-results">
          <div v-for="r in searchResults" :key="r.bvid" class="search-result-card">
            <div class="result-header">
              <div class="result-title">{{ r.title }}</div>
              <div class="result-meta">
                <span v-if="r.owner_name">{{ r.owner_name }}</span>
                <span class="score-badge">相关度 {{ ((r._score || 0) * 100).toFixed(0) }}%</span>
              </div>
            </div>
            <div class="result-content">{{ truncate(r.content, 200) }}</div>
            <div class="result-actions">
              <a v-if="r.url !== '#'" :href="r.url" target="_blank" class="btn btn-sm">B站查看</a>
              <button class="btn btn-sm btn-primary" @click="askAbout(r.title)">就此提问</button>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { marked } from 'marked'
import { chatApi, ChatSource, ChatMessage, SearchResult } from '../services/api'

const mode = ref<'chat' | 'search'>('chat')

// 对话
interface Message {
  role: 'user' | 'assistant'
  content: string
  sources?: ChatSource[]
}

const messages = ref<Message[]>([])
const input = ref('')
const loading = ref(false)
const errorMsg = ref('')
const chatBodyRef = ref<HTMLElement | null>(null)

// 搜索
const searchQuery = ref('')
const searchResults = ref<SearchResult[]>([])
const searching = ref(false)

const suggestions = [
  '这些视频主要讲了什么？',
  '帮我总结核心知识点',
  '有哪些实用技巧？',
  '从哪个视频开始学习？'
]

const renderMarkdown = (text: string) => marked.parse(text) as string

const truncate = (text: string, len: number) =>
  text && text.length > len ? text.substring(0, len) + '...' : text

const scrollToBottom = async () => {
  await nextTick()
  if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
}

const sendMessage = async (text?: string) => {
  const question = text || input.value.trim()
  if (!question || loading.value) return

  input.value = ''
  errorMsg.value = ''
  messages.value.push({ role: 'user', content: question })
  loading.value = true
  await scrollToBottom()

  // 构建历史（不含当前问题）
  const history: ChatMessage[] = messages.value
    .slice(0, -1)
    .map(m => ({ role: m.role, content: m.content }))

  try {
    const resp = await chatApi.ask(question, history)
    messages.value.push({
      role: 'assistant',
      content: resp.answer,
      sources: resp.sources
    })
  } catch (e: any) {
    const errText = e.response?.data?.message || e.message || '未知错误'
    errorMsg.value = errText
    messages.value.push({ role: 'assistant', content: `抱歉，出现了错误：${errText}` })
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

const clearMessages = () => {
  messages.value = []
  errorMsg.value = ''
}

const doSearch = async () => {
  if (!searchQuery.value.trim()) return
  searching.value = true
  searchResults.value = []
  try {
    const resp = await chatApi.search(searchQuery.value)
    searchResults.value = resp.results
  } catch (e) {
    console.error(e)
  } finally {
    searching.value = false
  }
}

const askAbout = (title: string) => {
  mode.value = 'chat'
  input.value = `请详细介绍《${title}》中的主要内容`
  sendMessage()
}
</script>

<style scoped>
.chat-panel-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 600px;
}

.header-actions {
  display: flex;
  gap: 4px;
}

.tab-btn {
  padding: 5px 14px;
  border: 1px solid var(--border);
  border-radius: 4px;
  background: white;
  cursor: pointer;
  font-size: 13px;
  color: var(--text);
  transition: all 0.15s;
}

.tab-btn.active {
  background: var(--primary);
  border-color: var(--primary);
  color: white;
}

/* 对话区 */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 400px;
  max-height: 520px;
}

.empty-state {
  text-align: center;
  padding: 32px 16px;
  color: var(--text-light);
}

.empty-icon { font-size: 48px; margin-bottom: 8px; }
.empty-title { font-size: 16px; font-weight: 500; margin-bottom: 4px; color: var(--text); }
.empty-desc { font-size: 13px; margin-bottom: 16px; }

.prompt-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  max-width: 400px;
  margin: 0 auto;
}

.prompt-chip {
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: white;
  cursor: pointer;
  font-size: 13px;
  text-align: left;
  color: var(--text);
  transition: all 0.15s;
}

.prompt-chip:hover { border-color: var(--primary); color: var(--primary); }

.message {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.message.user { flex-direction: row-reverse; }

.avatar {
  font-size: 20px;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-bubble {
  max-width: 78%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.65;
}

.message.user .message-bubble {
  background: var(--primary);
  color: white;
  border-top-right-radius: 4px;
}

.message.assistant .message-bubble {
  background: #f5f5f5;
  color: var(--text);
  border-top-left-radius: 4px;
}

.message-content :deep(p) { margin: 0 0 8px; }
.message-content :deep(p:last-child) { margin-bottom: 0; }
.message-content :deep(ul), .message-content :deep(ol) { padding-left: 20px; margin: 6px 0; }
.message-content :deep(li) { margin-bottom: 4px; }
.message-content :deep(code) {
  background: rgba(0,0,0,0.08);
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 13px;
  font-family: monospace;
}
.message-content :deep(pre) {
  background: rgba(0,0,0,0.08);
  padding: 10px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 8px 0;
}
.message-content :deep(pre code) { background: none; padding: 0; }
.message-content :deep(blockquote) {
  border-left: 3px solid var(--primary);
  padding-left: 10px;
  margin: 6px 0;
  color: #666;
}

.source-list {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(0,0,0,0.1);
}

.source-label { font-size: 12px; color: #888; margin-bottom: 5px; }

.source-link {
  display: block;
  font-size: 12px;
  color: var(--primary);
  text-decoration: none;
  margin-bottom: 3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.source-link:hover { text-decoration: underline; }

.typing-indicator {
  display: flex;
  gap: 5px;
  align-items: center;
  padding: 4px 0;
}

.typing-indicator span {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #bbb;
  animation: bounce 1.2s infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-6px); }
}

/* 底部输入区 */
.chat-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--border);
  background: white;
}

.error-banner {
  background: #fff2f0;
  color: var(--error);
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 12px;
  margin-bottom: 8px;
}

.input-row {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.chat-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 14px;
  resize: none;
  font-family: inherit;
  line-height: 1.5;
  transition: border-color 0.2s;
}

.chat-input:focus { outline: none; border-color: var(--primary); }

.send-btn { height: 56px; padding: 0 20px; white-space: nowrap; min-width: 64px; }

.footer-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}

.msg-count { font-size: 12px; color: var(--text-light); }

.btn-link {
  background: none;
  border: none;
  color: var(--text-light);
  font-size: 12px;
  cursor: pointer;
  padding: 0;
}

.btn-link:hover { color: var(--error); }
.btn-link:disabled { opacity: 0.4; cursor: not-allowed; }

/* 搜索模式 */
.search-body {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.search-bar {
  display: flex;
  gap: 8px;
}

.search-input { flex: 1; }

.search-results {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.search-result-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px;
  transition: box-shadow 0.2s;
}

.search-result-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.08); }

.result-header { margin-bottom: 6px; }

.result-title {
  font-weight: 500;
  font-size: 14px;
  margin-bottom: 3px;
}

.result-meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: var(--text-light);
  align-items: center;
}

.score-badge {
  background: #e6f7ff;
  color: var(--primary);
  padding: 1px 6px;
  border-radius: 10px;
}

.result-content {
  font-size: 13px;
  color: var(--text-light);
  line-height: 1.5;
  margin-bottom: 8px;
}

.result-actions {
  display: flex;
  gap: 6px;
}
</style>
