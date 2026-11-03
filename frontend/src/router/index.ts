import { createRouter, createWebHistory } from 'vue-router'

/** 解析 JWT 并检查是否过期（客户端先行判断，避免先渲染主页面再跳转） */
function isTokenExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.exp * 1000 < Date.now()
  } catch {
    return true // 解析失败视为过期
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/today',
      children: [
        {
          path: 'today',
          name: 'Today',
          component: () => import('@/views/Today.vue'),
          meta: { title: '今天' },
        },
        {
          path: 'taskbooks',
          name: 'TaskBooks',
          component: () => import('@/views/TaskBooks.vue'),
          meta: { title: '任务书' },
        },
        {
          path: 'taskbooks/:id',
          name: 'TaskBookDetail',
          component: () => import('@/views/TaskBookDetail.vue'),
          meta: { title: '任务书详情', menu: '/taskbooks' },
        },
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/Dashboard.vue'),
          meta: { title: '时间线' },
        },
        {
          path: 'goals',
          name: 'Goals',
          component: () => import('@/views/Goals.vue'),
          meta: { title: '目标管理' },
        },
        {
          path: 'courses',
          name: 'Courses',
          component: () => import('@/views/Courses.vue'),
          meta: { title: '课表管理' },
        },
        {
          path: 'ai-chat',
          name: 'AiChat',
          component: () => import('@/views/AiChat.vue'),
          meta: { title: 'AI 助手' },
        },
        {
          path: 'points',
          name: 'Points',
          component: () => import('@/views/Points.vue'),
          meta: { title: '我的积分' },
        },
      ],
    },
  ],
})

// 路由守卫：未登录或 token 过期跳转登录页
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const expired = token ? isTokenExpired(token) : false

  // 清除过期 token，避免后续代码误判为"已登录"
  if (expired) {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  if (to.path !== '/login' && (!token || expired)) {
    next('/login')
  } else if (to.path === '/login' && token && !expired) {
    next('/today')
  } else {
    next()
  }
})

export default router
