import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '@/api/auth'

const STORAGE_KEY = 'campus_market_admin_v1'

function loadFromStorage() {
  try {
    const s = localStorage.getItem(STORAGE_KEY)
    return s ? JSON.parse(s) : { token: '', user: null }
  } catch { return { token: '', user: null } }
}

export const useUserStore = defineStore('admin-user', () => {
  const init = loadFromStorage()
  const token = ref(init.token || '')
  const user  = ref(init.user || null)

  const isLoggedIn = computed(() => !!token.value)

  function persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ token: token.value, user: user.value }))
  }

  async function login(payload) {
    const res = await authApi.login(payload)
    // 管理端只允许 ADMIN 登录
    if (res.user?.role !== 'ADMIN') {
      throw new Error('该账号不是管理员，无法登录后台')
    }
    token.value = res.token
    user.value  = res.user
    persist()
    return res
  }

  async function logout() {
    try { await authApi.logout() } catch {}
    token.value = ''
    user.value  = null
    persist()
  }

  function clearSession() {
    token.value = ''
    user.value  = null
    persist()
  }

  async function refreshMe() {
    if (!token.value) return null
    const me = await authApi.me()
    user.value = me
    persist()
    return me
  }

  return { token, user, isLoggedIn, login, logout, clearSession, refreshMe }
})
