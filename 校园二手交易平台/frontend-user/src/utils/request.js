import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

/**
 * 全局 Axios 封装
 * - baseURL 取自 Vite 环境变量 VITE_API_BASE（默认 /api）
 * - 请求拦截：自动注入 Authorization: Bearer <token>
 * - 响应拦截：
 *     1) 后端 200 + Result{code:0}  → 直接返回 data
 *     2) 后端 200 + Result{code!=0} → 弹 ElMessage，抛业务错误
 *     3) HTTP 401 → 清本地会话 + 跳登录页
 *     4) 其他 HTTP 异常 → 弹错误消息
 */
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000
})

request.interceptors.request.use((cfg) => {
  // 为了避免 Pinia 循环依赖，这里直接读 localStorage
  try {
    const raw = localStorage.getItem('campus_market_user_v1')
    if (raw) {
      const { token } = JSON.parse(raw)
      if (token) cfg.headers.Authorization = `Bearer ${token}`
    }
  } catch { /* ignore */ }
  return cfg
})

request.interceptors.response.use(
  (resp) => {
    const body = resp.data
    // 后端统一 Result<T>：{ code, message, data }
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      ElMessage.error(body.message || '操作失败')
      return Promise.reject(Object.assign(new Error(body.message || '业务错误'), {
        code: body.code,
        data: body.data
      }))
    }
    return body
  },
  (err) => {
    const status = err.response?.status
    const body   = err.response?.data
    if (status === 401) {
      // 清本地会话，跳登录
      try { localStorage.removeItem('campus_market_user_v1') } catch { /* ignore */ }
      ElMessage.warning('登录已过期，请重新登录')
      if (router.currentRoute.value.name !== 'Login') {
        router.replace({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
      }
      return Promise.reject(err)
    }
    // 后端错误对象也可能带 Result 结构
    const msg = body?.message || err.message || `请求失败 (${status || 'Network'})`
    // 可选：调用方传 { silent: true } 时不弹全局 Toast（如首页轮播缺失时降级为空）
    if (!err.config?.silent) ElMessage.error(msg)
    return Promise.reject(err)
  }
)

export default request
