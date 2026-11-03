<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const goals = ref<any[]>([])
const dialogVisible = ref(false)
const form = ref({ name: '', description: '', category: 'STUDY', energyLevel: 'MEDIUM', priority: 5, defaultDuration: 60, weeklyFrequency: 3 })

async function loadGoals() {
  try {
    const { data } = await api.get('/goals')
    goals.value = data
  } catch (e) {
    ElMessage.error('加载目标失败')
  }
}

async function createGoal() {
  try {
    await api.post('/goals', form.value)
    ElMessage.success('目标已创建')
    dialogVisible.value = false
    loadGoals()
  } catch (e: any) {
    const data = e.response?.data
    let msg = '创建失败'
    if (data?.error) {
      msg = data.error
    } else if (data?.fieldErrors) {
      msg = Object.values(data.fieldErrors).join('; ')
    }
    ElMessage.error(msg)
  }
}

async function deleteGoal(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除此目标吗？', '确认', { type: 'warning' })
  } catch {
    return // user cancelled
  }
  try {
    await api.delete(`/goals/${id}`)
    ElMessage.success('已删除')
    loadGoals()
  } catch (e: any) {
    const data = e.response?.data
    ElMessage.error(data?.error || '删除失败')
  }
}

function getCategoryTag(cat: string) {
  const map: Record<string, string> = { STUDY: '', WORK: 'info', EXERCISE: 'success', HOBBY: 'warning', OTHER: '' }
  return map[cat] || ''
}

onMounted(loadGoals)
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
      <h2 style="margin: 0;">我的目标</h2>
      <el-button type="primary" @click="dialogVisible = true; form = { name: '', description: '', category: 'STUDY', energyLevel: 'MEDIUM', priority: 5, defaultDuration: 60, weeklyFrequency: 3 }">
        添加目标
      </el-button>
    </div>

    <el-empty v-if="goals.length === 0" description="还没有目标，添加一个开始吧" />

    <el-row :gutter="16" v-else>
      <el-col :span="8" v-for="goal in goals" :key="goal.id" style="margin-bottom: 16px;">
        <el-card shadow="hover">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <strong>{{ goal.name }}</strong>
              <el-tag :type="getCategoryTag(goal.category)">{{ goal.category }}</el-tag>
            </div>
          </template>
          <p style="color: #64748b;">{{ goal.description || '暂无描述' }}</p>
          <div style="display: flex; gap: 8px; flex-wrap: wrap;">
            <el-tag size="small">⏱ {{ goal.defaultDuration }}分钟</el-tag>
            <el-tag size="small">📊 精力: {{ goal.energyLevel }}</el-tag>
            <el-tag size="small">⭐ 优先级: {{ goal.priority }}</el-tag>
            <el-tag size="small">📅 每周 {{ goal.weeklyFrequency }} 次</el-tag>
          </div>
          <template #footer>
            <el-button type="danger" size="small" @click="deleteGoal(goal.id)">删除</el-button>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" title="添加新目标" width="500px">
      <el-form :model="form" label-position="top">
        <el-form-item label="目标名称" required>
          <el-input v-model="form.name" placeholder="如：英语学习" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" placeholder="详细描述" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="form.category">
                <el-option value="STUDY" label="学习" />
                <el-option value="WORK" label="工作" />
                <el-option value="EXERCISE" label="运动" />
                <el-option value="HOBBY" label="爱好" />
                <el-option value="OTHER" label="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所需精力">
              <el-select v-model="form.energyLevel">
                <el-option value="LOW" label="低" />
                <el-option value="MEDIUM" label="中" />
                <el-option value="HIGH" label="高" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="时长(分钟)">
              <el-input-number v-model="form.defaultDuration" :min="10" :max="480" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="优先级">
              <el-input-number v-model="form.priority" :min="1" :max="10" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="每周次数">
              <el-input-number v-model="form.weeklyFrequency" :min="1" :max="7" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createGoal">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>
