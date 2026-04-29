import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const STORAGE_KEY = 'campus_market_admin_v1'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000
})

request.interceptors.request.use((cfg) => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const { token } = JSON.parse(raw)
      if (token) cfg.headers.Authorization = `Bearer ${token}`
    }
  } catch {}
  return cfg
})

request.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      ElMessage.error(body.message || '操作失败')
      return Promise.reject(Object.assign(new Error(body.message || '业务错误'), {
        code: body.code, data: body.data
      }))
    }
    return body
  },
  (err) => {
    const status = err.response?.status
    const body   = err.response?.data
    if (status === 401) {
      try { localStorage.removeItem(STORAGE_KEY) } catch {}
      ElMessage.warning('登录已过期，请重新登录')
      if (router.currentRoute.value.name !== 'Login') {
        router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
      }
      return Promise.reject(err)
    }
    const msg = body?.message || err.message || `请求失败 (${status || 'Network'})`
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

export default request
