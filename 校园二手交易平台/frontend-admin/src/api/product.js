import request from '@/utils/request'

/**
 * 管理员：商品列表（按状态 / 关键词）
 */
export const listProducts = (params) =>
  request.get('/admin/products', { params })

export const approveProduct = (id) =>
  request.post(`/admin/products/${id}/approve`)

export const rejectProduct = (id, reason) =>
  request.post(`/admin/products/${id}/reject`, { reason })

/** 公开商品详情（复用公共接口） */
export const getProductDetail = (id) =>
  request.get(`/products/${id}`)
