import request from '@/utils/request'

/**
 * 查询用户/商家列表
 * @param {Object} params
 * @param {'USER'|'MERCHANT'} [params.role]
 * @param {'PENDING'|'APPROVED'|'REJECTED'|'BANNED'} [params.status]
 * @param {string} [params.keyword]
 * @param {number} [params.pageNum=1]
 * @param {number} [params.pageSize=20]
 * @returns {Promise<{records:Array, total:number, pageNum:number, pageSize:number}>}
 */
export const listUsers = (params) => request.get('/admin/users', { params })

export const approveUser = (id) =>
  request.post(`/admin/users/${id}/approve`)

export const rejectUser = (id, reason) =>
  request.post(`/admin/users/${id}/reject`, { reason })

export const getAdminUser = (id) => request.get(`/admin/users/${id}`)

export const updateAdminUser = (id, data) => request.put(`/admin/users/${id}`, data)

export const deleteAdminUser = (id) => request.delete(`/admin/users/${id}`)

export const rechargeUser = (id, data) => request.post(`/admin/users/${id}/recharge`, data)

export const setMerchantLevel = (id, data) => request.post(`/admin/users/${id}/merchant-level`, data)

/** 轮播图（管理端） */
export const listCarouselsAdmin = (params) => request.get('/admin/carousels', { params })
export const createCarousel = (data) => request.post('/admin/carousels', data)
export const updateCarousel = (id, data) => request.put(`/admin/carousels/${id}`, data)
export const deleteCarousel = (id) => request.delete(`/admin/carousels/${id}`)

/** 全平台买家黑名单 */
export const listPlatformBlacklist = (params) => request.get('/admin/blacklist', { params })
export const addPlatformBlacklist = (data) => request.post('/admin/blacklist', data)
export const removePlatformBlacklist = (id) => request.delete(`/admin/blacklist/${id}`)
