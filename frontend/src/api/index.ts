import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 300000, // 5分钟超时，AI调用可能较慢
})

// 请求拦截器：自动附加 token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

// 响应拦截器：统一处理错误
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    } else {
      ElMessage.error(error.response?.data?.error || '请求失败')
    }
    return Promise.reject(error)
  }
)

export default api

/** 把接口错误转成一句人话，用于页面级降级提示（不依赖后端返回的具体措辞） */
export function describeApiError(e: any, path: string): string {
  const status = e?.response?.status
  const serverMsg = e?.response?.data?.error || e?.response?.data?.message
  if (serverMsg) return String(serverMsg)
  if (!status) return '无法连接后端服务（' + path + '）'
  if (status === 404) return '接口不存在（' + path + '），后端可能还是旧版本'
  if (status === 405) return '接口方法不支持（' + path + '），后端可能还是旧版本'
  return '请求失败 HTTP ' + status + '（' + path + '）'
}

/* ==================== 任务书（契约 docs/任务书_改造契约.md §3） ==================== */

export type TaskBookStatus = 'DRAFT' | 'CONFIRMED' | 'ARCHIVED'
export type ItemStatus = 'PENDING' | 'DONE' | 'DROPPED'
export type ItemCategory = 'STUDY' | 'PRACTICE' | 'REVIEW'

/** task_book_items 一行 */
export interface TaskBookItem {
  id: number
  bookId: number
  weekId: number | null
  planDate: string
  originDate: string
  title: string
  learnContent: string
  estMinutes: number
  doneCriteria: string
  category: ItemCategory | string
  status: ItemStatus | string
  deferCount: number
  sortOrder: number
}

/** 任务书详情里的一周 */
export interface TaskBookWeek {
  id: number
  weekIndex: number
  theme: string
  focus: string
  startDate: string
  endDate: string
  items: TaskBookItem[]
}

/** GET /api/taskbooks/{id}、PUT、PUT status、POST generate 的返回 */
export interface TaskBookDetail {
  id: number
  title: string
  purpose: string
  requirements: string
  goalId: number | null
  goalName: string | null
  startDate: string
  endDate: string
  status: TaskBookStatus | string
  progress: { done: number; total: number }
  weeks: TaskBookWeek[]
  aiGenerated?: boolean
}

/** GET /api/taskbooks 列表行 */
export interface TaskBookSummary {
  id: number
  title: string
  goalId: number | null
  goalName: string | null
  startDate: string
  endDate: string
  status: TaskBookStatus | string
  totalItems: number
  doneItems: number
}

/** GET /api/taskbooks/today 的条目（§3.1，字段是精简版） */
export interface TodayItem {
  id: number
  title: string
  learnContent: string
  estMinutes: number
  doneCriteria: string
  category: ItemCategory | string
  status: ItemStatus | string
  deferCount: number
}

export interface TodayBook {
  bookId: number
  title: string
  theme: string | null
  items: TodayItem[]
}

export interface BehindItem {
  itemId: number
  bookId: number
  title: string
  originDate: string
  planDate: string
  deferCount: number
}

export interface TodayPayload {
  date: string
  books: TodayBook[]
  behind: { count: number; items: BehindItem[] }
  progress: { done: number; total: number }
}

/** 顺延后被动改期的条目 */
export interface RescheduledEntry {
  itemId: number
  planDate: string
}

/** PUT items/{id}/status 与 POST items/{id}/defer 的返回 */
export interface ItemMutationResult {
  item: TaskBookItem
  rescheduled: RescheduledEntry[]
}

/** POST /api/taskbooks/reschedule 的返回 */
export interface RescheduleResult {
  moved: { itemId: number; from: string; to: string }[]
}

export interface ItemUpdatePayload {
  title?: string
  learnContent?: string
  estMinutes?: number
  doneCriteria?: string
  planDate?: string
}

export interface BookUpdatePayload {
  title?: string
  purpose?: string
  requirements?: string
}

export const taskbookApi = {
  /** GET /api/taskbooks */
  list: () => api.get<TaskBookSummary[]>('/taskbooks'),

  /** GET /api/taskbooks/{id} */
  detail: (id: number) => api.get<TaskBookDetail>('/taskbooks/' + id),

  /** POST /api/taskbooks/generate —— startDate 可省，默认今天 */
  generate: (goalId: number, startDate?: string) =>
    api.post<TaskBookDetail>('/taskbooks/generate', { goalId, startDate }),

  /** PUT /api/taskbooks/{id} —— 标题/目的/要求 */
  update: (id: number, payload: BookUpdatePayload) =>
    api.put<TaskBookDetail>('/taskbooks/' + id, payload),

  /** PUT /api/taskbooks/{id}/status —— DRAFT / CONFIRMED / ARCHIVED */
  updateStatus: (id: number, status: TaskBookStatus) =>
    api.put<TaskBookDetail>('/taskbooks/' + id + '/status', { status }),

  /** PUT /api/taskbooks/items/{itemId} */
  updateItem: (itemId: number, payload: ItemUpdatePayload) =>
    api.put<TaskBookItem>('/taskbooks/items/' + itemId, payload),

  /** PUT /api/taskbooks/items/{itemId}/status —— DONE / PENDING / DROPPED */
  setItemStatus: (itemId: number, status: ItemStatus) =>
    api.put<ItemMutationResult>('/taskbooks/items/' + itemId + '/status', { status }),

  /** POST /api/taskbooks/items/{itemId}/defer —— 把这一条顺延到最近可用日 */
  deferItem: (itemId: number) =>
    api.post<ItemMutationResult>('/taskbooks/items/' + itemId + '/defer'),

  /** POST /api/taskbooks/reschedule —— 把所有逾期未完成条目顺延 */
  reschedule: () => api.post<RescheduleResult>('/taskbooks/reschedule'),

  /** GET /api/taskbooks/today?date=yyyy-MM-dd（只读） */
  today: (date?: string) =>
    api.get<TodayPayload>('/taskbooks/today', { params: date ? { date } : {} }),

  /* --- 契约 §3 未列出、但详情页「条目可加/可删」必需的两个接口 ---
     后端若尚未提供，详情页会捕获 404/405 并提示，不影响其他功能。 */
  createItem: (
    bookId: number,
    payload: { planDate: string; title: string; learnContent?: string; estMinutes?: number; doneCriteria?: string }
  ) => api.post<TaskBookItem>('/taskbooks/' + bookId + '/items', payload),

  deleteItem: (itemId: number) => api.delete<void>('/taskbooks/items/' + itemId),
}
