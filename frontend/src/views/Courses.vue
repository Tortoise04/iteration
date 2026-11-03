<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Delete } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

// ===== 课程配色 =====
const COURSE_COLORS = [
  { bg: '#eff6ff', border: '#93c5fd', text: '#1e40af' }, // blue
  { bg: '#fff7ed', border: '#fdba74', text: '#9a3412' }, // orange
  { bg: '#f0fdf4', border: '#86efac', text: '#166534' }, // green
  { bg: '#fdf2f8', border: '#f9a8d4', text: '#9d174d' }, // pink
  { bg: '#faf5ff', border: '#d8b4fe', text: '#6b21a8' }, // purple
  { bg: '#ecfeff', border: '#67e8f9', text: '#155e75' }, // cyan
  { bg: '#fefce8', border: '#fde047', text: '#854d0e' }, // yellow
  { bg: '#fff1f2', border: '#fda4af', text: '#9f1239' }, // rose
]

function getCourseColor(name: string) {
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return COURSE_COLORS[Math.abs(hash) % COURSE_COLORS.length]
}

// ===== 时间表网格 =====
const START_HOUR = 7
const END_HOUR = 22
const SLOT_MINUTES = 30
const ROW_HEIGHT = 28 // 每半小时的像素高度

const timeSlots: string[] = []
for (let h = START_HOUR; h < END_HOUR; h++) {
  timeSlots.push(`${String(h).padStart(2, '0')}:00`)
  timeSlots.push(`${String(h).padStart(2, '0')}:30`)
}
timeSlots.push(`${String(END_HOUR).padStart(2, '0')}:00`)

function timeToMinutes(t: string | Date | null | undefined): number {
  if (!t) return START_HOUR * 60
  const s = fmtTime(t)
  const [hh, mm] = s.split(':').map(Number)
  return hh * 60 + mm
}

const courses = ref<any[]>([])
const dialogVisible = ref(false)
const ocrVisible = ref(false)
const ocrLoading = ref(false)
const ocrImage = ref('')
const ocrResults = ref<any[]>([])
const ocrRawText = ref('')
const fileInputRef = ref<HTMLInputElement>()

function makeTime(hh: number, mm: number): Date {
  return dayjs().hour(hh).minute(mm).second(0).millisecond(0).toDate()
}

function fmtTime(d: Date | string | null | undefined): string {
  if (!d) return '08:00'
  if (typeof d === 'string') {
    const m = d.match(/^(\d{1,2}):(\d{2})/)
    if (m) return m[1].padStart(2, '0') + ':' + m[2]
    const parsed = dayjs(d)
    if (parsed.isValid()) return parsed.format('HH:mm')
    return '08:00'
  }
  if (d instanceof Date) return dayjs(d).format('HH:mm')
  return '08:00'
}

function toDate(val: any): Date {
  if (val instanceof Date) return val
  if (typeof val === 'string') {
    const m = val.match(/^(\d{1,2}):(\d{2})/)
    if (m) return makeTime(+m[1], +m[2])
    const d = dayjs(val)
    if (d.isValid()) return d.toDate()
  }
  return makeTime(8, 0)
}

function defaultForm() {
  return {
    name: '',
    dayOfWeek: 1,
    startTime: makeTime(8, 0),
    endTime: makeTime(9, 30),
    location: '',
  }
}
const form = ref(defaultForm())

const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const weekDaysShort = ['一', '二', '三', '四', '五', '六', '日']

function getDayCourses(day: number) {
  return courses.value.filter((c) => c.dayOfWeek === day)
}

function courseCardStyle(course: any) {
  const startMin = timeToMinutes(course.startTime)
  const endMin = timeToMinutes(course.endTime)
  const top = ((startMin - START_HOUR * 60) / SLOT_MINUTES) * ROW_HEIGHT
  const height = Math.max(((endMin - startMin) / SLOT_MINUTES) * ROW_HEIGHT, ROW_HEIGHT * 0.8)
  const color = getCourseColor(course.name)
  return {
    top: `${top}px`,
    height: `${height}px`,
    backgroundColor: color.bg,
    borderLeftColor: color.border,
    color: color.text,
  }
}

// ===== 数据加载 =====
async function loadCourses() {
  try {
    const { data } = await api.get('/courses')
    courses.value = data
  } catch (e: any) {
    ElMessage.error(e.response?.data?.error || '加载课程失败')
  }
}

// ===== 添加课程 =====
function openAddDialog(day?: number, time?: string) {
  const f = defaultForm()
  if (day) f.dayOfWeek = day
  if (time) {
    const [hh, mm] = time.split(':').map(Number)
    f.startTime = makeTime(hh, mm)
    f.endTime = makeTime(hh, mm + 60 > 23 ? 23 : mm + 60)
  }
  form.value = f
  dialogVisible.value = true
}

async function createCourse() {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入课程名称')
    return
  }
  try {
    await api.post('/courses', {
      name: form.value.name.trim(),
      dayOfWeek: form.value.dayOfWeek,
      startTime: fmtTime(form.value.startTime),
      endTime: fmtTime(form.value.endTime),
      location: form.value.location || '',
    })
    ElMessage.success('课程已添加')
    dialogVisible.value = false
    loadCourses()
  } catch (e: any) {
    const data = e.response?.data
    let msg = '添加失败'
    if (data?.error) {
      msg = data.error
    } else if (data?.fieldErrors) {
      msg = Object.values(data.fieldErrors).join('; ')
    }
    ElMessage.error(msg)
  }
}

// ===== 删除课程 =====
async function deleteCourse(id: number) {
  try {
    await api.delete(`/courses/${id}`)
    ElMessage.success('已删除')
    loadCourses()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.error || '删除失败')
  }
}

function confirmDelete(course: any) {
  ElMessageBox.confirm(
    `${course.name}（${fmtTime(course.startTime)}-${fmtTime(course.endTime)}）`,
    '删除课程',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' },
  ).then(() => deleteCourse(course.id)).catch(() => {})
}

// ===== OCR 相关 =====
function openOcr() {
  ocrVisible.value = true
  ocrImage.value = ''
  ocrResults.value = []
  ocrRawText.value = ''
}

function triggerFileInput() {
  fileInputRef.value?.click()
}

function onFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = (ev) => {
    ocrImage.value = ev.target?.result as string
  }
  reader.readAsDataURL(file)
}

async function startOcr() {
  if (!ocrImage.value) {
    ElMessage.warning('请先选择课表图片')
    return
  }

  ocrLoading.value = true
  try {
    const resp = await fetch(ocrImage.value)
    const blob = await resp.blob()

    const formData = new FormData()
    formData.append('image', blob, 'schedule.jpg')

    const { data } = await api.post('/ocr/parse-course-table', formData)

    ocrResults.value = (data.courses || []).map((c: any) => ({
      ...c,
      startTime: toDate(c.startTime),
      endTime: toDate(c.endTime),
      location: c.location || '',
    }))
    ocrRawText.value = data.rawText || ''

    if (ocrResults.value.length === 0) {
      ElMessage.warning('未能识别到课程，请检查图片清晰度或手动添加')
    } else {
      ElMessage.success(`识别到 ${ocrResults.value.length} 门课程`)
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.error || '识别失败')
  } finally {
    ocrLoading.value = false
  }
}

async function batchSaveCourses() {
  if (ocrResults.value.length === 0) return

  let saved = 0
  for (const course of ocrResults.value) {
    try {
      await api.post('/courses', {
        name: course.name,
        dayOfWeek: course.dayOfWeek,
        startTime: fmtTime(course.startTime),
        endTime: fmtTime(course.endTime),
        location: course.location || '',
      })
      saved++
    } catch (e) {
      // 继续保存
    }
  }

  ElMessage.success(`已保存 ${saved} 门课程`)
  ocrVisible.value = false
  loadCourses()
}

function removeOcrCourse(index: number) {
  ocrResults.value.splice(index, 1)
}

onMounted(loadCourses)
</script>

<template>
  <div>
    <div class="page-header">
      <h2>课表管理</h2>
      <div class="header-actions">
        <el-button type="success" :icon="Picture" @click="openOcr">
          拍照识别课表
        </el-button>
        <el-button type="primary" @click="openAddDialog()">手动添加课程</el-button>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty v-if="courses.length === 0" description="还没有课程，点击上方按钮添加" />

    <!-- 周视图时间表 -->
    <div v-else class="timetable-wrapper">
      <div class="timetable-grid">
        <!-- 表头 -->
        <div class="time-gutter-header"></div>
        <div v-for="(d, i) in weekDays" :key="'dh'+i" class="day-header">
          {{ d }}
        </div>

        <!-- 时间刻度列 -->
        <div v-for="(t, ti) in timeSlots" :key="'tl'+ti" class="time-label" :style="{ gridRow: ti + 2 }">
          {{ t }}
        </div>

        <!-- 日列 -->
        <div v-for="day in 7" :key="'day'+day" class="day-column" :style="{ gridColumn: day + 1, gridRow: '2 / ' + (timeSlots.length + 2) }">
          <!-- 网格线 -->
          <div
            v-for="(t, ti) in timeSlots"
            :key="'cell'+ti"
            class="slot-cell"
            :class="{ 'slot-hour': t.endsWith(':00') }"
            @click="openAddDialog(day, t)"
          ></div>

          <!-- 课程卡片 -->
          <div
            v-for="course in getDayCourses(day)"
            :key="course.id"
            class="course-card"
            :style="courseCardStyle(course)"
            @click="confirmDelete(course)"
          >
            <div class="card-name">{{ course.name }}</div>
            <div class="card-time">{{ fmtTime(course.startTime) }}-{{ fmtTime(course.endTime) }}</div>
            <div v-if="course.location" class="card-location">{{ course.location }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 手动添加课程弹窗 -->
    <el-dialog v-model="dialogVisible" title="添加课程" width="450px">
      <el-form :model="form" label-position="top">
        <el-form-item label="课程名称" required>
          <el-input v-model="form.name" placeholder="如：高等数学" />
        </el-form-item>
        <el-form-item label="星期">
          <el-select v-model="form.dayOfWeek" style="width: 100%">
            <el-option v-for="(d, i) in weekDays" :key="i + 1" :value="i + 1" :label="d" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始时间">
              <el-time-picker v-model="form.startTime" format="HH:mm" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间">
              <el-time-picker v-model="form.endTime" format="HH:mm" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="地点">
          <el-input v-model="form.location" placeholder="如：教学楼A201" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createCourse">添加</el-button>
      </template>
    </el-dialog>

    <!-- OCR 拍照识别课表弹窗 -->
    <el-dialog v-model="ocrVisible" title="拍照识别课表" width="650px" :close-on-click-modal="false">
      <input
        ref="fileInputRef"
        type="file"
        accept="image/*"
        style="display: none"
        @change="onFileSelected"
      />

      <div style="margin-bottom: 20px;">
        <div v-if="!ocrImage" class="ocr-upload-zone" @click="triggerFileInput">
          <el-icon :size="40" color="#94a3b8"><Picture /></el-icon>
          <p class="ocr-hint">点击上传课表截图或照片</p>
          <p class="ocr-sub-hint">支持 JPG / PNG / WebP，建议清晰拍摄</p>
        </div>

        <div v-else style="text-align: center;">
          <img :src="ocrImage" class="ocr-preview-img" />
          <div style="margin-top: 12px;">
            <el-button size="small" @click="ocrImage = ''; ocrResults = []">重新选择</el-button>
            <el-button
              v-if="ocrResults.length === 0"
              type="primary"
              size="small"
              :loading="ocrLoading"
              @click="startOcr"
            >
              开始识别
            </el-button>
          </div>
        </div>
      </div>

      <div v-if="ocrResults.length > 0">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
          <span class="ocr-result-title">
            识别结果（{{ ocrResults.length }} 门课程）
          </span>
          <el-button type="primary" size="small" @click="batchSaveCourses">
            一键保存全部
          </el-button>
        </div>

        <el-table :data="ocrResults" size="small" max-height="280">
          <el-table-column prop="name" label="课程名称" min-width="100">
            <template #default="{ row }">
              <el-input v-model="row.name" size="small" />
            </template>
          </el-table-column>
          <el-table-column prop="dayOfWeek" label="星期" width="100">
            <template #default="{ row }">
              <el-select v-model="row.dayOfWeek" size="small" style="width: 80px">
                <el-option v-for="(d, i) in weekDays" :key="i + 1" :value="i + 1" :label="d" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="时间" min-width="140">
            <template #default="{ row }">
              <div style="display: flex; gap: 4px; align-items: center;">
                <el-time-picker v-model="row.startTime" format="HH:mm" size="small" style="width: 90px" />
                <span>-</span>
                <el-time-picker v-model="row.endTime" format="HH:mm" size="small" style="width: 90px" />
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="location" label="地点" min-width="100">
            <template #default="{ row }">
              <el-input v-model="row.location" size="small" />
            </template>
          </el-table-column>
          <el-table-column width="50">
            <template #default="{ $index }">
              <el-button type="danger" size="small" :icon="Delete" circle text @click="removeOcrCourse($index)" />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <template #footer>
        <el-button @click="ocrVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
}

/* ===== 时间表网格 ===== */
.timetable-wrapper {
  overflow-x: auto;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
}

.timetable-grid {
  display: grid;
  grid-template-columns: 56px repeat(7, 1fr);
  min-width: 750px;
}

/* 表头 */
.day-header {
  text-align: center;
  font-weight: 600;
  font-size: .9rem;
  color: #475569;
  padding: 10px 4px;
  background: #f8fafc;
  border-bottom: 2px solid #e2e8f0;
  border-left: 1px solid #e2e8f0;
  position: sticky;
  top: 0;
  z-index: 2;
}

.time-gutter-header {
  background: #f8fafc;
  border-bottom: 2px solid #e2e8f0;
  position: sticky;
  top: 0;
  z-index: 2;
}

/* 时间刻度 */
.time-label {
  font-size: .7rem;
  color: #94a3b8;
  text-align: right;
  padding: 0 6px 0 0;
  line-height: 28px;
  height: 28px;
  border-right: 1px solid #e2e8f0;
  user-select: none;
}

/* 日列 */
.day-column {
  position: relative;
  border-left: 1px solid #e2e8f0;
}

/* 网格线 */
.slot-cell {
  height: 28px;
  cursor: pointer;
  transition: background .15s;
}

.slot-cell:hover {
  background: #f1f5f9;
}

.slot-cell.slot-hour {
  border-top: 1px solid #e2e8f0;
}

/* 课程卡片 */
.course-card {
  position: absolute;
  left: 2px;
  right: 2px;
  border-radius: 6px;
  border-left: 3px solid;
  padding: 3px 6px;
  cursor: pointer;
  overflow: hidden;
  transition: transform .15s, box-shadow .15s;
  z-index: 1;
  box-shadow: 0 1px 2px rgba(0,0,0,.05);
}

.course-card:hover {
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(0,0,0,.1);
  z-index: 3;
}

.card-name {
  font-size: .78rem;
  font-weight: 600;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-time {
  font-size: .65rem;
  opacity: .75;
}

.card-location {
  font-size: .65rem;
  opacity: .7;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== OCR 区域 ===== */
.ocr-upload-zone {
  border: 2px dashed #cbd5e1;
  border-radius: 14px;
  padding: 48px;
  text-align: center;
  cursor: pointer;
  transition: all .2s;
  background: rgba(148,163,184,.04);
}

.ocr-upload-zone:hover {
  border-color: #94a3b8;
  background: rgba(148,163,184,.08);
}

.ocr-hint {
  color: #64748b;
  margin-top: 12px;
  font-size: .95rem;
}

.ocr-sub-hint {
  color: #94a3b8;
  font-size: .8rem;
  margin-top: 4px;
}

.ocr-preview-img {
  max-width: 100%;
  max-height: 280px;
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(0,0,0,.08);
}

.ocr-result-title {
  font-weight: 520;
  color: #1e293b;
}
</style>
