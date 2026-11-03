<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '@/api'
import { TrophyBase } from '@element-plus/icons-vue'

const points = ref({ totalPoints: 0, level: 1, levelName: '' })
const history = ref<any[]>([])

onMounted(async () => {
  const [p, h] = await Promise.all([
    api.get('/points'),
    api.get('/points/history'),
  ])
  points.value = p.data
  history.value = h.data
})

function getLevelPercent(level: number) {
  return Math.min(((points.value.totalPoints % 200) / 200) * 100, 100)
}
</script>

<template>
  <div style="max-width: 600px; margin: 0 auto;">
    <h2 style="margin-bottom: 20px;">我的积分</h2>

    <el-card style="margin-bottom: 24px; text-align: center;">
      <div style="font-size: 3rem; margin-bottom: 8px;">🏆</div>
      <h1 style="margin: 0; font-size: 2.5rem; color: #2563eb;">{{ points.totalPoints }}</h1>
      <p style="color: #64748b;">累计积分</p>
      <el-tag size="large" type="warning" style="margin-top: 8px;">
        Lv.{{ points.level }} {{ points.levelName }}
      </el-tag>
      <div style="margin-top: 16px;">
        <el-progress
          :percentage="getLevelPercent(points.level)"
          :text-inside="true"
          :stroke-width="20"
          status="success"
        >
          <span>{{ points.totalPoints % 200 }} / 200</span>
        </el-progress>
      </div>
    </el-card>

    <h3 style="margin-bottom: 12px;">积分历史</h3>
    <el-empty v-if="history.length === 0" description="暂无积分记录" />
    <el-timeline v-else>
      <el-timeline-item
        v-for="r in history"
        :key="r.id"
        :timestamp="r.createdAt"
        :color="r.points > 0 ? '#22c55e' : '#ef4444'"
      >
        {{ r.reason }} <el-tag size="small">{{ r.points > 0 ? '+' + r.points : r.points }}</el-tag>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>
