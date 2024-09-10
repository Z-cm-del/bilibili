import axios, { AxiosInstance } from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 90000
})

api.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('bili_session')
      localStorage.removeItem('bili_user')
      window.location.href = '/'
    }
    return Promise.reject(error)
  }
)

// ---- 类型定义 ----

export interface UserInfo {
  mid: number
  uname: string
  face: string
  level?: number
}

export interface FavoriteFolder {
  media_id: number
  title: string
  media_count: number
  is_selected: boolean
}

export interface VideoItem {
  id: number
  bvid: string
  title: string
  description: string
  owner_name: string
  content_source: string
  pic_url: string
  duration: number
  is_processed: boolean
  process_error: string | null
  created_at: string
  content_preview: string
}

export interface ChatSource {
  bvid: string
  title: string
  url: string
  pic_url?: string
}

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface ChatResponse {
  answer: string
  sources: ChatSource[]
  error?: string
}

export interface SearchResult {
  bvid: string
  title: string
  owner_name: string
  content: string
  url: string
  _score: number
}

// ---- API 服务 ----

export const authApi = {
  getQRCode: () => api.get('/auth/qrcode'),
  pollQRCode: (qrcodeKey: string) => api.get(`/auth/qrcode/poll/${qrcodeKey}`),
  getSession: (sessionId: string) => api.get(`/auth/session/${sessionId}`),
  logout: (sessionId: string) => api.delete(`/auth/session/${sessionId}`)
}

export const favoritesApi = {
  getList: (sessionId: string): Promise<FavoriteFolder[]> =>
    api.get(`/favorites/list/${sessionId}`),
  sync: (sessionId: string, sessdata: string, biliJct: string, dedeuserId: string) =>
    api.post(`/favorites/sync/${sessionId}`, {}, { params: { sessdata, biliJct, dedeuserId } }),
  select: (folderId: number, selected: boolean) =>
    api.post(`/favorites/select/${folderId}`, {}, { params: { selected } })
}

export const videoApi = {
  list: (): Promise<{ videos: VideoItem[]; total: number }> =>
    api.get('/videos/list'),
  crawl: (url: string): Promise<{ success: boolean; message: string; title?: string; bvid?: string; content_source?: string; content_length?: number }> =>
    api.post('/videos/crawl', { url }),
  add: (data: {
    bvid?: string
    title: string
    content: string
    description?: string
    owner_name?: string
  }): Promise<{ success: boolean; message: string; bvid?: string }> =>
    api.post('/videos/add', data),
  upload: (formData: FormData): Promise<{ success: boolean; message: string; bvid?: string }> =>
    api.post('/videos/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }),
  delete: (bvid: string) => api.delete(`/videos/${bvid}`),
  reindex: (bvid: string) => api.post(`/videos/${bvid}/reindex`)
}

export const chatApi = {
  ask: (question: string, history: ChatMessage[] = [], k = 5): Promise<ChatResponse> =>
    api.post('/chat/ask', { question, history, k }),
  search: (query: string, k = 8): Promise<{ results: SearchResult[]; total: number }> =>
    api.get('/chat/search', { params: { query, k } })
}

export const healthApi = {
  check: () => api.get('/health')
}

export default api
