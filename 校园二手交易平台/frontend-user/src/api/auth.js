import request from '@/utils/request'

export const captcha = () => request.get('/captcha')

export const login = (data) => request.post('/auth/login', data)

export const registerUser = (data) => request.post('/auth/register/user', data)

export const registerMerchant = (data) => request.post('/auth/register/merchant', data)

export const me = () => request.get('/auth/me')

export const logout = () => request.post('/auth/logout')
