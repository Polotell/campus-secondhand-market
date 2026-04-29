import request from '@/utils/request'

export const addCartItem = (data) => request.post('/cart', data)

export const getCart = () => request.get('/cart')

export const updateCartItem = (id, data) =>
  request.put(`/cart/${id}`, data)

export const removeCartItem = (id) =>
  request.delete(`/cart/${id}`)

export const selectAllCartItems = (selected) =>
  request.post('/cart/select-all', null, { params: { selected } })

export const clearSelectedCartItems = () =>
  request.post('/cart/clear-selected')
