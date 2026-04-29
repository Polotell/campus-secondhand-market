import request from '@/utils/request'

export const previewOrder = (cartItemIds) =>
  request.post('/orders/preview', { cartItemIds })

export const createOrder = (data) =>
  request.post('/orders', data)

export const listMyOrders = (params) =>
  request.get('/orders', { params })

export const getOrderDetail = (id) =>
  request.get(`/orders/${id}`)

export const cancelOrder = (id) =>
  request.post(`/orders/${id}/cancel`)

export const confirmReceiveOrder = (id) =>
  request.post(`/orders/${id}/confirm-receive`)

// ===== 商家订单 =====

export const listMerchantOrders = (params) =>
  request.get('/merchant/orders', { params })

export const getMerchantOrderDetail = (id) =>
  request.get(`/merchant/orders/${id}`)

export const shipOrder = (id) =>
  request.post(`/merchant/orders/${id}/ship`)

// ===== 退货 =====

/** 买家：申请退货 { reason, images? } */
export const applyReturn = (orderId, data) =>
  request.post(`/orders/${orderId}/return-apply`, data)

/** 商家：同意退货（退款 + 还库存，事务） */
export const approveReturn = (orderId) =>
  request.post(`/merchant/orders/${orderId}/return-approve`)

/** 商家：拒绝退货 { remark } */
export const rejectReturn = (orderId, data) =>
  request.post(`/merchant/orders/${orderId}/return-reject`, data)

// ===== 评价（订单须 COMPLETED） =====

export const submitProductReviews = (orderId, data) =>
  request.post(`/orders/${orderId}/reviews/products`, data)

export const submitMerchantServiceReview = (orderId, data) =>
  request.post(`/orders/${orderId}/reviews/merchant-service`, data)

export const submitBuyerReview = (orderId, data) =>
  request.post(`/merchant/orders/${orderId}/reviews/buyer`, data)

