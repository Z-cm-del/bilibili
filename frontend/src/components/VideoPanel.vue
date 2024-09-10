<template>
  <div class="panel-inner">
    <div class="panel-header">
      <div>
        <div class="panel-title">知识库管理</div>
        <div class="panel-subtitle">
          {{ processedCount }}/{{ videos.length }} 已向量化
          <span v-if="pendingCount > 0" class="processing-badge">{{ pendingCount }} 处理中</span>
        </div>
      </div>
      <button class="btn btn-primary" @click="showUpload = !showUpload">
        {{ showUpload ? '收起' : '+ 添加' }}
      </button>
    </div>

    <transition name="slide">
      <div v-if="showUpload" class="upload-area">
        <!-- 模式切换 -->
        <div class="tab-row">
          <button :class="['tab-btn', uploadMode === 'crawl' ? 'active' : '']" @click="uploadMode = 'crawl'">
            🔗 B站链接
          </button>
          <button :class="['tab-btn', uploadMode === 'text' ? 'active' : '']" @click="uploadMode = 'text'">
            ✏️ 手动填写
          </button>
          <button :class="['tab-btn', uploadMode === 'file' ? 'active' : '']" @click="uploadMode = 'file'">
            📄 上传字幕
          </button>
        </div>

        <!-- B站链接爬取 -->
        <div v-if="uploadMode === 'crawl'" class="form-list">
          <div class="crawl-tip">
            输入 B站视频链接或 BV 号，自动提取视频标题、字幕、AI总结等内容入库
          </div>
          <div class="form-item">
            <label>B站视频链接 / BV号 *</label>
            <input
              v-model="crawlUrl"
              class="input"
              placeholder="https://www.bilibili.com/video/BV1xx... 或 BV1xx411c7mD"
              @keyup.enter="submitCrawl"
            />
          </div>
          <div class="crawl-sources">
            <span class="source-tip">自动尝试获取（优先级）：</span>
            <span class="source-item">🤖 AI总结</span>
            <span class="arrow">›</span>
            <span class="source-item">📝 CC字幕</span>
            <span class="arrow">›</span>
            <span class="source-item">📋 视频简介</span>
          </div>
          <button class="btn btn-primary w-full" @click="submitCrawl" :disabled="submitting || !crawlUrl.trim()">
            {{ submitting ? '爬取中...' : '🚀 爬取并入库' }}
          </button>
        </div>

        <!-- 手动填写 -->
        <div v-if="uploadMode === 'text'" class="form-list">
          <div class="form-item">
            <label>标题 *</label>
            <input v-model="form.title" class="input" placeholder="视频标题" />
          </div>
          <div class="form-item">
            <label>内容 * <span class="hint">（字幕、笔记、摘要均可）</span></label>
            <textarea v-model="form.content" class="input" rows="6"
              placeholder="粘贴视频字幕或笔记内容..."></textarea>
          </div>
          <div class="form-row">
            <div class="form-item">
              <label>UP主</label>
              <input v-model="form.owner_name" class="input" placeholder="可选" />
            </div>
            <div class="form-item">
              <label>BV号</label>
              <input v-model="form.bvid" class="input" placeholder="留空自动生成" />
            </div>
          </div>
          <div class="form-item">
            <label>简介</label>
            <input v-model="form.description" class="input" placeholder="可选" />
          </div>
          <button class="btn btn-primary w-full" @click="submitText" :disabled="submitting">
            {{ submitting ? '处理中...' : '提交入库' }}
          </button>
        </div>

        <!-- 上传字幕文件 -->
        <div v-if="uploadMode === 'file'" class="form-list">
          <div class="form-item">
            <label>标题 *</label>
            <input v-model="fileForm.title" class="input" placeholder="视频标题" />
          </div>
          <div class="form-item">
            <label>字幕/文本文件 * <span class="hint">(.srt / .vtt / .txt)</span></label>
            <div class="drop-zone" :class="{ 'drag-over': isDragging }"
              @dragover.prevent="isDragging = true"
              @dragleave="isDragging = false"
              @drop.prevent="onDrop"
              @click="fileInputRef?.click()">
              <div v-if="!selectedFile" class="drop-placeholder">
                <div class="drop-icon">📄</div>
                <p>点击或拖拽文件到此处</p>
                <p class="hint">支持 .srt .vtt .txt，最大 50MB</p>
              </div>
              <div v-else class="file-selected">
                <span>✅ {{ selectedFile.name }}</span>
                <button class="btn btn-sm" @click.stop="selectedFile = null">移除</button>
              </div>
            </div>
            <input ref="fileInputRef" type="file" accept=".srt,.vtt,.txt" style="display:none"
              @change="onFileChange" />
          </div>
          <div class="form-item">
            <label>UP主</label>
            <input v-model="fileForm.owner_name" class="input" placeholder="可选" />
          </div>
          <button class="btn btn-primary w-full" @click="submitFile"
            :disabled="submitting || !selectedFile || !fileForm.title">
            {{ submitting ? '上传中...' : '上传入库' }}
          </button>
        </div>

        <div v-if="uploadMsg" :class="['upload-msg', uploadSuccess ? 'success' : 'error']">
          {{ uploadMsg }}
        </div>
      </div>
    </transition>

    <!-- 视频列表 -->
    <div class="panel-body video-body">
      <div v-if="listLoading" class="loading"><div class="loading-spinner"></div></div>
      <div v-else-if="videos.length === 0" class="empty-state">
        <div style="font-size:36px;margin-bottom:8px">📭</div>
        <p>知识库为空</p>
        <p class="hint">点击右上角「+ 添加」，粘贴 B站链接即可入库</p>
      </div>
      <div v-else class="video-list">
        <div v-for="v in videos" :key="v.bvid" class="video-card"
          :class="{ processing: !v.is_processed && !v.process_error }">
          <div class="video-card-body">
            <div class="video-title" :title="v.title">{{ v.title }}</div>
            <div class="video-meta">
              <span v-if="v.owner_name" class="meta-item">{{ v.owner_name }}</span>
              <span class="source-badge">{{ sourceLabel(v.content_source) }}</span>
              <span :class="['status-pill', statusClass(v)]">{{ statusText(v) }}</span>
            </div>
            <div v-if="v.content_preview" class="video-preview">{{ v.content_preview }}</div>
            <div v-if="v.process_error" class="error-text" :title="v.process_error">
              ⚠️ {{ v.process_error.substring(0, 100) }}
            </div>
          </div>
          <div class="video-card-actions">
            <a v-if="!v.bvid.startsWith('LOCAL_')"
              :href="`https://www.bilibili.com/video/${v.bvid}`"
              target="_blank" class="btn btn-sm">B站</a>
            <button v-if="v.process_error" class="btn btn-sm" @click="reindex(v.bvid)">重试</button>
            <button class="btn btn-sm btn-danger" @click="deleteVideo(v.bvid)">删除</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { videoApi, VideoItem } from '../services/api'

const videos = ref<VideoItem[]>([])
const listLoading = ref(false)
const showUpload = ref(false)
const uploadMode = ref<'crawl' | 'text' | 'file'>('crawl')
const submitting = ref(false)
const uploadMsg = ref('')
const uploadSuccess = ref(false)
const isDragging = ref(false)
const selectedFile = ref<File | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)

// 爬取模式
const crawlUrl = ref('')

// 手动模式
const form = ref({ title: '', content: '', description: '', owner_name: '', bvid: '' })

// 文件模式
const fileForm = ref({ title: '', owner_name: '' })

let pollTimer: ReturnType<typeof setInterval> | null = null

const processedCount = computed(() => videos.value.filter(v => v.is_processed).length)
const pendingCount = computed(() => videos.value.filter(v => !v.is_processed && !v.process_error).length)

const loadVideos = async () => {
  try {
    const resp = await videoApi.list()
    videos.value = resp.videos
    if (pendingCount.value > 0) startPolling()
    else stopPolling()
  } catch (e) {
    console.error('加载视频列表失败', e)
  }
}

const startPolling = () => {
  if (pollTimer) return
  pollTimer = setInterval(loadVideos, 3000)
}

const stopPolling = () => {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

const showResult = (success: boolean, msg: string) => {
  uploadSuccess.value = success
  uploadMsg.value = msg
  setTimeout(() => { uploadMsg.value = '' }, 5000)
}

// ── 爬取 B站链接 ──
const submitCrawl = async () => {
  if (!crawlUrl.value.trim()) return
  submitting.value = true
  uploadMsg.value = ''
  try {
    const resp = await videoApi.crawl(crawlUrl.value.trim())
    if (resp.success) {
      showResult(true, `✅ 爬取成功：《${resp.title}》（${resp.content_source}，${resp.content_length} 字）正在向量化...`)
      crawlUrl.value = ''
      await loadVideos()
      startPolling()
    } else {
      showResult(false, '❌ ' + resp.message)
    }
  } catch (e: any) {
    showResult(false, '❌ 请求失败：' + (e.response?.data?.message || e.message))
  } finally {
    submitting.value = false
  }
}

// ── 手动填写 ──
const submitText = async () => {
  if (!form.value.title.trim()) return showResult(false, '标题不能为空')
  if (!form.value.content.trim()) return showResult(false, '内容不能为空')
  submitting.value = true
  uploadMsg.value = ''
  try {
    const resp = await videoApi.add(form.value)
    if (resp.success) {
      showResult(true, '✅ 已提交，正在向量化入库...')
      form.value = { title: '', content: '', description: '', owner_name: '', bvid: '' }
      await loadVideos()
      startPolling()
    } else {
      showResult(false, '❌ ' + resp.message)
    }
  } catch (e: any) {
    showResult(false, '❌ ' + (e.response?.data?.message || e.message))
  } finally {
    submitting.value = false
  }
}

// ── 上传文件 ──
const submitFile = async () => {
  if (!selectedFile.value || !fileForm.value.title.trim()) return
  submitting.value = true
  uploadMsg.value = ''
  try {
    const fd = new FormData()
    fd.append('file', selectedFile.value)
    fd.append('title', fileForm.value.title)
    fd.append('owner_name', fileForm.value.owner_name)
    const resp = await videoApi.upload(fd)
    if (resp.success) {
      showResult(true, '✅ 上传成功，正在向量化入库...')
      selectedFile.value = null
      fileForm.value = { title: '', owner_name: '' }
      await loadVideos()
      startPolling()
    } else {
      showResult(false, '❌ ' + resp.message)
    }
  } catch (e: any) {
    showResult(false, '❌ ' + (e.response?.data?.message || e.message))
  } finally {
    submitting.value = false
  }
}

const onFileChange = (e: Event) => {
  const input = e.target as HTMLInputElement
  if (input.files?.[0]) selectedFile.value = input.files[0]
}

const onDrop = (e: DragEvent) => {
  isDragging.value = false
  const file = e.dataTransfer?.files[0]
  if (file) selectedFile.value = file
}

const deleteVideo = async (bvid: string) => {
  if (!confirm('确认删除该视频及其向量索引？')) return
  try {
    await videoApi.delete(bvid)
    await loadVideos()
  } catch { alert('删除失败') }
}

const reindex = async (bvid: string) => {
  try {
    await videoApi.reindex(bvid)
    await loadVideos()
    startPolling()
  } catch { alert('重试失败') }
}

const sourceLabel = (s: string) =>
  ({ manual: '手动', upload: '上传', bilibili: 'B站', subtitle: '字幕', ai_summary: 'AI总结', description: '简介' } as Record<string, string>)[s] || s

const statusClass = (v: VideoItem) => v.is_processed ? 'ok' : v.process_error ? 'empty' : 'partial'
const statusText = (v: VideoItem) => v.is_processed ? '已入库' : v.process_error ? '失败' : '向量化中...'

onMounted(async () => {
  listLoading.value = true
  await loadVideos()
  listLoading.value = false
})

onUnmounted(() => stopPolling())
</script>

<style scoped>
.upload-area {
  padding: 14px 16px;
  border-bottom: 1px solid var(--border);
  background: #fafafa;
}

.tab-row { display: flex; gap: 6px; margin-bottom: 14px; }

.tab-btn {
  padding: 5px 12px;
  border: 1px solid var(--border);
  border-radius: 4px;
  background: white;
  cursor: pointer;
  font-size: 12px;
  color: var(--text);
  transition: all 0.15s;
}

.tab-btn.active { background: var(--primary); border-color: var(--primary); color: white; }

.form-list { display: flex; flex-direction: column; gap: 10px; }

.crawl-tip {
  font-size: 12px;
  color: var(--text-light);
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 6px;
  padding: 8px 10px;
  line-height: 1.5;
}

.crawl-sources {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-light);
  flex-wrap: wrap;
}

.source-tip { color: #aaa; }
.source-item {
  background: white;
  border: 1px solid var(--border);
  padding: 2px 8px;
  border-radius: 10px;
}
.arrow { color: #ccc; }

.form-row { display: flex; gap: 10px; }
.form-row .form-item { flex: 1; }

.form-item { display: flex; flex-direction: column; gap: 3px; }
.form-item label { font-size: 12px; color: var(--text-light); }
.hint { font-size: 11px; color: #aaa; }

.drop-zone {
  border: 2px dashed var(--border);
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  color: var(--text-light);
  font-size: 13px;
}

.drop-zone:hover, .drop-zone.drag-over { border-color: var(--primary); background: #e6f7ff; }
.drop-placeholder .drop-icon { font-size: 28px; margin-bottom: 6px; }

.file-selected {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
}

.upload-msg {
  margin-top: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.5;
}

.upload-msg.success { background: #f6ffed; color: var(--success); }
.upload-msg.error { background: #fff2f0; color: var(--error); }

.video-body { overflow-y: auto; max-height: 460px; }
.video-list { display: flex; flex-direction: column; gap: 8px; }

.video-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px 12px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  transition: border-color 0.2s;
}

.video-card.processing { border-color: var(--warning); background: #fffbe6; }
.video-card-body { flex: 1; min-width: 0; }

.video-title {
  font-weight: 500;
  font-size: 13px;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.video-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--text-light);
  margin-bottom: 4px;
  flex-wrap: wrap;
}

.meta-item { max-width: 80px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.source-badge { background: #f0f0f0; padding: 1px 5px; border-radius: 8px; }

.video-preview {
  font-size: 11px;
  color: #aaa;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.error-text {
  font-size: 11px;
  color: var(--error);
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.video-card-actions { display: flex; flex-direction: column; gap: 4px; flex-shrink: 0; }

.processing-badge {
  background: var(--warning);
  color: white;
  padding: 1px 6px;
  border-radius: 8px;
  font-size: 11px;
  margin-left: 6px;
}

.btn-danger { border-color: var(--error); color: var(--error); }
.btn-danger:hover { background: var(--error); color: white; }
.w-full { width: 100%; }

.slide-enter-active, .slide-leave-active { transition: all 0.2s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateY(-8px); }
</style>
