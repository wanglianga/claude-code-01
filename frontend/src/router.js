import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('./pages/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('./layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('./pages/Dashboard.vue'), meta: { title: '工作台' } },
      { path: 'applications', component: () => import('./pages/ApplicationList.vue'), meta: { title: '工单列表' } },
      { path: 'applications/new', component: () => import('./pages/NewApplication.vue'), meta: { title: '提交改造申请', roles: ['FAMILY'] } },
      { path: 'applications/:id', component: () => import('./pages/ApplicationDetail.vue'), meta: { title: '工单详情' } }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) {
    return '/login'
  }
  if (to.path === '/login' && token) {
    return '/dashboard'
  }
  if (to.meta.roles) {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    if (user && !to.meta.roles.includes(user.role)) {
      return '/dashboard'
    }
  }
  return true
})

export default router
