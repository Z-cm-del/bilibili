<template>
  <div class="panel-inner">
    <div class="panel-header">
      <div>
        <div class="panel-title">收藏夹</div>
        <div class="panel-subtitle">{{ folders.length }} 个</div>
      </div>
      <div class="panel-actions">
        <button @click="refresh" class="btn" :disabled="loading">
          {{ loading ? '加载中...' : '刷新' }}
        </button>
      </div>
    </div>
    
    <div class="panel-body">
      <div class="sources-scroll">
        <div v-if="loading" class="loading">
          <div class="loading-spinner"></div>
        </div>
        <div v-else-if="folders.length === 0" class="text-center text-sm text-gray-500 py-6">
          暂无收藏夹
        </div>
        <div v-else class="space-y-2">
          <div 
            v-for="folder in folders" 
            :key="folder.media_id"
            class="folder-card" 
            :class="{ selected: selected.has(folder.media_id) }"
          >
            <div class="folder-head" @click="toggleExpand(folder.media_id)">
              <input 
                type="checkbox"
                :checked="selected.has(folder.media_id)"
                @change="toggleSelect(folder.media_id)"
                @click.stop
                class="w-4 h-4 mr-2"
              />
              <div class="folder-meta">
                <div class="folder-title" :title="folder.title">{{ folder.title }}</div>
                <div class="folder-count">
                  {{ folder.media_count }} 个视频
                </div>
              </div>
              <div class="folder-toggle" :class="{ rotated: folder.expanded }">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                </svg>
              </div>
            </div>
            
            <div v-if="folder.expanded" class="folder-list">
              <div v-if="folder.loading" class="text-xs text-gray-500">
                加载中...
              </div>
              <div v-else-if="folder.videos && folder.videos.length === 0" class="text-xs text-gray-500">
                暂无视频
              </div>
              <div v-else-if="folder.videos" class="space-y-2">
                <div v-for="video in folder.videos" :key="video.bvid" class="video-item">
                  <span class="text-primary">▶</span>
                  <span class="truncate" :title="video.title">{{ video.title }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div class="panel-footer">
      <button 
        @click="buildKnowledge"
        :disabled="selected.size === 0 || building"
        class="btn btn-primary w-full"
      >
        {{ getButtonText() }}
      </button>
      
      <p class="text-xs text-gray-500 text-center mt-2">
        入库后可在右侧进行问答
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { favoritesApi, FavoriteFolder } from '../services/api'

const props = defineProps<{
  sessionId: string
}>()

const emit = defineEmits<{
  (e: 'build-done'): void
  (e: 'selection-change', folderIds: number[]): void
}>()

interface FolderWithExtra extends FavoriteFolder {
  videos?: Array<{
    bvid: string
    title: string
  }>
  expanded?: boolean
  loading?: boolean
}

const folders = ref<FolderWithExtra[]>([])
const selected = ref<Set<number>>(new Set())
const loading = ref(true)
const building = ref(false)

const loadFolders = async () => {
  loading.value = true
  try {
    const data = await favoritesApi.getList(props.sessionId)
    folders.value = data.map((f) => ({ ...f, expanded: false }))
  } catch (error) {
    console.error('加载收藏夹失败:', error)
  } finally {
    loading.value = false
  }
}

const refresh = async () => {
  await loadFolders()
}

const toggleExpand = async (mediaId: number) => {
  const folder = folders.value.find(f => f.media_id === mediaId)
  if (!folder) return
  
  folder.expanded = !folder.expanded
  
  if (folder.expanded && !folder.videos) {
    folder.loading = true
    try {
      // 模拟获取视频列表
      setTimeout(() => {
        folder.videos = Array.from({ length: 5 }, (_, i) => ({
          bvid: `BV${Math.random().toString(36).substr(2, 10)}`,
          title: `视频 ${i + 1} - ${folder.title}`
        }))
        folder.loading = false
      }, 1000)
    } catch (error) {
      console.error('加载视频列表失败:', error)
      folder.loading = false
    }
  }
}

const toggleSelect = (mediaId: number) => {
  const newSelected = new Set(selected.value)
  if (newSelected.has(mediaId)) {
    newSelected.delete(mediaId)
  } else {
    newSelected.add(mediaId)
  }
  selected.value = newSelected
  emit('selection-change', Array.from(newSelected))
}

const buildKnowledge = async () => {
  if (selected.value.size === 0) return
  
  building.value = true
  try {
    // 模拟构建过程
    setTimeout(() => {
      building.value = false
      emit('build-done')
    }, 2000)
  } catch (error) {
    console.error('构建知识库失败:', error)
    building.value = false
  }
}

const getButtonText = () => {
  if (building.value) return '处理中...'
  if (selected.value.size === 0) return '选择收藏夹'
  return `入库 (${selected.value.size})`
}

onMounted(() => {
  loadFolders()
})
</script>

<style scoped>
.space-y-2 {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.w-full {
  width: 100%;
}

.text-center {
  text-align: center;
}

.py-6 {
  padding-top: 1.5rem;
  padding-bottom: 1.5rem;
}

.text-gray-500 {
  color: #666;
}

.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-primary {
  color: var(--primary);
}

.mr-2 {
  margin-right: 0.5rem;
}

.w-4 {
  width: 1rem;
}

.h-4 {
  height: 1rem;
}

.panel-actions {
  display: flex;
  gap: 0.5rem;
}

.rotated {
  transform: rotate(90deg);
  transition: transform 0.2s;
}
</style>