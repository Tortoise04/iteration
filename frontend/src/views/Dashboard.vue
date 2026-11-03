<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, Refresh } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const route = useRoute()
const plan = ref<any>(null)
const tasks = ref<any[]>([])
const loading = ref(false)
const selectedDate = ref(dayjs().format('YYYY-MM-DD'))

/** 过滤掉休息块（BREAK）：契约 §5 —— 前端不再渲染 BREAK 任务 */
function filterBreak(list: any[]) {
  return list.filter((t: any) => t.taskType !== 'BREAK')
}

const sortedTasks = computed(() => {
  return [...tasks.value].sort((a, b) => {
    if (!a.scheduledStart) return 1
    if (!b.scheduledStart) return -1
    return a.scheduledStart.localeCompare(b.scheduledStart)
  })
})

const hasOnlyCourses = computed(() => {
  return tasks.value.length > 0 && tasks.value.every((t: any) => t.taskType === 'COURSE')
})

const emptyHint = computed(() => {
  if (tasks.value.length === 0) return '暂无任务，请先添加课程和目标，然后点击上方"生成/刷新日程"按钮'
  if (hasOnlyCourses.value) return '当前仅有课程任务。添加学习/工作目标可以让 AI 帮你更充分地规划空余时间'
  return ''
})

const displayDate = computed(() => {
  const d = dayjs(selectedDate.value)
  const today = dayjs().format('YYYY-MM-DD')
  const tomorrow = dayjs().add(1, 'day').format('YYYY-MM-DD')
  const yesterday = dayjs().subtract(1, 'day').format('YYYY-MM-DD')
  if (selectedDate.value === today) return '今日日程'
  if (selectedDate.value === tomorrow) return '明日日程'
  if (selectedDate.value === yesterday) return '昨日日程'
  return d.format('M月D日 (ddd)') + ' 日程'
})

onMounted(() => {
  if (route.query.date) {
    selectedDate.value = route.query.date as string
  }
  loadSchedule()
})

async function loadSchedule() {
  loading.value = true
  try {
    const { data } = await api.get('/schedule/today', { params: { date: selectedDate.value } })
    plan.value = data.plan
    // 休息块（BREAK）不再作为待办展示：后端已不下发，这里再过滤一次历史遗留数据
    tasks.value = filterBreak(data.tasks || [])
    if (data.message) {
      ElMessage.info(data.message)
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.error || '加载日程失败')
  } finally {
    loading.value = false
  }
}

async function generatePlan() {
  loading.value = true
  try {
    const { data } = await api.post('/schedule/generate', null, { params: { date: selectedDate.value } })
    plan.value = data.plan
    tasks.value = filterBreak(data.tasks || [])
    ElMessage.success('日程已生成')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.error || '生成日程失败')
  } finally {
    loading.value = false
  }
}

async function updateTaskStatus(taskId: number, status: string) {
  try {
    await api.put(`/schedule/tasks/${taskId}/status`, { status })
    ElMessage.success(status === 'COMPLETED' ? '任务完成！' : '状态已更新')
    loadSchedule()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.error || '操作失败')
  }
}

function prevDay() {
  selectedDate.value = dayjs(selectedDate.value).subtract(1, 'day').format('YYYY-MM-DD')
  loadSchedule()
}

function nextDay() {
  selectedDate.value = dayjs(selectedDate.value).add(1, 'day').format('YYYY-MM-DD')
  loadSchedule()
}

function goToToday() {
  selectedDate.value = dayjs().format('YYYY-MM-DD')
  loadSchedule()
}

function getStatusColor(status: string) {
  const map: Record<string, string> = {
    'PENDING': '#94a3b8',
    'IN_PROGRESS': '#3b82f6',
    'COMPLETED': '#22c55e',
    'SKIPPED': '#f59e0b',
    'POSTPONED': '#ef4444',
  }
  return map[status] || '#94a3b8'
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    'PENDING': '待开始',
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成',
    'SKIPPED': '已跳过',
    'POSTPONED': '已推迟',
  }
  return map[status] || status
}

function getTaskTypeIcon(type: string) {
  return type === 'COURSE' ? '📘' : '📝'
}
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
      <div style="display: flex; align-items: center; gap: 12px;">
        <el-button :icon="ArrowLeft" circle size="small" @click="prevDay" />
        <h2 style="margin: 0; min-width: 180px; text-align: center;">{{ displayDate }}</h2>
        <el-button :icon="ArrowRight" circle size="small" @click="nextDay" />
        <el-date-picker
          v-model="selectedDate"
          value-format="YYYY-MM-DD"
          size="small"
          style="width: 140px;"
          @change="loadSchedule"
        />
        <el-button size="small" @click="goToToday">今天</el-button>
      </div>
      <el-button type="primary" :icon="Refresh" @click="generatePlan" :loading="loading">
        生成/刷新日程
      </el-button>
    </div>

    <!-- AI 建议 -->
    <el-alert
      v-if="plan?.aiSuggestion"
      :title="plan.aiSuggestion"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 20px;"
    />

    <!-- 积分预估 -->
    <div v-if="plan" style="margin-bottom: 16px; display: flex; gap: 16px;">
      <el-tag type="warning">预计积分: +{{ plan.estimatedPoints }}</el-tag>
      <el-tag :type="plan.status === 'CONFIRMED' ? 'success' : 'info'">
        {{ plan.status === 'CONFIRMED' ? '已确认' : '草稿' }}
      </el-tag>
    </div>

    <!-- 任务列表 -->
    <el-empty v-if="sortedTasks.length === 0" :description="emptyHint">
      <template v-if="!plan">
        <el-button type="primary" @click="generatePlan" :loading="loading">生成日程</el-button>
      </template>
    </el-empty>

    <template v-else>
      <el-alert
        v-if="hasOnlyCourses"
        title="提示"
        type="warning"
        :description="emptyHint"
        :closable="true"
        show-icon
        style="margin-bottom: 16px;"
      />

      <el-timeline>
      <el-timeline-item
        v-for="task in sortedTasks"
        :key="task.id"
        :color="getStatusColor(task.status)"
        :timestamp="task.scheduledStart?.substring(11, 16) + ' - ' + task.scheduledEnd?.substring(11, 16)"
      >
        <el-card shadow="hover" style="margin-bottom: 8px;">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <div>
              <span>{{ getTaskTypeIcon(task.taskType) }}</span>
              <strong style="margin-left: 4px;">{{ task.name }}</strong>
              <el-tag size="small" style="margin-left: 8px;" :color="getStatusColor(task.status)" effect="dark">
                {{ getStatusText(task.status) }}
              </el-tag>
            </div>
            <div v-if="task.status === 'PENDING'" style="display: flex; gap: 8px;">
              <el-button size="small" type="success" @click="updateTaskStatus(task.id, 'COMPLETED')">
                完成
              </el-button>
              <el-button size="small" type="warning" @click="updateTaskStatus(task.id, 'SKIPPED')">
                跳过
              </el-button>
              <el-button size="small" type="danger" @click="updateTaskStatus(task.id, 'POSTPONED')">
                推迟
              </el-button>
            </div>
          </div>
          <div style="margin-top: 8px; font-size: 0.85rem; color: #94a3b8;">
            ⏱ {{ task.durationMinutes }}分钟 · 精力: {{ task.energyLevel === 'HIGH' ? '🔴高' : task.energyLevel === 'MEDIUM' ? '🟡中' : '🟢低' }}
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>
    </template>
  </div>
</template>
