import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const routes = [
  { path: '/', redirect: '/dashboard' },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { anon: true, title: '管理员登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'audit/users',
        name: 'UserAudit',
        component: () => import('@/views/UserAudit.vue'),
        meta: { title: '用户审核', role: 'USER' }
      },
      {
        path: 'audit/merchants',
        name: 'MerchantAudit',
        component: () => import('@/views/UserAudit.vue'),
        meta: { title: '商家审核', role: 'MERCHANT' }
      },
      {
        path: 'audit/products',
        name: 'ProductAudit',
        component: () => import('@/views/ProductAudit.vue'),
        meta: { title: '商品审核' }
      },
      {
        path: 'users/manage',
        name: 'UserManage',
        component: () => import('@/views/UserManage.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'content/carousels',
        name: 'CarouselManage',
        component: () => import('@/views/CarouselManage.vue'),
        meta: { title: '首页轮播' }
      },
      {
        path: 'risk/blacklist',
        name: 'BlacklistManage',
        component: () => import('@/views/BlacklistManage.vue'),
        meta: { title: '平台黑名单' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const isLoggedIn = !!userStore.token
  const isAnon = to.meta.anon === true

  document.title = to.meta.title
    ? `${to.meta.title} | 管理后台`
    : '校园二手交易平台 · 管理后台'

  if (!isAnon && !isLoggedIn) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }
  // 已登录：强制要求 ADMIN 角色（普通用户/商家即使持有 token 也不能进后台）
  if (isLoggedIn && userStore.user?.role !== 'ADMIN') {
    ElMessage.error('您不是管理员，无权访问后台')
    userStore.clearSession()
    return next('/login')
  }
  if (isAnon && isLoggedIn) return next('/dashboard')
  next()
})

export default router
