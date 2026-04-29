import request from '@/utils/request'

/**
 * 公开商品列表
 * @param {Object} p
 * @param {number} [p.categoryId]
 * @param {string} [p.keyword]
 * @param {'latest'|'price_asc'|'price_desc'|'sales_desc'|'rating_desc'} [p.sort]
 * @param {number} [p.pageNum=1]
 * @param {number} [p.pageSize=20]
 */
export const listProducts = (p) => request.get('/products', { params: p })

export const getProductDetail = (id) => request.get(`/products/${id}`)

export const listProductReviews = (productId, params) =>
  request.get(`/products/${productId}/reviews`, { params })

// ================= 商家端 =================

export const listMyProducts = (p) =>
  request.get('/merchant/products', { params: p })

export const createProduct = (dto) =>
  request.post('/merchant/products', dto)

export const offShelfProduct = (id) =>
  request.post(`/merchant/products/${id}/off-shelf`)

export const onShelfProduct = (id) =>
  request.post(`/merchant/products/${id}/on-shelf`)
