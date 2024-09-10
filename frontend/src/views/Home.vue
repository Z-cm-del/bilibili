<template>
  <div class="home">
    <!-- 顶栏 -->
    <header class="app-topbar">
      <div class="brand">
        <span class="brand-logo">📚</span>
        <div>
          <div class="brand-title">BiliMind</div>
          <div class="brand-subtitle">收藏夹知识库助手</div>
        </div>
      </div>

      <div class="topbar-center" v-if="session">
        <div class="health-bar">
          <span :class="['health-dot', health.es]" :title="'Elasticsearch: ' + health.es"></span>
          <span :class="['health-dot', health.llm ? 'ok' : 'empty']" title="LLM API"></span>
          <span class="health-label">{{ health.llm ? health.model : 'LLM 未配置' }}</span>
        </div>
      </div>

      <div class="topbar-actions">
        <template v-if="user">
          <span class="user-chip">
            <span>👤</span>
            <strong>{{ user }}</strong>
            <span v-if="isGuest" class="guest-tag">访客</span>
          </span>
          <button v-if="!isGuest" @click="showLogin = true" class="btn">切换账号</button>
          <button @click="onLogout" class="btn">退出</button>
        </template>
        <template v-else>
          <button @click="enterGuestMode" class="btn">访客体验</button>
          <button @click="showLogin = true" class="btn btn-primary">扫码登录 B站</button>
        </template>
      </div>
    </header>

    <main class="app-main">
      <!-- 落地页 -->
      <template v-if="!session">
        <section class="hero">
          <div class="hero-content">
            <div class="hero-badge">🚀 RAG · Elasticsearch · Spring Boot · Vue3</div>
            <h1 class="hero-title">把"收藏"变成真正可用的知识</h1>
            <p class="hero-desc">
              收藏了大量学习视频却没时间看？<br>
              BiliMind 帮你自动提炼要点、语义检索、AI 对话式回顾。
            </p>
            <div class="hero-actions">
              <button @click="showLogin = true" class="btn btn-primary btn-lg">
                📱 扫码登录 B站
              </button>
              <button @click="enterGuestMode" class="btn btn-lg">
                ⚡ 直接体验（上传字幕）
              </button>
            </div>
          </div>

          <div class="pipeline-row">
            <div class="pipeline-card" v-for="step in steps" :key="step.num">
              <div class="pipeline-icon">{{ step.icon }}</div>
              <div class="pipeline-text">
                <strong>{{ step.title }}</strong>
                <span>{{ step.desc }}</span>
              </div>
            </div>
          </div>

          <div class="tech-stack">
            <span v-for="t in techStack" :key="t" class="tech-tag">{{ t }}</span>
          </div>
        </section>
      </template>

      <!-- 工作区 -->
      <template v-else>
        <div class="workspace">
          <!-- 左侧：B站收藏夹（仅登录用户） -->
          <aside v-if="!isGuest" class="side-panel panel">
            <SourcesPanel :sessionId="session" @build-done="handleBuildDone" />
          </aside>

          <!-- 中间：知识库管理 -->
          <section class="mid-panel panel">
            <VideoPanel />
          </section>

          <!-- 右侧：AI 对话 -->
          <section class="chat-panel-wrap panel">
            <ChatPanel />
          </section>
        </div>
      </template>
    </main>

    <footer class="app-footer">
      BiliMind © 2026 &nbsp;·&nbsp; Spring Boot 3 + Vue 3 + Elasticsearch 8 + OpenAI RAG
    </footer>

    <LoginModal v-if="showLogin" @close="showLogin = false" @success="onLogin" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import LoginModal from '../components/LoginModal.vue'
import SourcesPanel from '../components/SourcesPanel.vue'
import ChatPanel from '../components/ChatPanel.vue'
import VideoPanel from '../components/VideoPanel.vue'
import { healthApi } from '../services/api'

const session = ref<string | null>(null)
const user = ref<string | null>(null)
const isGuest = ref(false)
const showLogin = ref(false)
const health = ref({ es: 'partial', llm: false, model: '' })

const steps = [
  { num: '1', icon: '📥', title: '同步 / 上传', desc: '接入收藏夹或上传字幕文件' },
  { num: '2', icon: '🧠', title: '向量化', desc: 'Embedding 语义嵌入' },
  { num: '3', icon: '🔍', title: '检索', desc: 'ES KNN 相似度搜索' },
  { num: '4', icon: '💬', title: '对话', desc: 'RAG 问答回顾知识' }
]

const techStack = [
  'Spring Boot 3', 'Vue 3 + TypeScript', 'Elasticsearch 8',
  'OpenAI Embedding', 'SQLite', 'RAG'
]

const checkHealth = async () => {
  try {
    const resp: any = await healthApi.check()
    health.value = {
      es: resp.elasticsearch === 'UP' ? 'ok' : 'empty',
      llm: resp.llm_configured === true,
      model: resp.llm_model || ''
    }
  } catch (e) {
    health.value = { es: 'empty', llm: false, model: '' }
  }
}

onMounted(async () => {
  const storedSession = localStorage.getItem('bili_session')
  const storedUser = localStorage.getItem('bili_user')
  if (storedSession && storedUser) {
    session.value = storedSession
    user.value = storedUser
    isGuest.value = storedSession === 'guest'
  }
  await checkHealth()
})

const onLogin = (sid: string, info: any) => {
  session.value = sid
  user.value = info.uname || info.name || '用户'
  isGuest.value = false
  showLogin.value = false
  localStorage.setItem('bili_session', sid)
  localStorage.setItem('bili_user', user.value!)
  checkHealth()
}

const enterGuestMode = () => {
  session.value = 'guest'
  user.value = '访客'
  isGuest.value = true
  localStorage.setItem('bili_session', 'guest')
  localStorage.setItem('bili_user', '访客')
  checkHealth()
}

const onLogout = () => {
  session.value = null
  user.value = null
  isGuest.value = false
  localStorage.removeItem('bili_session')
  localStorage.removeItem('bili_user')
}

const handleBuildDone = () => console.log('收藏夹构建完成')
</script>

<style scoped>
.workspace {
  display: flex;
  flex: 1;
  padding: 16px 20px;
  gap: 16px;
  min-height: calc(100vh - 110px);
  align-items: flex-start;
}

.side-panel { width: 270px; flex-shrink: 0; }
.mid-panel { width: 340px; flex-shrink: 0; }
.chat-panel-wrap { flex: 1; min-width: 0; }

.brand-logo { font-size: 26px; margin-right: 8px; }

/* 健康状态 */
.health-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-light);
}

.health-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.health-dot.ok { background: var(--success); }
.health-dot.partial { background: var(--warning); }
.health-dot.empty { background: var(--error); }

.health-label { font-size: 12px; color: var(--text-light); }

.guest-tag {
  background: #f0f0f0;
  padding: 1px 5px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: normal;
}

/* 落地页 */
.hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 24px 40px;
  background: linear-gradient(160deg, #f0f4ff 0%, #e8f5ff 50%, #f5f0ff 100%);
  min-height: calc(100vh - 110px);
  text-align: center;
}

.hero-badge {
  display: inline-block;
  background: rgba(0, 161, 214, 0.1);
  color: var(--primary);
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 13px;
  margin-bottom: 20px;
  border: 1px solid rgba(0, 161, 214, 0.2);
}

.hero-title {
  font-size: 38px;
  font-weight: 700;
  margin-bottom: 16px;
  color: var(--text);
  line-height: 1.3;
}

.hero-desc {
  font-size: 17px;
  color: var(--text-light);
  margin-bottom: 32px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 48px;
  flex-wrap: wrap;
}

.btn-lg { padding: 12px 28px; font-size: 15px; }

.pipeline-row {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
  margin-bottom: 32px;
}

.pipeline-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  min-width: 180px;
}

.pipeline-icon { font-size: 28px; }

.pipeline-text { text-align: left; }
.pipeline-text strong { display: block; font-size: 15px; margin-bottom: 2px; }
.pipeline-text span { font-size: 12px; color: var(--text-light); }

.tech-stack {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.tech-tag {
  background: white;
  border: 1px solid var(--border);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  color: var(--text-light);
}

@media (max-width: 960px) {
  .workspace { flex-direction: column; }
  .side-panel, .mid-panel { width: 100%; }
  .topbar-center { display: none; }
}
</style>
