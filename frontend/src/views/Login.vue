<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, ChatDotRound } from '@element-plus/icons-vue'

const router = useRouter()
const auth = useAuthStore()

const isLogin = ref(true)
const loading = ref(false)
const form = reactive({
  username: 'demo',
  password: 'demo123',
  nickname: '',
})

async function handleSubmit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    if (isLogin.value) {
      await auth.login(form.username, form.password)
    } else {
      await auth.register(form.username, form.password, form.nickname || form.username)
    }
    ElMessage.success(isLogin.value ? '欢迎回来' : '注册成功')
    router.push('/today')
  } catch (e: any) {
    const msg = e.response?.data?.error || e.message || '操作失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

function toggleMode() {
  isLogin.value = !isLogin.value
}
</script>

<template>
  <div class="login-page">
    <!-- 居中的毛玻璃卡片 -->
    <div class="login-card">
      <div class="card-header">
        <div class="brand-icon">📅</div>
        <h1>SmartSchedule</h1>
        <p>AI 驱动的智能日程规划助手</p>
      </div>

      <h2 class="form-title">{{ isLogin ? '欢迎回来' : '创建账户' }}</h2>
      <p class="form-subtitle">{{ isLogin ? '登录以继续管理你的日程' : '开始你的智能日程管理之旅' }}</p>

      <el-form @submit.prevent="handleSubmit" label-position="top" class="login-form">
        <el-form-item label="用户名">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>

        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>

        <el-form-item v-if="!isLogin" label="昵称">
          <el-input
            v-model="form.nickname"
            placeholder="给自己起个名字（可选）"
            size="large"
            :prefix-icon="ChatDotRound"
          />
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          class="submit-btn"
          :loading="loading"
          native-type="submit"
        >
          {{ isLogin ? '登  录' : '注  册' }}
        </el-button>

        <div class="toggle-row">
          <span>{{ isLogin ? '还没有账户？' : '已有账户？' }}</span>
          <el-button link type="primary" @click="toggleMode">
            {{ isLogin ? '立即注册' : '去登录' }}
          </el-button>
        </div>
      </el-form>

      <div class="demo-hint">演示账户：demo / demo123</div>
    </div>
  </div>
</template>

<style scoped>
/* ===== 全屏渐变浅色背景 ===== */
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f0fe 0%, #f0e6ff 25%, #fce4ec 50%, #e8f5e9 75%, #e3f2fd 100%);
  background-size: 400% 400%;
  animation: gradientShift 20s ease infinite;
  padding: 24px;
  position: relative;
  overflow: hidden;
}

/* 背景动画光斑 */
.login-page::before,
.login-page::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.35;
  z-index: 0;
}
.login-page::before {
  width: 400px;
  height: 400px;
  background: #93c5fd;
  top: -100px;
  right: -100px;
  animation: float1 12s ease-in-out infinite;
}
.login-page::after {
  width: 350px;
  height: 350px;
  background: #c4b5fd;
  bottom: -80px;
  left: -80px;
  animation: float2 15s ease-in-out infinite;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}
@keyframes float1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-30px, 40px) scale(1.15); }
}
@keyframes float2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(30px, -30px) scale(1.1); }
}

/* ===== 毛玻璃卡片 ===== */
.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(20px) saturate(160%);
  -webkit-backdrop-filter: blur(20px) saturate(160%);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 20px;
  padding: 40px 36px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.06),
    0 2px 8px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: transform 0.2s, box-shadow 0.2s;
}
.login-card:hover {
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.08),
    0 4px 12px rgba(0, 0, 0, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

/* ===== 卡片头部 ===== */
.card-header {
  text-align: center;
  margin-bottom: 28px;
}
.brand-icon {
  font-size: 48px;
  margin-bottom: 8px;
}
.card-header h1 {
  margin: 0;
  font-size: 1.6rem;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: -0.02em;
}
.card-header p {
  margin: 6px 0 0;
  font-size: 0.85rem;
  color: #64748b;
}

/* ===== 表单标题 ===== */
.form-title {
  margin: 0 0 4px;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e293b;
  text-align: center;
}
.form-subtitle {
  margin: 0 0 24px;
  font-size: 0.85rem;
  color: #94a3b8;
  text-align: center;
}

/* ===== 表单 ===== */
.login-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #475569;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  height: 44px;
  font-size: 1rem;
  font-weight: 600;
  letter-spacing: 0.05em;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  border: none;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.3);
  transition: all 0.2s;
}
.submit-btn:hover {
  background: linear-gradient(135deg, #2563eb, #4f46e5);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
  transform: translateY(-1px);
}

.toggle-row {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  margin-top: 16px;
  font-size: 0.85rem;
  color: #64748b;
}

.demo-hint {
  margin-top: 20px;
  text-align: center;
  font-size: 0.8rem;
  color: #94a3b8;
  padding: 10px;
  background: rgba(148, 163, 184, 0.1);
  border-radius: 8px;
}

/* ===== 深色主题覆盖 ===== */
@media (prefers-color-scheme: dark) {
  .login-page {
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  }
  .login-card {
    background: rgba(30, 41, 59, 0.65);
    border-color: rgba(255, 255, 255, 0.08);
    box-shadow:
      0 8px 32px rgba(0, 0, 0, 0.3),
      0 2px 8px rgba(0, 0, 0, 0.2),
      inset 0 1px 0 rgba(255, 255, 255, 0.05);
  }
  .card-header h1,
  .form-title {
    color: #e2e8f0;
  }
  .card-header p,
  .form-subtitle,
  .toggle-row,
  .demo-hint {
    color: #94a3b8;
  }
  .demo-hint {
    background: rgba(148, 163, 184, 0.08);
  }
}
</style>
