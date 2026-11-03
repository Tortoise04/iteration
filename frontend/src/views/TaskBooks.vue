<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import api, { taskbookApi, describeApiError, type TaskBookSummary } from '@/api'

interface GoalOption {
  id: number
  name: string
  description?: string
}

const router = useRouter()

const loading = ref(false)
const loadError = ref('')
const books = ref<TaskBookSummary[]>([])

// 新建流程
const dialogVisible = ref(false)
const generating = ref(false)
const goals = ref<GoalOption[]>([])
const goalLoading = ref(false)
const goalId = ref<number | null>(null)
const startDate = ref(dayjs().format('YYYY-MM-DD'))

const bookCount = computed(() => books.value.length)

const STATUS_TEXT: Record<string, string> = {
  DRAFT: '草稿',
  CONFIRMED: '已确认',
  ARCHIVED: '已归档',
}

function statusType(status: string) {
  if (status === 'CONFIRMED') return 'success'
  if (status === 'ARCHIVED') return 'info'
  return 'warning'
}

function percentOf(book: TaskBookSummary) {
  if (!book.totalItems) return 0
  return Math.round((book.doneItems / book.totalItems) * 100)
}

async function loadBooks() {
  loading.value = true
  loadError.value = ''
  try {
    const { data } = await taskbookApi.list()
    books.value = Array.isArray(data) ? data : []
  } catch (e: any) {
    books.value = []
    loadError.value = describeApiError(e, 'GET /api/taskbooks')
  } finally {
    loading.value = false
  }
}

async function openCreate() {
  dialogVisible.value = true
  if (goals.value.length === 0) {
    goalLoading.value = true
    try {
      const { data } = await api.get('/goals')
      goals.value = Array.isArray(data) ? data : []
    } catch {
      goals.value = []
    } finally {
      goalLoading.value = false
    }
  }
}

async function generate() {
  if (!goalId.value) {
    ElMessage.warning('请先选择一个目标')
    return
  }
  generating.value = true
  try {
    const { data } = await taskbookApi.generate(goalId.value, startDate.value)
    ElMessage.success('任务书已生成' + (data?.aiGenerated === false ? '（AI 未生效，使用兜底模板）' : ''))
    dialogVisible.value = false
    if (data?.id) {
      router.push('/taskbooks/' + data.id)
    } else {
      await loadBooks()
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.error || '生成失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

function openDetail(book: TaskBookSummary) {
  router.push('/taskbooks/' + book.id)
}

onMounted(loadBooks)
</script>

<template>
  <div class="tb-page">
    <div class="tb-toolbar">
      <div class="tb-count">
        <span v-if="!loadError">共 {{ bookCount }} 本任务书</span>
      </div>
      <div class="tb-toolbar-actions">
        <el-button :icon="Refresh" size="small" :loading="loading" @click="loadBooks">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建任务书</el-button>
      </div>
    </div>

    <el-alert
      v-if="loadError"
      type="error"
      :closable="false"
      show-icon
      title="任务书列表加载失败"
      style="margin-bottom: 16px;"
    >
      <div>{{ loadError }}</div>
      <div style="margin-top: 8px; font-size: 0.8rem; color: #94a3b8;">
        可能原因：后端服务未启动，或任务书接口尚未就绪（GET /api/taskbooks）。
      </div>
      <el-button size="small" type="primary" :icon="Refresh" :loading="loading" style="margin-top: 10px;" @click="loadBooks">
        重新加载
      </el-button>
    </el-alert>

    <el-skeleton v-if="loading && books.length === 0" :rows="4" animated />

    <el-empty
      v-else-if="!loadError && books.length === 0"
      description="还没有任务书。选一个目标，让 AI 帮你排整本。"
    >
      <el-button type="primary" :icon="Plus" @click="openCreate">新建任务书</el-button>
    </el-empty>

    <div v-else class="tb-list">
      <el-card v-for="book in books" :key="book.id" shadow="hover" class="tb-card" @click="openDetail(book)">
        <div class="tb-card-head">
          <span class="tb-title">{{ book.title }}</span>
          <el-tag size="small" :type="statusType(book.status)">{{ STATUS_TEXT[book.status] || book.status }}</el-tag>
        </div>
        <div class="tb-meta">
          <span v-if="book.goalName">🎯 {{ book.goalName }}</span>
          <span>📅 {{ book.startDate }} ~ {{ book.endDate }}</span>
          <span>📦 {{ book.doneItems }}/{{ book.totalItems }} 条完成</span>
        </div>
        <el-progress :percentage="percentOf(book)" :stroke-width="6" color="#6366f1" style="margin-top: 10px;" />
      </el-card>
    </div>

    <!-- 新建：选目标 → 生成 → 跳详情 -->
    <el-dialog v-model="dialogVisible" title="新建任务书" width="480px" :close-on-click-modal="!generating">
      <el-form label-width="88px">
        <el-form-item label="目标">
          <el-select
            v-model="goalId"
            placeholder="选择一个目标"
            :loading="goalLoading"
            style="width: 100%;"
            :disabled="generating"
          >
            <el-option v-for="g in goals" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
          <div v-if="!goalLoading && goals.length === 0" class="dialog-hint">
            还没有目标，请先到「目标管理」创建一个。
          </div>
        </el-form-item>
        <el-form-item label="起始日">
          <el-date-picker
            v-model="startDate"
            value-format="YYYY-MM-DD"
            type="date"
            style="width: 100%;"
            :disabled="generating"
          />
        </el-form-item>
      </el-form>

      <el-alert
        v-if="generating"
        type="info"
        :closable="false"
        show-icon
        title="AI 正在生成整本任务书（按周分段、每天拆条），通常需要 20~60 秒，请不要关闭页面。"
      />

      <template #footer>
        <el-button :disabled="generating" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="generating" @click="generate">
          {{ generating ? '生成中…' : '开始生成' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.tb-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tb-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.tb-count {
  font-size: 0.86rem;
  color: #94a3b8;
}
.tb-toolbar-actions {
  display: flex;
  gap: 8px;
}

.tb-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
}
.tb-card {
  cursor: pointer;
  background: rgba(255, 255, 255, 0.78);
}
.tb-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.tb-title {
  font-size: 0.98rem;
  font-weight: 500;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tb-meta {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 0.8rem;
  color: #94a3b8;
}
.dialog-hint {
  margin-top: 4px;
  font-size: 0.78rem;
  color: #f59e0b;
}
</style>
