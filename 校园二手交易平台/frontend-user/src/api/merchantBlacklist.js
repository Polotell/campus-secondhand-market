import request from '@/utils/request'

export const listMerchantBlacklist = (params) =>
  request.get('/merchant/blacklist', { params })

export const addMerchantBlacklist = (data) =>
  request.post('/merchant/blacklist', data)

export const removeMerchantBlacklist = (id) =>
  request.delete(`/merchant/blacklist/${id}`)
