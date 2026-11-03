<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Check, Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import {
  taskbookApi,
  describeApiError,
  type TaskBookDetail,
  type TaskBookItem,
  type TaskBookWeek,
} from '@/api'

const route = useRoute()
const router = useRouter()
const bookId = computed(() => Number(route.params.id))

const book = ref<TaskBookDetail | null>(null)
const loading = ref(false)
const loadError = ref('')
const saving = ref(false)
const statusBusy = ref(false)
const busyItemId = ref<number | null>(null)

const form = ref({ title: '', purpose: '', requirements: '' })

const activeWeeks = ref<string[]>([])

const WEEK_CN = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
const STATUS_TEXT: Record<string, string> = { DRAFT: '草稿', CONFIRMED: '已确认', ARCHIVED: '已归档' }
const ITEM_STATUS_TEXT: Record<string, string> = { PENDING: '待做', DONE: '已完成', DROPPED: '已放弃' }
const CATEGORY_TEXT: Record<string, string> = { STUDY: '学习', PRACTICE: '练习', REVIEW: '复习' }

const isDraft = computed(() => book.value?.status === 'DRAFT')
const doneCount = computed(() => book.value?.progress?.done ?? 0)
const totalCount = computed(() => book.value?.progress?.total ?? 0)
const percent = computed(() =>
  totalCount.value > 0 ? Math.round((doneCount.value / totalCount.value) * 100) : 0
)

// 条目编辑 / 新增对话框
const itemDialogVisible = ref(false)
const itemDialogMode = ref<'edit' | 'create'>('edit')
const itemSaving = ref(false)
const itemForm = ref({
  id: 0,
  planDate: '',
  title: '',
  learnContent: '',
  estMinutes: 30,
  doneCriteria: '',
})

function statusType(status: string) {
  if (status === 'CONFIRMED') return 'success'
  if (status === 'ARCHIVED') return 'info'
  return 'warning'
}

function fmtDay(date: string) {
  if (!date) return ''
  const d = dayjs(date)
  return d.format('M月D日') + ' ' + WEEK_CN[d.day()]
}

/** 把一周的条目按 planDate 分组，返回按日期升序的每一天 */
function dayGroups(week: TaskBookWeek) {
  const map: Record<string, TaskBookItem[]> = {}
  ;(week.items ?? []).forEach((it) => {
    if (!map[it.planDate]) map[it.planDate] = []
    map[it.planDate].push(it)
  })
  return Object.keys(map)
    .sort()
    .map((date) => ({
      date,
      items: (map[date] ?? []).slice().sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0)),
    }))
}

async function loadBook() {
  loading.value = true
  loadError.value = ''
  try {
    const { data } = await taskbookApi.detail(bookId.value)
    book.value = data
    form.value = {
      title: data.title ?? '',
      purpose: data.purpose ?? '',
      requirements: data.requirements ?? '',
    }
    activeWeeks.value = (data.weeks ?? []).map((w) => String(w.id))
  } catch (e: any) {
    book.value = null
    loadError.value = describeApiError(e, 'GET /api/taskbooks/' + bookId.value)
  } finally {
    loading.value = false
  }
}

async function saveBook() {
  saving.value = true
  try {
    const { data } = await taskbookApi.update(bookId.value, {
      title: form.value.title,
      purpose: form.value.purpose,
      requirements: form.value.requirements,
    })
    book.value = data
    ElMessage.success('已保存')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.error || '保存失败')
  } finally {
    saving.value = false
  }
}

async function confirmBook() {
  try {
    await ElMessageBox.confirm('确认后任务书进入执行状态，未完成的条目会被顺延重排。确定确认吗？', '确认任务书', {
      type: 'warning',
    })
  } catch {
    return
  }
  statusBusy.value = true
  try {
    const { data } = await taskbookApi.updateStatus(bookId.value, 'CONFIRMED')
    book.value = data
    ElMessage.success('任务书已确认')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.error || '确认失败')
  } finally {
    statusBusy.value = false
  }
}

function openCreateItem(date: string) {
  itemDialogMode.value = 'create'
  itemForm.value = {
    id: 0,
    planDate: date || dayjs().format('YYYY-MM-DD'),
    title: '',
    learnContent: '',
    estMinutes: 30,
    doneCriteria: '',
  }
  itemDialogVisible.value = true
}

function openEditItem(item: TaskBookItem) {
  itemDialogMode.value = 'edit'
  itemForm.value = {
    id: item.id,
    planDate: item.planDate,
    title: item.title ?? '',
    learnContent: item.learnContent ?? '',
    estMinutes: item.estMinutes ?? 0,
    doneCriteria: item.doneCriteria ?? '',
  }
  itemDialogVisible.value = true
}

/** 404 / 405 说明后端还没提供该接口（契约 §3 未列出新增/删除条目） */
function isMissingEndpoint(e: any) {
  const s = e?.response?.status
  return s === 404 || s === 405 || s === 501
}

async function saveItem() {
  if (!itemForm.value.title.trim()) {
    ElMessage.warning('请填写「做什么」')
    return
  }
  itemSaving.value = true
  const payload = {
    planDate: itemForm.value.planDate,
    title: itemForm.value.title.trim(),
    learnContent: itemForm.value.learnContent,
    estMinutes: Number(itemForm.value.estMinutes) || 0,
    doneCriteria: itemForm.value.doneCriteria,
  }
  try {
    if (itemDialogMode.value === 'create') {
      await taskbookApi.createItem(bookId.value, payload)
      ElMessage.success('已新增条目')
    } else {
      await taskbookApi.updateItem(itemForm.value.id, payload)
      ElMessage.success('已保存条目')
    }
    itemDialogVisible.value = false
    await loadBook()
  } catch (e: any) {
    if (isMissingEndpoint(e)) {
      ElMessage.error('后端暂未提供新增/编辑条目接口（契约 §3 未定义），本次修改未提交')
    } else {
      ElMessage.error(e?.response?.data?.error || '保存失败')
    }
  } finally {
    itemSaving.value = false
  }
}

async function removeItem(item: TaskBookItem) {
  try {
    await ElMessageBox.confirm('删除条目「' + item.title + '」？', '确认删除', { type: 'warning' })
  } catch {
    return
  }
  busyItemId.value = item.id
  try {
    await taskbookApi.deleteItem(item.id)
    ElMessage.success('已删除')
    await loadBook()
  } catch (e: any) {
    if (isMissingEndpoint(e)) {
      ElMessage.error('后端暂未提供删除条目接口（契约 §3 未定义），本次删除未生效')
    } else {
      ElMessage.error(e?.response?.data?.error || '删除失败')
    }
  } finally {
    busyItemId.value = null
  }
}

onMounted(loadBook)
</script>

<template>
  <div class="tbd-page">
    <div class="tbd-toolbar">
      <el-button :icon="ArrowLeft" size="small" @click="router.push('/taskbooks')">返回列表</el-button>
      <div class="tbd-toolbar-actions">
        <el-button :icon="Refresh" size="small" :loading="loading" @click="loadBook">刷新</el-button>
        <el-button
          v-if="isDraft"
          type="primary"
          :icon="Check"
          :loading="statusBusy"
          @click="confirmBook"
        >
          确认任务书
        </el-button>
        <el-tag v-if="book" :type="statusType(book.status)" size="small">
          {{ STATUS_TEXT[book.status] || book.status }}
        </el-tag>
      </div>
    </div>

    <!-- 加载失败态 -->
    <el-alert
      v-if="loadError"
      type="error"
      :closable="false"
      show-icon
      title="任务书加载失败"
      style="margin-bottom: 16px;"
    >
      <div>{{ loadError }}</div>
      <el-button size="small" type="primary" :icon="Refresh" :loading="loading" style="margin-top: 10px;" @click="loadBook">
        重新加载
      </el-button>
    </el-alert>

    <el-skeleton v-if="loading && !book" :rows="6" animated />

    <template v-else-if="book">
      <!-- 头部：标题 / 区间 / 进度 -->
      <el-card shadow="never" class="tbd-head">
        <div class="tbd-head-top">
          <span class="tbd-title">{{ book.title }}</span>
          <el-progress
            :percentage="percent"
            :stroke-width="6"
            color="#6366f1"
            style="width: 220px;"
          />
        </div>
        <div class="tbd-meta">
          <span v-if="book.goalName">🎯 {{ book.goalName }}</span>
          <span>📅 {{ book.startDate }} ~ {{ book.endDate }}</span>
          <span>📦 {{ doneCount }}/{{ totalCount }} 条完成</span>
          <el-tag v-if="book.aiGenerated === false" size="small" type="info">AI 未生效 · 兜底模板</el-tag>
        </div>
      </el-card>

      <!-- 目的 / 要求：可编辑并保存 -->
      <el-card shadow="never" class="tbd-edit">
        <el-form label-width="110px">
          <el-form-item label="标题">
            <el-input v-model="form.title" maxlength="120" show-word-limit />
          </el-form-item>
          <el-form-item label="目的">
            <el-input v-model="form.purpose" type="textarea" :rows="3" placeholder="为什么这样排、预期达到什么水平" />
          </el-form-item>
          <el-form-item label="任务内容和要求">
            <el-input v-model="form.requirements" type="textarea" :rows="5" placeholder="总要求，一行一条" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="saveBook">保存修改</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 按周折叠 -->
      <el-empty v-if="(book.weeks ?? []).length === 0" description="这本任务书还没有任何周安排" />

      <el-collapse v-else v-model="activeWeeks" class="tbd-weeks">
        <el-collapse-item
          v-for="week in book.weeks"
          :key="week.id"
          :name="String(week.id)"
        >
          <template #title>
            <div class="week-title">
              <span class="week-index">第 {{ week.weekIndex }} 周</span>
              <span class="week-theme">{{ week.theme }}</span>
              <span class="week-range">{{ week.startDate }} ~ {{ week.endDate }}</span>
              <el-button
                size="small"
                text
                type="primary"
                :icon="Plus"
                class="week-add"
                @click.stop="openCreateItem(week.startDate)"
              >
                加一条
              </el-button>
            </div>
          </template>

          <div v-if="week.focus" class="week-focus">📌 本周要点：{{ week.focus }}</div>

          <div v-if="(week.items ?? []).length === 0" class="week-empty">
            本周还没有条目。
            <el-button size="small" text type="primary" :icon="Plus" @click="openCreateItem(week.startDate)">
              加一条
            </el-button>
          </div>

          <div v-for="day in dayGroups(week)" :key="day.date" class="day-block">
            <div class="day-head">
              <span class="day-date">{{ fmtDay(day.date) }}</span>
              <span class="day-count">{{ day.items.length }} 条</span>
              <el-button size="small" text type="primary" :icon="Plus" @click="openCreateItem(day.date)">
                加一条
              </el-button>
            </div>

            <div v-for="item in day.items" :key="item.id" class="item-row">
              <div class="item-body">
                <div class="item-title-line">
                  <span class="item-title" :class="{ 'is-done': item.status === 'DONE' }">{{ item.title }}</span>
                  <el-tag size="small" effect="plain">{{ CATEGORY_TEXT[item.category] || item.category }}</el-tag>
                  <el-tag v-if="item.status !== 'PENDING'" size="small" type="success">
                    {{ ITEM_STATUS_TEXT[item.status] || item.status }}
                  </el-tag>
                  <el-tag v-if="item.deferCount > 0" size="small" type="warning">
                    顺延 {{ item.deferCount }} 次
                  </el-tag>
                </div>
                <div v-if="item.learnContent" class="item-learn">📖 {{ item.learnContent }}</div>
                <div class="item-meta">
                  <span>⏱ 约 {{ item.estMinutes || 0 }} 分钟</span>
                  <span v-if="item.doneCriteria">✅ 算完成：{{ item.doneCriteria }}</span>
                  <span v-if="item.originDate !== item.planDate">↩ 原定 {{ item.originDate }}</span>
                </div>
              </div>
              <div class="item-actions">
                <el-button size="small" text type="primary" :icon="Edit" @click="openEditItem(item)">
                  编辑
                </el-button>
                <el-button
                  size="small"
                  text
                  type="danger"
                  :icon="Delete"
                  :loading="busyItemId === item.id"
                  @click="removeItem(item)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </template>

    <!-- 条目编辑 / 新增 -->
    <el-dialog
      v-model="itemDialogVisible"
      :title="itemDialogMode === 'create' ? '新增条目' : '编辑条目'"
      width="520px"
    >
      <el-form label-width="88px">
        <el-form-item label="安排日期">
          <el-date-picker v-model="itemForm.planDate" value-format="YYYY-MM-DD" type="date" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="做什么">
          <el-input v-model="itemForm.title" maxlength="200" placeholder="例：四级听力精听" />
        </el-form-item>
        <el-form-item label="学什么">
          <el-input
            v-model="itemForm.learnContent"
            type="textarea"
            :rows="3"
            placeholder="具体到材料 / 章节 / 题量，例：2019.12 第一套 Section A 三篇，听 2 遍逐句跟读"
          />
        </el-form-item>
        <el-form-item label="预计时长">
          <el-input-number v-model="itemForm.estMinutes" :min="0" :max="600" :step="5" />
          <span class="unit">分钟</span>
        </el-form-item>
        <el-form-item label="算完成">
          <el-input v-model="itemForm.doneCriteria" maxlength="255" placeholder="例：3 篇能不看文本复述大意" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="itemSaving" @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="itemSaving" @click="saveItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.tbd-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tbd-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.tbd-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tbd-head {
  background: rgba(255, 255, 255, 0.72);
}
.tbd-head-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.tbd-title {
  font-size: 1.15rem;
  font-weight: 520;
  color: #1e293b;
}
.tbd-meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 0.82rem;
  color: #94a3b8;
}

.tbd-edit {
  background: rgba(255, 255, 255, 0.82);
}

.tbd-weeks {
  border: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.tbd-weeks :deep(.el-collapse-item) {
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(226, 232, 240, 0.7);
  border-radius: 14px;
  overflow: hidden;
}
.tbd-weeks :deep(.el-collapse-item__header) {
  padding: 0 16px;
  height: 52px;
  border-bottom: none;
  background: transparent;
}
.tbd-weeks :deep(.el-collapse-item__wrap) {
  background: transparent;
}
.tbd-weeks :deep(.el-collapse-item__content) {
  padding: 0 16px 16px;
}

.week-title {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-width: 0;
}
.week-index {
  font-weight: 520;
  color: #4f46e5;
  flex-shrink: 0;
}
.week-theme {
  color: #1e293b;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.week-range {
  margin-left: auto;
  font-size: 0.78rem;
  color: #94a3b8;
  flex-shrink: 0;
}
.week-add {
  flex-shrink: 0;
}

.week-focus {
  font-size: 0.84rem;
  color: #475569;
  padding: 8px 0 4px;
}
.week-empty {
  font-size: 0.84rem;
  color: #94a3b8;
  padding: 8px 0;
}

.day-block {
  margin-top: 10px;
  border-top: 1px solid rgba(241, 245, 249, 1);
  padding-top: 6px;
}
.day-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 0;
}
.day-date {
  font-size: 0.86rem;
  font-weight: 500;
  color: #334155;
}
.day-count {
  font-size: 0.76rem;
  color: #94a3b8;
}

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 0 10px 12px;
  border-left: 2px solid rgba(99, 102, 241, 0.18);
  margin: 6px 0 6px 4px;
}
.item-body {
  min-width: 0;
  flex: 1;
}
.item-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.item-title {
  font-size: 0.92rem;
  font-weight: 500;
  color: #1e293b;
}
.item-title.is-done {
  color: #94a3b8;
  text-decoration: line-through;
}
.item-learn {
  margin-top: 5px;
  font-size: 0.83rem;
  color: #475569;
  line-height: 1.6;
}
.item-meta {
  margin-top: 5px;
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  font-size: 0.77rem;
  color: #94a3b8;
}
.item-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}
.unit {
  margin-left: 8px;
  font-size: 0.82rem;
  color: #94a3b8;
}
</style>
