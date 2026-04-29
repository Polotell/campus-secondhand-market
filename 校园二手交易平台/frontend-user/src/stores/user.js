import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '@/api/auth'

const STORAGE_KEY = 'campus_market_user_v1'

function loadFromStorage() {
  try {
    const s = localStorage.getItem(STORAGE_KEY)
    return s ? JSON.parse(s) : { token: '', user: null }
  } catch { return { token: '', user: null } }
}

/**
 * 用户全局状态：
 *  - token：JWT（请求拦截器会自动带上）
 *  - user：当前登录用户 UserVO
 *  - 提供 login / logout / refreshMe
 */
export const useUserStore = defineStore('user', () => {
  const initial = loadFromStorage()
  const token = ref(initial.token || '')
  const user  = ref(initial.user || null)

  const isLoggedIn = computed(() => !!token.value)
  const role       = computed(() => user.value?.role || 'GUEST')

  function persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      token: token.value,
      user: user.value
    }))
  }

  async function login(payload) {
    const res = await authApi.login(payload)
    token.value = res.token
    user.value  = res.user
    persist()
    return res
  }

  async function refreshMe() {
    if (!token.value) return null
    const me = await authApi.me()
    user.value = me
    persist()
    return me
  }

  async function logout() {
    try { await authApi.logout() } catch { /* 忽略 */ }
    token.value = ''
    user.value  = null
    persist()
  }

  // token 失效（被拦截器调用）：清状态但不调接口
  function clearSession() {
    token.value = ''
    user.value  = null
    persist()
  }

  return { token, user, isLoggedIn, role, login, logout, refreshMe, clearSession }
})
