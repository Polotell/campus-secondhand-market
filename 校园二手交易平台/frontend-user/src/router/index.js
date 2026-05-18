import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

/**
 * meta 约定：
 * - anon:   true  → 仅未登录可访问（登录/注册页）
 * - public: true  → 任何人可访问（商品广场/详情）
 * - role:   'MERCHANT' → 额外限定角色
 * - 其它页面默认：必须登录
 */
const routes = [
  { path: '/', redirect: '/home' },

  // 无需登录 / 仅未登录
  {
    path: '/login', name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { anon: true, title: '登录' }
  },
  {
    path: '/register', name: 'RegisterUser',
    component: () => import('@/views/RegisterUser.vue'),
    meta: { anon: true, title: '用户注册' }
  },
  {
    path: '/register-merchant', name: 'RegisterMerchant',
    component: () => import('@/views/RegisterMerchant.vue'),
    meta: { anon: true, title: '商家入驻' }
  },

  // 主站（Layout 套壳）
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      {
        path: 'home', name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { public: true, title: '商品广场' }
      },
      {
        path: 'product/:id', name: 'ProductDetail',
        component: () => import('@/views/ProductDetail.vue'),
        meta: { public: true, title: '商品详情' }
      },
      {
        path: 'me', name: 'Me',
        component: () => import('@/views/Me.vue'),
        meta: { title: '我的账号' }
      },
      {
        path: 'cart', name: 'Cart',
        component: () => import('@/views/Cart.vue'),
        meta: { title: '购物车', role: 'USER' }
      },
      {
        path: 'checkout', name: 'Checkout',
        component: () => import('@/views/Checkout.vue'),
        meta: { title: '结算', role: 'USER' }
      },
      {
        path: 'orders', name: 'MyOrders',
        component: () => import('@/views/MyOrders.vue'),
        meta: { title: '我的订单', role: 'USER' }
      },
      {
        path: 'merchant/products', name: 'MyProducts',
        component: () => import('@/views/merchant/MyProducts.vue'),
        meta: { title: '我的商品', role: 'MERCHANT' }
      },
      {
        path: 'merchant/product/new', name: 'ProductEditor',
        component: () => import('@/views/merchant/ProductEditor.vue'),
        meta: { title: '发布商品', role: 'MERCHANT' }
      },
      {
        path: 'merchant/orders', name: 'MerchantOrders',
        component: () => import('@/views/merchant/MerchantOrders.vue'),
        meta: { title: '店铺订单', role: 'MERCHANT' }
      },
      {
        path: 'merchant/blacklist', name: 'MerchantBlacklist',
        component: () => import('@/views/merchant/MerchantBlacklist.vue'),
        meta: { title: '买家黑名单', role: 'MERCHANT' }
      }
    ]
  },

  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const isLoggedIn = !!userStore.token
  const isAnon   = to.meta.anon === true
  const isPublic = to.meta.public === true
  const needRole = to.meta.role

  document.title = to.meta.title
    ? `${to.meta.title} | 校园二手交易平台`
    : '校园二手交易平台'

  // 1. 仅游客页：如果本地有 token，先校验 token 是否仍有效。
  //    避免“token 已失效但仍被当作已登录”导致 /login、/register 被错误拦回 /home。
  if (isAnon && isLoggedIn) {
    if (!userStore.user) {
      try {
        await userStore.refreshMe()
        return next('/home')
      } catch {
        userStore.clearSession()
        return next()
      }
    }
    return next('/home')
  }

  // 未登录访问登录/注册页：必须在此放行。
  // 否则会落入下方「必须登录」分支，反复 next({ path:'/login', query:{ redirect } })，表现为路由卡住。
  if (isAnon) return next()

  // 2. public：任何人放行
  if (isPublic) return next()

  // 3. 非 public：必须登录
  if (!isLoggedIn) return next({ path: '/login', query: { redirect: to.fullPath } })

  // 4. 角色限定（MERCHANT 专属页）
  if (needRole && userStore.role !== needRole) {
    const roleLabel = needRole === 'MERCHANT'
      ? '商家'
      : needRole === 'USER'
        ? '普通用户'
        : needRole
    ElMessage.warning(`该页面仅 ${roleLabel} 可访问`)
    return next('/home')
  }

  next()
})

export default router
