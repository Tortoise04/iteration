<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ref, computed } from 'vue'
import {
  Sunny, Notebook, Timer, Aim, Calendar, ChatDotRound, TrophyBase, SwitchButton
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

// 菜单顺序：今天 / 任务书 / 时间线 / 目标 / 课表 / AI 助手 / 积分
const menuItems = [
  { path: '/today', title: '今天', icon: Sunny },
  { path: '/taskbooks', title: '任务书', icon: Notebook },
  { path: '/dashboard', title: '时间线', icon: Timer },
  { path: '/goals', title: '目标管理', icon: Aim },
  { path: '/courses', title: '课表管理', icon: Calendar },
  { path: '/ai-chat', title: 'AI 助手', icon: ChatDotRound },
  { path: '/points', title: '我的积分', icon: TrophyBase },
]

// 详情页（/taskbooks/:id）高亮「任务书」这一项
const activeMenu = computed(() => (route.meta.menu as string) || route.path)

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app-shell">
    <!-- ===== 左侧毛玻璃导航 ===== -->
    <aside class="sidebar">
      <div class="sidebar-brand">
        <span class="brand-emoji">📅</span>
        <span class="brand-text">SmartSchedule</span>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: activeMenu === item.path }"
        >
          <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
          <span class="nav-label">{{ item.title }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="user-chip">
          <span class="user-avatar">👋</span>
          <span class="user-name">{{ auth.user?.nickname || auth.user?.username }}</span>
        </div>
        <el-button
          text
          class="logout-btn"
          @click="handleLogout"
        >
          <el-icon><SwitchButton /></el-icon>
          退出
        </el-button>
      </div>
    </aside>

    <!-- ===== 右侧内容区 ===== -->
    <div class="main-area">
      <header class="topbar">
        <h2 class="page-title">{{ route.meta.title }}</h2>
      </header>
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
/* ===== 整体布局 ===== */
.app-shell {
  display: flex;
  min-height: 100vh;
  background: linear-gradient(160deg, #eef2ff 0%, #f5f3ff 30%, #fdf2f8 60%, #ecfdf5 100%);
}

/* ===== 左侧毛玻璃 ===== */
.sidebar {
  width: 230px;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(24px) saturate(160%);
  -webkit-backdrop-filter: blur(24px) saturate(160%);
  border-right: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 2px 0 20px rgba(0, 0, 0, 0.04);
  padding: 0;
  position: sticky;
  top: 0;
  height: 100vh;
  z-index: 10;
}

/* 品牌区 */
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 24px 20px 20px;
}
.brand-emoji {
  font-size: 26px;
  filter: drop-shadow(0 2px 4px rgba(0,0,0,.08));
}
.brand-text {
  font-size: 1.1rem;
  font-weight: 550;
  color: #1e293b;
  letter-spacing: -0.02em;
}

/* 导航 */
.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 12px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  border-radius: 12px;
  text-decoration: none;
  color: #475569;
  font-size: 0.92rem;
  font-weight: 430;
  transition: all 0.18s;
}
.nav-item:hover {
  background: rgba(99, 102, 241, 0.08);
  color: #4f46e5;
}
.nav-item.active {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.12), rgba(139, 92, 246, 0.1));
  color: #4f46e5;
  font-weight: 520;
  box-shadow: inset 0 0 0 1px rgba(99, 102, 241, 0.15);
}
.nav-icon {
  font-size: 18px;
  flex-shrink: 0;
}
.nav-label {
  white-space: nowrap;
}

/* 底部用户区 */
.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(226, 232, 240, 0.5);
}
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 10px;
  background: rgba(148, 163, 184, 0.08);
  margin-bottom: 10px;
}
.user-avatar { font-size: 16px; }
.user-name {
  font-size: 0.85rem;
  color: #475569;
  font-weight: 450;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.logout-btn {
  width: 100%;
  justify-content: center;
  color: #94a3b8;
  font-size: 0.82rem;
  font-weight: 430;
  padding: 8px;
  border-radius: 10px;
  transition: all 0.15s;
}
.logout-btn:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.06);
}

/* ===== 右侧主内容 ===== */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  padding: 20px 32px 0;
  background: transparent;
}
.page-title {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 520;
  color: #1e293b;
  letter-spacing: -0.01em;
}

.content {
  flex: 1;
  padding: 20px 32px 40px;
  overflow-y: auto;
}
</style>
