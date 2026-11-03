<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Check, ArrowRight, Notebook } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { taskbookApi, describeApiError, type TodayPayload, type TodayItem, type BehindItem } from '@/api'

const router = useRouter()

const loading = ref(false)
const rescheduling = ref(false)
const loadError = ref('')
const payload = ref<TodayPayload | null>(null)
const busyId = ref<number | null>(null)

const WEEK_CN = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

const todayLabel = computed(() => {
  const d = payload.value?.date ? dayjs(payload.value.date) : dayjs()
  return d.format('YYYY年M月D日') + ' ' + WEEK_CN[d.day()]
})

const books = computed(() => payload.value?.books ?? [])
const behindItems = computed<BehindItem[]>(() => payload.value?.behind?.items ?? [])
const behindCount = computed(() => payload.value?.behind?.count ?? behindItems.value.length)
const doneCount = computed(() => payload.value?.progress?.done ?? 0)
const totalCount = computed(() => payload.value?.progress?.total ?? 0)
const percent = computed(() =>
  totalCount.value > 0 ? Math.round((doneCount.value / totalCount.value) * 100) : 0
)
const hasTodayItems = computed(() => books.value.some((b) => (b.items ?? []).length > 0))
const visibleBooks = computed(() => books.value.filter((b) => (b.items ?? []).length > 0))

const categoryText: Record<string, string> = {
  STUDY: '学习',
  PRACTICE: '练习',
  REVIEW: '复习',
}

function categoryType(cat: string) {
  if (cat === 'PRACTICE') return 'success'
  if (cat === 'REVIEW') return 'warning'
  return ''
}

/** 只在加载时顺延：顺延失败不阻断今日数据展示（降级为只读视图） */
async function loadToday() {
  loading.value = true
  loadError.value = ''
  try {
    await taskbookApi.reschedule()
  } catch {
    // 顺延接口失败（后端未就绪 / 无逾期条目）时继续尝试读取今日数据
  }
  try {
    const { data } = await taskbookApi.today()
    payload.value = data
  } catch (e: any) {
    payload.value = null
    loadError.value = describeApiError(e, 'GET /api/taskbooks/today')
  } finally {
    loading.value = false
  }
}

/** 完成 / 未完成（未完成会由后端顺延剩余条目） */
async function settle(item: TodayItem, status: 'DONE' | 'PENDING') {
  busyId.value = item.id
  try {
    const { data } = await taskbookApi.setItemStatus(item.id, status)
    const moved = data?.rescheduled?.length ?? 0
    if (status === 'DONE') {
      ElMessage.success('已完成：' + item.title)
    } else {
      ElMessage.info(moved > 0 ? '已标记未完成，顺延 ' + moved + ' 条' : '已标记未完成')
    }
    await loadToday()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.error || '操作失败')
  } finally {
    busyId.value = null
  }
}

/** 单条逾期条目顺延到最近可用日 */
async function deferOne(item: BehindItem) {
  busyId.value = item.itemId
  try {
    await taskbookApi.deferItem(item.itemId)
    ElMessage.success('已顺延：' + item.title)
    await loadToday()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.error || '顺延失败')
  } finally {
    busyId.value = null
  }
}

/** 全部顺延 */
async function deferAll() {
  rescheduling.value = true
  try {
    const { data } = await taskbookApi.reschedule()
    const moved = data?.moved?.length ?? 0
    ElMessage.success(moved > 0 ? '已顺延 ' + moved + ' 条' : '没有需要顺延的条目')
    await loadToday()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.error || '顺延失败')
  } finally {
    rescheduling.value = false
  }
}

onMounted(loadToday)
</script>

<template>
  <div class="today-page">
    <!-- 顶部：日期 + 进度环 -->
    <el-card shadow="never" class="head-card">
      <div class="head-row">
        <div class="head-left">
          <div class="head-date">{{ todayLabel }}</div>
          <div class="head-sub">
            <template v-if="payload">
              共 {{ totalCount }} 条 · 已完成 {{ doneCount }} 条
              <el-tag v-if="behindCount > 0" size="small" type="warning" style="margin-left: 8px;">
                {{ behindCount }} 条落后
              </el-tag>
            </template>
            <template v-else>今天要做的事</template>
          </div>
        </div>
        <div class="head-right">
          <el-progress
            type="circle"
            :percentage="percent"
            :width="88"
            :stroke-width="8"
            color="#6366f1"
          />
          <el-button :icon="Refresh" size="small" :loading="loading" @click="loadToday">
            刷新
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 加载失败态：明确提示，不白屏 -->
    <el-card v-if="loadError" shadow="never" class="fail-card">
      <div class="fail-box">
        <div class="fail-emoji">🔌</div>
        <div class="fail-title">今日任务加载失败</div>
        <div class="fail-desc">{{ loadError }}</div>
        <div class="fail-hint">
          可能原因：后端服务未启动，或任务书接口尚未就绪（GET /api/taskbooks/today）。
        </div>
        <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadToday">
          重新加载
        </el-button>
      </div>
    </el-card>

    <template v-else>
      <!-- 落后区 -->
      <el-card v-if="behindCount > 0" shadow="never" class="behind-card">
        <div class="section-head">
          <span class="section-title">⏰ 落后区 · {{ behindCount }} 条逾期未完成</span>
          <el-button
            type="warning"
            size="small"
            :icon="ArrowRight"
            :loading="rescheduling"
            @click="deferAll"
          >
            全部顺延
          </el-button>
        </div>
        <div v-for="b in behindItems" :key="b.itemId" class="behind-row">
          <div class="behind-main">
            <span class="behind-title">{{ b.title }}</span>
            <span class="behind-meta">
              原定 {{ b.originDate }} · 现排 {{ b.planDate }}
              <el-tag v-if="b.deferCount > 0" size="small" type="info">已顺延 {{ b.deferCount }} 次</el-tag>
            </span>
          </div>
          <el-button
            size="small"
            text
            type="primary"
            :loading="busyId === b.itemId"
            @click="deferOne(b)"
          >
            顺延这条
          </el-button>
        </div>
      </el-card>

      <!-- 今日条目：按任务书分组 -->
      <el-skeleton v-if="loading && !payload" :rows="4" animated style="margin-top: 16px;" />

      <el-empty
        v-else-if="!hasTodayItems && !loading"
        description="今天还没有安排的任务。先创建一本任务书吧。"
      >
        <el-button type="primary" :icon="Notebook" @click="router.push('/taskbooks')">
          去创建任务书
        </el-button>
      </el-empty>

      <template v-else>
      <el-card
        v-for="book in visibleBooks"
        :key="book.bookId"
        shadow="hover"
        class="book-card"
      >
        <div class="section-head">
          <span class="section-title">{{ book.title }}</span>
          <span class="book-theme">
            <el-tag v-if="book.theme" size="small" effect="plain">{{ book.theme }}</el-tag>
          </span>
        </div>

        <div v-for="item in book.items" :key="item.id" class="item-row">
          <div class="item-body">
            <div class="item-title-line">
              <span class="item-title" :class="{ 'is-done': item.status === 'DONE' }">
                {{ item.title }}
              </span>
              <el-tag size="small" :type="categoryType(item.category)" effect="plain">
                {{ categoryText[item.category] || item.category }}
              </el-tag>
              <el-tag v-if="item.deferCount > 0" size="small" type="warning">
                顺延 {{ item.deferCount }} 次
              </el-tag>
              <el-tag v-if="item.status === 'DONE'" size="small" type="success">已完成</el-tag>
            </div>
            <div v-if="item.learnContent" class="item-learn">📖 {{ item.learnContent }}</div>
            <div class="item-meta">
              <span>⏱ 约 {{ item.estMinutes || 0 }} 分钟</span>
              <span v-if="item.doneCriteria">✅ 算完成：{{ item.doneCriteria }}</span>
            </div>
          </div>
          <div class="item-actions">
            <el-button
              v-if="item.status !== 'DONE'"
              type="success"
              size="small"
              :icon="Check"
              :loading="busyId === item.id"
              @click="settle(item, 'DONE')"
            >
              完成
            </el-button>
            <el-button
              v-if="item.status !== 'DONE'"
              type="warning"
              size="small"
              plain
              :loading="busyId === item.id"
              @click="settle(item, 'PENDING')"
            >
              未完成（顺延）
            </el-button>
            <span v-else class="item-done-hint">已记录</span>
          </div>
        </div>
      </el-card>
      </template>
    </template>
  </div>
</template>

<style scoped>
.today-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.head-card {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(12px);
}
.head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.head-date {
  font-size: 1.35rem;
  font-weight: 520;
  color: #1e293b;
  letter-spacing: -0.01em;
}
.head-sub {
  margin-top: 6px;
  font-size: 0.86rem;
  color: #94a3b8;
}
.head-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.fail-card {
  border-color: rgba(248, 113, 113, 0.35) !important;
}
.fail-box {
  text-align: center;
  padding: 24px 12px;
}
.fail-emoji {
  font-size: 30px;
}
.fail-title {
  margin-top: 8px;
  font-size: 1.02rem;
  font-weight: 500;
  color: #b91c1c;
}
.fail-desc {
  margin-top: 6px;
  font-size: 0.88rem;
  color: #475569;
  word-break: break-all;
}
.fail-hint {
  margin: 8px auto 16px;
  max-width: 460px;
  font-size: 0.8rem;
  color: #94a3b8;
  line-height: 1.6;
}

.behind-card {
  border-color: rgba(251, 191, 36, 0.35) !important;
  background: rgba(255, 251, 235, 0.6);
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.section-title {
  font-size: 0.98rem;
  font-weight: 500;
  color: #334155;
}
.behind-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-top: 1px dashed rgba(226, 232, 240, 0.9);
}
.behind-row:first-of-type {
  border-top: none;
}
.behind-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.behind-title {
  font-size: 0.9rem;
  color: #1e293b;
}
.behind-meta {
  font-size: 0.78rem;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.book-card {
  background: rgba(255, 255, 255, 0.78);
}
.book-theme {
  display: flex;
  gap: 6px;
}

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 12px 0;
  border-top: 1px solid rgba(241, 245, 249, 1);
}
.item-row:first-of-type {
  border-top: none;
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
  font-size: 0.95rem;
  font-weight: 500;
  color: #1e293b;
}
.item-title.is-done {
  color: #94a3b8;
  text-decoration: line-through;
}
.item-learn {
  margin-top: 6px;
  font-size: 0.85rem;
  color: #475569;
  line-height: 1.6;
}
.item-meta {
  margin-top: 6px;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 0.78rem;
  color: #94a3b8;
}
.item-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.item-done-hint {
  font-size: 0.78rem;
  color: #22c55e;
}
</style>
